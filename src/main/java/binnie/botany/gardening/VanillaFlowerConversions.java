package binnie.botany.gardening;

import binnie.botany.BinnieBotany;
import binnie.botany.api.genetics.IFlower;
import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.genetics.FlowerColor;
import binnie.botany.genetics.FlowerSpeciesType;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.Map;

/** Converts vanilla flowers to their Botany genetic equivalents. */
public final class VanillaFlowerConversions {
    private VanillaFlowerConversions() {}

    @Nullable
    public static IFlower create(ItemStack stack) {
        Block block = Block.byItem(stack.getItem());
        ResourceLocation id = speciesId(block);
        if (id == null) return null;
        try {
            IFlowerSpecies species = FlowerSpeciesType.get().getSpecies(id);
            IFlower flower = species.createIndividual();
            ResourceLocation primaryColor = primaryColorId(block);
            if (primaryColor == null) return flower;

            FlowerColor color = FlowerSpeciesType.get().getColor(primaryColor);
            AllelePair<ResourceLocation> pair = AllelePair.both(new Allele<>(color.id(), color.dominant()));
            IGenome genome = flower.getGenome().copyWithPairs(Map.of(FlowerChromosomes.PRIMARY_COLOR, pair));
            return species.createIndividual(genome);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @Nullable
    public static ResourceLocation speciesId(Block block) {
        String path = null;
        if (block == Blocks.DANDELION) path = "dandelion";
        else if (block == Blocks.POPPY) path = "poppy";
        else if (block == Blocks.BLUE_ORCHID) path = "orchid";
        else if (block == Blocks.ALLIUM) path = "allium";
        else if (block == Blocks.AZURE_BLUET) path = "bluet";
        else if (block == Blocks.RED_TULIP || block == Blocks.ORANGE_TULIP
                || block == Blocks.WHITE_TULIP || block == Blocks.PINK_TULIP) path = "tulip";
        else if (block == Blocks.OXEYE_DAISY) path = "daisy";
        else if (block == Blocks.CORNFLOWER) path = "cornflower";
        else if (block == Blocks.LILAC) path = "lilac";
        else if (block == Blocks.ROSE_BUSH) path = "rose";
        else if (block == Blocks.PEONY) path = "peony";
        return path == null ? null : ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, path);
    }

    /**
      * The 1.12 conversion table gave three vanilla tulips explicit primary-colour alleles.
      * Pink used the Tulip template unchanged, whose 1.12 default is violet.
      */
    @Nullable
    public static ResourceLocation primaryColorId(Block block) {
        String path = null;
        if (block == Blocks.RED_TULIP) path = "crimson";
        else if (block == Blocks.ORANGE_TULIP) path = "dark_orange";
        else if (block == Blocks.WHITE_TULIP) path = "white";
        return path == null ? null : ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, path);
    }
}
