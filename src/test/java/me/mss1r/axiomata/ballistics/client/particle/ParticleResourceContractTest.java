package me.mss1r.axiomata.ballistics.client.particle;

import com.google.gson.JsonParser;
import me.mss1r.axiomata.ballistics.particle.ParticleSet;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ParticleResourceContractTest {
    @Test
    void internalFlashDoesNotBecomeARequiredConsumerParticleId() {
        assertFalse(Arrays.stream(ParticleSet.class.getFields()).anyMatch(field -> field.getName().equals("MUZZLE_CORE")));
    }

    @Test
    void libraryOwnsTheFlashSpriteDefinition() throws Exception {
        try (var input = getClass().getResourceAsStream("/assets/axiomata/particles/muzzle_core.json")) {
            assertNotNull(input);
            var definition = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("minecraft:flash", definition.getAsJsonArray("textures").get(0).getAsString());
        }
    }
}
