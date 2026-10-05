package me.mss1r.axiomata.blueprint.internal.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Placement;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Result;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Stage;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Starter;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Reads and writes blueprint JSON. Reads format 3 and converts the legacy keyed format. Errors include the JSON path of
 * the problem.
 */
public final class BlueprintFormat {
    private static final Set<String> ROOT_FIELDS = Set.of("formatVersion", "result", "stages", "minStages",
            "starter", "outline", "returns");
    private static final Set<String> RESULT_FIELDS = Set.of("item", "count", "data", "entity", "placement");
    private static final Set<String> STAGE_FIELDS = Set.of("section", "hits", "materials", "adds");
    private static final Set<String> STARTER_FIELDS = Set.of("item", "builtStages");

    private BlueprintFormat() {
    }

    /** Either a definition or the errors that prevented it. */
    public record Parsed(@Nullable BlueprintDefinition definition, List<String> errors, boolean legacy) {
        public boolean valid() {
            return definition != null && errors.isEmpty();
        }
    }

    /** Registry checks used to validate item and entity ids. */
    public record Ids(Predicate<ResourceLocation> item, Predicate<ResourceLocation> entity) {
    }

    public static Parsed parse(JsonElement json, Ids ids) {
        List<String> errors = new ArrayList<>();
        if (json != null && json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            if (array.size() != 1 || !array.get(0).isJsonObject()) {
                errors.add("an old-format file must hold exactly one blueprint");
                return new Parsed(null, errors, true);
            }
            return legacy(array.get(0).getAsJsonObject(), ids, errors);
        }
        if (json == null || !json.isJsonObject()) {
            errors.add("expected a blueprint object with \"result\" and \"stages\"");
            return new Parsed(null, errors, false);
        }
        JsonObject root = json.getAsJsonObject();
        if (root.has("key") || root.has("construction")) {
            return legacy(root, ids, errors);
        }
        return current(root, ids, errors);
    }

    private static Parsed current(JsonObject root, Ids ids, List<String> errors) {
        unknownFields(root, ROOT_FIELDS, "", errors);
        if (root.has("formatVersion")) {
            Integer version = integer(root.get("formatVersion"), "formatVersion", errors);
            if (version != null && version != BlueprintDefinition.FORMAT_VERSION) {
                errors.add("formatVersion: this version reads format " + BlueprintDefinition.FORMAT_VERSION
                        + ", not " + version);
            }
        }
        Result result = result(object(root, "result", "", true, errors), ids, errors);
        List<Stage> stages = returns(root, stages(root, ids, errors), ids, errors);
        int minStages = 0;
        if (root.has("minStages")) {
            Integer value = integer(root.get("minStages"), "minStages", errors);
            if (value != null) {
                if (value < 1 || value >= stages.size()) {
                    errors.add("minStages: must be from 1 to " + Math.max(1, stages.size() - 1)
                            + " (one less than the " + stages.size() + " stages); leave it out when every stage"
                            + " must be built");
                } else {
                    minStages = value;
                }
            }
        }
        Starter starter = null;
        JsonObject starterJson = object(root, "starter", "", false, errors);
        if (starterJson != null) {
            unknownFields(starterJson, STARTER_FIELDS, "starter.", errors);
            ResourceLocation item = itemId(starterJson, "item", "starter.", ids, errors);
            int built = 1;
            if (starterJson.has("builtStages")) {
                Integer value = integer(starterJson.get("builtStages"), "starter.builtStages", errors);
                if (value != null && (value < 0 || value > stages.size())) {
                    errors.add("starter.builtStages: must be from 0 to " + stages.size());
                } else if (value != null) {
                    built = value;
                }
            }
            if (item != null) {
                starter = new Starter(item, built);
            }
        }
        ResourceLocation outline = null;
        if (root.has("outline")) {
            outline = id(root.get("outline"), "outline", errors);
        }
        if (result == null || !errors.isEmpty()) {
            return new Parsed(null, errors, false);
        }
        return new Parsed(new BlueprintDefinition(result, stages, minStages, starter, outline), errors, false);
    }

