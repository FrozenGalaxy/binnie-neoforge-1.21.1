package binnie.botany.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Data-pack extensible block groups used by Botany interactions. */
public final class BotanyBlockTags {
    public static final TagKey<Block> TROWEL_USABLE_SOILS = tag("trowel_usable_soils");

    private BotanyBlockTags() {}

    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("binniebotany", path));
    }
}
