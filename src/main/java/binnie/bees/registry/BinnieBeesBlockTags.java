package binnie.bees.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Data-pack extensible block groups used by Binnie's Bees world generation. */
public final class BinnieBeesBlockTags {
    public static final TagKey<Block> WATER_HIVE_SUPPORT = tag("water_hive_support");
    public static final TagKey<Block> ROCK_HIVE_STONE = tag("rock_hive_stone");

    private BinnieBeesBlockTags() {}

    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("binniebees", path));
    }
}
