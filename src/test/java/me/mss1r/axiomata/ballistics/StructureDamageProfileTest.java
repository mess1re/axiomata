package me.mss1r.axiomata.ballistics;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.mss1r.axiomata.ballistics.damage.StructureDamageProfile;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class StructureDamageProfileTest {
    @Test void omittedCategoriesAreNeutralAndProfilesRoundTrip() {
        var profile = StructureDamageProfile.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"multipliers\":{\"bolt\":0.1,\"solid_projectile\":0.5}}")).result().orElseThrow();
        assertTrue(profile.validationError().isEmpty());
        assertEquals(.1, profile.multiplier("bolt"));
        assertEquals(1, profile.multiplier("fragment"));
        assertEquals(profile, StructureDamageProfile.CODEC.parse(JsonOps.INSTANCE,
                StructureDamageProfile.CODEC.encodeStart(JsonOps.INSTANCE, profile).result().orElseThrow()).result().orElseThrow());
    }

    @Test void invalidCoefficientsAndMisspelledCategoriesAreRejected() {
        for (double value : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY})
            assertTrue(new StructureDamageProfile(Map.of("bolt", value)).validationError().isPresent());
        assertTrue(new StructureDamageProfile(Map.of("boltt", .5)).validationError().isPresent());
        assertTrue(new StructureDamageProfile(Map.of("bolt", 0.0, "solid_projectile", 2.0)).validationError().isEmpty());
    }

    @Test void categoryMapCannotChangeAfterPublication() {
        var values = new java.util.HashMap<String, Double>();
        values.put("bolt", .3);
        var profile = new StructureDamageProfile(values);
        values.put("bolt", 99.0);
        assertEquals(.3, profile.multiplier("bolt"));
        assertThrows(UnsupportedOperationException.class, () -> profile.multipliers().put("bolt", 2.0));
    }
}
