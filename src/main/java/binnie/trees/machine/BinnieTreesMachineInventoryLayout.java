package binnie.trees.machine;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Supplementary 1.12 slots for Binnie's Trees packages that exceed the old six-slot compatibility layout. */
public final class BinnieTreesMachineInventoryLayout {
    public enum Mode { INPUT, PROCESS_RESERVE }
    public record SlotDefinition(int slot, int x, int y, Mode mode) {}
    private BinnieTreesMachineInventoryLayout() {}
    public static int inventorySize(ResourceLocation id) {
        if (!"binnietrees".equals(id.getNamespace())) return 6;
        return switch (id.getPath()) { case "press" -> 7; case "brewery" -> 17; default -> 6; };
    }
    public static List<SlotDefinition> extraSlots(ResourceLocation id) {
        if (!"binnietrees".equals(id.getNamespace())) return List.of();
        if ("press".equals(id.getPath())) return List.of(new SlotDefinition(6, -1000, -1000, Mode.PROCESS_RESERVE));
        if (!"brewery".equals(id.getPath())) return List.of();
    // MachineMenu positions vanilla Slot item areas, while the 1.12
    // ControlSlotArray coordinates denote the surrounding 18px frame.
    // The first grain comes through MachineUiLayout#itemArea (43,33);
    // keep its two siblings in that same coordinate space.
        return List.of(new SlotDefinition(6, 43, 51, Mode.INPUT), new SlotDefinition(7, 43, 69, Mode.INPUT),
                new SlotDefinition(8, 33, 104, Mode.PROCESS_RESERVE), new SlotDefinition(9, 51, 104, Mode.PROCESS_RESERVE),
                new SlotDefinition(10, 69, 104, Mode.PROCESS_RESERVE), new SlotDefinition(11, 87, 104, Mode.PROCESS_RESERVE),
                new SlotDefinition(12, 105, 104, Mode.PROCESS_RESERVE), new SlotDefinition(13, 123, 104, Mode.PROCESS_RESERVE),
                new SlotDefinition(14, 141, 104, Mode.PROCESS_RESERVE), new SlotDefinition(15, 159, 104, Mode.PROCESS_RESERVE),
                new SlotDefinition(16, 177, 104, Mode.PROCESS_RESERVE));
    }
}
