package me.mss1r.axiomata.blueprint.internal.definition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.network.S2CBlueprintCatalogPacket;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import net.minecraft.core.registries.BuiltInRegistries;
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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlueprintDefinitionCatalog extends SimplePreparableReloadListener<Map<String, BlueprintDefinition>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BLUEPRINT_DIRECTORY = "blueprints";
    private static final String BLUEPRINT_PREFIX = BLUEPRINT_DIRECTORY + "/";
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type BLUEPRINT_FILE_TYPE = new TypeToken<List<BlueprintDefinition>>() {}.getType();
    private static final Type BLUEPRINT_MAP_TYPE = new TypeToken<Map<String, BlueprintDefinition>>() {}.getType();
    private static Map<String, BlueprintDefinition> definitions = Map.of();

    public static void syncToClients(OnDatapackSyncEvent event) {
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
        resourceManager.listResources(BLUEPRINT_DIRECTORY, location ->
                        location.getPath().endsWith(".json") && !location.getPath().endsWith("/index.json"))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> loadBlueprint(entry.getKey(), entry.getValue(), loaded));
        return loaded;
    }

    @Override
    protected void apply(Map<String, BlueprintDefinition> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
        LOGGER.info("Loaded {} blueprint definitions", definitions.size());
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

    public static String toNetworkJson() {
        return GSON.toJson(definitions, BLUEPRINT_MAP_TYPE);
    }

    public static void applySyncedCatalog(String json) {
        try {
            Map<String, BlueprintDefinition> synced = GSON.fromJson(json, BLUEPRINT_MAP_TYPE);
            Map<String, BlueprintDefinition> validated = new LinkedHashMap<>();
            if (synced != null) {
                synced.forEach((id, definition) -> {
                    if (isValidDefinition(id, definition)) {
                        validated.put(id, definition);
                    }
                });
            }
            definitions = Collections.unmodifiableMap(validated);
        } catch (JsonParseException exception) {
            LOGGER.error("Failed to read the synced Axiomata blueprint catalog", exception);
            definitions = Map.of();
        }
    }

    private static void loadBlueprint(ResourceLocation location, Resource resource,
                                      Map<String, BlueprintDefinition> loaded) {
        String id = blueprintId(location);
        if (id == null) {
            return;
        }

        try (Reader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            BlueprintDefinition definition;
            if (root.isJsonArray()) {
                List<BlueprintDefinition> entries = GSON.fromJson(root, BLUEPRINT_FILE_TYPE);
                if (entries == null || entries.size() != 1) {
                    LOGGER.warn("Skipping blueprint {} because each file must contain exactly one recipe", id);
                    return;
                }
                definition = entries.get(0);
            } else if (root.isJsonObject()) {
                definition = GSON.fromJson(root, BlueprintDefinition.class);
            } else {
                LOGGER.warn("Skipping blueprint {} because its root must be an object or one-element array", id);
                return;
            }

            if (isValidDefinition(id, definition)) {
                loaded.put(id, definition);
            }
        } catch (IOException | JsonParseException exception) {
            LOGGER.error("Failed to load blueprint {} from {}", id, location, exception);
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

    private static boolean isValidDefinition(String id, BlueprintDefinition definition) {
        if (definition == null || definition.key == null || definition.result == null) {
            LOGGER.warn("Skipping blueprint {} because it is missing key or result data", id);
            return false;
        }
        if (definition.result.count < 1 || !isRegisteredItem(definition.result.item)) {
            LOGGER.debug("Skipping blueprint {} because result item {} is unavailable", id, definition.result.item);
            return false;
        }

        for (Map.Entry<String, BlueprintDefinition.IngredientSpec> entry : definition.key.entrySet()) {
            BlueprintDefinition.IngredientSpec spec = entry.getValue();
            if (entry.getKey().length() != 1 || spec == null || spec.count < 1 || !isRegisteredItem(spec.item)) {
                LOGGER.debug("Skipping blueprint {} because ingredient {} is invalid or unavailable", id, entry.getKey());
                return false;
            }
        }
        if (definition.result.custom_data == null) {
            definition.result.custom_data = new LinkedHashMap<>();
        }
        if (definition.construction != null) {
            Map<String, Integer> staged = new LinkedHashMap<>();
            for (BlueprintDefinition.StageSpec stage : definition.construction) {
                if (stage == null) {
                    continue;
                }
                for (BlueprintDefinition.StagePartSpec part : stage.parts()) {
                    BlueprintDefinition.IngredientSpec ingredient =
                            part.key == null ? null : definition.key.get(part.key);
                    if (ingredient == null) {
                        LOGGER.warn("Blueprint {} has a construction stage using unknown ingredient {}",
                                id, part.key);
                        continue;
                    }
                    int taken = part.count > 0 ? part.count : Math.max(1, ingredient.count);
                    staged.merge(part.key, taken, Integer::sum);
                }
            }
            for (Map.Entry<String, BlueprintDefinition.IngredientSpec> entry : definition.key.entrySet()) {
                int asked = Math.max(1, entry.getValue().count);
                int spent = staged.getOrDefault(entry.getKey(), 0);
                if (spent != asked) {
                    LOGGER.warn("Blueprint {} asks for {} of ingredient {} but its stages spend {}",
                            id, asked, entry.getKey(), spent);
                }
            }
        }
        if (definition.result.deployment != null && !definition.result.deployment.isValid()) {
            LOGGER.warn("Skipping blueprint {} because its deployment data is invalid", id);
            return false;
        }
        if (definition.result.deployment != null && definition.result.deployment.preview_entity != null) {
            ResourceLocation previewEntity = ResourceLocation.tryParse(definition.result.deployment.preview_entity);
            if (previewEntity == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(previewEntity)) {
                LOGGER.debug("Skipping blueprint {} because preview entity {} is unavailable",
                        id, definition.result.deployment.preview_entity);
                return false;
            }
        }
        return true;
    }

    private static boolean isRegisteredItem(String itemId) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
        return resourceLocation != null && BuiltInRegistries.ITEM.containsKey(resourceLocation);
    }

}
