package binnie.genetics.client;

import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.genetics.menu.KnowledgeUiContract;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Portable two-slot Analyst with the original bottom sliding player inventory. */
public final class AnalystScreen extends AbstractGeneticKnowledgeScreen {
    private final AnalystInventoryDrawer inventoryDrawer = new AnalystInventoryDrawer();

    public AnalystScreen(GeneticKnowledgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ScreenKind.ANALYST, null);
    }

    @Override
    protected void init() {
        super.init();
    // The 1.12 Analyst opens with its player-inventory drawer already extended.
        menu.setAnalystInventoryVisible(inventoryDrawer.slotsVisible());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        inventoryDrawer.updateAnalyzedSpecimen(hasAnalystSpecimen());
        inventoryDrawer.tick();
        menu.setAnalystInventoryVisible(inventoryDrawer.slotsVisible());
    }

    @Override
    public void removed() {
        menu.setAnalystInventoryVisible(false);
        super.removed();
    }

    @Override protected void configureSearchBox() { }

    @Override
    protected void renderScreenBackground(GuiGraphics graphics, int mouseX, int mouseY) {
        renderAnalystFrame(graphics, mouseX, mouseY);
        renderInventoryDrawer(graphics);
    }

    private void renderInventoryDrawer(GuiGraphics graphics) {
        int top = inventoryDrawer.top();
        int x = KnowledgeUiContract.ANALYST_DRAWER_X;
        int width = KnowledgeUiContract.ANALYST_DRAWER_WIDTH;

        graphics.enableScissor(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight);
        LegacyGeneticsGui.window(graphics, leftPos + x, topPos + top,
                width, KnowledgeUiContract.ANALYST_DRAWER_HEIGHT);
        if (inventoryDrawer.slotsVisible()) {
            drawPlayerInventoryChrome(graphics,
                    KnowledgeUiContract.ANALYST_INVENTORY_X - 1,
                    KnowledgeUiContract.ANALYST_INVENTORY_Y - 1);
        }
        graphics.disableScissor();

    }

    @Override
    protected void renderScreenOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderInventoryDrawerHandle(graphics);
    }

    private void renderInventoryDrawerHandle(GuiGraphics graphics) {
        int handleX = drawerHandleX();
        int handleY = drawerHandleY();
        int width = KnowledgeUiContract.ANALYST_DRAWER_HANDLE_WIDTH;
        int height = KnowledgeUiContract.ANALYST_DRAWER_HANDLE_HEIGHT;
        int absoluteX = leftPos + handleX;
        int absoluteY = topPos + handleY;

    // Draw after pages and slots so the toggle can never be hidden by Analyst content.
        LegacyGeneticsGui.tabSolid(graphics, absoluteX, absoluteY, width, height);
        LegacyGeneticsGui.tabOutline(graphics, absoluteX, absoluteY, width, height);
        LegacyGeneticsGui.arrow(graphics, inventoryDrawer.isOpenRequested() ? "down" : "up",
                absoluteX + (width - 16) / 2, absoluteY + (height - 16) / 2);
    }

    private int drawerHandleX() {
        return KnowledgeUiContract.ANALYST_DRAWER_X
                + (KnowledgeUiContract.ANALYST_DRAWER_WIDTH - KnowledgeUiContract.ANALYST_DRAWER_HANDLE_WIDTH) / 2;
    }

    private int drawerHandleY() {
        return inventoryDrawer.top() - KnowledgeUiContract.ANALYST_DRAWER_HANDLE_HEIGHT + 2;
    }

    @Override
    protected int analystPageClipBottom() {
        if (inventoryDrawer.progress() <= 0.0F) return super.analystPageClipBottom();
        return Math.min(super.analystPageClipBottom(), inventoryDrawer.top() - 2);
    }

    @Override protected void renderScreenContent(GuiGraphics graphics, int mouseX, int mouseY) { renderAnalystContent(graphics, mouseX, mouseY); }

    @Override
    protected void renderScreenTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        renderAnalystTabTooltip(graphics, mouseX, mouseY);
        renderAnalystContentTooltip(graphics, mouseX, mouseY);
        if (inside(mouseX - leftPos, mouseY - topPos,
                drawerHandleX(), drawerHandleY(),
                KnowledgeUiContract.ANALYST_DRAWER_HANDLE_WIDTH,
                KnowledgeUiContract.ANALYST_DRAWER_HANDLE_HEIGHT)) {
            graphics.renderTooltip(font, Component.translatable(inventoryDrawer.isOpenRequested()
                    ? "screen.binniegenetics.analyst.inventory.close"
                    : "screen.binniegenetics.analyst.inventory.open"), mouseX, mouseY);
        }
    }

    @Override
    protected boolean handleScreenClick(double x, double y, int button) {
        if (button == 0 && inside(x, y,
                drawerHandleX(), drawerHandleY(),
                KnowledgeUiContract.ANALYST_DRAWER_HANDLE_WIDTH,
                KnowledgeUiContract.ANALYST_DRAWER_HANDLE_HEIGHT)) {
            inventoryDrawer.toggle();
            // Slots remain inactive during the animation and become active once fully open.
            menu.setAnalystInventoryVisible(inventoryDrawer.slotsVisible());
            return true;
        }
        return handleAnalystClick(x, y, button);
    }
}
