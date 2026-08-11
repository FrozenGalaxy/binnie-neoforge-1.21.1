package binnie.botany;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class BotanyItemModelTest {
    @Test
    void componentBackedCeramicItemsInheritNormalBlockItemTransforms() throws Exception {
        List<String> models = List.of(
                "ceramic", "stained", "ceramic_brick_tile", "ceramic_brick_brick",
                "ceramic_brick_brickstripe", "ceramic_brick_bricklarge", "ceramic_brick_split",
                "ceramic_brick_cheque", "ceramic_brick_mixed", "ceramic_brick_verticalbrick",
                "ceramic_brick_verticalbrickstripe", "ceramic_brick_verticalbricklarge");
        for (String model : models) {
            assertEquals("minecraft:block/block", read("/assets/binniebotany/models/block/" + model + ".json")
                    .get("parent").getAsString(), model);
        }
    }

    @Test
    void ceramicBrickItemSelectsEverySourcePatternModel() throws Exception {
        JsonObject item = read("/assets/binniebotany/models/item/ceramic_brick.json");
        assertEquals("binniebotany:block/ceramic_brick_tile", item.get("parent").getAsString());
        assertEquals(9, item.getAsJsonArray("overrides").size());
        for (int index = 0; index < 9; index++) {
            JsonObject override = item.getAsJsonArray("overrides").get(index).getAsJsonObject();
            assertEquals(index + 1, override.getAsJsonObject("predicate")
                    .get("binniebotany:ceramic_brick_type").getAsInt());
        }
    }

    private static JsonObject read(String path) throws Exception {
        var stream = BotanyItemModelTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
