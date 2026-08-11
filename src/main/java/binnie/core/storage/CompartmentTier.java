package binnie.core.storage;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** 1.12 storage tiers and capacities. */
public enum CompartmentTier implements StringRepresentable {
    PLAIN("plain", 4, 25),
    COPPER("copper", 6, 25),
    BRONZE("bronze", 8, 25),
    IRON("iron", 4, 50),
    GOLD("gold", 6, 50),
    DIAMOND("diamond", 8, 50);

    private final String serializedName;
    private final int tabs;
    private final int slotsPerTab;

    CompartmentTier(String serializedName, int tabs, int slotsPerTab) {
        this.serializedName = serializedName;
        this.tabs = tabs;
        this.slotsPerTab = slotsPerTab;
    }

    public int tabs() {
        return tabs;
    }

    public int slotsPerTab() {
        return slotsPerTab;
    }

    public int capacity() {
        return tabs * slotsPerTab;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public static CompartmentTier byName(String name) {
        if (name == null) return PLAIN;
        for (CompartmentTier tier : values()) {
            if (tier.serializedName.equals(name.toLowerCase(Locale.ROOT))) return tier;
        }
        return PLAIN;
    }
}