    @Nullable
    private static Result result(@Nullable JsonObject json, Ids ids, List<String> errors) {
        if (json == null) {
            return null;
        }
        unknownFields(json, RESULT_FIELDS, "result.", errors);
        ResourceLocation item = itemId(json, "item", "result.", ids, errors);
        int count = 1;
        if (json.has("count")) {
            Integer value = integer(json.get("count"), "result.count", errors);
            if (value != null && value < 1) {
                errors.add("result.count: must be at least 1");
            } else if (value != null) {
                count = value;
            }
        }
        Map<String, Integer> data = new LinkedHashMap<>();
        JsonObject dataJson = object(json, "data", "result.", false, errors);
        if (dataJson != null) {
            dataJson.entrySet().forEach(entry -> {
                Integer value = integer(entry.getValue(), "result.data." + entry.getKey(), errors);
                if (value != null) {
                    data.put(entry.getKey(), value);
                }
            });
        }
        ResourceLocation entity = null;
        if (json.has("entity")) {
            entity = id(json.get("entity"), "result.entity", errors);
            if (entity != null && !ids.entity().test(entity)) {
                errors.add("result.entity: there is no entity '" + entity + "'");
            }
        }
        Placement placement = entity != null ? Placement.GROUND : null;
        if (json.has("placement")) {
            String value = string(json.get("placement"), "result.placement", errors);
            if (value != null) {
                try {
                    placement = Placement.valueOf(value.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    errors.add("result.placement: must be \"ground\" or \"water\", not \"" + value + "\"");
                }
            }
        }
        if (placement != null && entity == null) {
            errors.add("result.entity: placing in the world needs the entity it places; leave out placement to give"
                    + " the item instead");
        }
        return item == null ? null : new Result(item, count, data, placement, entity);
    }

    private static List<Stage> stages(JsonObject root, Ids ids, List<String> errors) {
        List<Stage> stages = new ArrayList<>();
        if (!root.has("stages")) {
            errors.add("stages: missing; a blueprint is built in at least one stage");
            return stages;
        }
        if (!root.get("stages").isJsonArray() || root.getAsJsonArray("stages").isEmpty()) {
            errors.add("stages: must be a list of at least one stage");
            return stages;
        }
        JsonArray array = root.getAsJsonArray("stages");
        for (int index = 0; index < array.size(); index++) {
            String path = "stages[" + index + "]";
            if (!array.get(index).isJsonObject()) {
                errors.add(path + ": must be an object");
                continue;
            }
            JsonObject json = array.get(index).getAsJsonObject();
            unknownFields(json, STAGE_FIELDS, path + ".", errors);
            String section = json.has("section") ? string(json.get("section"), path + ".section", errors) : null;
            if (section == null) {
                errors.add(path + ".section: missing; name the part of the model this stage raises");
                section = "";
            }
            int hits = 0;
            if (json.has("hits")) {
                Integer value = integer(json.get("hits"), path + ".hits", errors);
                if (value != null && value < 0) {
                    errors.add(path + ".hits: cannot be negative; leave it out to count blows from the materials");
                } else if (value != null) {
                    hits = value;
                }
            }
            List<Material> materials = new ArrayList<>();
            JsonObject materialsJson = object(json, "materials", path + ".", false, errors);
            if (materialsJson != null) {
                materialsJson.entrySet().forEach(entry -> {
                    String where = path + ".materials." + entry.getKey();
                    Integer count = integer(entry.getValue(), where, errors);
                    if (count != null && count < 1) {
                        errors.add(where + ": count must be at least 1");
                        return;
                    }
                    Material material = material(entry.getKey(), count == null ? 1 : count, where, ids, errors);
                    if (material != null && count != null) {
                        materials.add(material);
                    }
                });
            }
            stages.add(new Stage(section, hits, materials, adds(json, path, errors)));
        }
        return stages;
    }

    /** {@code "returns": {"#tag": "item"}}: the item each tag material is given back as. */
    private static List<Stage> returns(JsonObject root, List<Stage> stages, Ids ids, List<String> errors) {
        JsonObject json = object(root, "returns", "", false, errors);
        if (json == null) {
            return stages;
        }
        Map<ResourceLocation, ResourceLocation> returns = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String where = "returns." + entry.getKey();
            ResourceLocation tag = entry.getKey().startsWith("#")
                    ? ResourceLocation.tryParse(entry.getKey().substring(1)) : null;
            if (tag == null) {
                errors.add(where + ": must be a tag with # in front, as written in the stage materials");
                continue;
            }
            if (stages.stream().flatMap(stage -> stage.materials().stream()).noneMatch(m -> tag.equals(m.tag()))) {
                errors.add(where + ": no stage takes this tag");
                continue;
            }
            ResourceLocation item = id(entry.getValue(), where, errors);
            if (item != null && !ids.item().test(item)) {
                errors.add(where + ": there is no item '" + item + "'");
            } else if (item != null) {
                returns.put(tag, item);
            }
        }
        List<Stage> result = new ArrayList<>(stages.size());
        for (Stage stage : stages) {
            List<Material> materials = new ArrayList<>(stage.materials().size());
            for (Material material : stage.materials()) {
                materials.add(material.tag() != null && returns.containsKey(material.tag())
                        ? material.withReturns(returns.get(material.tag())) : material);
            }
            result.add(new Stage(stage.section(), stage.hits(), materials, stage.adds()));
        }
        return result;
    }

