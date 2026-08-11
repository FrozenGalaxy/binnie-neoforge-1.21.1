package binnie.design.data;

import binnie.core.data.DesignData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DesignCatalogTest {
    @Test
    void preservesEveryLegacyDesignAndPatternWithoutDuplicateIds() {
        assertEquals(101, DesignCatalog.DESIGNS.size());
        assertEquals(101, DesignCatalog.DESIGNS.stream().distinct().count());
        assertEquals(213, DesignCatalog.PATTERNS.size());
        assertEquals(213, DesignCatalog.PATTERNS.stream().distinct().count());
        assertTrue(DesignCatalog.isDesign("letter_z"));
        assertTrue(DesignCatalog.isPattern("overlapped_split_barred2"));
    }

    @Test
    void selectorCategoriesUseExactModernIdsAndExcludeTheUnregisteredTestDesign() {
        List<String> categorized = DesignCatalog.CATEGORY_ORDER.stream()
                .flatMap(category -> category.getValue().stream()).toList();
        assertEquals(100, categorized.size());
        assertEquals(100, new LinkedHashSet<>(categorized).size(), "A design must appear in one selector category");
        assertTrue(DesignCatalog.DESIGNS.containsAll(categorized));
        assertEquals(List.of("test"), DesignCatalog.DESIGNS.stream()
                .filter(design -> !categorized.contains(design)).toList());

        var categories = DesignCatalog.CATEGORY_ORDER.stream()
                .collect(java.util.stream.Collectors.toMap(java.util.Map.Entry::getKey,
                        java.util.Map.Entry::getValue, (first, ignored) -> first, java.util.LinkedHashMap::new));
        assertEquals(26, categories.get("Letters").size());
        assertEquals("letter_a", categories.get("Letters").getFirst());
        assertEquals("letter_z", categories.get("Letters").getLast());
        assertTrue(categories.get("Designs & Emblems").contains("emblem1"));
        assertTrue(categories.get("Squares & Stripes").contains("chequered_b"));
        assertTrue(categories.get("Edges").contains("corner"));
        assertTrue(categories.get("Diagonals").contains("diagonal1_edged"));
        assertFalse(categories.get("Bars").contains("emblem1"));
    }

    @Test
    void exactLegacyLayoutsAndPatternFallbacksRemainDistinct() {
        DesignData exact = new DesignData("diamond", "diamond", ResourceLocation.withDefaultNamespace("oak_planks"),
                ResourceLocation.withDefaultNamespace("birch_planks"), 0, false);
        DesignData custom = new DesignData("diamond", "overlapped_split_barred2", exact.primaryMaterial(), exact.secondaryMaterial(), 0, false);
        assertTrue(DesignCatalog.usesLegacyLayout(exact));
        assertFalse(DesignCatalog.usesLegacyLayout(custom));
        assertEquals("diamond", DesignLayoutCatalog.layout(exact, net.minecraft.core.Direction.UP).pattern());
        assertEquals("overlapped_split_barred2", DesignLayoutCatalog.layout(custom, net.minecraft.core.Direction.NORTH).pattern());
    }

    @Test
    void validationRetainsMaterialsAndBoundsTransforms() {
        DesignData invalid = new DesignData("diamond", "missing", ResourceLocation.withDefaultNamespace("oak_planks"),
                ResourceLocation.withDefaultNamespace("birch_planks"), 7, true);
        DesignData validated = DesignCatalog.validate(invalid);
        assertEquals("diamond", validated.design());
        assertEquals("diamond", validated.pattern());
        assertEquals(3, validated.rotation());
        assertTrue(validated.mirrored());
        assertEquals(invalid.primaryMaterial(), validated.primaryMaterial());
        assertEquals(invalid.secondaryMaterial(), validated.secondaryMaterial());
    }

    @Test
    void sourceDerivedLayoutRetainsAllFacesAndHashes() throws Exception {
        JsonObject root;
        var stream = DesignCatalogTest.class.getResourceAsStream("/data/binniedesign/design_layouts.json");
        assertNotNull(stream);
        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        assertEquals(101, root.getAsJsonObject("counts").get("designs").getAsInt());
        assertEquals(213, root.getAsJsonObject("counts").get("patterns").getAsInt());
        assertEquals(64, root.getAsJsonObject("source").getAsJsonObject("enum_design").get("sha256").getAsString().length());
        assertEquals(64, root.getAsJsonObject("source").getAsJsonObject("enum_pattern").get("sha256").getAsString().length());
        JsonObject designs = root.getAsJsonObject("designs");
        assertEquals(101, designs.size());
        designs.entrySet().forEach(entry -> assertEquals(6, entry.getValue().getAsJsonObject().size(), entry.getKey()));
    }

    @Test
    void asymmetricLegacyLayoutsAreNotFlattenedToOneMask() throws Exception {
        JsonObject root;
        var stream = DesignCatalogTest.class.getResourceAsStream("/data/binniedesign/design_layouts.json");
        assertNotNull(stream);
        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("designs");
        }
        JsonObject corner = root.getAsJsonObject("corner");
        assertEquals("corner", corner.getAsJsonObject("up").get("pattern").getAsString());
        assertEquals("corner4", corner.getAsJsonObject("down").get("pattern").getAsString());
        assertEquals("halved", corner.getAsJsonObject("east").get("pattern").getAsString());
        assertTrue(corner.getAsJsonObject("east").get("inverted").getAsBoolean());
        assertEquals("letter_c", root.getAsJsonObject("letter_c").getAsJsonObject("up").get("pattern").getAsString(),
                "The visible selector must not retain the source's blank Letter C bug");
    }

    @Test
    void everySelectableLetterHasAVisibleTwoPassGlyph() throws Exception {
        JsonObject root;
        try (InputStream stream = DesignCatalogTest.class.getResourceAsStream("/data/binniedesign/design_layouts.json")) {
            assertNotNull(stream);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
        assertEquals(21, root.getAsJsonObject("modernizations")
                .getAsJsonArray("completed_letter_masks").size());
        JsonObject designs = root.getAsJsonObject("designs");
        for (char letter = 'a'; letter <= 'z'; letter++) {
            String id = "letter_" + letter;
            assertEquals(id, designs.getAsJsonObject(id).getAsJsonObject("up")
                    .get("pattern").getAsString(), id);
            assertNotNull(DesignCatalogTest.class.getResource(
                    "/assets/binnietrees/textures/block/design_patterns/wood/" + id + "_primary.png"), id);
            assertNotNull(DesignCatalogTest.class.getResource(
                    "/assets/binnietrees/textures/block/design_patterns/wood/" + id + "_secondary.png"), id);
        }
    }

    @Test
    void componentBackedDesignItemsUseOrdinaryThreeDimensionalBlockModels() throws Exception {
        for (String item : new String[]{"patterned_wood", "patterned_panel", "patterned_glass"}) {
            JsonObject model = readModelResource("/assets/binnietrees/models/item/" + item + ".json");
            assertEquals("binnietrees:block/" + item, model.get("parent").getAsString());
        }
        JsonObject ceramic = readModelResource("/assets/binniebotany/models/item/ceramic_tile.json");
        assertEquals("binniebotany:block/ceramic_tile", ceramic.get("parent").getAsString());
    }

    @Test
    void materialPaletteUsesOnlySourceRegisteredInputsAndExactSourceColours() {
        assertEquals(12815444, DesignMaterialCatalog.defaultWoodColor());
        assertEquals(16777215, DesignMaterialCatalog.defaultGlassColor());
        assertEquals(6305064, DesignMaterialCatalog.woodColor(ResourceLocation.fromNamespaceAndPath("binnietrees", "apple_planks")).orElseThrow());
        assertEquals(6305064, DesignMaterialCatalog.woodColor(ResourceLocation.fromNamespaceAndPath("binnietrees", "apple_fireproof_planks")).orElseThrow());
        assertEquals(9745287, DesignMaterialCatalog.woodColor(ResourceLocation.fromNamespaceAndPath("forestry", "camelthorn_planks")).orElseThrow());
        assertEquals(10266653, DesignMaterialCatalog.woodColor(ResourceLocation.fromNamespaceAndPath("forestry", "lemon_planks")).orElseThrow());
        assertEquals(10040115, DesignMaterialCatalog.glassColor(ResourceLocation.withDefaultNamespace("red_stained_glass")).orElseThrow());
        assertEquals(16711680, DesignMaterialCatalog.glassColor(ResourceLocation.fromNamespaceAndPath("binniebotany", "stained"), "red").orElseThrow());
        assertTrue(DesignMaterialCatalog.glassColor(ResourceLocation.fromNamespaceAndPath("binniebotany", "stained"), "not_a_flower_colour").isEmpty());
        assertTrue(DesignMaterialCatalog.woodColor(ResourceLocation.withDefaultNamespace("mangrove_planks")).isEmpty());
        assertTrue(DesignMaterialCatalog.woodColor(ResourceLocation.withDefaultNamespace("oak_slab")).isEmpty());
        assertTrue(DesignMaterialCatalog.glassColor(ResourceLocation.withDefaultNamespace("glass")).isEmpty());
    }

    @Test
    void everyRenderPatternHasExactWoodGlassAndCeramicSpriteModels() throws Exception {
        assertEquals(234, DesignLayoutCatalog.renderPatterns().size());
        for (String pattern : DesignLayoutCatalog.renderPatterns()) {
            for (String pass : new String[]{"primary", "secondary"}) {
                assertNotNull(DesignCatalogTest.class.getResource(
                        "/assets/binnietrees/models/design_sprite/wood/" + pattern + "_" + pass + ".json"));
                assertNotNull(DesignCatalogTest.class.getResource(
                        "/assets/binnietrees/models/design_sprite/glass/" + pattern + "_" + pass + ".json"));
                assertNotNull(DesignCatalogTest.class.getResource(
                        "/assets/binniebotany/models/design_sprite/ceramic/" + pattern + "_" + pass + ".json"));
            }
        }
    }

    @Test
    void glassUsesItsOwnPartiallyTransparentLegacyMasks() throws Exception {
        java.awt.image.BufferedImage glass = javax.imageio.ImageIO.read(DesignCatalogTest.class.getResourceAsStream(
                "/assets/binnietrees/textures/block/design_patterns/glass/letter_a_primary.png"));
        java.awt.image.BufferedImage wood = javax.imageio.ImageIO.read(DesignCatalogTest.class.getResourceAsStream(
                "/assets/binnietrees/textures/block/design_patterns/wood/letter_a_primary.png"));
        boolean partial = false;
        for (int y = 0; y < glass.getHeight(); y++) for (int x = 0; x < glass.getWidth(); x++) {
            int alpha = (glass.getRGB(x, y) >>> 24) & 0xFF;
            partial |= alpha > 0 && alpha < 255;
        }
        assertTrue(partial, "The exact glass family must retain translucent source alpha");
        boolean differs = false;
        for (int y = 0; y < glass.getHeight() && !differs; y++) for (int x = 0; x < glass.getWidth(); x++) {
            if (glass.getRGB(x, y) != wood.getRGB(x, y)) {
                differs = true;
                break;
            }
        }
        assertTrue(differs, "Glass must not reuse the opaque wood masks");
    }

    private static JsonObject readModelResource(String path) throws Exception {
        try (InputStream stream = DesignCatalogTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }
}
