package binnie.genetics.client;

import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.genetics.menu.KnowledgeUiContract;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Registry and Master Registry book screen. Master visibility is selected by the menu mode. */
public final class RegistryScreen extends AbstractGeneticKnowledgeScreen {
    public RegistryScreen(GeneticKnowledgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ScreenKind.REGISTRY, null);
    }

    @Override
    protected void configureSearchBox() {
        createSearch(leftPos + KnowledgeUiContract.ANALYST_LEFT_PAGE_X + 22,
                topPos + KnowledgeUiContract.ANALYST_PAGE_Y + 22,
                KnowledgeUiContract.ANALYST_PAGE_WIDTH - 44, 12,
                Component.translatable("screen.binniegenetics.knowledge.search"));
    }

    @Override protected void renderScreenBackground(GuiGraphics graphics, int mouseX, int mouseY) { renderAnalystFrame(graphics, mouseX, mouseY); }
    @Override protected void renderScreenContent(GuiGraphics graphics, int mouseX, int mouseY) { renderAnalystContent(graphics, mouseX, mouseY); }
    @Override protected void renderScreenTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        renderRegistryTooltip(graphics, mouseX, mouseY);
        renderAnalystContentTooltip(graphics, mouseX, mouseY);
    }
    @Override protected boolean handleScreenClick(double x, double y, int button) { return handleAnalystClick(x, y, button); }
}
