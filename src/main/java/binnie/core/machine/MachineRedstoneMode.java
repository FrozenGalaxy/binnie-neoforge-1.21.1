package binnie.core.machine;

import net.minecraft.network.chat.Component;

/** 1.12-style machine redstone gating with a stable persisted ordinal. */
public enum MachineRedstoneMode {
    IGNORED("ignored"),
    REQUIRE_SIGNAL("require_signal"),
    REQUIRE_NO_SIGNAL("require_no_signal");

    private static final MachineRedstoneMode[] VALUES = values();
    private final String serializedName;

    MachineRedstoneMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public boolean allows(boolean powered) {
        return switch (this) {
            case IGNORED -> true;
            case REQUIRE_SIGNAL -> powered;
            case REQUIRE_NO_SIGNAL -> !powered;
        };
    }

    public Component displayName() {
        return Component.translatable("screen.binniecore.redstone." + serializedName);
    }

    public static MachineRedstoneMode byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : IGNORED;
    }

    public MachineRedstoneMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
