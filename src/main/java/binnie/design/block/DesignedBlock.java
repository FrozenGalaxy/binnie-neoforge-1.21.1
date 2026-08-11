package binnie.design.block;

import binnie.core.data.DesignData;
import binnie.design.api.DesignedBlockKind;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DesignedBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape[] PANELS = {
            Block.box(0, 0, 0, 16, 1, 16), Block.box(0, 15, 0, 16, 16, 16),
            Block.box(0, 0, 15, 16, 16, 16), Block.box(0, 0, 0, 16, 16, 1),
            Block.box(15, 0, 0, 16, 16, 16), Block.box(0, 0, 0, 1, 16, 16)
    };

    private final DesignedBlockKind kind;
    private final MapCodec<DesignedBlock> codec;

    public DesignedBlock(Properties properties, DesignedBlockKind kind) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(p -> new DesignedBlock(p, kind));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    public DesignedBlockKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,
                placementFacing(kind, context.getClickedFace(), context.getNearestLookingDirection()));
    }

    /**
     * The stored facing is the outward normal of the designed face. Full blocks
     * therefore face back toward the placing player, while panels face away from
     * their supporting block through the clicked face.
     */
    public static Direction placementFacing(DesignedBlockKind kind, Direction clickedFace,
                                            Direction nearestLookingDirection) {
        return kind.isPanel() ? clickedFace : nearestLookingDirection.getOpposite();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        if (kind == DesignedBlockKind.GLASS && adjacentState.getBlock() == this) return true;
        return super.skipRendering(state, adjacentState, side);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind.isPanel() ? PANELS[state.getValue(FACING).ordinal()] : Shapes.block();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!kind.isPanel()) return true;
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DesignedBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DesignedBlockEntity designed)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) designed.mirror(); else designed.rotate();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof DesignedBlockEntity designed) {
            DesignedBlockItem item = designedItem();
            return item == null ? List.of() : List.of(item.create(designed.data(), 1));
        }
        return List.of();
    }

    @Nullable
    private DesignedBlockItem designedItem() {
        var id = BuiltInRegistries.BLOCK.getKey(this);
        var item = BuiltInRegistries.ITEM.get(id);
        return item instanceof DesignedBlockItem designed ? designed : null;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof DesignedBlockEntity designed) {
            DesignedBlockItem item = designedItem();
            return item == null ? ItemStack.EMPTY : item.create(designed.data(), 1);
        }
        return ItemStack.EMPTY;
    }
}
