package me.mss1r.axiomata.mixin;

import me.mss1r.axiomata.ballistics.DebrisPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FallingBlockEntity.class)
public abstract class FallingDebrisMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean axiomata$checkDebrisLanding(Level level, BlockPos pos, BlockState state, int flags) {
        FallingBlockEntity debris = (FallingBlockEntity) (Object) this;
        if (level instanceof ServerLevel server) {
            Boolean placed = DebrisPhysics.handleLanding(server, debris, pos, state);
            if (placed != null) {
                return placed;
            }
        }
        return level.setBlock(pos, state, flags);
    }
}
