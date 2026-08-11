package binnie.genetics.machine;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeneticsMachineLayoutTest {
    @Test
    void sourceWindowsRetainTheirDistinctLegacyDimensions() {
        Map<String, int[]> expected = Map.of(
                "isolator", new int[] {330, 208},
                "sequencer", new int[] {226, 224},
                "polymeriser", new int[] {278, 212},
                "inoculator", new int[] {266, 240},
                "splicer", new int[] {280, 240},
                "analyser", new int[] {220, 210},
                "genepool", new int[] {280, 198},
                "incubator", new int[] {228, 196},
                "acclimatiser", new int[] {280, 198}
        );

        expected.forEach((id, dimensions) -> {
            GeneticsMachineLayout.Layout layout = GeneticsMachineLayout.forMachine(ResourceLocation.fromNamespaceAndPath("binniegenetics", id))
                    .orElseThrow();
            assertEquals(dimensions[0], layout.width(), id);
            assertEquals(dimensions[1], layout.height(), id);
            assertTrue(layout.inputX() != 26 || layout.inputY() != 35, id + " retained the generic core input placement");
        });
    }

    @Test
    void nonGeneticsMachinesCannotAccidentallySelectTheGeneticsScreen() {
        assertTrue(GeneticsMachineLayout.forMachine(ResourceLocation.fromNamespaceAndPath("binnietrees", "press")).isEmpty());
    }

    @Test
    void playerInventoryAndPrimarySlotsUseSourceWindowCoordinates() {
        record Expected(int inputX, int inputY, int playerX, int playerY) {}
        Map<String, Expected> expected = Map.of(
                "isolator", new Expected(80, 35, 84, 120),
                "sequencer", new Expected(112, 48, 32, 136),
                "polymeriser", new Expected(50, 38, 58, 124),
                "inoculator", new Expected(176, 58, 52, 152),
                "splicer", new Expected(131, 74, 59, 152),
                "analyser", new Expected(101, 48, 29, 122),
                "genepool", new Expected(98, 53, 59, 110),
                "incubator", new Expected(-1_000, -1_000, 33, 108),
                "acclimatiser", new Expected(88, 32, 59, 110)
        );

        expected.forEach((id, value) -> {
            GeneticsMachineLayout.Layout layout = GeneticsMachineLayout.forMachine(ResourceLocation.fromNamespaceAndPath("binniegenetics", id))
                    .orElseThrow();
            assertEquals(value.inputX(), layout.inputX(), id + " input x");
            assertEquals(value.inputY(), layout.inputY(), id + " input y");
            assertEquals(value.playerX(), layout.playerX(), id + " player inventory x");
            assertEquals(value.playerY(), layout.playerY(), id + " player inventory y");
        });
    }

    @Test
    void sourceLaboratoriesExposeTheirCompleteSupplementaryInventoryGroups() {
        record Expected(int slots, int inputReserve, int serumReserve, int completed) {}
        Map<String, Expected> expected = Map.of(
                "isolator", new Expected(15, 3, 0, 6),
                "sequencer", new Expected(10, 4, 0, 0),
                "polymeriser", new Expected(14, 0, 4, 4),
                "inoculator", new Expected(18, 4, 2, 4),
                "splicer", new Expected(19, 5, 2, 4),
                "analyser", new Expected(18, 6, 0, 6),
                "genepool", new Expected(12, 6, 0, 0),
                "incubator", new Expected(12, 3, 0, 3),
                "acclimatiser", new Expected(15, 4, 0, 4)
        );

        expected.forEach((id, value) -> {
            ResourceLocation machine = ResourceLocation.fromNamespaceAndPath("binniegenetics", id);
            assertEquals(value.slots(), GeneticsMachineInventoryLayout.inventorySize(machine), id);
            int inputReserve = id.equals("incubator")
                    ? GeneticsMachineInventoryLayout.slots(machine, GeneticsMachineInventoryLayout.Group.QUEUE).size()
                    : GeneticsMachineInventoryLayout.slots(machine, GeneticsMachineInventoryLayout.Group.INPUT_RESERVE).size();
            assertEquals(value.inputReserve(), inputReserve, id + " input reserve");
            assertEquals(value.serumReserve(), GeneticsMachineInventoryLayout.slots(machine,
                    GeneticsMachineInventoryLayout.Group.SERUM_RESERVE).size(), id + " serum reserve");
            int completed = id.equals("incubator")
                    ? GeneticsMachineInventoryLayout.slots(machine, GeneticsMachineInventoryLayout.Group.INCUBATOR_OUTPUT).size()
                    : GeneticsMachineInventoryLayout.slots(machine, GeneticsMachineInventoryLayout.Group.FINISHED).size();
            assertEquals(value.completed(), completed, id + " completed");
        });
        assertEquals(1, GeneticsMachineInventoryLayout.slots(ResourceLocation.fromNamespaceAndPath("binniegenetics", "acclimatiser"),
                GeneticsMachineInventoryLayout.Group.ACCLIMATISER).size());
    }
}
