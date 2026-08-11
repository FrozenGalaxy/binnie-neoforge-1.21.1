package binnie.genetics.data;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class GeneticKnowledgeDataTest {
    private static final ResourceLocation FLOWER = ResourceLocation.fromNamespaceAndPath("binniebotany", "flower");
    private static final ResourceLocation PRIMARY = ResourceLocation.fromNamespaceAndPath("binniebotany", "primary");
    private static final ResourceLocation DANDELION = ResourceLocation.fromNamespaceAndPath("binniebotany", "dandelion");

    @Test
    void canonicalizesAndMergesWithoutLosingTypedAlleles() {
        GeneticKnowledgeEntry red = new GeneticKnowledgeEntry(FLOWER, PRIMARY,
                new AlleleValue.ResourceValue(ResourceLocation.fromNamespaceAndPath("binniebotany", "red")), true);
        GeneticKnowledgeEntry blue = new GeneticKnowledgeEntry(FLOWER, PRIMARY,
                new AlleleValue.ResourceValue(ResourceLocation.fromNamespaceAndPath("binniebotany", "blue")), false);
        GeneticSpeciesDiscovery species = new GeneticSpeciesDiscovery(FLOWER, DANDELION);

        GeneticKnowledgeData first = new GeneticKnowledgeData(List.of(red, red), List.of(species, species));
        GeneticKnowledgeData second = new GeneticKnowledgeData(List.of(blue, red), List.of(species));
        GeneticKnowledgeData merged = first.merge(second);

        assertEquals(2, merged.genes().size());
        assertEquals(1, merged.species().size());
        assertTrue(merged.knows(red));
        assertTrue(merged.knows(blue));
        assertTrue(merged.knows(species));
        assertThrows(UnsupportedOperationException.class, () -> merged.genes().add(red));
    }

    @Test
    void persistentAndNetworkCodecsRoundTripCanonicalSnapshot() {
        GeneticKnowledgeData data = new GeneticKnowledgeData(
                List.of(new GeneticKnowledgeEntry(FLOWER, PRIMARY,
                        new AlleleValue.ResourceValue(ResourceLocation.fromNamespaceAndPath("binniebotany", "white")), true)),
                List.of(new GeneticSpeciesDiscovery(FLOWER, DANDELION))
        );

        var json = GeneticKnowledgeData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        assertEquals(data, GeneticKnowledgeData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());

        var buffer = Unpooled.buffer();
        GeneticKnowledgeData.STREAM_CODEC.encode(buffer, data);
        assertEquals(data, GeneticKnowledgeData.STREAM_CODEC.decode(buffer));
    }

    @Test
    void activeAndInactiveObservationsNormalizeToOneDiscovery() {
        GeneticAlleleData active = new GeneticAlleleData(FLOWER, PRIMARY,
                new AlleleValue.ResourceValue(ResourceLocation.fromNamespaceAndPath("binniebotany", "yellow")), true,
                GeneticAlleleData.Origin.ACTIVE);
        GeneticAlleleData inactive = new GeneticAlleleData(FLOWER, PRIMARY,
                active.value(), true, GeneticAlleleData.Origin.INACTIVE);

        GeneticKnowledgeData data = GeneticKnowledgeData.EMPTY
                .withGene(GeneticKnowledgeEntry.fromAllele(active))
                .withGene(GeneticKnowledgeEntry.fromAllele(inactive));
        assertEquals(1, data.genes().size());
    }
    @Test
    void filtersPortableDatabaseToOneSpeciesType() {
        ResourceLocation tree = ResourceLocation.fromNamespaceAndPath("forestry", "tree");
        GeneticKnowledgeEntry flowerGene = new GeneticKnowledgeEntry(FLOWER, PRIMARY,
                new AlleleValue.ResourceValue(ResourceLocation.fromNamespaceAndPath("binniebotany", "red")), true);
        GeneticKnowledgeEntry treeGene = new GeneticKnowledgeEntry(tree,
                ResourceLocation.fromNamespaceAndPath("forestry", "tree_species"),
                new AlleleValue.ResourceValue(ResourceLocation.fromNamespaceAndPath("forestry", "oak")), true);
        GeneticKnowledgeData mixed = new GeneticKnowledgeData(
                List.of(flowerGene, treeGene),
                List.of(new GeneticSpeciesDiscovery(FLOWER, DANDELION),
                        new GeneticSpeciesDiscovery(tree, ResourceLocation.fromNamespaceAndPath("forestry", "oak"))));

        GeneticKnowledgeData flowersOnly = mixed.filterSpeciesType(FLOWER);
        assertEquals(List.of(flowerGene), flowersOnly.genes());
        assertEquals(List.of(new GeneticSpeciesDiscovery(FLOWER, DANDELION)), flowersOnly.species());
    }

}
