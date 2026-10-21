package me.mss1r.axiomata.blueprint;

import com.google.gson.JsonParser;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.internal.definition.BlueprintFormat;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintFormatTest {
    private static final Set<String> ITEMS = Set.of("minecraft:oak_log", "minecraft:stick", "minecraft:iron_ingot",
            "example:wheel", "example:cart_spawner");
    private static final BlueprintFormat.Ids IDS = new BlueprintFormat.Ids(
            id -> ITEMS.contains(id.toString()), id -> id.equals(ResourceLocation.tryParse("example:cart")));

    private static final String CURRENT = """
            {
              "formatVersion": 3,
              "result": { "item": "example:cart_spawner", "entity": "example:cart", "data": { "Wheels": 2 } },
              "stages": [
                { "section": "frame", "hits": 8, "materials": { "minecraft:oak_log": 6, "#minecraft:planks": 4 } },
                { "section": "wheels", "materials": { "example:wheel": 2, "minecraft:oak_log": 1 } }
              ]
            }
            """;

    private static final String OLD = """
            [{
              "key": {
                "L": { "item": "minecraft:oak_log", "count": 7 },
                "W": { "item": "example:wheel", "count": 2 }
              },
              "result": { "item": "example:cart_spawner", "count": 1, "custom_data": { "Wheels": 2 },
                          "deployment": { "mode": "ground", "preview_entity": "example:cart" } },
              "construction": [
                { "section": "frame", "hits": 8, "materials": [ { "key": "L", "count": 6 } ] },
                { "section": "wheels", "materials": [ { "key": "W" }, { "key": "L", "count": 1 } ] }
              ]
            }]
            """;

    private static BlueprintFormat.Parsed parse(String json) {
        return BlueprintFormat.parse(JsonParser.parseString(json), IDS);
    }

    @Test
    void readsTheCurrentFormatWithItemsAndTagsInEachStage() {
        BlueprintFormat.Parsed parsed = parse(CURRENT);
        assertTrue(parsed.valid(), () -> String.join("; ", parsed.errors()));
        BlueprintDefinition definition = parsed.definition();

        assertEquals(2, definition.stageCount());
        assertTrue(definition.buildsInWorld());
        assertEquals("#minecraft:planks", definition.stages().get(0).materials().get(1).key());
        assertEquals(7, definition.totals().stream()
                .filter(material -> material.key().equals("minecraft:oak_log")).findFirst().orElseThrow().count(),
                "Totals did not add up the logs of both stages");
    }

    @Test
    void readsTheOldFormatAsTheSameBlueprint() {
        BlueprintFormat.Parsed old = parse(OLD);
        assertTrue(old.valid() && old.legacy(), () -> String.join("; ", old.errors()));
        String current = CURRENT.replace(", \"#minecraft:planks\": 4", "");
        assertEquals(BlueprintFormat.write(parse(current).definition()).toString(),
                BlueprintFormat.write(old.definition()).toString());
    }

    @Test
    void writesWhatItReads() {
        BlueprintDefinition definition = parse(CURRENT).definition();
        String written = BlueprintFormat.write(definition).toString();
        assertEquals(written, BlueprintFormat.write(parse(written).definition()).toString());
    }

    @Test
    void tagMaterialsAreGivenBackAsTheirReturnItem() {
        String withReturns = CURRENT.replace("\"stages\": [",
                "\"returns\": { \"#minecraft:planks\": \"minecraft:oak_log\" }, \"stages\": [");
        BlueprintFormat.Parsed parsed = parse(withReturns);
        assertTrue(parsed.valid(), () -> String.join("; ", parsed.errors()));
        BlueprintDefinition.Material planks = parsed.definition().stages().get(0).materials().get(1);
        assertEquals(ResourceLocation.tryParse("minecraft:oak_log"), planks.returns());
        assertEquals(ResourceLocation.tryParse("minecraft:oak_log"), planks.withCount(9).returns());
        String written = BlueprintFormat.write(parsed.definition()).toString();
        assertEquals(written, BlueprintFormat.write(parse(written).definition()).toString());

        assertError(withReturns.replace("\"#minecraft:planks\": \"minecraft", "\"#minecraft:logs\": \"minecraft"),
                "returns.#minecraft:logs: no stage takes this tag");
        assertError(withReturns.replace("\"#minecraft:planks\": \"minecraft", "\"minecraft:planks\": \"minecraft"),
                "returns.minecraft:planks: must be a tag");
        assertError(withReturns.replace("\"minecraft:oak_log\" }", "\"minecraft:oak_lgo\" }"),
                "returns.#minecraft:planks: there is no item");
    }

    @Test
    void pointsAtTheMistakeInABrokenFile() {
        assertError(CURRENT.replace("minecraft:oak_log\": 6", "minecraft:oak_lgo\": 6"),
                "stages[0].materials.minecraft:oak_lgo: there is no item");
        assertError(CURRENT.replace("\"materials\": { \"example:wheel\"", "\"matrials\": { \"example:wheel\""),
                "stages[1].matrials: not a field here");
        assertError(CURRENT.replace("\"hits\": 8", "\"hits\": \"8\""), "stages[0].hits: must be a whole number");
        assertError(CURRENT.replace("\"example:wheel\": 2", "\"example:wheel\": 0"),
                "stages[1].materials.example:wheel: count must be at least 1");
        assertError(CURRENT.replace("\"#minecraft:planks\"", "\"minecraft:planks\""),
                "a tag needs # in front");
    }

    @Test
    void placingInTheWorldNeedsTheEntity() {
        assertError(CURRENT.replace("\"entity\": \"example:cart\", ", "\"placement\": \"ground\", "),
                "result.entity: placing in the world needs the entity");
        assertError(OLD.replace(", \"preview_entity\": \"example:cart\"", ""),
                "result.deployment.preview_entity: placing in the world needs the entity");
    }

    @Test
    void buildsThatEndEarlyAreNoLongerRead() {
        assertError(CURRENT.replace("\"stages\": [", "\"minStages\": 1, \"stages\": ["), "minStages: not a field here");
        assertError(CURRENT.replace("\"stages\": [", "\"starter\": { \"item\": \"example:wheel\" }, \"stages\": ["),
                "starter: not a field here");
        assertError(CURRENT.replace("\"hits\": 8,", "\"hits\": 8, \"adds\": { \"Wheels\": 2 },"),
                "stages[0].adds: not a field here");
        assertError(OLD.replace("\"construction\": [", "\"min_stages\": 1, \"construction\": ["),
                "min_stages and starter are no longer supported");
    }

    private static void assertError(String json, String expected) {
        BlueprintFormat.Parsed parsed = parse(json);
        assertFalse(parsed.valid(), "A broken file was read as valid: " + expected);
        assertTrue(parsed.errors().stream().anyMatch(error -> error.contains(expected)),
                () -> "Expected an error containing '" + expected + "' but got " + parsed.errors());
    }
}
