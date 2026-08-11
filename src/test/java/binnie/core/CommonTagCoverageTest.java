package binnie.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for common-tag interoperability and modern vanilla replacements. */
final class CommonTagCoverageTest {
    private static final Path DATA = Path.of("src/main/resources/data");

    @Test
    void everyBinnieDecorativeLeafIsInBothVanillaLeavesTags() throws Exception {
        Set<String> blocks = values("binnietrees/tags/block/decorative_leaves.json");
        Set<String> items = values("binnietrees/tags/item/decorative_leaves.json");
        assertEquals(97, blocks.size());
        assertEquals(blocks, items);
        assertEquals(Set.of("#binnietrees:decorative_leaves"), values("minecraft/tags/block/leaves.json"));
        assertEquals(Set.of("#binnietrees:decorative_leaves"), values("minecraft/tags/item/leaves.json"));
        for (String id : blocks) {
            String path = id.substring("binnietrees:".length());
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/binnietrees/models/block", path + ".json")), id);
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/binnietrees/models/item", path + ".json")), id);
        }
    }

    @Test
    void forestryAndCommonProductFamiliesIncludeBinnieItems() throws Exception {
        assertTrue(values("c/tags/item/fruits.json").contains("binnietrees:food"));
        assertTrue(values("c/tags/item/sawdust.json").contains("binnietrees:sawdust"));
        assertTrue(values("forestry/tags/item/combs.json").contains("binniebees:honey_comb"));
        assertTrue(values("forestry/tags/item/drop_honey.json").contains("binniebees:honey_drop"));
        assertTrue(values("forestry/tags/item/propolis.json").contains("binniebees:propolis"));
    }

    @Test
    void dustsUseSpecificCommonTagsAndCraftingConsumesThoseTags() throws Exception {
        List<String> dusts = List.of("coal", "iron", "gold", "silver", "platinum", "copper", "tin",
                "nickel", "lead", "zinc", "titanium", "tungsten", "clay", "uranium", "yellorium",
                "blutonium", "cyanite", "ash", "sulfur");
        Set<String> aggregate = values("c/tags/item/dusts.json");
        for (String dust : dusts) {
            assertTrue(aggregate.contains("#c:dusts/" + dust), dust);
            assertFalse(values("c/tags/item/dusts/" + dust + ".json").isEmpty(), dust);
        }
        for (String dust : List.of("coal", "iron", "gold", "copper", "tin")) {
            String suffix = dust.equals("coal") ? "coal" : "ingot";
            JsonObject recipe = json("binniebees/recipe/crafting/" + dust + "_dust_to_" + suffix + ".json");
            for (var ingredient : recipe.getAsJsonArray("ingredients")) {
                assertEquals("c:dusts/" + dust, ingredient.getAsJsonObject().get("tag").getAsString());
            }
        }
    }

    @Test
    void obsoletePreFlatteningDyesAreNotRegisteredOrPackaged() throws Exception {
        String registry = Files.readString(Path.of("src/main/java/binnie/bees/registry/BinnieBeesContent.java"));
        for (String color : List.of("red", "yellow", "blue", "green", "white", "black", "brown")) {
            String id = "dye_" + color;
            assertFalse(registry.contains('"' + id + '"'), id);
            assertFalse(Files.exists(Path.of("src/main/resources/assets/binniebees/models/item", id + ".json")), id);
            assertFalse(Files.exists(Path.of("src/main/resources/assets/binniebees/textures/item", id + ".png")), id);
        }
        Map<String, String> dyes = Map.ofEntries(
                Map.entry("red", "red"), Map.entry("yellow", "yellow"), Map.entry("blue", "blue"),
                Map.entry("green", "green"), Map.entry("white", "white"), Map.entry("black", "black"),
                Map.entry("brown", "brown"), Map.entry("orange", "orange"), Map.entry("cyan", "cyan"),
                Map.entry("purple", "purple"), Map.entry("gray", "gray"), Map.entry("lightblue", "light_blue"),
                Map.entry("pink", "pink"), Map.entry("limegreen", "lime"), Map.entry("magenta", "magenta"),
                Map.entry("lightgray", "light_gray"));
        for (var dye : dyes.entrySet()) {
            JsonObject recipe = json("binniebees/recipe/squeezer/" + dye.getKey() + "_honey_drop.json");
            assertEquals("minecraft:" + dye.getValue() + "_dye",
                    recipe.getAsJsonObject("remnant").get("id").getAsString(), dye.getKey());
        }
    }

    private static Set<String> values(String path) throws Exception {
        JsonArray values = json(path).getAsJsonArray("values");
        return values.asList().stream().map(value -> value.getAsString()).collect(Collectors.toSet());
    }

    private static JsonObject json(String path) throws Exception {
        return JsonParser.parseString(Files.readString(DATA.resolve(path))).getAsJsonObject();
    }
}
