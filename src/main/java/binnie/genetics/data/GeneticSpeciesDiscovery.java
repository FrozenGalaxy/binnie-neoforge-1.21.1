package binnie.genetics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** A discovered active or inactive species, associated with its genetic species type. */
public record GeneticSpeciesDiscovery(ResourceLocation speciesType, ResourceLocation species)
        implements Comparable<GeneticSpeciesDiscovery> {
    public static final Codec<GeneticSpeciesDiscovery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("species_type").forGetter(GeneticSpeciesDiscovery::speciesType),
            ResourceLocation.CODEC.fieldOf("species").forGetter(GeneticSpeciesDiscovery::species)
    ).apply(instance, GeneticSpeciesDiscovery::new));
    public static final StreamCodec<ByteBuf, GeneticSpeciesDiscovery> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public GeneticSpeciesDiscovery {
        if (speciesType == null || species == null) throw new IllegalArgumentException("Species discovery fields cannot be null");
    }

    @Override
    public int compareTo(GeneticSpeciesDiscovery other) {
        int result = speciesType.toString().compareTo(other.speciesType.toString());
        return result != 0 ? result : species.toString().compareTo(other.species.toString());
    }
}
