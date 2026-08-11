package binnie.core.data;

import io.netty.buffer.ByteBuf;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

public record GenomeData(ResourceLocation speciesType, ResourceLocation species, Map<String, String> alleles, boolean analyzed, int generation) {
    private static final Codec<Map<String, String>> ALLELES = Codec.unboundedMap(Codec.STRING, Codec.STRING);
    public static final Codec<GenomeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("species_type").forGetter(GenomeData::speciesType),
            ResourceLocation.CODEC.fieldOf("species").forGetter(GenomeData::species),
            ALLELES.fieldOf("alleles").forGetter(GenomeData::alleles),
            Codec.BOOL.optionalFieldOf("analyzed", false).forGetter(GenomeData::analyzed),
            Codec.INT.optionalFieldOf("generation", 0).forGetter(GenomeData::generation)
    ).apply(instance, GenomeData::new));
    public static final StreamCodec<ByteBuf, GenomeData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    public GenomeData {
        alleles = Map.copyOf(alleles);
        if (generation < 0) throw new IllegalArgumentException("Generation cannot be negative");
    }
}
