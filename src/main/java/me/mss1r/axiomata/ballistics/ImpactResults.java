package me.mss1r.axiomata.ballistics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface ImpactResults {
    /** Strength (Pa), drag (kg/m³), fracture energy (J/m³), solid volume (m³) and solid fraction. */
    public record Material(double strength, double drag, double fractureEnergy, double volume, double matter) {
        /** Energy to break the whole block, in joules. */
        public double breakEnergy() {
            return fractureEnergy * volume;
        }
    }

    public record Drive(boolean passedThrough, Vec3 position, double speed, BlockPos block, Vec3 mouth, Vec3 face) {
    }
}
