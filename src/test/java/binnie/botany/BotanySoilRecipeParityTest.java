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

    @Test
    void jeiExplainsTrowelAcquisitionAndEveryLegacySoilTransformation() throws Exception {
        String plugin = Files.readString(Path.of("src/main/java/binnie/botany/jei/BotanyJeiPlugin.java"));
        String category = Files.readString(Path.of("src/main/java/binnie/botany/jei/GardeningJeiCategory.java"));
        assertTrue(plugin.contains("RecipeType.create(BinnieBotany.MOD_ID, \"gardening\""));
        assertTrue(plugin.contains("BotanyContent.TROWEL_WOOD"));
        assertTrue(plugin.contains("new ItemStack(Items.DIRT)"));
        assertTrue(plugin.contains("BotanyContent.WEEDKILLER"));
        assertTrue(plugin.contains("item(\"forestry:mulch\")"));
        assertTrue(plugin.contains("item(\"forestry:wood_pulp\")"));
        assertTrue(plugin.contains("item(\"forestry:ash\")"));
        assertTrue(plugin.contains("item(\"forestry:fertilizer\")"));
        assertTrue(plugin.contains("BotanyWorldContent.SOIL_ITEM.get(), components"));
        assertTrue(plugin.contains("BotanyWorldContent.LOAM_ITEM.get(), components"));
        assertTrue(plugin.contains("BotanyWorldContent.FLOWERBED_ITEM.get(), components"));
        assertTrue(category.contains("addItemStacks(recipe.inputs().get(i))"));
        assertTrue(plugin.contains("ArrayList<ItemStack> acquiredSoils"));
        assertTrue(plugin.contains("guide(\"acquire\""), "acquisition must be one cycling JEI recipe");

        String content = Files.readString(Path.of("src/main/java/binnie/botany/registry/BotanyContent.java"));
        assertTrue(content.contains("BotanyWorldContent.soilStack(item, moisture, acidity"));
        assertTrue(!content.contains("for (var item : BotanyWorldContent.LEGACY_SOIL_ITEMS) output.accept(item.get())"),
                "JEI-visible soil variants must be the real recipe-bearing block items, not compatibility aliases");
    }

    @Test
    void realSoilItemsRetainEveryLegacyVariantModelAndFarmNamesTranslate() throws Exception {
        for (String family : new String[] {"soil", "loam", "flowerbed"}) {
            var model = JsonParser.parseString(Files.readString(Path.of(
                    "src/main/resources/assets/binniebotany/models/item/" + family + ".json")))
                    .getAsJsonObject();
            assertEquals(18, model.getAsJsonArray("overrides").size(),
                    family + " must render acidity, moisture and weedkiller components");
            assertTrue(model.toString().contains("binniebotany:soil_variant"));
        }

        var lang = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/binniebotany/lang/en_us.json"))).getAsJsonObject();
        for (String id : binnie.botany.farming.BotanyFarmVariants.variantIds()) {
            String key = "farm.binniebotany.garden_" + id;
            assertTrue(lang.has(key), "missing Forestry farm-type translation " + key);
            assertTrue(!lang.get(key).getAsString().contains("Manual")
                            && !lang.get(key).getAsString().contains("Managed"),
                    "Forestry supplies the manual/managed grammar prefix itself");
        }
    }
}
