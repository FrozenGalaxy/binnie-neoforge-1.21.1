package binnie.botany.block;

import binnie.botany.registry.BotanyWorldContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CeramicBrickBlockEntity extends BlockEntity {
    private String first = "white";
    private String second = "white";

    public CeramicBrickBlockEntity(BlockPos pos, BlockState state) {
        super(BotanyWorldContent.CERAMIC_BRICK_BLOCK_ENTITY.get(), pos, state);
    }
    public String first() { return first; }
    public String second() { return second; }
    public void setColors(String first, String second) {
        this.first = first;
        this.second = second;
        setChanged();
        requestModelDataUpdate();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.putString("First", first); tag.putString("Second", second);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("First")) first = tag.getString("First");
        if (tag.contains("Second")) second = tag.getString("Second");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
