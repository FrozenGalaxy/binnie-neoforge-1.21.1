package binnie.botany.api.genetics;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum SoilMoisture implements StringRepresentable {
    DRY,
    NORMAL,
    DAMP;

    public static final Codec<SoilMoisture> CODEC = StringRepresentable.fromEnum(SoilMoisture::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SoilMoisture byName(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }
}
