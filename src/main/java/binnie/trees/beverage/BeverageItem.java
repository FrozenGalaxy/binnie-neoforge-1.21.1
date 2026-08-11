package binnie.trees.beverage;

import binnie.core.data.BeverageData;
import binnie.core.item.VariantItem;
import binnie.core.registry.BinnieDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Variant item that keeps the matching 1.12 drink identity as a typed component. */
public final class BeverageItem extends VariantItem {
    private final String beverageCategory;

    public BeverageItem(Properties properties, String catalogCategory, String beverageCategory, List<String> variants) {
        super(properties, catalogCategory, variants);
        this.beverageCategory = beverageCategory;
    }

    @Override
    public ItemStack createStack(String value) {
        ItemStack stack = super.createStack(value);
        stack.set(BinnieDataComponents.BEVERAGE, LegacyBeverageCatalog.get(beverageCategory, value).data(250));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        BeverageData data = stack.get(BinnieDataComponents.BEVERAGE);
        if (data != null) {
            tooltip.add(Component.translatable("tooltip.binnietrees.beverage.volume", data.volume()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.binnietrees.beverage.alcohol", data.alcohol()).withStyle(ChatFormatting.DARK_RED));
        }
    }
}
