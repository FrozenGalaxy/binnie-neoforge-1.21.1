package binnie.core.machine;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** 1.12 Binnie's Trees machine window sizes and slot coordinates. */
public record MachineUiLayout(
        int width,
        int height,
        Point input,
        Point catalyst,
        Point reagent,
        Point output,
        Point secondaryOutput,
        Point tertiaryOutput,
        Point playerInventory
) {
    public static final Point HIDDEN = new Point(-1000, -1000);

    public record Point(int x, int y) {
    /** 1.12 ControlSlot positions describe the 18px frame; vanilla slots describe the 16px item area. */
        public Point itemArea() {
            return this == HIDDEN || x < 0 ? HIDDEN : new Point(x + 1, y + 1);
        }
    }

    public static Optional<MachineUiLayout> forMachine(ResourceLocation id) {
        if (!id.getNamespace().equals("binnietrees")) return Optional.empty();
        return Optional.ofNullable(switch (id.getPath()) {
            case "press" -> new MachineUiLayout(194, 192,
                    p(24, 52), HIDDEN, HIDDEN, HIDDEN, HIDDEN, HIDDEN, p(16, 104));
            case "brewery" -> new MachineUiLayout(228, 218,
                    p(16, 41), p(42, 32), p(105, 77), HIDDEN, HIDDEN, HIDDEN, p(33, 130));
            case "distillery" -> new MachineUiLayout(224, 192,
                    HIDDEN, HIDDEN, HIDDEN, HIDDEN, HIDDEN, HIDDEN, p(31, 104));
            case "lumbermill" -> new MachineUiLayout(220, 192,
                    p(42, 43), HIDDEN, HIDDEN, p(148, 43), p(172, 28), p(172, 58), p(29, 104));
            default -> null;
        });
    }

    private static Point p(int x, int y) {
        return new Point(x, y);
    }
}
