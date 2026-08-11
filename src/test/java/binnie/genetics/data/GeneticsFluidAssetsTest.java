package binnie.genetics.data;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeneticsFluidAssetsTest {
    @Test
    void everyLaboratoryFluidHasAWorldBlockAndForgeBucketModel() {
        Path assets = Path.of("src/main/resources/assets/binniegenetics");
        for (String id : List.of("growth_medium", "bacteria", "polymerising_bacteria", "raw_dna", "vector_bacteria")) {
            assertTrue(Files.isRegularFile(assets.resolve("blockstates/" + id + ".json")), id + " blockstate");
            assertTrue(Files.isRegularFile(assets.resolve("models/block/" + id + ".json")), id + " block model");
            assertTrue(Files.isRegularFile(assets.resolve("models/item/" + id + "_bucket.json")), id + " bucket model");
            assertTrue(Files.isRegularFile(assets.resolve("textures/block/fluid/" + id + ".png")), id + " texture");
        }
    }
}
