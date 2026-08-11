package binnie.trees.jei;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BinnieTreesJeiCatalogTest {
    @Test
    void timedProcessorCategoriesHaveStableDistinctIds() {
        assertEquals(7, BinnieTreesJeiPlugin.MACHINES.size());
        assertEquals(7, BinnieTreesJeiPlugin.MACHINES.stream().distinct().count());
        assertTrue(BinnieTreesJeiPlugin.MACHINES.containsAll(java.util.List.of("press", "brewery", "distillery", "lumbermill", "woodworker", "panelworker", "glassworker")));
        assertEquals(BinnieTreesJeiPlugin.MACHINES.stream().sorted().toList(), BinnieTreesJeiPlugin.CLICK_AREAS.keySet().stream().sorted().toList());
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
}
