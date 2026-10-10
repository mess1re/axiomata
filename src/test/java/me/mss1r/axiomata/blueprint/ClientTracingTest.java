package me.mss1r.axiomata.blueprint;

import me.mss1r.axiomata.blueprint.client.ClientTracing;
import me.mss1r.axiomata.blueprint.network.S2CTracingStatePacket;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientTracingTest {
    @AfterEach
    void clear() {
        ClientTracing.clear();
    }

    @Test
    void resetPacketDoesNotCarryInkOntoReplacementPaper() {
        ClientTracing.accept(packet(new byte[0], 0));
        ClientTracing.session().apply(0, 0);
        assertTrue(ClientTracing.session().coverage() > 0);
        ClientTracing.accept(packet(new byte[0], 0));
        assertEquals(0, ClientTracing.session().coverage());
        assertEquals(0, ClientTracing.session().wandered());
        assertEquals("test:blueprint", ClientTracing.blueprintId());
    }

    @Test
    void reopeningRestoresTheDrawingAndItsMistakes() {
        ClientTracing.accept(packet(new byte[0], 0));
        ClientTracing.session().apply(0, 0);
        ClientTracing.session().apply(255, 255);
        float coverage = ClientTracing.session().coverage();
        int wandered = ClientTracing.session().wandered();
        byte[] saved = ClientTracing.session().snapshot();
        ClientTracing.clear();
        ClientTracing.accept(packet(saved, wandered));
        assertEquals(coverage, ClientTracing.session().coverage());
        assertEquals(wandered, ClientTracing.session().wandered());
    }

    private static S2CTracingStatePacket packet(byte[] covered, int wandered) {
        byte[] mask = new byte[8];
        mask[0] = (byte) 0x80;
        return new S2CTracingStatePacket("test:blueprint", ResourceLocation.tryParse("test:outline"),
                8, mask, covered, wandered);
    }
}
