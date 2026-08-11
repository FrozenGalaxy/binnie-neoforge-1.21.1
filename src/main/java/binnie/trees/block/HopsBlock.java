package binnie.trees.block;

import binnie.trees.registry.BinnieTreesContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.12-style two-block hops crop. Both halves carry the same age for stable save/render state. */
public final class HopsBlock extends CropBlock {
    public static final MapCodec<HopsBlock> CODEC = simpleCodec(HopsBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public HopsBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(AGE, 0)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && !isMaxAge(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) != DoubleBlockHalf.LOWER) return;
        super.randomTick(state, level, pos, random);
        syncPair(level, pos);
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        PosAndState lower = lowerHalf(level, pos, state);
        if (lower == null) return;
        int age = Math.min(getMaxAge(), getAge(lower.state()) + getBonemealAgeIncrease(level));
        setPairAge(level, lower.pos(), age, 2);
    }

    @Override
    protected int getBonemealAgeIncrease(Level level) {
        return Math.max(1, super.getBonemealAgeIncrease(level) / 2);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        PosAndState lower = lowerHalf(level, pos, state);
        return lower != null && !isMaxAge(lower.state());
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        growCrops(level, pos, state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        BlockState above = level.getBlockState(pos.above());
        return above.is(this) && above.getValue(HALF) == DoubleBlockHalf.UPPER && super.canSurvive(state, level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                     LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        Direction partnerDirection = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        if (facing == partnerDirection) {
            if (!facingState.is(this) || facingState.getValue(HALF) == half) {
                return Blocks.AIR.defaultBlockState();
            }
            return state.setValue(AGE, facingState.getValue(AGE));
        }
        if (half == DoubleBlockHalf.LOWER && facing == Direction.DOWN && !state.canSurvive(level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        PosAndState lower = lowerHalf(level, pos, state);
        if (lower == null || !isMaxAge(lower.state())) return InteractionResult.PASS;
        if (!level.isClientSide) {
            int count = 2 + level.random.nextInt(3);
            popResource(level, lower.pos(), new ItemStack(BinnieTreesContent.HOPS.get(), count));
            setPairAge(level, lower.pos(), 0, 3);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos lowerPos = pos.below();
            BlockState lower = level.getBlockState(lowerPos);
            if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
                if (player.isCreative()) level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), 35);
                else level.destroyBlock(lowerPos, true, player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return BinnieTreesContent.HOPS.get();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(BinnieTreesContent.HOPS.get());
    }

    public void placeAt(LevelAccessor level, BlockPos lowerPos, int age, int flags) {
        BlockState lower = defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER).setValue(AGE, age);
        BlockState upper = defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER).setValue(AGE, age);
        level.setBlock(lowerPos, lower, flags & ~1);
        level.setBlock(lowerPos.above(), upper, flags);
        level.setBlock(lowerPos, lower, flags);
    }

    public void setPairAge(LevelAccessor level, BlockPos lowerPos, int age, int flags) {
        int bounded = Math.max(0, Math.min(getMaxAge(), age));
        BlockState lower = level.getBlockState(lowerPos);
        BlockState upper = level.getBlockState(lowerPos.above());
        if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(lowerPos, lower.setValue(AGE, bounded), flags);
        }
        if (upper.is(this) && upper.getValue(HALF) == DoubleBlockHalf.UPPER) {
            level.setBlock(lowerPos.above(), upper.setValue(AGE, bounded), flags);
        }
    }

    private void syncPair(LevelAccessor level, BlockPos lowerPos) {
        BlockState lower = level.getBlockState(lowerPos);
        if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
            setPairAge(level, lowerPos, lower.getValue(AGE), 2);
        }
    }

    private PosAndState lowerHalf(LevelReader level, BlockPos pos, BlockState state) {
        if (!state.is(this)) return null;
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) return new PosAndState(pos, state);
        BlockPos lowerPos = pos.below();
        BlockState lower = level.getBlockState(lowerPos);
        return lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new PosAndState(lowerPos, lower)
                : null;
    }

    private record PosAndState(BlockPos pos, BlockState state) {}
}
