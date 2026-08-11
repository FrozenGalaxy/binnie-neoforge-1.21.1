package binnie.bees.forestry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Guards the generated species catalog against collapsing every comb to the default barren variant. */
final class LegacyBeeCombProductTest {
    private static final Set<String> INACTIVE_COMBS = Set.of(
            "bronze", "mint", "citrus", "peat", "brass", "electrum",
            "steel", "iridium", "olivine", "invar", "pulp", "mulch");
    private static final Set<String> INACTIVE_DROPS = Set.of("citrus", "fruit", "vegetable", "pumpkin", "melon");
    private static final Set<String> INACTIVE_PROPOLIS = Set.of("milk", "fruit", "seed", "alcohol", "glacial", "peat");

    @Test
    void everyHoneyCombProductCarriesAValidExplicitVariant() throws Exception {
        JsonObject index = read("/data/binniebees/binnie_catalog/bee_species.json");
        JsonArray known = read("/data/binniebees/binnie_catalog/honey_comb.json").getAsJsonArray("values");
        Set<String> variants = new HashSet<>();
        known.forEach(value -> variants.add(value.getAsString()));

        int combProducts = 0;
        Set<String> observed = new HashSet<>();
        for (var id : index.getAsJsonArray("values")) {
            JsonObject species = read("/data/binniebees/bee_species/" + id.getAsString() + ".json");
            combProducts += inspect(species.getAsJsonArray("products"), variants, observed);
            combProducts += inspect(species.getAsJsonArray("specialties"), variants, observed);
        }

        assertTrue(combProducts > 100, "Expected the full Binnie's Bees comb product matrix");
        assertTrue(observed.size() > 40, "Comb products collapsed to too few variants: " + observed);
        assertTrue(observed.contains("barren"));
        assertTrue(observed.contains("coal"));
        assertTrue(observed.contains("iron"));
        assertTrue(observed.contains("diamond"));
    }

    @Test
    void sourceInactiveProductsAreNeitherCataloguedNorGivenInventedRecipes() throws Exception {
        assertInactive("honey_comb", INACTIVE_COMBS, "centrifuge", "_comb");
        assertInactive("honey_drop", INACTIVE_DROPS, "squeezer", "_honey_drop");
        assertInactive("propolis", INACTIVE_PROPOLIS, "squeezer", "_propolis");
    }

    private static void assertInactive(String catalog, Set<String> inactive, String machine, String suffix) throws Exception {
        Set<String> registered = new HashSet<>();
        read("/data/binniebees/binnie_catalog/" + catalog + ".json").getAsJsonArray("values")
                .forEach(value -> registered.add(value.getAsString()));
        assertTrue(java.util.Collections.disjoint(registered, inactive),
                "Source-inactive " + catalog + " variants were exposed: " + registered.stream().filter(inactive::contains).toList());
        for (String value : inactive) {
            assertNull(LegacyBeeCombProductTest.class.getResource(
                    "/data/binniebees/recipe/" + machine + "/" + value + suffix + ".json"),
                    "Source-inactive product has an invented processing recipe: " + value);
        }
    }

    private static int inspect(JsonArray products, Set<String> variants, Set<String> observed) {
        if (products == null) return 0;
        int count = 0;
        for (var element : products) {
            JsonObject product = element.getAsJsonObject();
            if (!"binniebees:honey_comb".equals(product.get("item").getAsString())) continue;
            assertFalse(product.has("components"), "Forestry ignores the non-codec product field components: " + product);
            JsonObject tag = product.getAsJsonObject("tag");
            assertNotNull(tag, "Honey comb product is missing Forestry's product tag field: " + product);
            JsonObject component = tag.getAsJsonObject("binniecore:variant");
            assertNotNull(component, "Honey comb product is missing Forestry product tag binniecore:variant: " + product);
            assertEquals("binniebees.honey_comb", component.get("category").getAsString());
            String value = component.get("value").getAsString();
            assertTrue(variants.contains(value), "Unknown comb variant " + value);
            observed.add(value);
            count++;
        }
        return count;
    }

    private static JsonObject read(String path) throws Exception {
        try (var stream = LegacyBeeCombProductTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
