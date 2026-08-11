package binnie.core.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record DesignData(String design, String pattern, ResourceLocation primaryMaterial, ResourceLocation secondaryMaterial, int rotation, boolean mirrored,
                         String primaryVariant, String secondaryVariant) {
    public DesignData(String design, String pattern, ResourceLocation primaryMaterial, ResourceLocation secondaryMaterial, int rotation, boolean mirrored) {
        this(design, pattern, primaryMaterial, secondaryMaterial, rotation, mirrored, "", "");
    }
    public static final Codec<DesignData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("design").forGetter(DesignData::design),
            Codec.STRING.fieldOf("pattern").forGetter(DesignData::pattern),
            ResourceLocation.CODEC.fieldOf("primary_material").forGetter(DesignData::primaryMaterial),
            ResourceLocation.CODEC.fieldOf("secondary_material").forGetter(DesignData::secondaryMaterial),
            Codec.intRange(0, 3).optionalFieldOf("rotation", 0).forGetter(DesignData::rotation),
            Codec.BOOL.optionalFieldOf("mirrored", false).forGetter(DesignData::mirrored),
            Codec.STRING.optionalFieldOf("primary_variant", "").forGetter(DesignData::primaryVariant),
            Codec.STRING.optionalFieldOf("secondary_variant", "").forGetter(DesignData::secondaryVariant)
    ).apply(instance, DesignData::new));
    public static final StreamCodec<ByteBuf, DesignData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
