package me.mss1r.axiomata.ballistics;

import me.mss1r.axiomata.ballistics.profile.ImpactMaterialProfile;
import net.minecraft.world.phys.Vec3;

/** A structure supplies its material, solid volume and mass instead of being treated as a creature. */
public interface StructuralImpactTarget {
    ImpactMaterialProfile impactMaterial(Vec3 worldContact);
    double impactSolidVolumeM3();
    default double impactMassKg(Vec3 worldContact) { return impactSolidVolumeM3() * impactMaterial(worldContact).density(); }
}
