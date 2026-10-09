package binnie.bees.forestry;

import binnie.bees.BinnieBees;
import forestry.api.plugin.IGeneticRegistration;
import forestry.core.engine.genetics.flowers.TagFlowerType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Registers every 1.12 Binnie's Bees flower rule against its current block tag. */
public class LegacyBeeFlowerTypes {
    private static final List<String> TYPES = List.of(
            "sugar", "dead", "water", "book", "sapling", "redstone", "wood", "rock", "leaves", "fruit", "mystical"
    );

    private LegacyBeeFlowerTypes() {}

    public static int register(IGeneticRegistration binniegenetics) {
        for (String type : TYPES) {
            ResourceLocation tagId = ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, "flowers/" + type);
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagId);
            binniegenetics.registerFlowerType(ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, "flower_type_" + type),
                    new TagFlowerType(tag, true));
        }
        return TYPES.size();
    }
}
