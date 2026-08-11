package binnie.trees.item;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BinnieTreesFoodAssetsTest {
    @Test
    void everySourceFoodHasItsOwnModelAndTexture() throws Exception {
        Path resources = Path.of("src/main/resources");
        var catalog = JsonParser.parseString(Files.readString(
                resources.resolve("data/binnietrees/binnie_catalog/food.json"))).getAsJsonObject();
        var variants = catalog.getAsJsonArray("values");
        var root = JsonParser.parseString(Files.readString(
                resources.resolve("assets/binnietrees/models/item/food.json"))).getAsJsonObject();
        assertEquals(60, variants.size());
        assertEquals(60, root.getAsJsonArray("overrides").size());
        for (var value : variants) {
            String variant = value.getAsString();
            assertTrue(Files.isRegularFile(resources.resolve("assets/binnietrees/models/item/food/" + variant + ".json")),
                    "missing model for " + variant);
            assertTrue(Files.isRegularFile(resources.resolve("assets/binnietrees/textures/item/food/" + variant + ".png")),
                    "missing source texture for " + variant);
        }
        assertTrue(Files.isRegularFile(resources.resolve("assets/binnietrees/textures/block/fluid/beverage.png")));
        assertTrue(Files.isRegularFile(resources.resolve("assets/binnietrees/textures/block/fluid/beverage.png.mcmeta")));
    }
}
