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
import me.mss1r.axiomata.structure.ConstructionMarkup;
import me.mss1r.axiomata.structure.SectionBounds;
import me.mss1r.axiomata.structure.StructureSections;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import me.mss1r.axiomata.blueprint.network.S2CConstructionMarkupPacket;
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

// One file per model, written by the exporter: the cubes of each section for the client highlight, and their
// placement for hits. The server loads it with the datapacks and sends it to clients. Files with only a
// section envelope still load; such models are hit by the envelope and drawn finished.
public final class ConstructionMarkupCatalog extends SimplePreparableReloadListener<Map<ResourceLocation, ConstructionMarkup>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "construction";
    private static final String PREFIX = DIRECTORY + "/";

    private static Map<ResourceLocation, ConstructionMarkup> markup = Map.of();

    public static ConstructionMarkup get(ResourceLocation model) {
        return markup.get(model);
    }

    public static void syncToClients(OnDatapackSyncEvent event) {
        var packet = new S2CConstructionMarkupPacket(markup);
        if (event.getPlayer() != null) {
            NetworkHandler.sendToPlayer(event.getPlayer(), packet);
        } else {
            event.getPlayerList().getPlayers().forEach(player -> NetworkHandler.sendToPlayer(player, packet));
        }
    }

    public static void applySynced(Map<ResourceLocation, ConstructionMarkup> loaded) {
        markup = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
    }

    @Override
    protected Map<ResourceLocation, ConstructionMarkup> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ConstructionMarkup> loaded = new LinkedHashMap<>();
        resourceManager.listResources(DIRECTORY, location -> location.getPath().endsWith(".json"))
                .forEach((location, resource) -> load(location, resource, loaded));
        return loaded;
    }

    @Override
    protected void apply(Map<ResourceLocation, ConstructionMarkup> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        markup = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
        LOGGER.info("Loaded construction markup for {} models", markup.size());
    }

    private static void load(ResourceLocation location, Resource resource, Map<ResourceLocation, ConstructionMarkup> loaded) {
        String path = location.getPath();
        if (!path.startsWith(PREFIX) || !path.endsWith(".json")) {
            return;
        }
        String name = path.substring(PREFIX.length(), path.length() - ".json".length());
        ResourceLocation id = ResourceIds.id(location.getNamespace(), name);

        try (Reader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                LOGGER.warn("Skipping construction markup {} because its root must be an object", id);
                return;
            }
            JsonObject json = root.getAsJsonObject();
            JsonArray array = json.getAsJsonArray("sections");
            if (array == null) {
                throw new JsonParseException("Missing sections");
            }
            boolean withCubes = json.has("boneCubes");
            List<SectionBounds.Section> boxes = new ArrayList<>();
            List<StructureSections.Section> cubes = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject object = element.getAsJsonObject();
                if (!object.has("name")) {
                    throw new JsonParseException("A section has no name");
                }
                String section = object.get("name").getAsString();
                boxes.add(new SectionBounds.Section(section, box(object.getAsJsonArray("box")), parts(object)));
                if (withCubes) {
                    cubes.add(new StructureSections.Section(section, bones(object.getAsJsonObject("bones"))));
                }
            }
            if (boxes.isEmpty()) {
                LOGGER.warn("Skipping construction markup {} because it lists no sections", id);
                return;
            }
            loaded.put(id, new ConstructionMarkup(new SectionBounds(boxes),
                    withCubes ? new StructureSections(boneCubes(json.getAsJsonObject("boneCubes")), cubes) : null));
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            LOGGER.error("Failed to load construction markup {} from {}", id, location, exception);
        }
    }

    private static LocalBox box(JsonArray box) {
        requireVector(box, 6);
        return new LocalBox(box.get(0).getAsDouble(), box.get(1).getAsDouble(), box.get(2).getAsDouble(),
                box.get(3).getAsDouble(), box.get(4).getAsDouble(), box.get(5).getAsDouble());
    }

    private static List<OrientedBox> parts(JsonObject section) {
        List<OrientedBox> parts = new ArrayList<>();
        if (!section.has("parts")) {
            return parts;
        }
        for (JsonElement value : section.getAsJsonArray("parts")) {
            JsonObject part = value.getAsJsonObject();
            JsonArray rotation = part.getAsJsonArray("rotation");
            requireVector(rotation, 9);
            Vec3 half = vector(part.getAsJsonArray("halfExtent"));
            if (half.x < 0 || half.y < 0 || half.z < 0) {
                throw new JsonParseException("Negative cube size");
            }
            parts.add(new OrientedBox(vector(part.getAsJsonArray("center")), half, new Rotation3(
                    rotation.get(0).getAsDouble(), rotation.get(1).getAsDouble(), rotation.get(2).getAsDouble(),
                    rotation.get(3).getAsDouble(), rotation.get(4).getAsDouble(), rotation.get(5).getAsDouble(),
                    rotation.get(6).getAsDouble(), rotation.get(7).getAsDouble(), rotation.get(8).getAsDouble())));
        }
        return parts;
    }

    private static Map<String, List<Integer>> bones(JsonObject bones) {
        if (bones == null) {
            throw new JsonParseException("A section has no bones, but the file lists boneCubes");
        }
        Map<String, List<Integer>> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : bones.entrySet()) {
            List<Integer> cubes = new ArrayList<>();
            for (JsonElement index : entry.getValue().getAsJsonArray()) {
                cubes.add(index.getAsInt());
            }
            result.put(entry.getKey(), cubes);
        }
        return result;
    }

    private static Map<String, Integer> boneCubes(JsonObject counts) {
        Map<String, Integer> result = new LinkedHashMap<>();
        counts.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsInt()));
        return result;
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
