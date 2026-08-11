package binnie.core.client;

import binnie.core.menu.FieldKitMenu;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class FieldKitParityTest {
    @Test
    void alleleAnswerUsesTheSameGlobalMouseCoordinatesAsTheVisibleHotspot() throws Exception {
        String source = Files.readString(Path.of("src/main/java/binnie/core/client/FieldKitScreen.java"));
        assertEquals(-1, source.indexOf("hoveredPicker(mouseX + leftPos, mouseY + topPos"),
                "renderLabels double-offsets the chromosome hotspot hitbox");
    }

    @Test
    void menuUsesSourceWindowSlotsAndAnalysisDuration() {
        assertEquals(100, FieldKitMenu.ANALYSIS_TICKS);
        assertEquals(70, FieldKitMenu.INDIVIDUAL_X);
        assertEquals(58, FieldKitMenu.INDIVIDUAL_Y);
        assertEquals(208, FieldKitMenu.PAPER_X);
        assertEquals(8, FieldKitMenu.PAPER_Y);
        assertEquals(59, FieldKitMenu.PLAYER_X);
        assertEquals(142, FieldKitMenu.PLAYER_Y);
        assertEquals(200, FieldKitMenu.HOTBAR_Y);
        assertEquals(140, FieldKitScreen.HELP_TOOLTIP_WIDTH);
        assertEquals(200, FieldKitScreen.INFO_TOOLTIP_WIDTH);
    }

    @Test
    void originalGuiAtlasesAreCopiedByteForByteAndRetainSourceDimensions() throws Exception {
        assertAtlas("assets/binniecore/textures/gui/punnett.png", 256, 256,
                "660c18fb39f6ac8fedf8256e9bbf3ccaa4846033c6813723ab67c41ef346a649");
        assertAtlas("assets/binniecore/textures/gui/breeding.png", 256, 256,
                "7c208c42f3d32920baed8b0792ea0baa04b5a375d2608b73487fbfc96a27ec80");
    }

    @Test
    void fieldKitItemModelRetainsAllFourLegacyPaperStates() throws Exception {
        JsonObject model = json("assets/binniecore/models/item/field_kit.json");
        var overrides = model.getAsJsonArray("overrides");
        assertEquals(3, overrides.size());
        assertEquals("binniecore:item/field_kit2", overrides.get(0).getAsJsonObject().get("model").getAsString());
        assertEquals("binniecore:item/field_kit1", overrides.get(1).getAsJsonObject().get("model").getAsString());
        assertEquals("binniecore:item/field_kit_empty", overrides.get(2).getAsJsonObject().get("model").getAsString());
        assertEquals(24.0 / 64.0, overrides.get(0).getAsJsonObject().getAsJsonObject("predicate").get("damage").getAsDouble());
        assertEquals(48.0 / 64.0, overrides.get(1).getAsJsonObject().getAsJsonObject("predicate").get("damage").getAsDouble());
        assertEquals(1.0, overrides.get(2).getAsJsonObject().getAsJsonObject("predicate").get("damage").getAsDouble());
    }

    private static void assertAtlas(String path, int width, int height, String expectedSha256) throws Exception {
        byte[] bytes;
        try (InputStream stream = FieldKitParityTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing Field Kit atlas " + path);
            bytes = stream.readAllBytes();
        }
        try (InputStream stream = new java.io.ByteArrayInputStream(bytes)) {
            var image = ImageIO.read(stream);
            assertNotNull(image, "Unreadable Field Kit atlas " + path);
            assertEquals(width, image.getWidth(), path);
            assertEquals(height, image.getHeight(), path);
        }
        assertEquals(expectedSha256, java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)), path);
    }

    private static JsonObject json(String path) throws Exception {
        try (InputStream stream = FieldKitParityTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
