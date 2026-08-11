package binnie.botany;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BotanySoilRecipeParityTest {
    @Test
    void gardeningRecipesCoverEverySourceSoilState() throws Exception {
        Path recipes = Path.of("src/main/resources/data/binniebotany/recipe/gardening");
        assertEquals(171, Files.list(recipes).filter(path -> path.toString().endsWith(".json")).count());
        for (String family : new String[] {"soil", "loam", "flowerbed"}) {
            for (String acidity : new String[] {"acid", "neutral", "alkaline"}) {
                for (String moisture : new String[] {"dry", "normal", "damp"}) {
                    Path recipe = recipes.resolve("weedkiller_" + family + '_' + acidity + '_' + moisture + ".json");
                    var json = JsonParser.parseString(Files.readString(recipe)).getAsJsonObject();
                    assertEquals(5, json.getAsJsonArray("ingredients").size());
                    assertEquals(4, json.getAsJsonObject("result").get("count").getAsInt());
                    assertTrue(json.toString().contains("binniebotany:weedkiller"));
                    assertTrue(json.toString().contains("weedkilled"));
                }
            }
        }
    }
}
