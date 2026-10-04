package me.mss1r.axiomata.blueprint.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.axiomata.Axiomata;
import me.mss1r.axiomata.ResourceIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class BlueprintSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Axiomata.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> HAMMER_HIT = SOUNDS.register("hammer_hit",
            () -> SoundEvent.createVariableRangeEvent(ResourceIds.id(Axiomata.MOD_ID, "hammer_hit")));

    private BlueprintSounds() {
    }

    public static void register() {
        SOUNDS.register();
    }
}
