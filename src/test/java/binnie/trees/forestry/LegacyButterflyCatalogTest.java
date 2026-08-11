package binnie.trees.forestry;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runtime datapack and visual coverage for all active 1.12 Extra Trees butterflies. */
final class LegacyButterflyCatalogTest {
    @Test
    void everyLegacyButterflyIsARealForestryDatapackSpecies() throws Exception {
        assertEquals(22, LegacyButterflyCatalog.SPECIES.size());
        Path definitions = Path.of("src/main/resources/data/binnietrees/butterfly_species");
        assertEquals(22, Files.list(definitions).filter(path -> path.toString().endsWith(".json")).count());

        for (LegacyButterflyCatalog.Species species : LegacyButterflyCatalog.SPECIES) {
            Path definition = definitions.resolve("butterfly_" + species.id() + ".json");
            assertTrue(Files.isRegularFile(definition), "missing runtime species " + species.id());
            var json = JsonParser.parseString(Files.readString(definition)).getAsJsonObject();
            assertEquals(species.genus().toLowerCase(java.util.Locale.ROOT), json.get("genus").getAsString());
            assertEquals(species.species(), json.get("species").getAsString());
            assertEquals(species.color(), json.get("serum_color").getAsInt());
            assertEquals("Binnie", json.get("authority").getAsString());

            assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/binnietrees/textures/item/butterflies/"
                    + species.id() + ".png")), "missing item sprite " + species.id());
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/binnietrees/textures/entity/butterflies/"
                    + species.id() + ".png")), "missing entity texture " + species.id());
        }
    }

    @Test
    void meadowBrownAndRingletRemainConcreteLifecycleSpecies() {
        assertTrue(LegacyButterflyCatalog.SPECIES.stream().anyMatch(species -> species.id().equals("meadow_brown")));
        assertTrue(LegacyButterflyCatalog.SPECIES.stream().anyMatch(species -> species.id().equals("ringlet")));
        // Forestry's butterfly species type derives butterfly, caterpillar, cocoon and serum stacks
        // from each datapack species. They must not be represented as standalone Binnie serum items.
    }
}
