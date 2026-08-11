package binnie.botany.block;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** 1.12 Botany soil families, ordered by increasing growing quality. */
public enum SoilQuality implements StringRepresentable {
    SOIL(1),
    LOAM(2),
    FLOWERBED(3);

    private final int initialFertility;

    SoilQuality(int initialFertility) {
        this.initialFertility = initialFertility;
    }

    public int initialFertility() {
        return initialFertility;
    }

    public SoilQuality next() {
        return switch (this) {
            case SOIL -> LOAM;
            case LOAM, FLOWERBED -> FLOWERBED;
        };
    }

    public SoilQuality previous() {
        return switch (this) {
            case SOIL, LOAM -> SOIL;
            case FLOWERBED -> LOAM;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