    @Nullable
    private static Material material(String key, int count, String where, Ids ids, List<String> errors) {
        if (key.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(key.substring(1));
            if (tag == null) {
                errors.add(where + ": '" + key + "' is not a tag id");
                return null;
            }
            return Material.ofTag(tag, count);
        }
        ResourceLocation item = ResourceLocation.tryParse(key);
        if (item == null) {
            errors.add(where + ": '" + key + "' is not an item id");
            return null;
        }
        if (!ids.item().test(item)) {
            errors.add(where + ": there is no item '" + item + "' (a tag needs # in front)");
            return null;
        }
        return Material.ofItem(item, count);
    }

    private static Map<String, Integer> adds(JsonObject json, String path, List<String> errors) {
        Map<String, Integer> adds = new LinkedHashMap<>();
        JsonObject addsJson = object(json, "adds", path + ".", false, errors);
        if (addsJson != null) {
            addsJson.entrySet().forEach(entry -> {
                Integer value = integer(entry.getValue(), path + ".adds." + entry.getKey(), errors);
                if (value != null && value < 0) {
                    errors.add(path + ".adds." + entry.getKey() + ": cannot be negative");
                } else if (value != null) {
                    adds.put(entry.getKey(), value);
                }
            });
        }
        return adds;
    }

    /** Legacy format: lettered ingredients in {@code key}, which stages refer to by letter. */
    private static Parsed legacy(JsonObject root, Ids ids, List<String> errors) {
        Map<String, Material> key = new LinkedHashMap<>();
        JsonObject keyJson = object(root, "key", "", true, errors);
        if (keyJson != null) {
            keyJson.entrySet().forEach(entry -> {
                String where = "key." + entry.getKey();
                if (!entry.getValue().isJsonObject()) {
                    errors.add(where + ": must be an object with \"item\" and \"count\"");
                    return;
                }
                JsonObject spec = entry.getValue().getAsJsonObject();
                ResourceLocation item = itemId(spec, "item", where + ".", ids, errors);
                int count = spec.has("count") ? orZero(integer(spec.get("count"), where + ".count", errors)) : 1;
                if (count < 1) {
                    errors.add(where + ".count: must be at least 1");
                } else if (item != null) {
                    key.put(entry.getKey(), Material.ofItem(item, count));
                }
            });
        }

        JsonObject resultJson = object(root, "result", "", true, errors);
        Result result = null;
        if (resultJson != null) {
            ResourceLocation item = itemId(resultJson, "item", "result.", ids, errors);
            int count = resultJson.has("count") ? orZero(integer(resultJson.get("count"), "result.count", errors)) : 1;
            Map<String, Integer> data = new LinkedHashMap<>();
            JsonObject dataJson = object(resultJson, "custom_data", "result.", false, errors);
            if (dataJson != null) {
                dataJson.entrySet().forEach(entry -> {
                    Integer value = integer(entry.getValue(), "result.custom_data." + entry.getKey(), errors);
                    if (value != null) {
                        data.put(entry.getKey(), value);
                    }
                });
            }
            Placement placement = null;
            ResourceLocation entity = null;
            JsonObject deployment = object(resultJson, "deployment", "result.", false, errors);
            if (deployment != null) {
                String mode = deployment.has("mode")
                        ? string(deployment.get("mode"), "result.deployment.mode", errors) : "ground";
                placement = "water".equalsIgnoreCase(mode) ? Placement.WATER : Placement.GROUND;
                if (deployment.has("preview_entity")) {
                    entity = id(deployment.get("preview_entity"), "result.deployment.preview_entity", errors);
                    if (entity != null && !ids.entity().test(entity)) {
                        errors.add("result.deployment.preview_entity: there is no entity '" + entity + "'");
                    }
                } else {
                    errors.add("result.deployment.preview_entity: placing in the world needs the entity it places");
                }
            }
            if (item != null && count >= 1) {
                result = new Result(item, count, data, placement, entity);
            } else if (count < 1) {
                errors.add("result.count: must be at least 1");
            }
        }

        List<Stage> stages = new ArrayList<>();
        if (root.has("construction") && root.get("construction").isJsonArray()) {
            JsonArray array = root.getAsJsonArray("construction");
            for (int index = 0; index < array.size(); index++) {
                String path = "construction[" + index + "]";
                if (!array.get(index).isJsonObject()) {
                    errors.add(path + ": must be an object");
                    continue;
                }
                JsonObject json = array.get(index).getAsJsonObject();
                String section = json.has("section") ? string(json.get("section"), path + ".section", errors) : "";
                int hits = json.has("hits") ? orZero(integer(json.get("hits"), path + ".hits", errors)) : 0;
                List<Material> materials = new ArrayList<>();
                for (Map.Entry<String, Integer> part : legacyParts(json, path, errors).entrySet()) {
                    Material ingredient = key.get(part.getKey());
                    if (ingredient == null) {
                        errors.add(path + ": uses the letter '" + part.getKey() + "' that \"key\" does not define");
                        continue;
                    }
                    materials.add(ingredient.withCount(part.getValue() > 0 ? part.getValue() : ingredient.count()));
                }
                stages.add(new Stage(section == null ? "" : section, Math.max(0, hits), materials,
                        adds(json, path, errors)));
            }
        }
        if (stages.isEmpty()) {
            // Without stages, the old format took the whole key at once.
            stages.add(new Stage("", 0, new ArrayList<>(key.values()), Map.of()));
        }

        int minStages = 0;
        if (root.has("min_stages")) {
            Integer value = integer(root.get("min_stages"), "min_stages", errors);
            if (value != null && value > 0 && value < stages.size()) {
                minStages = value;
            } else if (value != null && value != 0) {
                errors.add("min_stages: must be from 1 to " + Math.max(1, stages.size() - 1));
            }
        }
        Starter starter = null;
        JsonObject starterJson = object(root, "starter", "", false, errors);
        if (starterJson != null) {
            ResourceLocation item = itemId(starterJson, "item", "starter.", ids, errors);
            int built = starterJson.has("built_stages")
                    ? orZero(integer(starterJson.get("built_stages"), "starter.built_stages", errors)) : 1;
            if (item != null) {
                starter = new Starter(item, Math.max(0, Math.min(built, stages.size())));
            }
        }
        if (result == null || !errors.isEmpty()) {
            return new Parsed(null, errors, true);
        }
        return new Parsed(new BlueprintDefinition(result, stages, minStages, starter, null), errors, true);
    }

