package binnie.bees.block;

import forestry.api.apiculture.IBeeModifier;

/** Active Binnie's Bees alveary components and their original GUI/inventory contracts. */
public enum AlvearyPartType {
    MUTATOR("mutator", 1, 176, true),
    FRAME("frame", 1, 144, true),
    RAIN_SHIELD("rain_shield", 0, 0, false),
    LIGHTING("lighting", 0, 0, false),
    STIMULATOR("stimulator", 1, 144, true),
    HATCHERY("hatchery", 5, 144, true),
    TRANSMISSION("transmission", 0, 0, false);

    private static final IBeeModifier NONE = new IBeeModifier() { };
    private static final IBeeModifier RAIN = new IBeeModifier() {
        @Override public boolean isSealed() { return true; }
    };
    private static final IBeeModifier LIGHT = new IBeeModifier() {
    /** Uses simulated sunlight because ForestryCE no longer has the self-lighted hook. */
        @Override public boolean isSunlightSimulated() { return true; }
    };

    private final String id;
    private final int inventorySlots;
    private final int guiHeight;
    private final boolean gui;

    AlvearyPartType(String id, int inventorySlots, int guiHeight, boolean gui) {
        this.id = id;
        this.inventorySlots = inventorySlots;
        this.guiHeight = guiHeight;
        this.gui = gui;
    }

    public String id() { return id; }
    public int inventorySlots() { return inventorySlots; }
    public int guiHeight() { return guiHeight; }
    public boolean hasGui() { return gui; }

    public IBeeModifier baseModifier() {
        return switch (this) {
            case RAIN_SHIELD -> RAIN;
            case LIGHTING -> LIGHT;
            default -> NONE;
        };
    }
}
