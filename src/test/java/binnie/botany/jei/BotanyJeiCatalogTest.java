package binnie.botany.jei;

import binnie.design.data.DesignCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class BotanyJeiCatalogTest {
    @Test void tileworkerUsesEverySourceDesignAndExactPreviewArea() {
        assertEquals(101, DesignCatalog.DESIGNS.size());
        assertEquals(100, DesignCatalog.SELECTABLE_DESIGNS.size());
        assertEquals(new BotanyJeiPlugin.ClickArea(92,50,18,18,0), BotanyJeiPlugin.CLICK_AREA);
        assertTrue(BotanyJeiPlugin.TITLE_CLICK_AREA.isTitle());
        assertEquals(new BotanyJeiPlugin.ClickArea(140, 8, 40, 12, 0),
                BotanyJeiPlugin.TITLE_CLICK_AREA.resolve(40));
    }

    @Test void pigmentUsesVanillaShapelessCraftingPresentation() throws IOException {
        String plugin = Files.readString(Path.of("src/main/java/binnie/botany/jei/BotanyJeiPlugin.java"));
        String extension = Files.readString(Path.of(
                "src/main/java/binnie/botany/jei/FlowerPigmentCraftingExtension.java"));
        assertTrue(plugin.contains("addExtension(FlowerPigmentRecipe.class"));
        assertFalse(plugin.contains("FLOWER_PIGMENT = RecipeType.create"));
        assertFalse(Files.exists(Path.of("src/main/java/binnie/botany/jei/PigmentJeiCategory.java")));
        assertTrue(extension.contains("createAndSetInputs(builder, List.of(flowers), 0, 0).get(4)"),
                "the one shapeless flower input must occupy the center of JEI's 3x3 crafting grid");
        assertTrue(extension.contains("createAndSetOutputs(builder, pigments)"));
        assertTrue(extension.contains("FlowerChromosomes.PRIMARY_COLOR"));
        assertTrue(extension.contains("createDisplayOverrides().addItemStack"),
                "the displayed pigment must remain tied to the cycling flower genome");
        assertTrue(extension.contains("flower_pigment.primary_color"),
                "flower hover information must state the required active Primary Color");
    }
}
