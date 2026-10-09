package binnie.genetics.api;

import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.data.GeneticAlleleData;
import binnie.genetics.data.GeneticKnowledgeEntry;
import binnie.genetics.data.GeneticSerumData;
import binnie.genetics.item.GeneticMaterialItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 1.12-compatible gene-bank operation for applying a sequenced gene to an empty serum container. */
public class GeneticSerumEngineering {
    private GeneticSerumEngineering() {}

    /**
      * Applies a discovered gene to a carried single serum or serum array.
      * Filled serum is accepted only after all charges are spent, matching the 1.12 gene bank.
      */
    public static ItemStack applyGene(ItemStack carried, GeneticKnowledgeEntry gene) {
        if (carried.isEmpty() || carried.getCount() != 1 || !(carried.getItem() instanceof GeneticMaterialItem material)) {
            return ItemStack.EMPTY;
        }
        GeneticMaterialItem.Kind kind;
        try {
            kind = material.kind(carried);
        } catch (IllegalArgumentException exception) {
            return ItemStack.EMPTY;
        }
        if (kind != GeneticMaterialItem.Kind.SERUM && kind != GeneticMaterialItem.Kind.SERUM_ARRAY) {
            return ItemStack.EMPTY;
        }

        GeneticSerumData engineered = engineer(kind, carried.get(BinnieDataComponents.GENETIC_SERUM), gene);
        if (engineered == null) return ItemStack.EMPTY;

        ItemStack result = carried.copyWithCount(1);
        result.set(BinnieDataComponents.GENETIC_SERUM, engineered);
        return result;
    }

    /** Pure authoritative transformation used by execution, JEI contracts and registry-free parity tests. */
    public static GeneticSerumData engineer(GeneticMaterialItem.Kind kind, GeneticSerumData current,
                                            GeneticKnowledgeEntry gene) {
        if (kind != GeneticMaterialItem.Kind.SERUM && kind != GeneticMaterialItem.Kind.SERUM_ARRAY) return null;
        if (current != null && current.charges() != 0) return null;
        GeneticAlleleData selected = gene.asSample(GeneticAlleleData.Origin.ACTIVE);
        List<GeneticAlleleData> alleles;
        int quality = current == null ? 100 : current.quality();
        if (kind == GeneticMaterialItem.Kind.SERUM || current == null) {
            alleles = List.of(selected);
        } else {
            if (!current.alleles().getFirst().speciesType().equals(gene.speciesType())) return null;
            ArrayList<GeneticAlleleData> merged = new ArrayList<>(current.alleles());
            merged.removeIf(allele -> allele.chromosome().equals(gene.chromosome()));
            merged.add(selected);
            alleles = List.copyOf(merged);
        }
        return new GeneticSerumData(alleles, quality, 0);
    }
}