    private static Map<String, Integer> legacyParts(JsonObject stage, String path, List<String> errors) {
        Map<String, Integer> parts = new LinkedHashMap<>();
        if (stage.has("materials") && stage.get("materials").isJsonArray()) {
            for (JsonElement element : stage.getAsJsonArray("materials")) {
                if (element.isJsonObject() && element.getAsJsonObject().has("key")) {
                    JsonObject part = element.getAsJsonObject();
                    int count = part.has("count") ? orZero(integer(part.get("count"), path + ".materials", errors)) : 0;
                    parts.merge(part.get("key").getAsString(), count, Integer::sum);
                }
            }
        } else if (stage.has("keys") && stage.get("keys").isJsonArray()) {
            stage.getAsJsonArray("keys").forEach(element -> parts.put(element.getAsString(), 0));
        } else if (stage.has("key")) {
            parts.put(stage.get("key").getAsString(), 0);
        }
        return parts;
    }

    /** Fields that do nothing for this blueprint, for a warning: early ends only exist for builds in the world. */
    public static List<String> warnings(BlueprintDefinition definition) {
        List<String> warnings = new ArrayList<>();
        if (!definition.buildsInWorld()) {
            if (definition.minStages() > 0) {
                warnings.add("minStages has no effect: only builds placed in the world can end early");
            }
            if (definition.stages().stream().anyMatch(stage -> !stage.adds().isEmpty())) {
                warnings.add("adds has no effect: only builds placed in the world can end early");
            }
        }
        return warnings;
    }

