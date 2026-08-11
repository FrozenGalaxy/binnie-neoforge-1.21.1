package binnie.genetics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

/** 1.12 sequence vial state: typed allele, strength, sequencing percentage, and analysis flag. */
public record GeneticSequenceData(GeneticAlleleData allele, int quality, int sequenced, boolean analyzed) {
    public static final Codec<GeneticSequenceData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GeneticAlleleData.CODEC.fieldOf("allele").forGetter(GeneticSequenceData::allele),
            Codec.intRange(1, 100).optionalFieldOf("quality", 100).forGetter(GeneticSequenceData::quality),
            Codec.intRange(0, 100).optionalFieldOf("sequenced", 100).forGetter(GeneticSequenceData::sequenced),
            Codec.BOOL.optionalFieldOf("analyzed", false).forGetter(GeneticSequenceData::analyzed)
    ).apply(instance, GeneticSequenceData::new));
    public static final StreamCodec<ByteBuf, GeneticSequenceData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public GeneticSequenceData {
        allele = Objects.requireNonNull(allele, "allele");
        if (quality < 1 || quality > 100) throw new IllegalArgumentException("Sequence quality must be between 1 and 100");
        if (sequenced < 0 || sequenced > 100) throw new IllegalArgumentException("Sequence progress must be between 0 and 100");
    }

    public GeneticSequenceData(GeneticAlleleData allele, int quality) {
        this(allele, quality, 100, false);
    }

    public GeneticSequenceData(GeneticAlleleData allele, int quality, boolean analyzed) {
        this(allele, quality, 100, analyzed);
    }

    public static GeneticSequenceData unsequenced(GeneticAlleleData allele, int quality) {
        return new GeneticSequenceData(allele, quality, 0, false);
    }

    public GeneticSequenceData withSequenced(int percentage) {
        int bounded = Math.max(0, Math.min(100, percentage));
        return bounded == sequenced ? this : new GeneticSequenceData(allele, quality, bounded, analyzed);
    }

    public GeneticSequenceData analyzedCopy() {
        return analyzed ? this : new GeneticSequenceData(allele, quality, sequenced, true);
    }
}
