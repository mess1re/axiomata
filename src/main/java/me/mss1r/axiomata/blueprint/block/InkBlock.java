package me.mss1r.axiomata.blueprint.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

public class InkBlock extends Block {
    private static final VoxelShape SHAPE =
            Block.box(6, 0, 6, 10, 5, 10);

    public InkBlock() {
        super(Properties.of().strength(1.5F, 6.0F).mapColor(MapColor.COLOR_BLACK));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
