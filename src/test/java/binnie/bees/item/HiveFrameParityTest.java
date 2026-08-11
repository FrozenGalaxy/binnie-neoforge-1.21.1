package binnie.bees.item;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HiveFrameParityTest {
    @Test
    void visibleFrameSetMatchesActiveLegacyEnum() {
        assertEquals(Set.of("cocoa", "cage", "soul", "clay", "debug"), Set.copyOf(HiveFrameItem.VARIANTS));
        assertEquals(80, HiveFrameItem.maxDamage("soul"));
        assertEquals(240, HiveFrameItem.maxDamage("cocoa"));
        assertEquals(240, HiveFrameItem.maxDamage("cage"));
        assertEquals(240, HiveFrameItem.maxDamage("clay"));
        assertEquals(240, HiveFrameItem.maxDamage("debug"));
    }

    @Test
    void onlyTheFourCraftableLegacyFramesHaveRecipes() {
        Path root = Path.of("src/main/resources/data/binniebees/recipe/crafting");
        for (String id : Set.of("cocoa", "cage", "soul", "clay")) {
            assertTrue(Files.isRegularFile(root.resolve(id + "_frame.json")), id);
        }
        assertFalse(Files.exists(root.resolve("debug_frame.json")), "The legacy debug/Nova frame had no recipe");
    }

    @Test
    void coatedFramesUseTheLegacyFourItemCrossRatherThanAnEightItemRing() throws Exception {
        Path root = Path.of("src/main/resources/data/binniebees/recipe/crafting");
        for (String id : List.of("cocoa", "clay")) {
            var json = JsonParser.parseString(Files.readString(root.resolve(id + "_frame.json"))).getAsJsonObject();
            var pattern = json.getAsJsonArray("pattern");
            assertEquals(List.of(" C ", "CFC", " C "), List.of(
                    pattern.get(0).getAsString(), pattern.get(1).getAsString(), pattern.get(2).getAsString()), id);
        }
    }
}
