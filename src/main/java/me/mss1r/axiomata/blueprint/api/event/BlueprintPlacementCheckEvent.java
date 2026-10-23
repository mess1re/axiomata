package me.mss1r.axiomata.blueprint.api.event;

import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** Server-side check of the final pose, after support offsets and before entity addition or item consumption. */
public final class BlueprintPlacementCheckEvent {
    private final UnderConstruction result;
    private final String recipeId;
    private final @Nullable LivingEntity worker;
    private boolean allowed = true;

    public BlueprintPlacementCheckEvent(UnderConstruction result, String recipeId, @Nullable LivingEntity worker) {
        this.result = result;
        this.recipeId = recipeId;
        this.worker = worker;
    }

    public UnderConstruction result() { return result; }
    public String recipeId() { return recipeId; }
    public @Nullable LivingEntity worker() { return worker; }
    public boolean allowed() { return allowed; }
    public void deny() { allowed = false; }
}
