package me.mss1r.axiomata.blueprint.api.event;

import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** The last blow of a build: the result is finished. Fired on the server. */
public final class BlueprintBuiltEvent {
    private final UnderConstruction result;
    private final String recipeId;
    @Nullable
    private final LivingEntity worker;

    public BlueprintBuiltEvent(UnderConstruction result, String recipeId, @Nullable LivingEntity worker) {
        this.result = result;
        this.recipeId = recipeId;
        this.worker = worker;
    }

    /** The finished result. */
    public UnderConstruction result() {
        return result;
    }

    /** The blueprint it was built from. */
    public String recipeId() {
        return recipeId;
    }

    /** Who struck the last blow: a player, a working mob, or null when the caller did not say. */
    @Nullable
    public LivingEntity worker() {
        return worker;
    }
}
