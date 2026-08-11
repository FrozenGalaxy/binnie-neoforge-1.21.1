package binnie.design.block;

import binnie.core.data.DesignData;
import binnie.design.api.DesignedBlockKind;
import binnie.design.data.DesignCatalog;
import binnie.trees.registry.BinnieTreesDesignContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public final class DesignedBlockEntity extends BlockEntity {
    public static final ModelProperty<DesignData> DESIGN_DATA = new ModelProperty<>();
    public static final ModelProperty<Direction> DESIGN_FACING = new ModelProperty<>();

    private DesignData data;

    public DesignedBlockEntity(BlockPos pos, BlockState state) {
        super(BinnieTreesDesignContent.DESIGNED_BLOCK_ENTITY.get(), pos, state);
        this.data = defaultData(state.getBlock() instanceof DesignedBlock block ? block.kind() : DesignedBlockKind.WOOD);
    }

    public DesignData data() {
        return data;
    }

    public void setData(DesignData data) {
        this.data = DesignCatalog.validate(data);
        setChangedAndSync();
    }

    public void rotate() {
        setData(new DesignData(data.design(), data.pattern(), data.primaryMaterial(), data.secondaryMaterial(),
                (data.rotation() + 1) & 3, data.mirrored(), data.primaryVariant(), data.secondaryVariant()));
    }

    public void mirror() {
        setData(new DesignData(data.design(), data.pattern(), data.primaryMaterial(), data.secondaryMaterial(),
                data.rotation(), !data.mirrored(), data.primaryVariant(), data.secondaryVariant()));
    }

    /** Shared default used when an unconfigured designed item is rendered or placed. */
    public static DesignData defaultData(DesignedBlockKind kind) {
        if (kind == DesignedBlockKind.CERAMIC) {
            ResourceLocation ceramic = ResourceLocation.fromNamespaceAndPath("binniebotany", "ceramic");
            return new DesignData("blank", "blank", ceramic, ceramic, 0, false, "white", "black");
        }
        ResourceLocation primary = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                kind == DesignedBlockKind.GLASS ? Items.GLASS : Items.OAK_PLANKS);
        ResourceLocation secondary = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                kind == DesignedBlockKind.GLASS ? Items.WHITE_STAINED_GLASS : Items.BIRCH_PLANKS);
        return new DesignData("blank", "blank", primary, secondary, 0, false);
    }

    private void setChangedAndSync() {
        setChanged();
        requestModelDataUpdate();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }


    @Override
    public ModelData getModelData() {
        Direction facing = getBlockState().hasProperty(DesignedBlock.FACING)
                ? getBlockState().getValue(DesignedBlock.FACING) : Direction.UP;
        return ModelData.builder().with(DESIGN_DATA, data).with(DESIGN_FACING, facing).build();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Design", DesignData.CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), data)
                .getOrThrow(message -> new IllegalStateException("Could not save design: " + message)));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Design")) {
            data = DesignData.CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("Design"))
                    .resultOrPartial(message -> System.err.println("Could not load design: " + message)).orElse(data);
            data = DesignCatalog.validate(data);
        }
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
