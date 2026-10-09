package me.mss1r.axiomata.ballistics;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.IEventBus;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
//?}

final class ProjectilePassageHandler {
    private ProjectilePassageHandler() {}

    static void register(IEventBus gameBus) {
        gameBus.addListener(ProjectilePassageHandler::onProjectileImpact);
    }

    private static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof ProjectilePassThroughControl target)) {
            return;
        }
        Projectile projectile = event.getProjectile();
        if (allowsPassage(target, projectile.position(), projectile.getDeltaMovement(), hit.getLocation())) {
            //? if forge {
            /*event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
            *///?} else {
            event.setCanceled(true);
            //?}
        }
    }

    static boolean allowsPassage(ProjectilePassThroughControl target, Vec3 start, Vec3 movement, Vec3 hit) {
        Vec3 end = movement.lengthSqr() > 1.0E-6D ? start.add(movement) : hit;
        return target.allowsProjectilePassage(start, end);
    }
}
