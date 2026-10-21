package me.mss1r.axiomata.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionStatus;
import me.mss1r.axiomata.blueprint.network.S2CConstructionStatePacket;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class ConstructionStatePacketTest {
    @Test
    void aBuildReachesTheClientAsItStands() {
        var sent = new S2CConstructionStatePacket(42, new ConstructionStatus("example:cart", 2, 5, 12, false));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        S2CConstructionStatePacket.encode(sent, buffer);
        assertEquals(sent, S2CConstructionStatePacket.decode(buffer));
    }

    @Test
    void aResultNeverBuiltReadsAsFinished() {
        assertEquals(ConstructionStatus.FINISHED, new ConstructionStatus(null, 0, 0, 0, true));
    }
}
