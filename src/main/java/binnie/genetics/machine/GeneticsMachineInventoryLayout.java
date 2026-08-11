package binnie.genetics.machine;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/**
  * 1.12 supplementary laboratory inventory groups.
  *
  * <p>The original 1.12 machines used their own differently-sized inventories.
  * The first NeoForge implementation established six persisted compatibility
  * slots for live processing before the 1.12 arrays were restored. Keeping
  * those slots at {@code 0..5} keeps existing saved machines; this layout
  * adds the 1.12 reserve, queue, exhausted, and completed groups after them.
  * The groups retain the 1.12 capacities and window positions.</p>
  */
public final class GeneticsMachineInventoryLayout {
    public static final int COMPATIBILITY_SLOTS = 6;

    public enum Group {
        INPUT_RESERVE,
        SERUM_RESERVE,
        SERUM_EXPENDED,
        FINISHED,
        QUEUE,
        INCUBATOR_OUTPUT,
        ACCLIMATISER
    }

    public enum SlotMode {
        INPUT,
        OUTPUT
    }

    public record SlotDefinition(int slot, int x, int y, Group group, SlotMode mode) {
    }

    public record Layout(int size, List<SlotDefinition> slots) {
        public List<Integer> slots(Group group) {
            return slots.stream().filter(slot -> slot.group() == group).map(SlotDefinition::slot).toList();
        }

        public Optional<SlotDefinition> slot(int index) {
            return slots.stream().filter(slot -> slot.slot() == index).findFirst();
        }
    }

    private GeneticsMachineInventoryLayout() {
    }

    public static int inventorySize(ResourceLocation id) {
        return forMachine(id).map(Layout::size).orElse(COMPATIBILITY_SLOTS);
    }

    public static List<SlotDefinition> extraSlots(ResourceLocation id) {
        return forMachine(id).map(Layout::slots).orElse(List.of());
    }

    public static List<Integer> slots(ResourceLocation id, Group group) {
        return forMachine(id).map(layout -> layout.slots(group)).orElse(List.of());
    }

    public static Optional<SlotDefinition> slot(ResourceLocation id, int slot) {
        return forMachine(id).flatMap(layout -> layout.slot(slot));
    }

    public static boolean isOutput(ResourceLocation id, int slot) {
        return slot(id, slot).map(definition -> definition.mode() == SlotMode.OUTPUT).orElse(false);
    }

