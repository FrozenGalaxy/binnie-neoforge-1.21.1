package binnie.botany.api.genetics;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum SoilAcidity implements StringRepresentable {
    ACID,
    NEUTRAL,
    ALKALINE;

    public static final Codec<SoilAcidity> CODEC = StringRepresentable.fromEnum(SoilAcidity::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SoilAcidity byName(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }
}
