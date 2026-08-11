package binnie.genetics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/** Immutable, canonical snapshot used by player trackers and portable database items. */
public record GeneticKnowledgeData(List<GeneticKnowledgeEntry> genes,
                                   List<GeneticSpeciesDiscovery> species) {
    public static final GeneticKnowledgeData EMPTY = new GeneticKnowledgeData(List.of(), List.of());
    public static final Codec<GeneticKnowledgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GeneticKnowledgeEntry.CODEC.listOf().optionalFieldOf("genes", List.of()).forGetter(GeneticKnowledgeData::genes),
            GeneticSpeciesDiscovery.CODEC.listOf().optionalFieldOf("species", List.of()).forGetter(GeneticKnowledgeData::species)
    ).apply(instance, GeneticKnowledgeData::new));
    public static final StreamCodec<ByteBuf, GeneticKnowledgeData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public GeneticKnowledgeData {
        genes = canonical(genes);
        species = canonicalSpecies(species);
    }

    public GeneticKnowledgeData withGene(GeneticKnowledgeEntry gene) {
        if (genes.contains(gene)) return this;
        ArrayList<GeneticKnowledgeEntry> next = new ArrayList<>(genes);
        next.add(gene);
        return new GeneticKnowledgeData(next, species);
    }

    public GeneticKnowledgeData withSpecies(GeneticSpeciesDiscovery discovery) {
        if (species.contains(discovery)) return this;
        ArrayList<GeneticSpeciesDiscovery> next = new ArrayList<>(species);
        next.add(discovery);
        return new GeneticKnowledgeData(genes, next);
    }

    public GeneticKnowledgeData merge(GeneticKnowledgeData other) {
        if (other == EMPTY || (other.genes.isEmpty() && other.species.isEmpty())) return this;
        ArrayList<GeneticKnowledgeEntry> mergedGenes = new ArrayList<>(genes);
        mergedGenes.addAll(other.genes);
        ArrayList<GeneticSpeciesDiscovery> mergedSpecies = new ArrayList<>(species);
        mergedSpecies.addAll(other.species);
        return new GeneticKnowledgeData(mergedGenes, mergedSpecies);
    }

    public boolean knows(GeneticKnowledgeEntry gene) {
        return genes.contains(gene);
    }

    public boolean knows(GeneticSpeciesDiscovery discovery) {
        return species.contains(discovery);
    }

    /** Returns the organism-specific view used by 1.12 Botanist and specialist databases. */
    public GeneticKnowledgeData filterSpeciesType(net.minecraft.resources.ResourceLocation speciesType) {
        if (speciesType == null) return this;
        return new GeneticKnowledgeData(
                genes.stream().filter(gene -> gene.speciesType().equals(speciesType)).toList(),
                species.stream().filter(discovery -> discovery.speciesType().equals(speciesType)).toList());
    }

    private static List<GeneticKnowledgeEntry> canonical(Collection<GeneticKnowledgeEntry> source) {
        if (source == null) throw new IllegalArgumentException("Gene collection cannot be null");
        return List.copyOf(new TreeSet<>(source));
    }

    private static List<GeneticSpeciesDiscovery> canonicalSpecies(Collection<GeneticSpeciesDiscovery> source) {
        if (source == null) throw new IllegalArgumentException("Species collection cannot be null");
        return List.copyOf(new TreeSet<>(source));
    }
}
