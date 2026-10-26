package binnie.botany;

import binnie.botany.item.BotanyPigments;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

final class BotanyCeramicRecipeParityTest {
    @Test
    void sourcePigmentRecipesAndLegacyDisplayFormatsArePresent() {
        JsonObject dynamic = resource("data/binniebotany/recipe/flower_pigment.json");
        assertEquals("binniebotany:flower_pigment", dynamic.get("type").getAsString());

        assertNull(BotanyCeramicRecipeParityTest.class.getClassLoader()
                .getResource("data/binniebotany/recipe/pigment_black.json"),
                "The removed black pigment must not retain its legacy conversion recipe");
        assertEquals(15, BotanyPigments.substitutions().size());
        assertEquals(65, BotanyPigments.customColors().size());

        JsonObject language = resource("assets/binniebotany/lang/en_us.json");
        assertEquals("%s Pigment", language.get("item.binniebotany.pigment.name").getAsString());
        assertEquals("%s Clay", language.get("item.binniebotany.clay.name").getAsString());
        for (String color : binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color")) {
            assertTrue(language.has("item.binniebotany.pigment." + color), "Missing colour name " + color);
        }
    }

    @Test
    void everySourceColourUsesPigmentedClayThenExactSmelting() {
        var colors = binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color");
        assertEquals(80, colors.size());
        for (String color : colors) {
            JsonObject clay = resource("data/binniebotany/recipe/ceramic/" + color + ".json");
            assertEquals("minecraft:crafting_shapeless", clay.get("type").getAsString());
            assertEquals(4, clay.getAsJsonArray("ingredients").size());
            JsonObject pigment = clay.getAsJsonArray("ingredients").get(3).getAsJsonObject();
            assertPigmentIngredient(pigment, color);
            assertResult(clay, "binniebotany:clay", color, 1);

            JsonObject smelting = resource("data/binniebotany/recipe/ceramic_smelting/" + color + ".json");
            assertEquals("minecraft:smelting", smelting.get("type").getAsString());
            assertEquals(200, smelting.get("cookingtime").getAsInt());
            assertEquals(0.0F, smelting.get("experience").getAsFloat());
            assertComponentIngredient(smelting.getAsJsonObject("ingredient"), "binniebotany:clay", color);
            assertResult(smelting, "binniebotany:ceramic", color, 1);

            JsonObject stained = resource("data/binniebotany/recipe/stained/" + color + ".json");
            assertPigmentIngredient(stained.getAsJsonObject("key").getAsJsonObject("P"), color);
            assertResult(stained, "binniebotany:stained", color, 4);
        }
    }

    @Test
    void workerTagsContainTheSourceMaterialIdentities() {
        JsonObject wood = resource("data/binnietrees/tags/item/designer_wood_materials.json");
        JsonObject glass = resource("data/binnietrees/tags/item/designer_glass_materials.json");
        JsonObject ceramic = resource("data/binniebotany/tags/item/designer_ceramic_materials.json");
        assertEquals(142, wood.getAsJsonArray("values").size());
        assertEquals(17, glass.getAsJsonArray("values").size());
        assertEquals("binniebotany:ceramic", ceramic.getAsJsonArray("values").get(0).getAsString());
    }

    private static void assertComponentIngredient(JsonObject ingredient, String item, String color) {
        assertEquals("neoforge:components", ingredient.get("type").getAsString());
        assertEquals(item, ingredient.get("items").getAsString());
        JsonObject variant = ingredient.getAsJsonObject("components").getAsJsonObject("binniecore:variant");
        assertEquals("binniebotany.flower_color", variant.get("category").getAsString());
        assertEquals(color, variant.get("value").getAsString());
    }

    private static void assertPigmentIngredient(JsonObject ingredient, String color) {
        var substitute = BotanyPigments.substitutions().get(color);
        if (substitute != null) {
            assertEquals("c:dyes/" + color, ingredient.get("tag").getAsString());
            assertFalse(ingredient.has("components"));
        } else {
            assertComponentIngredient(ingredient, "binniebotany:pigment", color);
        }
    }

    private static void assertResult(JsonObject recipe, String item, String color, int count) {
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals(item, result.get("id").getAsString());
        assertEquals(count, result.get("count").getAsInt());
        assertEquals(color, result.getAsJsonObject("components")
                .getAsJsonObject("binniecore:variant").get("value").getAsString());
    }

    private static JsonObject resource(String path) {
        var stream = BotanyCeramicRecipeParityTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing resource " + path);
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
