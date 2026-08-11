package binnie.bees.block;

import binnie.bees.registry.BinnieBeesWorldContent;
import forestry.core.platform.multiblock.MultiblockTicker;
import forestry.core.platform.multiblock.MultiblockValidation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class BinnieBeesAlvearyBlock extends Block implements EntityBlock {
    private final AlvearyPartType partType;

    public BinnieBeesAlvearyBlock(Properties properties, AlvearyPartType partType) {
        super(properties);
        this.partType = partType;
    }

    public AlvearyPartType partType() {
        return partType;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BinnieBeesAlvearyBlockEntity(BinnieBeesWorldContent.ALVEARY_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return MultiblockTicker.getTicker(level);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof BinnieBeesAlvearyBlockEntity alveary) {
            alveary.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!partType.hasGui()) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof BinnieBeesAlvearyBlockEntity alveary) {
            serverPlayer.openMenu(alveary, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeUtf(alveary.ownerName());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BinnieBeesAlvearyBlockEntity alveary) {
            MultiblockValidation.validateFor(level, pos, alveary);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BinnieBeesAlvearyBlockEntity alveary) {
            for (int slot = 0; slot < alveary.getInternalInventory().getContainerSize(); slot++) {
                ItemStack stack = alveary.getInternalInventory().getItem(slot);
                if (!stack.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
