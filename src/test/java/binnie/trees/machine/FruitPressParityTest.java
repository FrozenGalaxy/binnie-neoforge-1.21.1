package binnie.trees.machine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Values transcribed from ModuleCore's squeezer registrations and ModuleAlcohol's crop-key copy loop. */
final class FruitPressParityTest {
    @Test
    void sourceOwnedRoutesAreCompleteAndUnique() {
        var routes = BinnieTreesMachineProcesses.fruitPressOperations();
        assertEquals(22, routes.size(), "active Binnie Food routes matched by Juice#getSqueezing");
        assertEquals(22, routes.stream().map(BinnieTreesMachineProcesses.FruitPressOperation::inputVariant).distinct().count());
        assertFalse(routes.stream().anyMatch(route -> route.inputVariant().equals("kumquat")), "cropKumquat is not Juice's cropOrange key");
        assertFalse(routes.stream().anyMatch(route -> route.inputVariant().equals("plantain")), "cropPlantain has no Juice squeezing key");
    }

    @Test
    void preservesExactAmountsAndSeedOilConversion() {
        Map<String, BinnieTreesMachineProcesses.FruitPressOperation> routes = BinnieTreesMachineProcesses.fruitPressOperations().stream()
                .collect(java.util.stream.Collectors.toMap(BinnieTreesMachineProcesses.FruitPressOperation::inputVariant, route -> route));
        assertRoute(routes, "grapefruit", "grapefruit", 500, 10, 15, false);
        assertRoute(routes, "cherry_plum", "plum", 100, 10, 60, false);
        assertRoute(routes, "wild_cherry", "cherry", 100, 20, 5, true);
        assertRoute(routes, "olive", "olive", 100, 20, 3, true);
        assertRoute(routes, "cranberry", "cranberry", 50, 10, 5, false);
    }

    @Test
    void legacyOreDictionaryInputsUseModernCommonTags() {
        var routes = BinnieTreesMachineProcesses.taggedFruitPressOperations();
        assertEquals(7, routes.size());
        assertTrue(routes.stream().anyMatch(route -> route.inputTag().toString().equals("c:crops/tomato")
                && route.juice().equals("tomato") && route.outputAmount() == 200));
        assertTrue(routes.stream().anyMatch(route -> route.inputTag().toString().equals("c:fruits/lemon")
                && route.juice().equals("lemon") && route.outputAmount() == 400));
    }

    private static void assertRoute(Map<String, BinnieTreesMachineProcesses.FruitPressOperation> routes, String input, String juice,
                                    int amount, int time, int mulch, boolean oil) {
        var route = routes.get(input);
        assertNotNull(route);
        assertEquals(juice, route.juice()); assertEquals(amount, route.outputAmount());
        assertEquals(time, route.sourceSqueezerTime()); assertEquals(mulch, route.sourceMulchChance()); assertEquals(oil, route.sourceWasSeedOil());
    }
}
