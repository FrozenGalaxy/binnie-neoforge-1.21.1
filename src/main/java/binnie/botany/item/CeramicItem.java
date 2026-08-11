package binnie.botany.item;

import binnie.botany.block.CeramicBlock;
import binnie.core.catalog.LegacyCatalogs;
import binnie.core.data.VariantData;
import binnie.core.registry.BinnieDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Keeps the matching Botany flower-colour identity of fired ceramic blocks. */
public final class CeramicItem extends BlockItem {
    public static final String CATEGORY = "binniebotany.flower_color";
    private static final List<String> COLORS = List.copyOf(LegacyCatalogs.values(CATEGORY));

    public CeramicItem(CeramicBlock block, Properties properties) {
        super(block, properties);
    }

    public ItemStack createStack(String color) {
        int index = COLORS.indexOf(color);
        if (index < 0) throw new IllegalArgumentException("Unknown legacy Botany ceramic colour: " + color);
        ItemStack stack = new ItemStack(this);
        stack.set(BinnieDataComponents.VARIANT, new VariantData(CATEGORY, color));
        return stack;
    }

    public static String color(ItemStack stack) {
        VariantData data = stack.get(BinnieDataComponents.VARIANT);
        return data != null && CATEGORY.equals(data.category()) && COLORS.contains(data.value()) ? data.value() : "white";
    }

    private static int colorIndex(ItemStack stack) {
        int index = COLORS.indexOf(color(stack));
        return index < 0 ? 0 : index;
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(CeramicBlock.COLOR, colorIndex(context.getItemInHand()));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.binniebotany.ceramic", Component.translatable("item.binniebotany.pigment." + color(stack)));
    }
}
