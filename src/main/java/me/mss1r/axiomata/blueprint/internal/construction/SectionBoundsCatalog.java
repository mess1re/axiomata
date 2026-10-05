package me.mss1r.axiomata.blueprint.internal.construction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.geometry.LocalBox;
import me.mss1r.axiomata.collision.OrientedBox;
import me.mss1r.axiomata.collision.Rotation3;
import me.mss1r.axiomata.structure.SectionBounds;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import me.mss1r.axiomata.blueprint.network.S2CConstructionBoundsPacket;
//? if forge {
/*import net.minecraftforge.event.OnDatapackSyncEvent;
*///?} else {
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
//?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Cube parts use the same section assignments as the client highlight. Old files with only a
// section envelope still load.
public final class SectionBoundsCatalog extends SimplePreparableReloadListener<Map<ResourceLocation, SectionBounds>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "construction";
    private static final String PREFIX = DIRECTORY + "/";

    private static Map<ResourceLocation, SectionBounds> bounds = Map.of();

    public static SectionBounds get(ResourceLocation model) {
        return bounds.get(model);
    }

    public static void syncToClients(OnDatapackSyncEvent event) {
        var packet = new S2CConstructionBoundsPacket(bounds);
        if (event.getPlayer() != null) {
            NetworkHandler.sendToPlayer(event.getPlayer(), packet);
        } else {
            event.getPlayerList().getPlayers().forEach(player -> NetworkHandler.sendToPlayer(player, packet));
        }
    }

    public static void applySynced(Map<ResourceLocation, SectionBounds> loaded) {
        bounds = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
    }

    @Override
    protected Map<ResourceLocation, SectionBounds> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, SectionBounds> loaded = new LinkedHashMap<>();
        resourceManager.listResources(DIRECTORY, location -> location.getPath().endsWith(".json"))
                .forEach((location, resource) -> load(location, resource, loaded));
        return loaded;
    }

    @Override
    protected void apply(Map<ResourceLocation, SectionBounds> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        bounds = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
        LOGGER.info("Loaded stage bounds for {} models", bounds.size());
    }

    private static void load(ResourceLocation location, Resource resource, Map<ResourceLocation, SectionBounds> loaded) {
        String path = location.getPath();
        if (!path.startsWith(PREFIX) || !path.endsWith(".json")) {
            return;
        }
        String name = path.substring(PREFIX.length(), path.length() - ".json".length());
        ResourceLocation id = ResourceIds.id(location.getNamespace(), name);

        try (Reader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return;
            }
            JsonArray array = root.getAsJsonObject().getAsJsonArray("sections");
            List<SectionBounds.Section> boxes = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject object = element.getAsJsonObject();
                JsonArray box = object.getAsJsonArray("box");
                requireVector(box, 6);
                List<OrientedBox> parts = new ArrayList<>();
                if (object.has("parts")) {
                    for (JsonElement value : object.getAsJsonArray("parts")) {
                        JsonObject part = value.getAsJsonObject();
                        JsonArray rotation = part.getAsJsonArray("rotation");
                        requireVector(rotation, 9);
                        Vec3 half = vector(part.getAsJsonArray("halfExtent"));
                        if (half.x < 0 || half.y < 0 || half.z < 0) {
                            throw new JsonParseException("Negative cube size");
                        }
                        parts.add(new OrientedBox(vector(part.getAsJsonArray("center")),
                                half, new Rotation3(
                                rotation.get(0).getAsDouble(), rotation.get(1).getAsDouble(), rotation.get(2).getAsDouble(),
                                rotation.get(3).getAsDouble(), rotation.get(4).getAsDouble(), rotation.get(5).getAsDouble(),
                                rotation.get(6).getAsDouble(), rotation.get(7).getAsDouble(), rotation.get(8).getAsDouble())));
                    }
                }
                boxes.add(new SectionBounds.Section(object.get("name").getAsString(),
                        new LocalBox(
                                box.get(0).getAsDouble(), box.get(1).getAsDouble(), box.get(2).getAsDouble(),
                                box.get(3).getAsDouble(), box.get(4).getAsDouble(), box.get(5).getAsDouble()), parts));
            }
            if (!boxes.isEmpty()) {
                loaded.put(id, new SectionBounds(List.copyOf(boxes)));
            }
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            LOGGER.error("Failed to load stage bounds {} from {}", id, location, exception);
        }
    }

    private static Vec3 vector(JsonArray values) {
        requireVector(values, 3);
        return new Vec3(values.get(0).getAsDouble(), values.get(1).getAsDouble(), values.get(2).getAsDouble());
    }

    private static void requireVector(JsonArray values, int size) {
        if (values == null || values.size() != size) {
            throw new JsonParseException("Expected " + size + " coordinates");
        }
        for (JsonElement value : values) {
            if (!Double.isFinite(value.getAsDouble())) {
                throw new JsonParseException("Non-finite coordinate");
            }
        }
    }
}
