package binnie.trees.machine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DistilleryParityTest {
    @Test
    void moduleAlcoholRoutesRetainAllLevelSpecificOutputs() {
        var routes = BinnieTreesMachineProcesses.distillationRoutes();
        assertEquals(138, routes.size(), "23 alcohol families × six ModuleAlcohol distillery registrations");
    // DistilleryRecipeManager keeps all 138 registrations in recipeList,
    // while its level/input lookup maps collapse identical
    // later registrations. Both facts are source-visible.
        assertEquals(104, routes.stream().map(route -> route.inputBeverage() + ":" + route.mode()).distinct().count());
        var apple0 = routes.stream().filter(route -> route.inputBeverage().equals("binnietrees:alcohol/apple") && route.mode() == 0).findFirst().orElseThrow();
        assertEquals("apple_brandy", apple0.outputSpirit()); assertEquals(800, apple0.outputAmount());
        var apple2 = routes.stream().filter(route -> route.inputBeverage().equals("binnietrees:alcohol/apple") && route.mode() == 2).findFirst().orElseThrow();
        assertEquals("neutral_spirit", apple2.outputSpirit()); assertEquals(200, apple2.outputAmount());
    }

    @Test
    void beverageIdsKeepNamespaceOutOfTheLegacyCategory() {
        assertEquals(new BinnieTreesMachineProcesses.BeverageIdentity("alcohol", "apple"),
                BinnieTreesMachineProcesses.beverageIdentity("binnietrees:alcohol/apple"));
        assertThrows(IllegalArgumentException.class,
                () -> BinnieTreesMachineProcesses.beverageIdentity("binnietrees:alcohol:apple"));
    }
}
