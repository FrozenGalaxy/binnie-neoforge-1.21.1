package binnie.genetics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** A discovered allele normalized independently of whether it came from an active or inactive chromosome slot. */
public record GeneticKnowledgeEntry(ResourceLocation speciesType, ResourceLocation chromosome,
                                    AlleleValue value, boolean dominant) implements Comparable<GeneticKnowledgeEntry> {
    public static final Codec<GeneticKnowledgeEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("species_type").forGetter(GeneticKnowledgeEntry::speciesType),
            ResourceLocation.CODEC.fieldOf("chromosome").forGetter(GeneticKnowledgeEntry::chromosome),
            AlleleValue.CODEC.fieldOf("allele").forGetter(GeneticKnowledgeEntry::value),
            Codec.BOOL.fieldOf("dominant").forGetter(GeneticKnowledgeEntry::dominant)
    ).apply(instance, GeneticKnowledgeEntry::new));
    public static final StreamCodec<ByteBuf, GeneticKnowledgeEntry> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public GeneticKnowledgeEntry {
        if (speciesType == null || chromosome == null || value == null) {
            throw new IllegalArgumentException("Genetic knowledge fields cannot be null");
        }
    }

    public static GeneticKnowledgeEntry fromAllele(GeneticAlleleData data) {
        return new GeneticKnowledgeEntry(data.speciesType(), data.chromosome(), data.value(), data.dominant());
    }

    public GeneticAlleleData asSample(GeneticAlleleData.Origin origin) {
        return new GeneticAlleleData(speciesType, chromosome, value, dominant, origin);
    }

    @Override
    public int compareTo(GeneticKnowledgeEntry other) {
        int result = speciesType.toString().compareTo(other.speciesType.toString());
        if (result != 0) return result;
        result = chromosome.toString().compareTo(other.chromosome.toString());
        if (result != 0) return result;
        result = value.type().compareTo(other.value.type());
        if (result != 0) return result;
        result = value.toString().compareTo(other.value.toString());
        if (result != 0) return result;
        return Boolean.compare(dominant, other.dominant);
    }
}
