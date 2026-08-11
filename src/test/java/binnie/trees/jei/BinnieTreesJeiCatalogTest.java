package binnie.trees.jei;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class BinnieTreesJeiCatalogTest {
    @Test
    void timedProcessorCategoriesHaveStableDistinctIds() {
        assertEquals(8, BinnieTreesJeiPlugin.MACHINES.size());
        assertEquals(8, BinnieTreesJeiPlugin.MACHINES.stream().distinct().count());
        assertTrue(BinnieTreesJeiPlugin.MACHINES.containsAll(java.util.List.of("press", "brewery", "distillery", "lumbermill", "woodworker", "panelworker", "glassworker", "bottler")));
        assertEquals(BinnieTreesJeiPlugin.MACHINES.stream().filter(machine -> !machine.equals("bottler")).sorted().toList(),
                BinnieTreesJeiPlugin.CLICK_AREAS.keySet().stream().sorted().toList());
        assertEquals(224, BinnieTreesJeiPlugin.CLICK_AREAS.get("distillery").titleGuiWidth());
        assertEquals(220, BinnieTreesJeiPlugin.CLICK_AREAS.get("lumbermill").titleGuiWidth());
        assertEquals(320, BinnieTreesJeiPlugin.CLICK_AREAS.get("woodworker").titleGuiWidth());
        BinnieTreesJeiPlugin.CLICK_AREAS.forEach((machine, area) ->
                assertTrue(area.isTitle()));
        assertEquals(new BinnieTreesJeiPlugin.ClickArea(92, 50, 18, 18, 0),
                BinnieTreesJeiPlugin.WORKER_OPERATION_AREA);
        assertEquals(92, BinnieTreesJeiPlugin.CLICK_AREAS.get("distillery").resolve(40).x());
        assertEquals(40, BinnieTreesJeiPlugin.CLICK_AREAS.get("distillery").resolve(40).width());
    }


    @Test
    void timedMachinesKeepTheirLegacyJeiGeometry() throws Exception {
        String source = Files.readString(Path.of("src/main/java/binnie/trees/jei/BinnieTreesJeiCategory.java"));
        assertTrue(source.contains("case \"press\" -> 74"));
        assertTrue(source.contains("case \"distillery\" -> 84"));
        assertTrue(source.contains("\"brewery\", \"lumbermill\" -> 130"));
        assertTrue(source.contains("fluid(builder, recipe.fluidOutputs(), 55, 1, false, 1000)"));
        assertTrue(source.contains("fluid(builder, recipe.fluidOutputs(), 113, 1, false, 1000)"));
        assertTrue(source.contains("fluid(builder, recipe.fluidOutputs(), 65, 1, false, 1000)"));
    }

    @Test
    void glasswareIsRegisteredAsAComponentBearingJeiSubtype() throws Exception {
        String source = Files.readString(Path.of("src/main/java/binnie/trees/jei/BinnieTreesJeiPlugin.java"));
        assertTrue(source.contains("registerSubtypeInterpreter(BinnieTreesContent.GLASSWARE.get(), variants)"));
        assertTrue(source.contains("fluidData.beverage()"));
        assertTrue(source.contains("registerSubtypeInterpreter(CoreContent.GLASS.get()"));
        assertTrue(source.contains("addBottlerRecipes(r)"));
        assertTrue(source.contains("for (String shape : BinnieTreesContent.GLASSWARE.get().variants())"));
    }

    @Test
    void binnieButterfliesExposeForestrysSerumLifeStageRatherThanDuplicatingSerumGe() throws Exception {
        String content = Files.readString(Path.of("src/main/java/binnie/trees/registry/BinnieTreesContent.java"));
        String adapters = Files.readString(Path.of("src/main/java/binnie/genetics/api/GeneticOrganismAdapters.java"));
        assertTrue(content.contains("getSpeciesType(ForestrySpeciesTypes.BUTTERFLY)"));
        assertTrue(content.contains("for (var stage : type.getLifeStages()) for (var species : type.getAllSpecies())"));
        assertTrue(adapters.contains("stage == ButterflyLifeStage.SERUM"));
        assertFalse(content.contains("SERUM_GE"), "serum_ge is a Forestry-owned butterfly life-stage item");
    }
}
