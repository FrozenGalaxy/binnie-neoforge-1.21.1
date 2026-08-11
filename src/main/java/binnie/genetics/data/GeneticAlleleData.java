package binnie.genetics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** One typed active or inactive chromosome sample. */
public record GeneticAlleleData(ResourceLocation speciesType, ResourceLocation chromosome,
                                AlleleValue value, boolean dominant, Origin origin) {
    public enum Origin { ACTIVE, INACTIVE }

    public static final Codec<GeneticAlleleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("species_type").forGetter(GeneticAlleleData::speciesType),
            ResourceLocation.CODEC.fieldOf("chromosome").forGetter(GeneticAlleleData::chromosome),
            AlleleValue.CODEC.fieldOf("allele").forGetter(GeneticAlleleData::value),
            Codec.BOOL.fieldOf("dominant").forGetter(GeneticAlleleData::dominant),
            Codec.STRING.xmap(Origin::valueOf, Origin::name).optionalFieldOf("origin", Origin.ACTIVE).forGetter(GeneticAlleleData::origin)
    ).apply(instance, GeneticAlleleData::new));
    public static final StreamCodec<ByteBuf, GeneticAlleleData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public GeneticAlleleData {
        if (speciesType == null || chromosome == null || value == null || origin == null) {
            throw new IllegalArgumentException("Genetic allele fields cannot be null");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Allele<?> toAllele(IChromosome<?> target) {
        if (!target.id().equals(chromosome)) {
            throw new IllegalArgumentException("Sample chromosome " + chromosome + " cannot target " + target.id());
        }
        if (!value.isValidFor(target)) {
            throw new IllegalArgumentException("Allele value " + value + " is invalid for " + target.id());
        }
        return new Allele(value.javaValue(), dominant);
    }
}
