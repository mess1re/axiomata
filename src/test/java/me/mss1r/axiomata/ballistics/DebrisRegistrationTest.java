package me.mss1r.axiomata.ballistics;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DebrisRegistrationTest {
    @Test
    void constructionDoesNotRegisterAndRegistrationIsIdempotent() {
        String namespace = namespace();
        var physics = physics(namespace);
        var saved = context(namespace);
        assertNull(DebrisPhysics.handlerFor(saved));
        physics.register();
        assertSame(physics, DebrisPhysics.handlerFor(saved));
        assertDoesNotThrow(physics::register);
    }

    @Test
    void differentHandlersCannotClaimTheSameNamespace() {
        String namespace = namespace();
        var first = physics(namespace);
        var second = assertDoesNotThrow(() -> physics(namespace));
        first.register();
        assertThrows(IllegalArgumentException.class, second::register);
        assertSame(first, DebrisPhysics.handlerFor(context(namespace)));
    }

    @Test
    void legacyDebrisTagsResolveButOrdinaryFallingBlocksDoNot() {
        String namespace = namespace();
        var physics = physics(namespace);
        physics.register();
        var saved = new CompoundTag();
        saved.putBoolean(namespace + ":debris", true);
        assertSame(physics, DebrisPhysics.handlerFor(saved));
        assertNull(DebrisPhysics.handlerFor(new CompoundTag()));
    }

    private static DebrisPhysics physics(String namespace) {
        return new DebrisPhysics(namespace,
                new ProtectedBlockAccess(() -> ProtectedBlockAccess.Policy.NEVER), () -> true, () -> 4);
    }

    private static CompoundTag context(String namespace) {
        var saved = new CompoundTag();
        saved.putString("axiomata:debris_context", namespace);
        return saved;
    }

    private static String namespace() { return "test_debris_" + UUID.randomUUID(); }
}
