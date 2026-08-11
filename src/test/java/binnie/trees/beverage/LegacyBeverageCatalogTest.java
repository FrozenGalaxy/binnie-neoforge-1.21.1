package binnie.trees.beverage;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import binnie.core.data.BeverageData;
import binnie.trees.machine.BinnieTreesMachineProcesses;
import binnie.trees.registry.BinnieTreesFluids;

final class LegacyBeverageCatalogTest {
    @Test
    void containsEverySourceDefinedBeverageExactlyOnce() {
        Map<String, LegacyBeverageCatalog.Definition> values = LegacyBeverageCatalog.definitions();
        assertEquals(107, values.size());
        assertEquals(Map.of(
                "alcohol", 29L,
                "cocktail", 29L,
                "juice", 19L,
                "misc_fluid", 8L,
                "spirit", 22L),
                values.values().stream().collect(Collectors.groupingBy(
                        LegacyBeverageCatalog.Definition::category, Collectors.counting())));
        Set<String> ids = values.values().stream().map(value -> value.id().toString()).collect(Collectors.toSet());
        assertEquals(107, ids.size());
        assertTrue(values.values().stream().allMatch(value -> value.alcohol() >= 0 && value.alcohol() <= 100));
    }

    @Test
    void fixedLegacyFluidsUseTheirExactBeverageNames() {
        assertEquals("binnietrees.fluid.juice.apple",
                LegacyBeverageCatalog.translationKey(new BeverageData("binnietrees:juice/apple", 0, 0, 100)));
        assertEquals("binnietrees.fluid.spirit.applebrandy",
                LegacyBeverageCatalog.translationKey(new BeverageData("binnietrees:spirit/apple_brandy", 0, 40, 100)));
        assertEquals("item.binnietrees.cocktail.bellini",
                LegacyBeverageCatalog.translationKey(new BeverageData("binnietrees:cocktail/bellini", 0, 7, 100)));
    }

    @Test
    void moduleAlcoholRegistersOnlyItsFourConcreteFluidEnums() {
        var registered = java.util.stream.StreamSupport.stream(BinnieTreesFluids.beverages().spliterator(), false)
                .map(fluid -> fluid.definition().category() + ":" + fluid.definition().value()).toList();
        assertEquals(70, registered.size());
        assertTrue(registered.contains("juice:apple"));
        assertTrue(registered.contains("alcohol:ale"));
        assertTrue(registered.contains("spirit:whiskey"));
        assertTrue(registered.contains("spirit:apple_liquor"));
        assertTrue(registered.stream().noneMatch(id -> id.startsWith("liqueur:")));
        assertTrue(registered.stream().noneMatch(id -> Set.of(
                "spirit:white_rum", "spirit:dark_rum", "spirit:fortified_wine",
                "spirit:cachaca", "spirit:gin").contains(id)));
        assertTrue(registered.stream().noneMatch(id -> id.startsWith("cocktail:") || id.startsWith("misc_fluid:")));
    }

    @Test
    void fruitFermentationAndDistillationFormOneRealMultiStageChain() {
        assertTrue(BinnieTreesMachineProcesses.fruitPressOperations().stream()
                .anyMatch(route -> route.inputVariant().equals("crabapple") && route.juice().equals("apple")));
        assertTrue(BinnieTreesMachineProcesses.breweryFermentations().stream()
                .anyMatch(route -> route.inputBeverage().equals("binnietrees:juice/apple")
                        && route.outputAlcohol().equals("apple")));
        assertTrue(BinnieTreesMachineProcesses.distillationRoutes().stream()
                .anyMatch(route -> route.inputBeverage().equals("binnietrees:alcohol/apple")
                        && route.outputSpirit().equals("apple_brandy") && route.mode() == 0));
    }

    @Test
    void completedFruitLiquorsRetainTheirInputFruitColorsInsteadOfLegacyPlaceholderGrey() {
        assertEquals(0xFABE3C, LegacyBeverageCatalog.get("spirit", "apple_liquor").color());
        assertEquals(0xE5D067, LegacyBeverageCatalog.get("spirit", "pear_liquor").color());
        assertEquals(0xAB0416, LegacyBeverageCatalog.get("spirit", "cherry_liquor").color());
        assertEquals(0x950001, LegacyBeverageCatalog.get("spirit", "elderberry_liquor").color());
        assertEquals(0xF0CF36, LegacyBeverageCatalog.get("spirit", "apricot_liquor").color());
        assertEquals(0xFABE3C, LegacyBeverageCatalog.get("spirit", "fruit_liquor").color());
    }

    @Test
    void everyBeverageFluidIdentityHasAConcreteEnglishName() throws Exception {
        try (var stream = LegacyBeverageCatalogTest.class.getResourceAsStream("/assets/binnietrees/lang/en_us.json")) {
            assertTrue(stream != null, "Binnie's Trees English language resource is absent");
            var language = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var definition : LegacyBeverageCatalog.definitions().values()) {
                String key = LegacyBeverageCatalog.translationKey(definition.data(250));
                assertTrue(language.has(key), "missing beverage name " + key);
                assertTrue(!"Beverage".equals(language.get(key).getAsString()), "generic beverage name " + key);
            }
        }
    }
}
