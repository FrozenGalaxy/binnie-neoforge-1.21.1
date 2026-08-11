package binnie.trees.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class BreweryParityTest {
    @Test
    void moduleAlcoholRegistersAllLiquidAndGrainRoutes() {
        assertEquals(18, BinnieTreesMachineProcesses.breweryFermentations().size());
        assertEquals(10, BinnieTreesMachineProcesses.breweryGrainOperations().size());
        assertEquals(28, BinnieTreesMachineProcesses.breweryFermentations().size()
                + BinnieTreesMachineProcesses.breweryGrainOperations().size());
        assertEquals("citrus", BinnieTreesMachineProcesses.fermentationRoute("binnietrees:juice/grapefruit"));
        assertNull(BinnieTreesMachineProcesses.fermentationRoute("binnietrees:juice/olive"));
    }

    @Test
    void grainContractsRetainExactYeastIngredientAndOutputs() {
        var routes = BinnieTreesMachineProcesses.breweryGrainOperations();
        assertEquals(new BinnieTreesMachineProcesses.BreweryGrainOperation("barley", "hops", "yeast", "ale"), routes.get(0));
        assertEquals(new BinnieTreesMachineProcesses.BreweryGrainOperation("barley", "hops", "lager_yeast", "lager"), routes.get(1));
        assertEquals(new BinnieTreesMachineProcesses.BreweryGrainOperation("roasted", "hops", "yeast", "stout"), routes.get(2));
        assertEquals(new BinnieTreesMachineProcesses.BreweryGrainOperation("wheat", "", "yeast", "wheat"), routes.get(9));
        assertEquals(6, routes.stream().filter(route -> route.ingredient().equals("hops")).count());
        assertEquals(4, routes.stream().filter(route -> route.ingredient().isEmpty()).count());
    }
}
