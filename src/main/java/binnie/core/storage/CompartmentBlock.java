package binnie.core.storage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.component.DataComponents;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** One block identity carrying the six 1.12 storage tiers as block state. */
public final class CompartmentBlock extends Block implements EntityBlock {
    public static final EnumProperty<CompartmentTier> TIER = EnumProperty.create("tier", CompartmentTier.class);

    public CompartmentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIER, CompartmentTier.PLAIN));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(CompartmentBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(TIER, CompartmentBlockItem.tier(context.getItemInHand()));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof CompartmentBlockEntity compartment) {
            compartment.setTier(state.getValue(TIER));
            if (stack.has(DataComponents.CUSTOM_NAME)) compartment.setCustomName(stack.getHoverName());
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompartmentBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CompartmentBlockEntity compartment) {
            serverPlayer.openMenu(compartment, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack drop = CompartmentBlockItem.createStack(state.getValue(TIER));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CompartmentBlockEntity compartment
                && compartment.customName() != null) {
            drop.set(DataComponents.CUSTOM_NAME, compartment.customName());
        }
        return List.of(drop);
    }

    @Override
    @SuppressWarnings("deprecation") // Required Block override in Minecraft 1.21.1.
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack clone = CompartmentBlockItem.createStack(state.getValue(TIER));
        if (level.getBlockEntity(pos) instanceof CompartmentBlockEntity compartment && compartment.customName() != null) {
            clone.set(DataComponents.CUSTOM_NAME, compartment.customName());
        }
        return clone;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CompartmentBlockEntity compartment
                ? compartment.comparatorOutput() : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CompartmentBlockEntity compartment) {
            for (ItemStack stack : compartment.removeAllContents()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
