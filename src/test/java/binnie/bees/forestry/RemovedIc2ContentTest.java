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

/** Guards the intentional removal of obsolete IndustrialCraft 2-only Binnie's Bees content. */
final class RemovedIc2ContentTest {
    @Test
    void activeCatalogsExcludeIc2OnlyEntriesWithoutRemovingGenericRedstoneBees() throws Exception {
        Set<String> species = values("/data/binniebees/binnie_catalog/bee_species.json");
        Set<String> effects = values("/data/binniebees/binnie_catalog/bee_effect.json");
        Set<String> combs = values("/data/binniebees/binnie_catalog/honey_comb.json");
        Set<String> drops = values("/data/binniebees/binnie_catalog/honey_drop.json");

        assertFalse(species.contains("ecstatic"));
        assertFalse(effects.contains("power"));
        assertFalse(combs.contains("ic2_energy"));
        assertFalse(drops.contains("energy"));

        // These two are source-native Redstone/Lightning bees and do not depend on IC2.
        assertTrue(species.contains("excited"));
        assertTrue(species.contains("energetic"));
    }

    @Test
    void removedIc2OnlyResourcesAreNotPackaged() {
        assertMissing("/data/binniebees/bee_species/ecstatic.json");
        assertMissing("/data/binniebees/recipe/bee_mutation/ecstatic_1.json");
        assertMissing("/data/binniebees/recipe/centrifuge/ic2_energy_comb.json");
        assertMissing("/assets/binniebees/models/item/honey_crystal.json");
        assertMissing("/assets/binniebees/textures/item/honey_crystal.png");
    }

    @Test
    void remainingSpeciesProductsDoNotReferenceRemovedVariants() throws Exception {
        for (String id : values("/data/binniebees/binnie_catalog/bee_species.json")) {
            JsonObject species = read("/data/binniebees/bee_species/" + id + ".json");
            inspectProducts(id, species.getAsJsonArray("products"));
            inspectProducts(id, species.getAsJsonArray("specialties"));
        }
    }

    private static void inspectProducts(String species, JsonArray products) {
        if (products == null) return;
        for (var element : products) {
            JsonObject product = element.getAsJsonObject();
            if (!product.has("components")) continue;
            JsonObject components = product.getAsJsonObject("components");
            if (!components.has("binniecore:variant")) continue;
            String value = components.getAsJsonObject("binniecore:variant").get("value").getAsString();
            assertNotEquals("ic2_energy", value, species + " still produces the removed IC2 comb");
            assertNotEquals("energy", value, species + " still produces the removed IC2 energy drop");
        }
    }

    private static Set<String> values(String path) throws Exception {
        JsonArray values = read(path).getAsJsonArray("values");
        Set<String> result = new HashSet<>();
        values.forEach(value -> result.add(value.getAsString()));
        return result;
    }

    private static JsonObject read(String path) throws Exception {
        try (var stream = RemovedIc2ContentTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void assertMissing(String path) {
        assertNull(RemovedIc2ContentTest.class.getResource(path), "Removed resource is still packaged: " + path);
    }
}
