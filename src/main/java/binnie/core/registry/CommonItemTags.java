package binnie.core.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Common item tags used where the behavior is a material category rather than an exact item. */
public final class CommonItemTags {
    public static final TagKey<Item> PAPER = tag("paper");
    public static final TagKey<Item> SHEARS = tag("tools/shears");
    public static final TagKey<Item> GOLD_NUGGETS = tag("nuggets/gold");

    private CommonItemTags() {}

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
