package binnie.core.machine;

/** Stable server-authored process diagnostics synchronized through machine menus. */
public enum MachineProcessError {
    NONE("none"),
    NO_ITEM("no_item"),
    NO_SPACE("no_space"),
    NO_SPACE_TANK("no_space_tank"),
    INVALID_ITEM("invalid_item"),
    NO_POWER("no_power"),
    TANK_FULL("tank_full"),
    TANK_EMPTY("tank_empty"),
    TANK_DIFFERENT_FLUID("tank_different_fluid"),
    NO_RECIPE("no_recipe"),
    INVALID_RECIPE("invalid_recipe"),
    REDSTONE_PAUSED("redstone_paused"),
    OUTPUT_BLOCKED("output_blocked"),
    OUTPUT_TANK_FULL("output_tank_full"),
    INVALID_INPUT("invalid_input"),
    NO_OWNER("no_owner"),
    NO_INDIVIDUAL("no_individual"),
    NO_SERUM("no_serum"),
    INVALID_SERUM("invalid_serum"),
    INVALID_SERUM_MISMATCH("invalid_serum_mismatch"),
    DEFUNCT_SERUM("defunct_serum"),
    EMPTY_SERUM("empty_serum"),
    NO_ENZYME("no_enzyme"),
    NO_DYE("no_dye"),
    NO_EMPTY_SEQUENCER("no_empty_sequencer"),
    INSUFFICIENT_ETHANOL("insufficient_ethanol"),
    INSUFFICIENT_DNA("insufficient_dna"),
    INSUFFICIENT_BACTERIA("insufficient_bacteria"),
    INSUFFICIENT_VECTOR("insufficient_vector"),
    INSUFFICIENT_LIQUID("insufficient_liquid"),
    ALREADY_ANALYZED("already_analyzed"),
    ITEM_FILLED("item_filled"),
    NO_SEQUENCE("no_sequence"),
    CANNOT_ACCLIMATISE("cannot_acclimatise"),
    NO_GENE("no_gene");

    private static final MachineProcessError[] VALUES = values();
    private final String serializedName;

    MachineProcessError(String serializedName) {
        this.serializedName = serializedName;
    }

    public String translationKey() {
        return "screen.binniecore.machine_error." + serializedName;
    }

    public static MachineProcessError byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }
}
