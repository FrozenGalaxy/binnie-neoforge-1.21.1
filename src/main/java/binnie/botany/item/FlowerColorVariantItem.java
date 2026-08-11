package binnie.botany.item;

import binnie.core.item.VariantItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** A flower-colour variant whose 1.12 display name is {@code <colour> <kind>}. */
public final class FlowerColorVariantItem extends VariantItem {
    private final String nameKey;

    public FlowerColorVariantItem(Properties properties, String category, List<String> variants, String nameKey) {
        super(properties, category, variants);
        this.nameKey = nameKey;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.nameKey,
                Component.translatable("item.binniebotany.pigment." + value(stack)));
    }
}
