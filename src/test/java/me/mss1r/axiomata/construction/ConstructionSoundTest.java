package me.mss1r.axiomata.construction;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructionSoundTest {
    @Test
    void hammerUsesFourDistinctVanillaKnocksAndItsOwnSubtitle() throws Exception {
        var resource = getClass().getResourceAsStream("/assets/axiomata/sounds.json");
        assertNotNull(resource);
        try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            var event = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("hammer_hit");
            assertEquals("subtitles.axiomata.hammer_hit", event.get("subtitle").getAsString());
            var variants = new HashSet<String>();
            for (var sound : event.getAsJsonArray("sounds")) {
                assertTrue(sound.getAsString().matches("minecraft:mob/zombie/wood[1-4]"));
                variants.add(sound.getAsString());
            }
            assertEquals(4, variants.size());
        }
    }
}
