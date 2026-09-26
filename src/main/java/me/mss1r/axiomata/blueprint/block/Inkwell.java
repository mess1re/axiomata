package me.mss1r.axiomata.blueprint.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

public class Inkwell extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(7, 0, 6, 9, 5, 7),
            Block.box(7, 0, 9, 9, 5, 10),
            Block.box(6, 0, 6, 7, 5, 10),
            Block.box(9, 0, 6, 10, 5, 10),
            Block.box(7, 0, 7, 9, 1, 9)
    );

    public Inkwell() {
        super(Properties.of().strength(1.5F, 6.0F).mapColor(MapColor.METAL));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
