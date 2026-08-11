package binnie.botany.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Source-purpose Botany fertilizer groups used by recipes and machine validators. */
public final class BotanyItemTags {
    public static final TagKey<Item> NUTRIENT_FERTILISERS = tag("fertilisers/nutrient");
    public static final TagKey<Item> ACID_FERTILISERS = tag("fertilisers/acid");
    public static final TagKey<Item> ALKALINE_FERTILISERS = tag("fertilisers/alkaline");

    private BotanyItemTags() {}

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("binniebotany", path));
    }
}
