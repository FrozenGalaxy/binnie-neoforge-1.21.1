package binnie.genetics.client;

import binnie.genetics.menu.KnowledgeUiContract;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class AnalystInventoryDrawerTest {
    @Test
    void drawerStartsFullyOpenBeforeAnySpecimenIsInserted() {
        AnalystInventoryDrawer drawer = new AnalystInventoryDrawer();
        assertTrue(drawer.isOpenRequested());
        assertTrue(drawer.slotsVisible());
        assertEquals(KnowledgeUiContract.ANALYST_DRAWER_Y, drawer.top());
    }

    @Test
    void playerCanCollapseAndReopenDrawerWithoutAnySpecimen() {
        AnalystInventoryDrawer drawer = new AnalystInventoryDrawer();
        drawer.toggle();
        assertFalse(drawer.isOpenRequested());
        assertFalse(drawer.slotsVisible());
        for (int i = 0; i < 4; i++) drawer.tick();
        assertEquals(KnowledgeUiContract.ANALYST_DRAWER_Y
                        + KnowledgeUiContract.ANALYST_DRAWER_HEIGHT
                        - KnowledgeUiContract.ANALYST_DRAWER_COLLAPSED_HEIGHT,
                drawer.top());

        drawer.toggle();
        for (int i = 0; i < 4; i++) drawer.tick();
        assertTrue(drawer.isOpenRequested());
        assertTrue(drawer.slotsVisible());
        assertEquals(KnowledgeUiContract.ANALYST_DRAWER_Y, drawer.top());
    }

    @Test
    void analyzedSpecimenAutomaticallyCollapsesOnlyOnce() {
        AnalystInventoryDrawer drawer = new AnalystInventoryDrawer();
        drawer.updateAnalyzedSpecimen(true);
        for (int i = 0; i < 4; i++) drawer.tick();
        assertFalse(drawer.slotsVisible());
        drawer.toggle();
        for (int i = 0; i < 4; i++) drawer.tick();
        assertTrue(drawer.slotsVisible());
        drawer.updateAnalyzedSpecimen(true);
        assertTrue(drawer.isOpenRequested(), "the same specimen must not repeatedly force the drawer closed");
        drawer.updateAnalyzedSpecimen(false);
        drawer.updateAnalyzedSpecimen(true);
        assertFalse(drawer.isOpenRequested(), "a newly inserted analyzed specimen collapses the drawer");
    }
    @Test
    void toggleHandleIsRenderedInForegroundAfterPageContent() throws Exception {
        String base = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        int container = base.indexOf("super.render(graphics, mouseX, mouseY, partialTick)");
        int overlay = base.indexOf("renderScreenOverlay(graphics, mouseX, mouseY, partialTick)");
        int tooltip = base.indexOf("renderTooltip(graphics, mouseX, mouseY)");
        assertTrue(container >= 0 && overlay > container && tooltip > overlay,
                "drawer handle overlay must render after the screen and before tooltips");

        String analyst = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/binnie/genetics/client/AnalystScreen.java"));
        assertTrue(analyst.contains("renderInventoryDrawerHandle(graphics)"));
        assertTrue(analyst.contains("LegacyGeneticsGui.tabOutline"),
                "the always-visible drawer button needs a distinct outline");
    }

}
