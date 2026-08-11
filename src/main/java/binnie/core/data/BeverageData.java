package binnie.core.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BeverageData(String beverage, int color, int alcohol, int volume) {
    public static final Codec<BeverageData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("beverage").forGetter(BeverageData::beverage),
            Codec.INT.fieldOf("color").forGetter(BeverageData::color),
            Codec.intRange(0, 100).fieldOf("alcohol").forGetter(BeverageData::alcohol),
            Codec.intRange(1, 1000).fieldOf("volume").forGetter(BeverageData::volume)
    ).apply(instance, BeverageData::new));
    public static final StreamCodec<ByteBuf, BeverageData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
