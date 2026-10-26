package binnie.core.registry;

import forestry.api.core.IToolPipette;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Binnie item tags for optional integrations not covered by a Forestry or common tag. */
public final class CoreItemTags {
    public static final TagKey<Item> MACHINE_PIPETTES = tag("tools/pipette");

    private CoreItemTags() {}

    public static boolean isMachinePipette(ItemStack stack) {
        return stack.getItem() instanceof IToolPipette || stack.is(MACHINE_PIPETTES);
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("binniecore", path));
    }
}
