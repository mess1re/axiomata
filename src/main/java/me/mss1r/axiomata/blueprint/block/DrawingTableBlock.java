package me.mss1r.axiomata.blueprint.block;

//? if neoforge {
import com.mojang.serialization.MapCodec;
//?}
import dev.architectury.registry.menu.MenuRegistry;
import me.mss1r.axiomata.blueprint.blockentity.DrawingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerPlayer;
import me.mss1r.axiomata.blueprint.tracing.BlueprintOutline;
import me.mss1r.axiomata.blueprint.network.NetworkHandler;
import me.mss1r.axiomata.blueprint.network.S2CTracingStatePacket;

import java.util.Locale;

public class DrawingTableBlock extends HorizontalDirectionalBlock implements EntityBlock {
    //? if neoforge {
    public static final MapCodec<DrawingTableBlock> CODEC = simpleCodec(DrawingTableBlock::new);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<ModelType> MODEL_TYPE = EnumProperty.create("model", ModelType.class);

    private static final VoxelShape SHAPE_MAIN = Shapes.or(
            Block.box(0, 13, 0, 16, 16, 16),
            Block.box(12, 0, 1, 15, 13, 4),
            Block.box(12, 0, 12, 15, 13, 15)
    );
    private static final VoxelShape SHAPE_SIDE = Shapes.or(
            Block.box(0, 13, 0, 16, 16, 16),
            Block.box(1, 0, 1, 4, 13, 4),
            Block.box(1, 0, 12, 4, 13, 15)
    );

    public DrawingTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(MODEL_TYPE, ModelType.MAIN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODEL_TYPE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos sidePos = context.getClickedPos().relative(facing.getClockWise());
        if (!context.getLevel().getBlockState(sidePos).canBeReplaced()) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(MODEL_TYPE, ModelType.MAIN);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos sidePos = pos.relative(facing.getClockWise());
            level.setBlock(sidePos, state.setValue(MODEL_TYPE, ModelType.SIDE), Block.UPDATE_ALL);
        }
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    private void removeOtherHalf(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide) {
            if (state.getValue(MODEL_TYPE) == ModelType.MAIN) {
                BlockPos sidePos = pos.relative(state.getValue(FACING).getClockWise());
                BlockState sideState = level.getBlockState(sidePos);
                if (sideState.getBlock() == this && sideState.getValue(MODEL_TYPE) == ModelType.SIDE) {
                    level.setBlock(sidePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            } else {
                BlockPos mainPos = pos.relative(state.getValue(FACING).getCounterClockWise());
                BlockState mainState = level.getBlockState(mainPos);
                if (mainState.getBlock() == this && mainState.getValue(MODEL_TYPE) == ModelType.MAIN) {
                    level.setBlock(mainPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    //? if forge {
    /*@Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removeOtherHalf(level, pos, state);
        super.playerWillDestroy(level, pos, state, player);
    }
    *///?} else {
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removeOtherHalf(level, pos, state);
        return super.playerWillDestroy(level, pos, state, player);
    }
    //?}

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);

        if (state.getValue(MODEL_TYPE) == ModelType.SIDE) {
            BlockPos mainPos = pos.relative(facing.getCounterClockWise());
            BlockState mainState = level.getBlockState(mainPos);
            return mainState.getBlock() == this
                    && mainState.getValue(MODEL_TYPE) == ModelType.MAIN
                    && mainState.getValue(FACING) == facing;
        } else {
            BlockPos sidePos = pos.relative(facing.getClockWise());
            BlockState sideState = level.getBlockState(sidePos);

            return sideState.canBeReplaced()
                    || (sideState.getBlock() == this
                    && sideState.getValue(MODEL_TYPE) == ModelType.SIDE
                    && sideState.getValue(FACING) == facing);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(MODEL_TYPE) == ModelType.MAIN ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        Direction facing = state.getValue(FACING);
        if (state.getValue(MODEL_TYPE) == ModelType.MAIN) {
            return rotateShape(Direction.SOUTH, facing, SHAPE_MAIN);
        } else {
            return rotateShape(Direction.SOUTH, facing, SHAPE_SIDE);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(MODEL_TYPE) == ModelType.MAIN) {
            return new DrawingTableBlockEntity(pos, state);
        }
        return null;
    }

    //? if forge {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return openTable(state, level, pos, player);
    }
    *///?} else {
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        return openTable(state, level, pos, player);
    }
    //?}

    private InteractionResult openTable(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            BlockEntity be;
            if (state.getValue(MODEL_TYPE) == ModelType.MAIN) {
                be = level.getBlockEntity(pos);
            } else {
                Direction facing = state.getValue(FACING);
                BlockPos mainPos = pos.relative(facing.getCounterClockWise());
                be = level.getBlockEntity(mainPos);
            }
            if (be instanceof DrawingTableBlockEntity drawingTable && player instanceof ServerPlayer serverPlayer) {
                MenuRegistry.openExtendedMenu(serverPlayer, drawingTable);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        int times = ((to.get2DDataValue() - from.get2DDataValue()) + 4) % 4;
        VoxelShape rotated = shape;
        for (int i = 0; i < times; i++) {
            rotated = rotate90(rotated);
        }
        return rotated;
    }

    private static VoxelShape rotate90(VoxelShape shape) {
        VoxelShape[] buffer = { shape, Shapes.empty() };
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            buffer[1] = Shapes.or(buffer[1], Shapes.box(
                    1 - maxZ, minY, minX,
                    1 - minZ, maxY, maxX
            ));
        });
        return buffer[1];
    }

    public enum ModelType implements StringRepresentable {
        MAIN, SIDE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