    public static JsonObject write(BlueprintDefinition definition) {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", BlueprintDefinition.FORMAT_VERSION);
        Result result = definition.result();
        JsonObject resultJson = new JsonObject();
        resultJson.addProperty("item", result.item().toString());
        if (result.count() != 1) {
            resultJson.addProperty("count", result.count());
        }
        if (!result.data().isEmpty()) {
            JsonObject data = new JsonObject();
            result.data().forEach(data::addProperty);
            resultJson.add("data", data);
        }
        if (result.entity() != null) {
            resultJson.addProperty("entity", result.entity().toString());
        }
        if (result.placement() == Placement.WATER || result.placement() != null && result.entity() == null) {
            resultJson.addProperty("placement", result.placement().name().toLowerCase(Locale.ROOT));
        }
        root.add("result", resultJson);
        if (definition.minStages() > 0) {
            root.addProperty("minStages", definition.minStages());
        }
        if (definition.starter() != null) {
            JsonObject starter = new JsonObject();
            starter.addProperty("item", definition.starter().item().toString());
            if (definition.starter().builtStages() != 1) {
                starter.addProperty("builtStages", definition.starter().builtStages());
            }
            root.add("starter", starter);
        }
        if (definition.outline() != null) {
            root.addProperty("outline", definition.outline().toString());
        }
        JsonArray stages = new JsonArray();
        for (Stage stage : definition.stages()) {
            JsonObject json = new JsonObject();
            json.addProperty("section", stage.section());
            if (stage.hits() > 0) {
                json.addProperty("hits", stage.hits());
            }
            JsonObject materials = new JsonObject();
            stage.materials().forEach(material -> materials.addProperty(material.key(), material.count()));
            json.add("materials", materials);
            if (!stage.adds().isEmpty()) {
                JsonObject adds = new JsonObject();
                stage.adds().forEach(adds::addProperty);
                json.add("adds", adds);
            }
            stages.add(json);
        }
        root.add("stages", stages);
        JsonObject returns = new JsonObject();
        definition.stages().forEach(stage -> stage.materials().stream()
                .filter(material -> material.returns() != null)
                .forEach(material -> returns.addProperty(material.key(), material.returns().toString())));
        if (returns.size() > 0) {
            root.add("returns", returns);
        }
        return root;
    }

    private static void unknownFields(JsonObject json, Set<String> known, String path, List<String> errors) {
        for (String field : json.keySet()) {
            if (!known.contains(field)) {
                errors.add(path + field + ": not a field here; expected one of " + known.stream().sorted().toList());
            }
        }
    }

    @Nullable
    private static JsonObject object(JsonObject parent, String field, String path, boolean required,
                                     List<String> errors) {
        if (!parent.has(field)) {
            if (required) {
                errors.add(path + field + ": missing");
            }
            return null;
        }
        if (!parent.get(field).isJsonObject()) {
            errors.add(path + field + ": must be an object");
            return null;
        }
        return parent.getAsJsonObject(field);
    }

    @Nullable
    private static ResourceLocation itemId(JsonObject json, String field, String path, Ids ids, List<String> errors) {
        if (!json.has(field)) {
            errors.add(path + field + ": missing");
            return null;
        }
        ResourceLocation item = id(json.get(field), path + field, errors);
        if (item != null && !ids.item().test(item)) {
            errors.add(path + field + ": there is no item '" + item + "'");
            return null;
        }
        return item;
    }

    @Nullable
    private static ResourceLocation id(JsonElement element, String path, List<String> errors) {
        String value = string(element, path, errors);
        if (value == null) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            errors.add(path + ": '" + value + "' is not an id");
        }
        return id;
    }

    @Nullable
    private static String string(JsonElement element, String path, List<String> errors) {
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            return primitive.getAsString();
        }
        errors.add(path + ": must be text in quotes");
        return null;
    }

    @Nullable
    private static Integer integer(JsonElement element, String path, List<String> errors) {
        if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
            double value = primitive.getAsDouble();
            if (value == Math.rint(value) && Math.abs(value) <= Integer.MAX_VALUE) {
                return (int) value;
            }
        }
        errors.add(path + ": must be a whole number");
        return null;
    }

    private static int orZero(@Nullable Integer value) {
        return value == null ? 0 : value;
    }
}
