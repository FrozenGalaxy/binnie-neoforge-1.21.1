package binnie.botany.block;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Plant variants used for weeds and expired flowers. */
public enum GardenPlantType implements StringRepresentable {
    WEEDS,
    LONG_WEEDS,
    VERY_LONG_WEEDS,
    DEAD_FLOWER,
    DECAYING_FLOWER;

    public boolean isWeed() {
        return this == WEEDS || this == LONG_WEEDS || this == VERY_LONG_WEEDS;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
