package me.mss1r.axiomata.ballistics;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.axiomata.data.profile.ProfileCatalog;
import me.mss1r.axiomata.data.profile.ProfileSnapshotCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProfileTest {
    @Test
    void keepsCodecDefaultsAndPhysicalUnits() {
        var profile = ProjectilePhysicsProfile.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"mass\": 2, \"diameter\": 0.025}")).result().orElseThrow();
        assertTrue(profile.validationError().isEmpty());
        assertEquals(19600, profile.kineticEnergy(140));
        assertEquals(ProjectilePhysicsProfile.DEFAULT.entity(), profile.entity());
        assertTrue(profile.airDrag(0.025) > 0);
    }

    @Test
    void rejectsNonPhysicalValues() {
        for (String json : new String[] {"{\"mass\": 0}", "{\"diameter\": -1}",
                "{\"dragCoefficient\": -1}", "{\"entity\": {\"armorPiercing\": 1.1}}"}) {
            var profile = ProjectilePhysicsProfile.CODEC.parse(JsonOps.INSTANCE,
                    JsonParser.parseString(json)).result().orElseThrow();
            assertTrue(profile.validationError().isPresent(), json);
        }
    }

    @Test
    void catalogsAreIndependentAndClientApplicationDoesNotBroadcast() {
        var first = new ProfileCatalog<>("fallback-a");
        var second = new ProfileCatalog<>("fallback-b");
        var id = ResourceLocation.tryParse("example:shot");
        var publishes = new AtomicInteger();
        first.onPublish(publishes::incrementAndGet);
        first.publish(Map.of(id, "server"));
        first.acceptFromServer(Map.of(id, "client"));
        assertEquals(1, publishes.get());
        assertEquals("client", first.get(id));
        assertEquals("fallback-b", second.get(id));
        assertThrows(UnsupportedOperationException.class, () -> first.snapshot().put(id, "bad"));
        first.reset();
        assertFalse(first.hasSnapshot());
        assertEquals("fallback-a", first.get(id));
    }

    @Test
    void profilePacketRetainsItsNbtMapFormat() {
        var codec = new ProfileSnapshotCodec<>(ProjectilePhysicsProfile.CODEC);
        var profiles = Map.of(ResourceLocation.tryParse("example:shot"), ProjectilePhysicsProfile.DEFAULT);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.write(buffer, profiles);
            assertEquals(profiles, codec.read(buffer));
        } finally {
            buffer.release();
        }
    }
}