    private static Optional<Layout> forMachine(ResourceLocation id) {
        if (!"binniegenetics".equals(id.getNamespace())) return Optional.empty();
        return Optional.of(switch (id.getPath()) {
            // WindowIsolator: reserve 3, then a 2x3 completed output group.
            case "isolator" -> layout(15,
                    input(6, 42, 35, Group.INPUT_RESERVE), input(7, 42, 53, Group.INPUT_RESERVE), input(8, 42, 71, Group.INPUT_RESERVE),
                    output(9, 278, 35, Group.FINISHED), output(10, 296, 35, Group.FINISHED),
                    output(11, 278, 53, Group.FINISHED), output(12, 296, 53, Group.FINISHED),
                    output(13, 278, 71, Group.FINISHED), output(14, 296, 71, Group.FINISHED));
            // WindowSequencer: a 2x2 unsequenced reserve; its completed slot is compatibility slot 3.
            case "sequencer" -> layout(10,
                    input(6, 16, 32, Group.INPUT_RESERVE), input(7, 34, 32, Group.INPUT_RESERVE),
                    input(8, 16, 50, Group.INPUT_RESERVE), input(9, 34, 50, Group.INPUT_RESERVE));
            // WindowPolymeriser: vertical serum reserve and a 2x2 completed group.
            case "polymeriser" -> layout(14,
                    input(6, 16, 38, Group.SERUM_RESERVE), input(7, 16, 56, Group.SERUM_RESERVE),
                    input(8, 16, 74, Group.SERUM_RESERVE), input(9, 16, 92, Group.SERUM_RESERVE),
                    output(10, 228, 47, Group.FINISHED), output(11, 246, 47, Group.FINISHED),
                    output(12, 228, 65, Group.FINISHED), output(13, 246, 65, Group.FINISHED));
            // WindowInoculator: two serum queues, two spent-serum slots, and 4-wide organism input/output rows.
            case "inoculator" -> layout(18,
                    input(6, 16, 32, Group.SERUM_RESERVE), input(7, 34, 32, Group.SERUM_RESERVE),
                    output(8, 104, 32, Group.SERUM_EXPENDED), output(9, 122, 32, Group.SERUM_EXPENDED),
                    input(10, 176, 32, Group.INPUT_RESERVE), input(11, 194, 32, Group.INPUT_RESERVE),
                    input(12, 212, 32, Group.INPUT_RESERVE), input(13, 230, 32, Group.INPUT_RESERVE),
                    output(14, 176, 84, Group.FINISHED), output(15, 194, 84, Group.FINISHED),
                    output(16, 212, 84, Group.FINISHED), output(17, 230, 84, Group.FINISHED));
            // Splicer's 1.12 constant includes five reserve indices while its 1.12 window renders four.
            // Keep all five backing slots; the fifth is not given a duplicate visual position.
            case "splicer" -> layout(19,
                    input(6, 16, 62, Group.SERUM_RESERVE), input(7, 34, 62, Group.SERUM_RESERVE),
                    output(8, 28, 84, Group.SERUM_EXPENDED), output(9, 46, 84, Group.SERUM_EXPENDED),
                    input(10, 104, 32, Group.INPUT_RESERVE), input(11, 122, 32, Group.INPUT_RESERVE),
                    input(12, 140, 32, Group.INPUT_RESERVE), input(13, 158, 32, Group.INPUT_RESERVE),
                    input(14, -1_000, -1_000, Group.INPUT_RESERVE),
                    output(15, 104, 116, Group.FINISHED), output(16, 122, 116, Group.FINISHED),
                    output(17, 140, 116, Group.FINISHED), output(18, 158, 116, Group.FINISHED));
            // WindowAnalyser: two 2x3 organism grids.
            case "analyser" -> layout(18,
                    input(6, 16, 32, Group.INPUT_RESERVE), input(7, 34, 32, Group.INPUT_RESERVE),
                    input(8, 16, 50, Group.INPUT_RESERVE), input(9, 34, 50, Group.INPUT_RESERVE),
                    input(10, 16, 68, Group.INPUT_RESERVE), input(11, 34, 68, Group.INPUT_RESERVE),
                    output(12, 168, 32, Group.FINISHED), output(13, 186, 32, Group.FINISHED),
                    output(14, 168, 50, Group.FINISHED), output(15, 186, 50, Group.FINISHED),
                    output(16, 168, 68, Group.FINISHED), output(17, 186, 68, Group.FINISHED));
            // WindowGenepool: a 2x3 individual reserve.
            case "genepool" -> layout(12,
                    input(6, 42, 35, Group.INPUT_RESERVE), input(7, 60, 35, Group.INPUT_RESERVE),
                    input(8, 42, 53, Group.INPUT_RESERVE), input(9, 60, 53, Group.INPUT_RESERVE),
                    input(10, 42, 71, Group.INPUT_RESERVE), input(11, 60, 71, Group.INPUT_RESERVE));
            // WindowIncubator: three queued inputs and three completed item outputs.
            case "incubator" -> layout(12,
                    input(6, 42, 35, Group.QUEUE), input(7, 42, 53, Group.QUEUE), input(8, 42, 71, Group.QUEUE),
                    output(9, 138, 35, Group.INCUBATOR_OUTPUT), output(10, 138, 53, Group.INCUBATOR_OUTPUT), output(11, 138, 71, Group.INCUBATOR_OUTPUT));
            // WindowAcclimatiser: 2x2 reserve, the third acclimatiser position, and 2x2 completed individuals.
            case "acclimatiser" -> layout(15,
                    input(6, 16, 32, Group.INPUT_RESERVE), input(7, 34, 32, Group.INPUT_RESERVE),
                    input(8, 16, 50, Group.INPUT_RESERVE), input(9, 34, 50, Group.INPUT_RESERVE),
                    input(10, 106, 68, Group.ACCLIMATISER),
                    output(11, 160, 32, Group.FINISHED), output(12, 178, 32, Group.FINISHED),
                    output(13, 160, 50, Group.FINISHED), output(14, 178, 50, Group.FINISHED));
            default -> new Layout(COMPATIBILITY_SLOTS, List.of());
        });
    }

    private static Layout layout(int size, SlotDefinition... slots) {
        return new Layout(size, List.copyOf(Arrays.asList(slots)));
    }

    private static SlotDefinition input(int slot, int x, int y, Group group) {
        return new SlotDefinition(slot, x, y, group, SlotMode.INPUT);
    }

    private static SlotDefinition output(int slot, int x, int y, Group group) {
        return new SlotDefinition(slot, x, y, group, SlotMode.OUTPUT);
    }
}
