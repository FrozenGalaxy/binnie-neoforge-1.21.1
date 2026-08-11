package binnie.core.machine;

import binnie.core.catalog.LegacyCatalogs;
import binnie.genetics.registry.GeneticsContent;
import binnie.genetics.registry.GeneticsMachines;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Locks the 1.12 machine recipes to current common tags without weakening matching progression components. */
final class MachineAcquisitionRecipeTagTest {
    private static final Map<String, String> TARGETS = new LinkedHashMap<>();
    private static final Set<String> FORBIDDEN_GENERIC_ITEMS = Set.of(
            "minecraft:glass",
            "minecraft:glass_pane",
            "minecraft:iron_ingot",
            "minecraft:gold_nugget",
            "minecraft:redstone",
            "minecraft:emerald",
            "minecraft:furnace",
            "minecraft:water_bucket",
            "minecraft:lava_bucket",
            "minecraft:blaze_rod",
            "forestry:bronze_gear",
            "forestry:tin_ingot"
    );

    static {
        for (String machine : GeneticsMachines.REGISTRY.specs().keySet()) {
            TARGETS.put("data/binniegenetics/recipe/machine_" + machine + ".json", "binniegenetics:" + machine);
        }
        TARGETS.put("data/binnietrees/recipe/machine_brewery.json", "binnietrees:brewery");
        TARGETS.put("data/binnietrees/recipe/machine_press.json", "binnietrees:press");
        TARGETS.put("data/binnietrees/recipe/machine_lumbermill.json", "binnietrees:lumbermill");
        TARGETS.put("data/binnietrees/recipe/machine_distillery.json", "binnietrees:distillery");
    }

    @Test
    void labStandUsesTheRealMachineBlockItemInsteadOfTheRemovedVariantItem() throws Exception {
        assertTrue(GeneticsMachines.REGISTRY.specs().containsKey("laboratory_machine"));
        assertTrue(LegacyCatalogs.values("binniegenetics.machine").contains("lab_machine"));
        assertFalse(LegacyCatalogs.values("binniegenetics.machine").contains("laboratory_machine"));
        assertEquals("laboratory_machine", GeneticsMachines.runtimeId("lab_machine"));
        assertThrows(NoSuchFieldException.class, () -> GeneticsContent.class.getField("LABORATORY_MACHINE"),
                "The obsolete variant item would collide with the Lab Stand block item registry id");
    }

    @Test
    void everyRequestedMachineHasAnAcquisitionRecipeWithTheCorrectResult() {
        assertEquals(14, TARGETS.size());
        TARGETS.forEach((path, result) -> {
            JsonObject recipe = resource(path);
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), path);
            assertEquals(result, recipe.getAsJsonObject("result").get("id").getAsString(), path);
            assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt(), path);
        });
    }

    @Test
    void genericEcosystemIngredientsUseCommonTags() {
        TARGETS.forEach((path, ignored) -> assertNoForbiddenGenericItems(resource(path), path));

        assertTag("data/binniegenetics/recipe/machine_incubator.json", "F", "c:player_workstations/furnaces");
        assertTag("data/binniegenetics/recipe/machine_acclimatiser.json", "B", "c:buckets/lava");
        assertTag("data/binniegenetics/recipe/machine_acclimatiser.json", "a", "c:buckets/water");
        assertTag("data/binniegenetics/recipe/machine_splicer.json", "a", "c:rods/blaze");

        for (String path : TARGETS.keySet()) {
            JsonObject recipe = resource(path);
            JsonObject key = recipe.getAsJsonObject("key");
            for (Map.Entry<String, JsonElement> entry : key.entrySet()) {
                JsonObject ingredient = entry.getValue().getAsJsonObject();
                if (ingredient.has("tag") && ingredient.get("tag").getAsString().startsWith("c:glass_panes")) {
                    assertEquals("c:glass_panes", ingredient.get("tag").getAsString(), path + " key " + entry.getKey());
                }
            }
        }

        assertTag("data/binnietrees/recipe/machine_brewery.json", "b", "c:gears/bronze");
        assertTag("data/binnietrees/recipe/machine_brewery.json", "i", "c:ingots/iron");
        assertTag("data/binnietrees/recipe/machine_press.json", "t", "c:ingots/tin");
        assertTag("data/binnietrees/recipe/machine_distillery.json", "r", "c:dusts/redstone");
        assertTag("data/binnietrees/recipe/machine_lumbermill.json", "G", "c:glass_blocks");
    }

    @Test
    void exactTieredComponentsRemainExactItems() {
        assertItem("data/binniegenetics/recipe/machine_incubator.json", "C", "binniegenetics:laboratory_casing");
        assertItem("data/binniegenetics/recipe/machine_incubator.json", "c", "forestry:enhanced_circuit_board");
        assertItem("data/binniegenetics/recipe/machine_analyser.json", "B", "forestry:portable_analyzer");
        assertItem("data/binnietrees/recipe/machine_lumbermill.json", "g", "binnietrees:proven_gear");
        assertItem("data/binnietrees/recipe/machine_lumbermill.json", "A", "minecraft:iron_axe");
        assertItem("data/binnietrees/recipe/machine_lumbermill.json", "s", "forestry:sturdy_casing");
    }

    private static void assertNoForbiddenGenericItems(JsonElement element, String path) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("item")) {
                String item = object.get("item").getAsString();
                assertFalse(FORBIDDEN_GENERIC_ITEMS.contains(item), path + " hard-codes generic ingredient " + item);
            }
            object.entrySet().forEach(entry -> assertNoForbiddenGenericItems(entry.getValue(), path));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> assertNoForbiddenGenericItems(child, path));
        }
    }

    private static void assertTag(String path, String key, String tag) {
        JsonObject ingredient = resource(path).getAsJsonObject("key").getAsJsonObject(key);
        assertEquals(tag, ingredient.get("tag").getAsString(), path + " key " + key);
        assertFalse(ingredient.has("item"), path + " key " + key);
    }

    private static void assertItem(String path, String key, String item) {
        JsonObject ingredient = resource(path).getAsJsonObject("key").getAsJsonObject(key);
        assertEquals(item, ingredient.get("item").getAsString(), path + " key " + key);
        assertFalse(ingredient.has("tag"), path + " key " + key);
    }

    private static JsonObject resource(String path) {
        var stream = MachineAcquisitionRecipeTagTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing resource " + path);
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
