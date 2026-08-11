package binnie.core.data;

import io.netty.buffer.ByteBuf;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.StreamCodec;

public record VariantData(String category, String value) {
    public static final Codec<VariantData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("category").forGetter(VariantData::category),
            Codec.STRING.fieldOf("value").forGetter(VariantData::value)
    ).apply(instance, VariantData::new));
    public static final StreamCodec<ByteBuf, VariantData> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, VariantData::category,
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, VariantData::value,
                    VariantData::new);
    public VariantData {
        if (category.isBlank() || value.isBlank()) throw new IllegalArgumentException("Variant category and value must not be blank");
    }
}
