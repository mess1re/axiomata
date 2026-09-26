package me.mss1r.axiomata.blueprint.internal.construction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import me.mss1r.axiomata.geometry.LocalBox;
import me.mss1r.axiomata.structure.SectionBounds;
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

// Coarse stage boxes are server-side hit targets. The full render section map is intentionally not
// needed for deciding whether a hammer blow reached the current stage.
public final class SectionBoundsCatalog extends SimplePreparableReloadListener<Map<ResourceLocation, SectionBounds>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "construction";
    private static final String PREFIX = DIRECTORY + "/";

    private static Map<ResourceLocation, SectionBounds> bounds = Map.of();

    public static SectionBounds get(ResourceLocation model) {
        return bounds.get(model);
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
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), name);

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
                boxes.add(new SectionBounds.Section(object.get("name").getAsString(),
                        new LocalBox(
                                box.get(0).getAsDouble(), box.get(1).getAsDouble(), box.get(2).getAsDouble(),
                                box.get(3).getAsDouble(), box.get(4).getAsDouble(), box.get(5).getAsDouble())));
            }
            if (!boxes.isEmpty()) {
                loaded.put(id, new SectionBounds(List.copyOf(boxes)));
            }
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            LOGGER.error("Failed to load stage bounds {} from {}", id, location, exception);
        }
    }
}
