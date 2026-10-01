package binnie.bees.forestry;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PrimevalConsolidationTest {
    @Test
    void forestryPrimevalReplacesDuplicateWithoutBreakingDescendantMutations() throws Exception {
        assertNull(getClass().getResource("/data/binniebees/bee_species/primeval.json"));
        assertNull(getClass().getResource("/data/binniebees/recipe/bee_mutation/primeval_1.json"));
        for (String recipe : List.of("prehistoric_1", "coal_1", "coal_2", "resin_1", "oil_1", "oil_2")) {
            try (var stream = getClass().getResourceAsStream("/data/binniebees/recipe/bee_mutation/" + recipe + ".json")) {
                assertNotNull(stream, recipe);
                var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                var parents = List.of(json.get("first").getAsString(), json.get("second").getAsString());
                assertTrue(parents.contains("forestry:primeval"), recipe);
                assertFalse(parents.contains("binniebees:primeval"), recipe);
            }
        }
    }
}
