package me.mss1r.axiomata.mixin;

import me.mss1r.axiomata.collision.system.StructurePathfindingSystem;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if forge {
/*import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
*///?} else {
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
//?}

// The node-classification method changed signature between 1.20.1 and 1.21.1, hence the two
// Stonecutter branches below. Both injections only replace otherwise-walkable structure nodes.
@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {
    //? if forge {
    /*@Inject(method = "getBlockPathType(Lnet/minecraft/world/level/BlockGetter;IIILnet/minecraft/world/entity/Mob;)Lnet/minecraft/world/level/pathfinder/BlockPathTypes;",
            at = @At("RETURN"), cancellable = true)
    private void axiomata$blockStructureNodes(BlockGetter level, int x, int y, int z, Mob mob,
                                              CallbackInfoReturnable<BlockPathTypes> cir) {
        if (cir.getReturnValue() != BlockPathTypes.BLOCKED
                && StructurePathfindingSystem.blocked(mob, x, y, z)) {
            cir.setReturnValue(BlockPathTypes.BLOCKED);
        }
    }
    *///?} else {
    @Inject(method = "getPathTypeOfMob", at = @At("RETURN"), cancellable = true)
    private void axiomata$blockStructureNodes(PathfindingContext context, int x, int y, int z, Mob mob,
                                              CallbackInfoReturnable<PathType> cir) {
        if (cir.getReturnValue() != PathType.BLOCKED
                && StructurePathfindingSystem.blocked(mob, x, y, z)) {
            cir.setReturnValue(PathType.BLOCKED);
        }
    }
    //?}
}
