package binnie.trees.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
  * 1.12-compatible Binnie's Trees hedge.
  *
  * <p>The 1.12 hedge renderer used a full-height 8x8 centre column and full-height
  * 4x8 arms extending to connected neighbours. It was much denser
  * than a vanilla wall and connected to other hedges, leaves, fences/gates and
  * sturdy full blocks.</p>
  */
public final class HedgeBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    private static final VoxelShape CENTRE = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape ARM_NORTH = Block.box(4, 0, 0, 12, 16, 4);
    private static final VoxelShape ARM_EAST = Block.box(12, 0, 4, 16, 16, 12);
    private static final VoxelShape ARM_SOUTH = Block.box(4, 0, 12, 12, 16, 16);
    private static final VoxelShape ARM_WEST = Block.box(0, 0, 4, 4, 16, 12);

    private static final VoxelShape COLLISION_CENTRE = Block.box(4, 0, 4, 12, 24, 12);
    private static final VoxelShape COLLISION_NORTH = Block.box(4, 0, 0, 12, 24, 4);
    private static final VoxelShape COLLISION_EAST = Block.box(12, 0, 4, 16, 24, 12);
    private static final VoxelShape COLLISION_SOUTH = Block.box(4, 0, 12, 12, 24, 16);
    private static final VoxelShape COLLISION_WEST = Block.box(0, 0, 4, 4, 24, 12);

    private final VoxelShape[] visualShapes = buildShapes(false);
    private final VoxelShape[] collisionShapes = buildShapes(true);

    public HedgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false));
    }

    private static VoxelShape[] buildShapes(boolean collision) {
        VoxelShape centre = collision ? COLLISION_CENTRE : CENTRE;
        VoxelShape north = collision ? COLLISION_NORTH : ARM_NORTH;
        VoxelShape east = collision ? COLLISION_EAST : ARM_EAST;
        VoxelShape south = collision ? COLLISION_SOUTH : ARM_SOUTH;
        VoxelShape west = collision ? COLLISION_WEST : ARM_WEST;
        VoxelShape[] shapes = new VoxelShape[16];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = centre;
            if ((mask & 1) != 0) shape = Shapes.or(shape, north);
            if ((mask & 2) != 0) shape = Shapes.or(shape, east);
            if ((mask & 4) != 0) shape = Shapes.or(shape, south);
            if ((mask & 8) != 0) shape = Shapes.or(shape, west);
            shapes[mask] = shape.optimize();
        }
        return shapes;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(NORTH, connectsTo(level, pos.north(), Direction.SOUTH))
                .setValue(EAST, connectsTo(level, pos.east(), Direction.WEST))
                .setValue(SOUTH, connectsTo(level, pos.south(), Direction.NORTH))
                .setValue(WEST, connectsTo(level, pos.west(), Direction.EAST));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        BooleanProperty property = property(direction);
        if (property == null) {
            return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
        }
        return state.setValue(property, connectsTo(level, neighbourPos, direction.getOpposite()));
    }

    private boolean connectsTo(BlockGetter level, BlockPos neighbourPos, Direction faceTowardThis) {
        BlockState neighbour = level.getBlockState(neighbourPos);
        Block block = neighbour.getBlock();
        if (block instanceof HedgeBlock || block instanceof LeavesBlock || block instanceof FenceBlock || block instanceof FenceGateBlock) {
            return true;
        }
        return neighbour.isFaceSturdy(level, neighbourPos, faceTowardThis);
    }

    private static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> null;
        };
    }

    private static int mask(BlockState state) {
        int mask = 0;
        if (state.getValue(NORTH)) mask |= 1;
        if (state.getValue(EAST)) mask |= 2;
        if (state.getValue(SOUTH)) mask |= 4;
        if (state.getValue(WEST)) mask |= 8;
        return mask;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return visualShapes[mask(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisionShapes[mask(state)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return false;
    }
}
