package binnie.genetics.client;

import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.genetics.menu.KnowledgeUiContract;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Shared mechanics for the four original specialist databases. */
public abstract class AbstractSpeciesDatabaseScreen extends AbstractGeneticKnowledgeScreen {
    protected AbstractSpeciesDatabaseScreen(GeneticKnowledgeMenu menu, Inventory inventory, Component title,
                                            ResourceLocation speciesType) {
        super(menu, inventory, title, ScreenKind.SPECIES_DATABASE, speciesType);
    }

    @Override
    protected final void configureSearchBox() {
        createSearch(leftPos + KnowledgeUiContract.DATABASE_SEARCH_X + 3,
                topPos + KnowledgeUiContract.DATABASE_SEARCH_Y + 3,
                KnowledgeUiContract.databaseSelectionWidth(databaseFamily()) - 6,
                KnowledgeUiContract.DATABASE_SEARCH_HEIGHT - 5,
                Component.translatable("screen.binniegenetics.knowledge.search"));
    }

    @Override protected final void renderScreenBackground(GuiGraphics graphics, int mouseX, int mouseY) { renderArboristFrame(graphics, mouseX, mouseY); }
    @Override protected final void renderScreenContent(GuiGraphics graphics, int mouseX, int mouseY) { renderArboristContent(graphics, mouseX, mouseY); }
    @Override protected final void renderScreenTooltip(GuiGraphics graphics, int mouseX, int mouseY) { renderArboristTooltip(graphics, mouseX, mouseY); }
    @Override protected final boolean handleScreenClick(double x, double y, int button) { return handleArboristClick(x, y, button); }
}
