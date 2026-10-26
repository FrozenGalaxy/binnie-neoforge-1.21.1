package binnie.botany.block;

import binnie.botany.api.genetics.IFlower;
import binnie.botany.registry.BotanyWorldContent;
import binnie.core.registry.CoreContent;
import com.mojang.serialization.MapCodec;
import forestry.core.engine.genetics.ItemGE;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** World representation of a full genetic flower individual. */
public final class FlowerBlock extends Block implements EntityBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 15);
    public static final BooleanProperty FLOWERED = BooleanProperty.create("flowered");
    public static final BooleanProperty WILTED = BooleanProperty.create("wilted");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 15, 14);

    public FlowerBlock(Properties properties) {
        super(properties.noCollission().instabreak().randomTicks());
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(FLOWERED, false).setValue(WILTED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(FlowerBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, FLOWERED, WILTED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return BotanySoilBlock.isSuitableSoil(below) || below.is(net.minecraft.tags.BlockTags.DIRT);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block changedBlock, BlockPos changedPos, boolean movedByPiston) {
        if (!level.isClientSide && !canSurvive(state, level, pos)) level.destroyBlock(pos, true);
        super.neighborChanged(state, level, pos, changedBlock, changedPos, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FlowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != BotanyWorldContent.FLOWER_BLOCK_ENTITY.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> FlowerBlockEntity.serverTick(tickLevel, pos, tickState, (FlowerBlockEntity) blockEntity);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FlowerBlockEntity flower)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(CoreContent.FIELD_KIT.get())) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(flower.inspectionSummary(), false);
                stack.hurtAndBreak(1, serverPlayer, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(binnie.core.registry.CommonItemTags.SHEARS)) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && flower.shear(serverPlayer)) {
                stack.hurtAndBreak(1, serverPlayer, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(binnie.botany.registry.BotanyContent.POLLEN.get())
                && ItemGE.getIndividual(stack) instanceof IFlower pollen) {
            if (level.isClientSide) {
                return flower.isHarvestable()
                        ? ItemInteractionResult.SUCCESS
                        : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (flower.applyPollen(pollen)) {
                if (!player.isCreative()) stack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FlowerBlockEntity flower) || !flower.isHarvestable()) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) flower.harvest(serverPlayer);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FlowerBlockEntity flower && !flower.getFlowerStack().isEmpty()) {
            return Collections.singletonList(flower.getFlowerStack().copy());
        }
        return Collections.emptyList();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof FlowerBlockEntity flower) return flower.getFlowerStack().copy();
        return ItemStack.EMPTY;
    }
}
