package binnie.core.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class GenesisPunnettParityTest {
    @Test
    void genesisUsesTheSourceWindowAndControlGeometry() throws Exception {
        String screen = Files.readString(Path.of("src/main/java/binnie/core/client/GenesisScreen.java"));
        String menu = Files.readString(Path.of("src/main/java/binnie/core/menu/GenesisMenu.java"));
        assertTrue(screen.contains("imageWidth = 420"));
        assertTrue(screen.contains("imageHeight = 228"));
        assertTrue(screen.contains("GENE_X = 32, GENE_Y = 28, GENE_W = 230, GENE_H = 100"));
        assertTrue(screen.contains("OPTION_X = 274, OPTION_Y = 28, OPTION_W = 130, OPTION_H = 100"));
        assertTrue(screen.contains("PICKUP_X = 16, PICKUP_Y = 140, PICKUP_W = 60, PICKUP_H = 42"));
        assertTrue(menu.contains("129 + col * 18, 140 + row * 18"));
        assertTrue(menu.contains("129 + col * 18, 198"));
        assertTrue(screen.contains("GenesisMenu.Action.ACTIVE"));
        assertTrue(screen.contains("GenesisMenu.Action.INACTIVE"));
        assertTrue(screen.contains("GenesisMenu.Action.CREATE"));
        assertTrue(screen.contains("GenesisMenu.Action.TOGGLE_ANALYZED"));
        assertTrue(menu.contains("private boolean analyzed = true"));
        assertTrue(menu.contains("if (analyzed) individual.analyze()"));
        assertTrue(screen.contains("Checkbox.builder"));
        assertTrue(menu.contains("if (analyzed) individual.analyze();\n            return individual.createStack(stage)"));
        assertTrue(screen.contains("menu.select(action, index)"));
        assertTrue(screen.contains("drawCenteredNoShadow"));
        assertTrue(menu.contains("copyWithPairs(selectedPairs)"));
        assertTrue(menu.contains("sorted.sort(Comparator.comparing"));
        assertTrue(menu.contains("Double.compare(a.doubleValue(), b.doubleValue())"));
        assertFalse(menu.contains("heldGenesis() == openedGenesis"));
        assertTrue(screen.contains("value instanceof Vec3i vector"));
    }

    @Test
    void punnettUsesOriginalParchmentAndTypedFourOutcomeCalculation() throws Exception {
        String screen = Files.readString(Path.of("src/main/java/binnie/genetics/client/PunnettSquareScreen.java"));
        String menu = Files.readString(Path.of("src/main/java/binnie/genetics/menu/PunnettSquareMenu.java"));
        assertTrue(screen.contains("imageWidth = 380"));
        assertTrue(screen.contains("imageHeight = 225"));
        assertTrue(screen.contains("LegacyParchmentGui.parchment"));
        assertTrue(screen.contains("menu.parentPair(0)"));
        assertTrue(screen.contains("menu.parentPair(1)"));
        assertTrue(screen.contains("ROW_HEADER_WIDTH = 95"));
        assertTrue(screen.contains("HEADER_CENTER = TABLE_X + ROW_HEADER_WIDTH + OUTCOME_WIDTH / 2"));
        assertTrue(screen.contains("graphics.drawString(font, value, centerX - font.width(value) / 2, y, color, false)"));
        assertTrue(menu.contains("GeneticOperations.punnett"));
        assertTrue(menu.contains("GeneticOrganismAdapters.forStack"));
        var atlas = ImageIO.read(Path.of("src/main/resources/assets/binniecore/textures/gui/punnett.png").toFile());
        assertEquals(256, atlas.getWidth());
        assertEquals(256, atlas.getHeight());
    }
}
