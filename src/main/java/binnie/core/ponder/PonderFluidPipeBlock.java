package binnie.core.ponder;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * A deliberately non-obtainable visual aid for Ponder scenes. It is not a
 * transport block: it is one straight, non-connecting tube whose block entity
 * stores the fluid shown by the client renderer. Facing describes the visual
 * flow direction; arrows optionally expose that direction in Ponder scenes.
 */
public final class PonderFluidPipeBlock extends BaseEntityBlock {
    private static final MapCodec<PonderFluidPipeBlock> CODEC = simpleCodec(PonderFluidPipeBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ARROWS = BooleanProperty.create("arrows");
    private static final VoxelShape X_SHAPE = box(0, 4, 4, 16, 12, 12);
    private static final VoxelShape Y_SHAPE = box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape Z_SHAPE = box(4, 4, 0, 12, 12, 16);

    public PonderFluidPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ARROWS, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PonderFluidPipeBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> X_SHAPE;
            case Y -> Y_SHAPE;
            case Z -> Z_SHAPE;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ARROWS);
    }
}
