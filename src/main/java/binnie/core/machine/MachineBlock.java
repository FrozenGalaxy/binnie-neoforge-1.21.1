package binnie.core.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import binnie.genetics.item.GeneticKnowledgeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class MachineBlock extends Block implements EntityBlock {
    private final MachineSpec spec;
    private final java.util.function.Supplier<BlockEntityType<MachineBlockEntity>> type;

    public MachineBlock(Properties properties, MachineSpec spec, java.util.function.Supplier<BlockEntityType<MachineBlockEntity>> type) {
        super(properties);
        this.spec = spec;
        this.type = type;
    }

    public MachineSpec spec() {
        return spec;
    }

    @Override
    protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return hasCutoutModel() ? 0 : super.getLightBlock(state, level, pos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return hasCutoutModel() || super.propagatesSkylightDown(state, level, pos);
    }

    private boolean hasCutoutModel() {
        return switch (spec.id().getNamespace()) {
            case "binniebotany", "binniegenetics", "binnietrees" -> true;
            default -> false;
        };
    }

    public BlockEntityType<MachineBlockEntity> blockEntityType() {
        return type.get();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(properties -> new MachineBlock(properties, spec, type));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(type.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != type.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> MachineBlockEntity.serverTick(tickLevel, pos, tickState, (MachineBlockEntity) blockEntity);
    }


    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            machine.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            if (isLabStand()) {
                ItemStack mounted = machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT);
                if (player.isShiftKeyDown() && !mounted.isEmpty()) {
                    ItemStack removed = mounted.copy();
                    machine.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, ItemStack.EMPTY);
                    if (!player.getInventory().add(removed)) player.drop(removed, false);
                    machine.setChanged();
                } else if (mounted.getItem() instanceof GeneticKnowledgeItem database) {
                    database.openFromLabStand(serverPlayer, mounted);
                    machine.setChanged();
                }
                return InteractionResult.SUCCESS;
            }
            serverPlayer.openMenu(machine, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeUtf(machine.ownerName());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (isLabStand() && stack.getItem() instanceof GeneticKnowledgeItem
                && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            ItemStack mounted = machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT);
            if (mounted.isEmpty()) {
                if (!level.isClientSide) {
                    machine.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, stack.copyWithCount(1));
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    machine.setChanged();
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private boolean isLabStand() {
        return spec.id().getNamespace().equals("binniegenetics") && spec.id().getPath().equals("laboratory_machine");
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MachineBlockEntity machine ? machine.comparatorOutput() : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            for (ItemStack stack : machine.removeAllInventoryContents()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
