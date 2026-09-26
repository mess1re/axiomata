package me.mss1r.axiomata.blueprint.internal.construction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import me.mss1r.axiomata.structure.StructureSections;
import net.minecraft.resources.ResourceLocation;
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

// Cube-to-stage maps are render data, so they live in assets and never need to be sent by the
// server. A large model can contain hundreds of cube indices.
public final class BuildSectionCatalog extends SimplePreparableReloadListener<Map<ResourceLocation, StructureSections>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "construction";
    private static final String PREFIX = DIRECTORY + "/";

    private static Map<ResourceLocation, StructureSections> sections = Map.of();

    public static StructureSections get(ResourceLocation id) {
        return sections.get(id);
    }

    @Override
    protected Map<ResourceLocation, StructureSections> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, StructureSections> loaded = new LinkedHashMap<>();
        resourceManager.listResources(DIRECTORY, location -> location.getPath().endsWith(".json"))
                .forEach((location, resource) -> load(location, resource, loaded));
        return loaded;
    }

    @Override
    protected void apply(Map<ResourceLocation, StructureSections> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        sections = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
        LOGGER.info("Loaded build sections for {} models", sections.size());
    }

    private static void load(ResourceLocation location, Resource resource, Map<ResourceLocation, StructureSections> loaded) {
        String path = location.getPath();
        if (!path.startsWith(PREFIX) || !path.endsWith(".json")) {
            return;
        }
        String name = path.substring(PREFIX.length(), path.length() - ".json".length());
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), name);

        try (Reader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                LOGGER.warn("Skipping build sections {} because its root must be an object", id);
                return;
            }
            StructureSections read = read(id, root.getAsJsonObject());
            if (read != null) {
                loaded.put(id, read);
            }
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            LOGGER.error("Failed to load build sections {} from {}", id, location, exception);
        }
    }

    private static StructureSections read(ResourceLocation id, JsonObject json) {
        if (!json.has("boneCubes") || !json.has("sections")) {
            LOGGER.warn("Skipping build sections {} because it is missing boneCubes or sections", id);
            return null;
        }

        Map<String, Integer> boneCubes = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("boneCubes").entrySet()) {
            boneCubes.put(entry.getKey(), entry.getValue().getAsInt());
        }

        List<StructureSections.Section> sections = new ArrayList<>();
        JsonArray array = json.getAsJsonArray("sections");
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            String name = object.get("name").getAsString();
            Map<String, List<Integer>> bones = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("bones").entrySet()) {
                JsonArray indices = entry.getValue().getAsJsonArray();
                List<Integer> cubes = new ArrayList<>(indices.size());
                for (JsonElement index : indices) {
                    cubes.add(index.getAsInt());
                }
                bones.put(entry.getKey(), cubes);
            }
            sections.add(new StructureSections.Section(name, bones));
        }
        if (sections.isEmpty()) {
            LOGGER.warn("Skipping build sections {} because it lists no sections", id);
            return null;
        }
        return new StructureSections(boneCubes, sections);
    }
}
