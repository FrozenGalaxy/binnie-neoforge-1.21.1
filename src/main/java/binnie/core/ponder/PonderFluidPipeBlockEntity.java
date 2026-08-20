package binnie.core.ponder;

import binnie.core.registry.CoreRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/** Visual state only; this deliberately exposes no fluid capability. */
public final class PonderFluidPipeBlockEntity extends BlockEntity {
    private FluidStack displayedFluid = FluidStack.EMPTY;

    public PonderFluidPipeBlockEntity(BlockPos pos, BlockState state) {
        this(CoreRegistries.PONDER_FLUID_PIPE_BLOCK_ENTITY.get(), pos, state);
    }

    public PonderFluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidStack displayedFluid() {
        return displayedFluid.copy();
    }

    public void setDisplayedFluid(FluidStack fluid) {
        displayedFluid = fluid.copyWithAmount(1_000);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!displayedFluid.isEmpty()) tag.put("DisplayedFluid", displayedFluid.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        displayedFluid = tag.contains("DisplayedFluid")
                ? FluidStack.parseOptional(registries, tag.getCompound("DisplayedFluid"))
                : FluidStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
