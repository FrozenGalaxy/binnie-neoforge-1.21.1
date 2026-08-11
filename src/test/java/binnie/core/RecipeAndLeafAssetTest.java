package binnie.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Regression coverage for broad glass tags, requested utility recipes, soils and 3D leaf items. */
final class RecipeAndLeafAssetTest {
    @Test
    void allBinnieRecipeGlassIngredientsUseAggregateTags() throws Exception {
        Path resources = Path.of(RecipeAndLeafAssetTest.class.getClassLoader().getResource("data").toURI());
        try (var paths = Files.walk(resources)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json") && p.toString().contains("/recipe/")).toList()) {
                JsonElement json = JsonParser.parseString(Files.readString(path));
                rejectGlass(json, resources.relativize(path).toString());
            }
        }
    }

    @Test
    void glassAuditTraversesObjectValuedRecipeFields() {
        JsonElement recipe = JsonParser.parseString("""
                {"item":{"ingredient":{"tag":"c:glass_blocks"}}}
                """);
        assertDoesNotThrow(() -> rejectGlass(recipe, "synthetic/object-valued-field.json"));
    }

    @Test
    void requestedMajorRecipesExistAndUseTags() {
        assertResult("data/binniegenetics/recipe/growth_medium.json", "binniegenetics:growth_medium", 2);
        assertResult("data/binniebotany/recipe/botanist_backpack.json", "binniebotany:botanist_backpack", 1);
        assertResult("data/binniebotany/recipe/soil.json", "binniebotany:soil", 4);
        assertResult("data/binniebotany/recipe/loam.json", "binniebotany:loam", 4);
        assertResult("data/binniebotany/recipe/flowerbed.json", "binniebotany:flowerbed", 4);

        assertIngredientTag("data/binniegenetics/recipe/growth_medium.json", "c:dyes/white");
        assertIngredientTag("data/binniebotany/recipe/botanist_backpack.json", "c:strings");
        assertIngredientTag("data/binniebotany/recipe/botanist_backpack.json", "minecraft:wool");
        assertIngredientTag("data/binniebotany/recipe/botanist_backpack.json", "c:chests/wooden");
        assertIngredientTag("data/binniebotany/recipe/botanist_backpack.json", "c:tools/trowel");
        assertIngredientTag("data/binniebotany/recipe/soil.json", "minecraft:dirt");
        assertIngredientTag("data/binniebotany/recipe/soil.json", "binniebotany:fertilisers/nutrient");
    }


    @Test
    void creativeAndDiagnosticToolsRemainIntentionallyUncraftable() {
        assertNull(RecipeAndLeafAssetTest.class.getClassLoader()
                .getResource("data/binniecore/recipe/genesis.json"),
                "Genesis is a creative custom-organism tool and must not have a survival recipe");
        assertNull(RecipeAndLeafAssetTest.class.getClassLoader()
                .getResource("data/binniegenetics/recipe/punnett_square.json"),
                "Punnett Square intentionally has no crafting recipe");
        assertNull(RecipeAndLeafAssetTest.class.getClassLoader()
                .getResource("data/binniegenetics/recipe/master_registry.json"),
                "Master Registry is a creative/developer tool and must not have a survival recipe");
        assertNull(RecipeAndLeafAssetTest.class.getClassLoader()
                .getResource("data/binniegenetics/recipe/master_database.json"),
                "Master Gene Database is a creative/developer tool and must not have a survival recipe");
    }

    @Test
    void decorativeLeavesUseDedicatedThreeDimensionalItemModels() throws Exception {
        JsonObject template = resource("assets/binnietrees/models/item/decorative_leaves_template.json");
        assertEquals("minecraft:block/block", template.get("parent").getAsString());
        assertEquals("side", template.get("gui_light").getAsString());
        JsonObject faces = template.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
        assertEquals(Set.of("down", "up", "north", "south", "west", "east"), faces.keySet());
        faces.entrySet().forEach(entry -> assertFalse(entry.getValue().getAsJsonObject().has("cullface"), entry.getKey()));

        Path root = Path.of(RecipeAndLeafAssetTest.class.getClassLoader().getResource("assets/binnietrees/models/item").toURI());
        long leaves = 0;
        try (var paths = Files.list(root)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().startsWith("decorative_leaves_")
                    && !p.getFileName().toString().equals("decorative_leaves_template.json")).toList()) {
                JsonObject model = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                assertEquals("binnietrees:item/decorative_leaves_template", model.get("parent").getAsString(), path.toString());
                assertTrue(model.getAsJsonObject("textures").has("all"), path.toString());
                leaves++;
            }
        }
        assertEquals(97, leaves);
    }

    private static void rejectGlass(JsonElement element, String path) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (hasString(object, "item")) {
                assertNotEquals("minecraft:glass", object.get("item").getAsString(), path);
                assertNotEquals("minecraft:glass_pane", object.get("item").getAsString(), path);
            }
            if (hasString(object, "tag")) {
                assertNotEquals("c:glass_blocks/colorless", object.get("tag").getAsString(), path);
                assertNotEquals("c:glass_panes/colorless", object.get("tag").getAsString(), path);
            }
            object.entrySet().forEach(entry -> rejectGlass(entry.getValue(), path));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> rejectGlass(child, path));
        }
    }

    private static boolean hasString(JsonObject object, String member) {
        JsonElement value = object.get(member);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }

    private static void assertResult(String path, String id, int count) {
        JsonObject result = resource(path).getAsJsonObject("result");
        assertEquals(id, result.get("id").getAsString(), path);
        assertEquals(count, result.get("count").getAsInt(), path);
    }

    private static void assertIngredientTag(String path, String expected) {
        JsonObject recipe = resource(path);
        assertTrue(containsTag(recipe, expected), path + " lacks tag " + expected);
    }

    private static boolean containsTag(JsonElement element, String tag) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("tag") && tag.equals(object.get("tag").getAsString())) return true;
            for (var entry : object.entrySet()) if (containsTag(entry.getValue(), tag)) return true;
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) if (containsTag(child, tag)) return true;
        }
        return false;
    }

    private static JsonObject resource(String path) {
        var stream = RecipeAndLeafAssetTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing resource " + path);
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
