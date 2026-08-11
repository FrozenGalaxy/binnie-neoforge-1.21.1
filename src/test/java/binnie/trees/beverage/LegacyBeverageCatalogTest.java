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

final class LegacyBeverageCatalogTest {
    @Test
    void containsEverySourceDefinedBeverageExactlyOnce() {
        Map<String, LegacyBeverageCatalog.Definition> values = LegacyBeverageCatalog.definitions();
        assertEquals(129, values.size());
        assertEquals(Map.of(
                "alcohol", 29L,
                "cocktail", 29L,
                "juice", 19L,
                "liqueur", 17L,
                "misc_fluid", 8L,
                "spirit", 27L),
                values.values().stream().collect(Collectors.groupingBy(
                        LegacyBeverageCatalog.Definition::category, Collectors.counting())));
        Set<String> ids = values.values().stream().map(value -> value.id().toString()).collect(Collectors.toSet());
        assertEquals(129, ids.size());
        assertTrue(values.values().stream().allMatch(value -> value.alcohol() >= 0 && value.alcohol() <= 100));
    }

    @Test
    void componentBearingFluidsUseTheirExactBeverageNames() {
        assertEquals("binnietrees.fluid.juice.apple",
                LegacyBeverageCatalog.translationKey(new BeverageData("binnietrees:juice/apple", 0, 0, 100)));
        assertEquals("binnietrees.fluid.spirit.applebrandy",
                LegacyBeverageCatalog.translationKey(new BeverageData("binnietrees:spirit/apple_brandy", 0, 40, 100)));
        assertEquals("item.binnietrees.cocktail.bellini",
                LegacyBeverageCatalog.translationKey(new BeverageData("binnietrees:cocktail/bellini", 0, 7, 100)));
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
