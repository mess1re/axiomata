package me.mss1r.axiomata.blueprint.tracing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import me.mss1r.axiomata.blueprint.network.S2COutlineIndexPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
//? if forge {
/*import net.minecraftforge.event.OnDatapackSyncEvent;
*///?} else {
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
//?}
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class OutlineCatalog extends SimplePreparableReloadListener<Map<String, BlueprintOutline>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String OUTLINE_DIRECTORY = "blueprint_outlines";
    private static final String OUTLINE_PREFIX = OUTLINE_DIRECTORY + "/";

    private static Map<String, BlueprintOutline> outlines = Map.of();
    private static final Map<String, BlueprintOutline> received = new ConcurrentHashMap<>();
    private static Set<String> drawable = Set.of();

    public static void syncToClients(OnDatapackSyncEvent event) {
        // Send only the index here. Masks are comparatively large and are sent on selection, one
        // blueprint at a time.
        S2COutlineIndexPacket packet = new S2COutlineIndexPacket(List.copyOf(outlines.keySet()));
        if (event.getPlayer() != null) {
            NetworkHandler.sendToPlayer(event.getPlayer(), packet);
        } else {
            event.getPlayerList().getPlayers().forEach(player -> NetworkHandler.sendToPlayer(player, packet));
        }
    }

    public static void setDrawable(List<String> ids) {
        drawable = new LinkedHashSet<>(ids);
    }

    public static boolean isDrawable(String blueprintId) {
        return drawable.contains(blueprintId);
    }

    public static BlueprintOutline get(String blueprintId) {
        return outlines.get(blueprintId);
    }

    public static void remember(String blueprintId, BlueprintOutline outline) {
        received.put(blueprintId, outline);
    }

    @Override
    protected Map<String, BlueprintOutline> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, BlueprintOutline> loaded = new LinkedHashMap<>();
        resourceManager.listResources(OUTLINE_DIRECTORY, location -> location.getPath().endsWith(".json"))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> load(entry.getKey(), entry.getValue(), loaded));
        return loaded;
    }

    @Override
    protected void apply(Map<String, BlueprintOutline> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        outlines = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
        received.clear();
        drawable = Set.copyOf(outlines.keySet());
        LOGGER.info("Loaded {} Axiomata blueprint outlines", outlines.size());
    }

    private static void load(ResourceLocation location, Resource resource, Map<String, BlueprintOutline> loaded) {
        String id = outlineId(location);
        if (id == null) {
            return;
        }

        try (Reader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                LOGGER.warn("Skipping outline {} because its root must be an object", id);
                return;
            }
            BlueprintOutline outline = read(id, root.getAsJsonObject());
            if (outline != null) {
                loaded.put(id, outline);
            }
        } catch (IOException | JsonParseException exception) {
            LOGGER.error("Failed to load outline {} from {}", id, location, exception);
        }
    }

    private static BlueprintOutline read(String id, JsonObject json) {
        if (!json.has("texture") || !json.has("resolution") || !json.has("mask") || !json.has("cells")) {
            LOGGER.warn("Skipping outline {} because it is missing texture, resolution, cells, or mask", id);
            return null;
        }

        ResourceLocation texture = ResourceLocation.tryParse(json.get("texture").getAsString());
        if (texture == null) {
            LOGGER.warn("Skipping outline {} because its texture path is not a valid resource location", id);
            return null;
        }

        OutlineMask mask;
        try {
            byte[] bits = Base64.getDecoder().decode(json.get("mask").getAsString());
            mask = new OutlineMask(json.get("resolution").getAsInt(), bits);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Skipping outline {} because its mask is malformed: {}", id, exception.getMessage());
            return null;
        }

        int declared = json.get("cells").getAsInt();
        if (declared != mask.cells()) {
            LOGGER.warn("Outline {} claims {} cells but its mask holds {}", id, declared, mask.cells());
        }
        if (mask.cells() == 0) {
            LOGGER.warn("Skipping outline {} because its mask is empty", id);
            return null;
        }
        return new BlueprintOutline(texture, mask);
    }

    private static String outlineId(ResourceLocation location) {
        String path = location.getPath();
        if (!path.startsWith(OUTLINE_PREFIX) || !path.endsWith(".json")) {
            return null;
        }
        String name = path.substring(OUTLINE_PREFIX.length(), path.length() - ".json".length());
        return name.isBlank() ? null
                : ResourceIds.id(location.getNamespace(), name).toString();
    }
}
