package me.mss1r.axiomata.blueprint.internal.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import me.mss1r.axiomata.PackPriority;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.network.S2CBlueprintCatalogPacket;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import me.mss1r.axiomata.blueprint.tracing.OutlineCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
//? if forge {
/*import net.minecraftforge.event.OnDatapackSyncEvent;
*///?} else {
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
//?}
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads blueprints from {@code data/<namespace>/blueprints/<name>.json}. A higher pack replaces the whole file, and any
 * datapack ranks above files shipped in mod jars (see {@link PackPriority}). Invalid files are logged with their pack
 * and errors, and the next valid file below is used; on reload the previous definition is kept if none is valid.
 */
public final class BlueprintDefinitionCatalog extends SimplePreparableReloadListener<Map<String, BlueprintDefinition>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BLUEPRINT_DIRECTORY = "blueprints";
    private static final String BLUEPRINT_PREFIX = BLUEPRINT_DIRECTORY + "/";
    private static final BlueprintFormat.Ids IDS = new BlueprintFormat.Ids(
            BuiltInRegistries.ITEM::containsKey, BuiltInRegistries.ENTITY_TYPE::containsKey);
    private static Map<String, BlueprintDefinition> definitions = Map.of();

    public static void syncToClients(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            validate(event.getPlayerList().getServer());
        }
        S2CBlueprintCatalogPacket packet = new S2CBlueprintCatalogPacket(toNetworkJson());
        if (event.getPlayer() != null) {
            NetworkHandler.sendToPlayer(event.getPlayer(), packet);
        } else {
            event.getPlayerList().getPlayers().forEach(player -> NetworkHandler.sendToPlayer(player, packet));
        }
    }

    @Override
    protected Map<String, BlueprintDefinition> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, BlueprintDefinition> loaded = new LinkedHashMap<>();
        Map<String, BlueprintDefinition> previous = definitions;
        resourceManager.listResourceStacks(BLUEPRINT_DIRECTORY, location ->
                        location.getPath().endsWith(".json") && !location.getPath().endsWith("/index.json"))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String id = blueprintId(entry.getKey());
                    if (id == null) {
                        return;
                    }
                    BlueprintDefinition definition = loadStack(id, PackPriority.datapacksOverMods(entry.getValue()));
                    if (definition == null && previous.containsKey(id)) {
                        LOGGER.error("Blueprint {} has no good file; keeping the one loaded before", id);
                        definition = previous.get(id);
                    }
                    if (definition != null) {
                        loaded.put(id, definition);
                    }
                });
        return loaded;
    }

    /** Top-most valid file in the stack, falling back to lower packs. */
    private static BlueprintDefinition loadStack(String id, List<Resource> stack) {
        for (int index = stack.size() - 1; index >= 0; index--) {
            Resource resource = stack.get(index);
            String source = "blueprint " + id + " from pack '" + resource.sourcePackId() + "'";
            try (Reader reader = resource.openAsReader()) {
                BlueprintFormat.Parsed parsed = BlueprintFormat.parse(JsonParser.parseReader(reader), IDS);
                if (!parsed.valid()) {
                    LOGGER.error("Skipping {}: {}", source, String.join("; ", parsed.errors()));
                    continue;
                }
                BlueprintFormat.warnings(parsed.definition())
                        .forEach(warning -> LOGGER.warn("In {}: {}", source, warning));
                if (parsed.legacy()) {
                    LOGGER.info("{} uses the old blueprint format with lettered ingredients; it still loads, "
                            + "see https://github.com/mess1re/axiomata/wiki/Blueprint-Data for the current one", source);
                }
                if (index < stack.size() - 1) {
                    LOGGER.error("Using {} instead of the broken file above it", source);
                }
                return parsed.definition();
            } catch (Exception exception) {
                LOGGER.error("Skipping {}: it is not valid JSON ({})", source, exception.getMessage());
            }
        }
        return null;
    }

    @Override
    protected void apply(Map<String, BlueprintDefinition> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
        LOGGER.info("Loaded {} blueprint definitions", definitions.size());
    }

    /**
     * Checks what only a running world can tell. A build placed in the world must use an entity that supports
     * construction; other blueprints are dropped so players never see them. A blueprint with neither an outline nor a
     * starter cannot be obtained, which is only warned about.
     */
    public static void validate(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (level == null) {
            return;
        }
        Map<String, BlueprintDefinition> kept = new LinkedHashMap<>();
        definitions.forEach((id, definition) -> {
            if (definition.buildsInWorld() && !supportsConstruction(level, definition.result().entity())) {
                LOGGER.error("Skipping blueprint {}: entity {} does not support construction in the world. Building it"
                        + " needs a mod that adds that support; to give the item instead, remove result.entity", id,
                        definition.result().entity());
                return;
            }
            if (definition.starter() == null && OutlineCatalog.get(definition.outlineId(id)) == null) {
                LOGGER.warn("Blueprint {} cannot be obtained: there is no outline {} to draw it at the drawing table"
                        + " and no starter item", id, definition.outlineId(id));
            }
            kept.put(id, definition);
        });
        definitions = Collections.unmodifiableMap(kept);
    }

    private static boolean supportsConstruction(ServerLevel level, @Nullable ResourceLocation entityId) {
        Entity probe = entityId == null ? null
                : BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).map(type -> type.create(level)).orElse(null);
        if (probe == null) {
            return false;
        }
        probe.discard();
        return probe instanceof UnderConstruction;
    }

    public static BlueprintDefinition get(String id) {
        return definitions.get(id);
    }

    public static Map<String, BlueprintDefinition> allById() {
        return definitions;
    }

    public static List<BlueprintDefinition> all() {
        return new ArrayList<>(definitions.values());
    }

    /** All definitions written in the current format, for syncing to clients. */
    public static String toNetworkJson() {
        JsonObject catalog = new JsonObject();
        definitions.forEach((id, definition) -> catalog.add(id, BlueprintFormat.write(definition)));
        return catalog.toString();
    }

    public static void applySyncedCatalog(String json) {
        try {
            Map<String, BlueprintDefinition> synced = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : JsonParser.parseString(json).getAsJsonObject().entrySet()) {
                BlueprintFormat.Parsed parsed = BlueprintFormat.parse(entry.getValue(), IDS);
                if (parsed.valid()) {
                    synced.put(entry.getKey(), parsed.definition());
                } else {
                    LOGGER.error("The server sent blueprint {} this client cannot read: {}", entry.getKey(),
                            String.join("; ", parsed.errors()));
                }
            }
            definitions = Collections.unmodifiableMap(synced);
        } catch (JsonParseException | IllegalStateException exception) {
            LOGGER.error("Failed to read the synced Axiomata blueprint catalog", exception);
            definitions = Map.of();
        }
    }

    private static String blueprintId(ResourceLocation location) {
        String path = location.getPath();
        if (!path.startsWith(BLUEPRINT_PREFIX) || !path.endsWith(".json")) {
            return null;
        }
        String recipePath = path.substring(BLUEPRINT_PREFIX.length(), path.length() - ".json".length());
        return recipePath.isBlank() ? null
                : ResourceIds.id(location.getNamespace(), recipePath).toString();
    }
}
