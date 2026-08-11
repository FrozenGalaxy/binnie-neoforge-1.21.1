package binnie.trees.item;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BinnieTreesFixedFluidAssetsTest {
    private static final List<String> LIQUIDS = List.of("resin", "turpentine");

    @Test
    void everySourceLiquidHasAWorldBlockBucketModelTextureAndTranslation() throws Exception {
        Path assets = Path.of("src/main/resources/assets/binnietrees");
        var language = JsonParser.parseString(Files.readString(assets.resolve("lang/en_us.json"))).getAsJsonObject();
        for (String liquid : LIQUIDS) {
            Path bucketModel = assets.resolve("models/item/" + liquid + "_bucket.json");
            Path legacyBlockItemModel = assets.resolve("models/item/" + liquid + ".json");
            Path blockModel = assets.resolve("models/block/" + liquid + ".json");
            Path blockstate = assets.resolve("blockstates/" + liquid + ".json");
            assertTrue(Files.isRegularFile(bucketModel), "missing bucket model for " + liquid);
            assertTrue(Files.isRegularFile(legacyBlockItemModel), "missing legacy fluid-block item model for " + liquid);
            assertTrue(Files.isRegularFile(blockModel), "missing block model for " + liquid);
            assertTrue(Files.isRegularFile(blockstate), "missing blockstate for " + liquid);
            assertTrue(Files.isRegularFile(assets.resolve("textures/block/fluid/" + liquid + ".png")),
                    "missing original fluid texture for " + liquid);
            assertTrue(Files.isRegularFile(assets.resolve("textures/block/fluid/" + liquid + ".png.mcmeta")),
                    "missing original animation metadata for " + liquid);

            var bucket = JsonParser.parseString(Files.readString(bucketModel)).getAsJsonObject();
            assertEquals("neoforge:fluid_container", bucket.get("loader").getAsString());
            assertEquals("binnietrees:" + liquid, bucket.get("fluid").getAsString());
            assertTrue(language.has("fluid.binnietrees." + liquid));
            assertTrue(language.has("item.binnietrees." + liquid + "_bucket"));
            assertTrue(language.has("block.binnietrees." + liquid));
        }
    }

    @Test
    void sourceLiquidsWithoutAcquisitionOrUseAreNotRegisteredOrPackaged() throws Exception {
        String source = Files.readString(Path.of("src/main/java/binnie/trees/registry/BinnieTreesFluids.java"));
        assertTrue(!source.contains("register(\"sap\""));
        assertTrue(!source.contains("register(\"latex\""));
        Path assets = Path.of("src/main/resources/assets/binnietrees");
        for (String removed : List.of("sap", "latex")) {
            assertTrue(!Files.exists(assets.resolve("models/item/" + removed + ".json")));
            assertTrue(!Files.exists(assets.resolve("models/item/" + removed + "_bucket.json")));
            assertTrue(!Files.exists(assets.resolve("models/block/" + removed + ".json")));
            assertTrue(!Files.exists(assets.resolve("blockstates/" + removed + ".json")));
            assertTrue(!Files.exists(assets.resolve("textures/block/fluid/" + removed + ".png")));
        }
    }

    @Test
    void everyRegisteredBeverageHasAPlaceableFluidAndBucketModel() throws Exception {
        Path assets = Path.of("src/main/resources/assets/binnietrees");
        var language = JsonParser.parseString(Files.readString(assets.resolve("lang/en_us.json"))).getAsJsonObject();
        int count = 0;
        for (var definition : binnie.trees.beverage.LegacyBeverageCatalog.definitions().values()) {
            if (!List.of("juice", "alcohol", "spirit", "liqueur").contains(definition.category())) continue;
            String id = definition.legacyIdentifier().toLowerCase(java.util.Locale.ROOT);
            assertTrue(Files.isRegularFile(assets.resolve("models/item/" + id + "_bucket.json")), id);
            assertTrue(Files.isRegularFile(assets.resolve("models/block/" + id + ".json")), id);
            assertTrue(Files.isRegularFile(assets.resolve("blockstates/" + id + ".json")), id);
            assertTrue(language.has("item.binnietrees." + id + "_bucket"), id);
            count++;
        }
        assertEquals(92, count);
    }

    @Test
    void allFifteenGlassShapesUseSeparateOriginalEmptyAndFilledModels() throws Exception {
        Path assets = Path.of("src/main/resources/assets/binnietrees");
        var catalog = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/binnietrees/binnie_catalog/glassware.json"))).getAsJsonObject();
        var values = catalog.getAsJsonArray("values");
        assertEquals(15, values.size());
        for (var value : values) {
            String id = value.getAsString();
            assertTrue(Files.isRegularFile(assets.resolve("models/item/glassware/" + id + "_empty.json")), id);
            assertTrue(Files.isRegularFile(assets.resolve("models/item/glassware/" + id + "_filled.json")), id);
            assertTrue(Files.isRegularFile(assets.resolve("textures/item/glassware/" + id + "_glass.png")), id);
            assertTrue(Files.isRegularFile(assets.resolve("textures/item/glassware/" + id + "_contents.png")), id);
        }
    }
}
