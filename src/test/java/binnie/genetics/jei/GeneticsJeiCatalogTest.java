package binnie.genetics.jei;

import binnie.genetics.client.GeneticsJeiAreas;
import binnie.genetics.machine.GeneticsMachineProcesses;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeneticsJeiCatalogTest {
    @Test
    void compactCatalogCoversEveryLaboratoryMachine() {
        assertEquals(9, GeneticsJeiRecipes.MACHINES.size());
        assertEquals(9, GeneticsJeiRecipes.MACHINES.stream().distinct().count());
        assertTrue(GeneticsJeiRecipes.MACHINES.containsAll(java.util.List.of(
                "isolator", "sequencer", "polymeriser", "inoculator", "splicer",
                "analyser", "genepool", "incubator", "acclimatiser")));
        assertEquals(29, GeneticsJeiRecipes.EXPECTED_EXAMPLE_COUNT);
        assertEquals(GeneticsJeiRecipes.MACHINES.stream().sorted().toList(),
                GeneticsJeiAreas.AREAS.keySet().stream().sorted().toList());
    }

    @Test
    void displayedTimingsComeFromExecutionContracts() {
        assertEquals(4_800, GeneticsMachineProcesses.ISOLATOR_OPERATION.baseDuration());
        assertEquals(19_200, GeneticsMachineProcesses.SEQUENCER_OPERATION.baseDuration());
        assertEquals(2_400, GeneticsMachineProcesses.POLYMERISER_OPERATION.baseDuration());
        assertEquals(480, GeneticsMachineProcesses.POLYMERISER_ACCELERATED_OPERATION.baseDuration());
        assertEquals(12_000, GeneticsMachineProcesses.INOCULATOR_OPERATION.baseDuration());
        assertEquals(10_000, GeneticsMachineProcesses.SPLICER_OPERATION.energyPerTick());
        assertEquals(2, GeneticsMachineProcesses.INDEFINITE_OPERATION.energyPerTick());
        GeneticsJeiAreas.AREAS.values().forEach(area -> {
            var resolved = area.resolve(60);
            assertTrue(resolved.x() >= 0 && resolved.y() >= 0);
            assertEquals(60, resolved.width());
            assertEquals(12, resolved.height());
            assertTrue(resolved.x() + resolved.width() <= area.guiWidth());
            assertTrue(resolved.y() + resolved.height() <= 24);
        });
    }

    @Test
    void everyLaboratoryCategoryUsesTheForestryAnimatedProcessArrow() throws Exception {
        String category = Files.readString(Path.of(
                "src/main/java/binnie/genetics/jei/GeneticsJeiCategory.java"));
        assertTrue(category.contains("textures/gui/squeezersocket.png"));
        assertTrue(category.contains("createAnimatedDrawable"));
        assertTrue(category.contains("machine.equals(\"incubator\")"),
                "Incubator must retain its separately positioned process arrow");
        assertTrue(category.contains("this.processArrow.draw"));
    }
}
