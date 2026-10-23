package me.mss1r.axiomata.blueprint.api.event;

import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** Server-side work quotation for one hammer blow. Materials are not discounted. */
public final class BlueprintWorkEvent {
    private final UnderConstruction result;
    private final @Nullable LivingEntity worker;
    private double workUnits = 1;

    public BlueprintWorkEvent(UnderConstruction result, @Nullable LivingEntity worker) {
        this.result = result;
        this.worker = worker;
    }

    public UnderConstruction result() { return result; }
    public @Nullable LivingEntity worker() { return worker; }
    public double workUnits() { return workUnits; }

    public void multiplyWork(double multiplier) {
        double modified = workUnits * multiplier;
        if (!Double.isFinite(modified) || modified <= 0) {
            throw new IllegalArgumentException("Work must be finite and positive");
        }
        workUnits = modified;
    }
}
