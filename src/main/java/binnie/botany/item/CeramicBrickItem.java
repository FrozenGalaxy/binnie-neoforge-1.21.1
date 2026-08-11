package binnie.botany.item;

import binnie.botany.block.CeramicBrickBlock;
import binnie.botany.block.CeramicBrickType;
import binnie.core.data.DesignData;
import binnie.core.registry.BinnieDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class CeramicBrickItem extends BlockItem {
    private static final ResourceLocation CERAMIC = ResourceLocation.fromNamespaceAndPath("binniebotany", "ceramic");
    public CeramicBrickItem(CeramicBrickBlock block, Properties properties) { super(block, properties); }
    public ItemStack createStack(CeramicBrickType type, String first, String second) {
        ItemStack stack = new ItemStack(this);
        stack.set(BinnieDataComponents.DESIGN, new DesignData(type.getSerializedName(), type.getSerializedName(), CERAMIC, CERAMIC, 0, false, first, second));
        return stack;
    }
    private static DesignData data(ItemStack stack) { return stack.getOrDefault(BinnieDataComponents.DESIGN, new DesignData("tile", "tile", CERAMIC, CERAMIC, 0, false, "white", "white")); }
    public static String first(ItemStack stack) { return data(stack).primaryVariant(); }
    public static String second(ItemStack stack) { return data(stack).secondaryVariant(); }
    public static CeramicBrickType type(ItemStack stack) {
        String id = data(stack).design();
        for (CeramicBrickType type : CeramicBrickType.values()) if (type.getSerializedName().equals(id)) return type;
        return CeramicBrickType.TILE;
    }
    @Override protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(CeramicBrickBlock.TYPE, type(context.getItemInHand()));
    }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable("block.binniebotany.ceramic_brick", Component.translatable("item.binniebotany.pigment." + first(stack)), Component.translatable("item.binniebotany.pigment." + second(stack)), Component.translatable("block.binniebotany.ceramic_brick.type." + type(stack).getSerializedName()));
    }
}
