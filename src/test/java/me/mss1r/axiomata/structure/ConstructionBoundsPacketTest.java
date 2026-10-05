package me.mss1r.axiomata.structure;

import io.netty.buffer.Unpooled;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.network.S2CConstructionBoundsPacket;
import me.mss1r.axiomata.collision.OrientedBox;
import me.mss1r.axiomata.collision.Rotation3;
import me.mss1r.axiomata.geometry.LocalBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstructionBoundsPacketTest {
    @Test
    void keepsCubePlacementAndLegacyBoundsWhenSentToAClient() {
        var section = new SectionBounds.Section("arm", new LocalBox(-16, 0, -32, 16, 96, 32), List.of(
                new OrientedBox(new Vec3(0, 3, 1), new Vec3(0.25, 2, 0.5), Rotation3.aroundX(0.7F))));
        var packet = new S2CConstructionBoundsPacket(Map.of(ResourceIds.id("test", "machine"),
                new SectionBounds(List.of(section))));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            S2CConstructionBoundsPacket.encode(packet, buffer);
            assertEquals(packet, S2CConstructionBoundsPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
