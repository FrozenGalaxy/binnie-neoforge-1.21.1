package binnie.trees.item;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BinnieTreesFixedFluidAssetsTest {
    private static final List<String> LIQUIDS = List.of("sap", "resin", "latex", "turpentine");

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
}
