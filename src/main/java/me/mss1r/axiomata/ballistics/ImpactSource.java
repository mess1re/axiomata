package me.mss1r.axiomata.ballistics;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/** Captured attack attribution and policy survive removal of the projectile that released the fragments. */
public record ImpactSource(@Nullable Entity direct, @Nullable Entity owner, boolean mayDamageBlocks, Predicate<Entity> mayHit,
                           BiFunction<Entity, Float, Boolean> damage) {
    public static ImpactSource of(BallisticProjectile projectile) {
        Entity machine = projectile.getOwner();
        Entity attacker = projectile.impactAttacker();
        return new ImpactSource(projectile, attacker, projectile.canDamageBlocks(),
                target -> projectile.mayHitFrom(machine, attacker, target),
                (target, amount) -> target.hurt(ProjectileCombat.source(projectile.level(), ProjectileCombat.PHYSICAL_PROJECTILE, projectile, attacker), amount));
    }

    public static ImpactSource environmental(ServerLevel level, @Nullable Entity owner) {
        return new ImpactSource(null, owner, true, target -> target != owner && (owner == null || owner.getTeam() == null
                || owner.getTeam().isAllowFriendlyFire() || !owner.isAlliedTo(ProjectileCombat.logicalTarget(target))),
                (target, amount) -> target.hurt(ProjectileCombat.source(level, ProjectileCombat.PHYSICAL_PROJECTILE, null, owner), amount));
    }
}
