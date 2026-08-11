package binnie.genetics.data;

import binnie.genetics.api.GeneticSerumEngineering;
import binnie.genetics.item.GeneticMaterialItem;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class GeneticSerumEngineeringTest {
    private static final ResourceLocation FLOWER = ResourceLocation.fromNamespaceAndPath("binniebotany", "flower");
    private static final ResourceLocation COLOR = ResourceLocation.fromNamespaceAndPath("binniebotany", "primary_color");
    private static final ResourceLocation STEM = ResourceLocation.fromNamespaceAndPath("binniebotany", "stem");

    @Test
    void blankSingleSerumReceivesSelectedGeneAtZeroCharges() {
        GeneticKnowledgeEntry gene = gene(COLOR, "red");
        GeneticSerumData serum = GeneticSerumEngineering.engineer(GeneticMaterialItem.Kind.SERUM, null, gene);
        assertNotNull(serum);
        assertEquals(0, serum.charges());
        assertEquals(List.of(gene.asSample(GeneticAlleleData.Origin.ACTIVE)), serum.alleles());
    }

    @Test
    void serumArrayReplacesSameChromosomeAndRejectsMixedSpeciesOrChargedSerum() {
        GeneticKnowledgeEntry red = gene(COLOR, "red");
        GeneticKnowledgeEntry blue = gene(COLOR, "blue");
        GeneticKnowledgeEntry shortStem = gene(STEM, "short");
        GeneticSerumData array = new GeneticSerumData(List.of(red.asSample(GeneticAlleleData.Origin.ACTIVE),
                shortStem.asSample(GeneticAlleleData.Origin.ACTIVE)), 91, 0);
        GeneticSerumData data = GeneticSerumEngineering.engineer(GeneticMaterialItem.Kind.SERUM_ARRAY, array, blue);
        assertNotNull(data);
        assertEquals(2, data.alleles().size());
        assertEquals(91, data.quality());
        assertTrue(data.alleles().stream().anyMatch(allele -> allele.value().equals(blue.value())));
        assertFalse(data.alleles().stream().anyMatch(allele -> allele.value().equals(red.value())));

        assertNull(GeneticSerumEngineering.engineer(GeneticMaterialItem.Kind.SERUM_ARRAY,
                new GeneticSerumData(data.alleles(), 91, 1), red));
        GeneticKnowledgeEntry bee = new GeneticKnowledgeEntry(ResourceLocation.fromNamespaceAndPath("forestry", "bee"),
                ResourceLocation.fromNamespaceAndPath("forestry", "speed"),
                new AlleleValue.ResourceValue(ResourceLocation.fromNamespaceAndPath("forestry", "fast")), true);
        assertNull(GeneticSerumEngineering.engineer(GeneticMaterialItem.Kind.SERUM_ARRAY, data, bee));
    }

    private static GeneticKnowledgeEntry gene(ResourceLocation chromosome, String value) {
        return new GeneticKnowledgeEntry(FLOWER, chromosome,
                new AlleleValue.ResourceValue(ResourceLocation.fromNamespaceAndPath("test", value)), true);
    }
}
