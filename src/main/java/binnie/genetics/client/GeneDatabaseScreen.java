package binnie.genetics.client;

import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.genetics.menu.KnowledgeUiContract;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Gene Database and Master Gene Database. Master allele visibility is selected by the menu mode. */
public final class GeneDatabaseScreen extends AbstractGeneticKnowledgeScreen {
    public GeneDatabaseScreen(GeneticKnowledgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ScreenKind.GENE_DATABASE, null);
    }

    @Override
    protected void configureSearchBox() {
        createSearch(leftPos + KnowledgeUiContract.GENE_DATABASE_SEARCH_X + 3,
                topPos + KnowledgeUiContract.GENE_DATABASE_SEARCH_Y + 2,
                KnowledgeUiContract.GENE_DATABASE_SEARCH_WIDTH - 6,
                KnowledgeUiContract.GENE_DATABASE_SEARCH_HEIGHT - 4,
                Component.empty());
    }

    @Override protected void renderScreenBackground(GuiGraphics graphics, int mouseX, int mouseY) { renderGeneDatabaseFrame(graphics, mouseX, mouseY); }
    @Override protected void renderScreenContent(GuiGraphics graphics, int mouseX, int mouseY) { renderGeneDatabaseContent(graphics, mouseX, mouseY); }
    @Override protected void renderScreenTooltip(GuiGraphics graphics, int mouseX, int mouseY) { renderGeneTooltip(graphics, mouseX, mouseY); }
    @Override protected boolean handleScreenClick(double x, double y, int button) { return handleGeneClick(x, y, button); }
}
