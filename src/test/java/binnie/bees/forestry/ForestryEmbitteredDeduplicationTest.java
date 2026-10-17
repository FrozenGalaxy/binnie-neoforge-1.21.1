package binnie.bees.forestry;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ForestryEmbitteredDeduplicationTest {
    @Test
    void duplicateBasaltBeeAndNetherHiveAreRemoved() throws Exception {
        assertFalse(Files.exists(Path.of("src/main/resources/data/binniebees/bee_species/basalt.json")));
        assertFalse(Files.exists(Path.of("src/main/resources/data/binniebees/loot_table/blocks/nether_hive.json")));

        var species = JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/data/binniebees/binnie_catalog/bee_species.json"))).getAsJsonObject();
        assertFalse(species.getAsJsonArray("values").asList().stream()
                .anyMatch(value -> value.getAsString().equals("basalt")));

        Path mutations = Path.of("src/main/resources/data/binniebees/recipe/bee_mutation");
        try (var files = Files.list(mutations)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                String contents = Files.readString(file);
                assertFalse(contents.contains("binniebees:basalt"), file.toString());
            }
        }

        for (String id : Set.of("sinister_1", "fiendish_1", "tempered_1")) {
            var mutation = JsonParser.parseString(Files.readString(mutations.resolve(id + ".json"))).getAsJsonObject();
            assertTrue(mutation.get("first").getAsString().equals("forestry:embittered")
                    || mutation.get("second").getAsString().equals("forestry:embittered"), id);
        }
    }
}
