package binnie.botany.item;

import binnie.botany.block.StainedGlassBlock;
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

/** Component-backed replacement for the 80 metadata variants of 1.12 ItemStainedGlass. */
public final class StainedGlassItem extends BlockItem {
    private static final String CATEGORY = "binniebotany.flower_color";
    private static final List<String> COLORS = List.copyOf(LegacyCatalogs.values(CATEGORY));

    public StainedGlassItem(StainedGlassBlock block, Properties properties) {
        super(block, properties);
    }

    public ItemStack createStack(String color) {
        int index = colorIndex(color);
        if (index < 0) throw new IllegalArgumentException("Unknown legacy Botany flower colour: " + color);
        ItemStack stack = new ItemStack(this);
        stack.set(BinnieDataComponents.VARIANT, new VariantData(CATEGORY, color));
        return stack;
    }

    public static int colorIndex(ItemStack stack) {
        VariantData data = stack.get(BinnieDataComponents.VARIANT);
        int index = data != null && CATEGORY.equals(data.category()) ? colorIndex(data.value()) : -1;
        return index >= 0 ? index : 0;
    }

    private static int colorIndex(String color) {
        return COLORS.indexOf(color);
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(StainedGlassBlock.COLOR, colorIndex(context.getItemInHand()));
    }

    @Override
    public Component getName(ItemStack stack) {
        VariantData data = stack.get(BinnieDataComponents.VARIANT);
        if (data == null || !CATEGORY.equals(data.category())) return super.getName(stack);
        return Component.translatable("block.binniebotany.stained", Component.translatable("item.binniebotany.pigment." + data.value()));
    }
}
