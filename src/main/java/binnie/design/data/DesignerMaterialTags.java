package binnie.design.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** 1.12 material families shared by worker execution and JEI. */
public final class DesignerMaterialTags {
    public static final TagKey<Item> WOOD = tag("binnietrees", "designer_wood_materials");
    public static final TagKey<Item> GLASS = tag("binnietrees", "designer_glass_materials");
    public static final TagKey<Item> CERAMIC = tag("binniebotany", "designer_ceramic_materials");

    private DesignerMaterialTags() {}

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
