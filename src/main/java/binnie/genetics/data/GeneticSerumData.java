package binnie.genetics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** One or more typed genes with finite applications. */
public record GeneticSerumData(List<GeneticAlleleData> alleles, int quality, int charges) {
    private static final Codec<Integer> LEGACY_CHARGES_CODEC = Codec.intRange(0, 256)
            .xmap(value -> Math.min(value, 16), value -> value);
    public static final Codec<GeneticSerumData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GeneticAlleleData.CODEC.listOf().fieldOf("alleles").forGetter(GeneticSerumData::alleles),
            Codec.intRange(1, 100).optionalFieldOf("quality", 100).forGetter(GeneticSerumData::quality),
            LEGACY_CHARGES_CODEC.optionalFieldOf("charges", 16).forGetter(GeneticSerumData::charges)
    ).apply(instance, GeneticSerumData::new));
    public static final StreamCodec<ByteBuf, GeneticSerumData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public GeneticSerumData {
        alleles = List.copyOf(alleles);
        if (alleles.isEmpty()) throw new IllegalArgumentException("A serum requires at least one allele");
        var type = alleles.getFirst().speciesType();
        if (alleles.stream().anyMatch(allele -> !allele.speciesType().equals(type))) {
            throw new IllegalArgumentException("A serum array cannot mix organism types");
        }
        if (alleles.stream().map(GeneticAlleleData::chromosome).distinct().count() != alleles.size()) {
            throw new IllegalArgumentException("A serum array can contain only one allele per chromosome");
        }
        if (quality < 1 || quality > 100) {
            throw new IllegalArgumentException("Serum quality must be between 1 and 100");
        }
        if (charges < 0 || charges > 16) {
            throw new IllegalArgumentException("Serum charge must be between 0 and 16");
        }
    }

    public GeneticSerumData useCharge() {
        if (charges <= 0) throw new IllegalStateException("Serum is empty");
        return new GeneticSerumData(alleles, quality, charges - 1);
    }
}
