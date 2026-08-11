package binnie.core.item;

import binnie.core.data.VariantData;
import binnie.core.registry.BinnieDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class VariantItem extends Item {
    private final String category;
    private final List<String> variants;
    public VariantItem(Properties properties, String category, List<String> variants) {
        super(properties);
        this.category = category;
        this.variants = List.copyOf(variants);
        if (variants.isEmpty()) throw new IllegalArgumentException("Variant item requires at least one variant");
    }
    public List<String> variants() { return variants; }
    public ItemStack createStack(String value) {
        if (!variants.contains(value)) throw new IllegalArgumentException("Unknown " + category + " variant: " + value);
        ItemStack stack = new ItemStack(this);
        stack.set(BinnieDataComponents.VARIANT, new VariantData(category, value));
        return stack;
    }
    public String value(ItemStack stack) {
        VariantData data = stack.get(BinnieDataComponents.VARIANT);
        return data == null ? variants.getFirst() : data.value();
    }
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId() + "." + value(stack));
    }
}
