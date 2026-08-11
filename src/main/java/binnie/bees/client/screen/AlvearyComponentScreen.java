package binnie.bees.client.screen;

import binnie.core.client.BinnieLedgerOverlay;
import binnie.bees.block.AlvearyPartType;
import binnie.bees.menu.AlvearyMenu;
import forestry.api.core.ForestryError;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;


/** Shared exact-size renderer for the original 1.12 Binnie's Bees alveary windows. */
public abstract class AlvearyComponentScreen extends AbstractContainerScreen<AlvearyMenu> {
    private static final int BACKGROUND = 0xFFC6C6C6;
    private final ResourceLocation texture;
    private final int legacyImageHeight;
    private final BinnieLedgerOverlay ledgers = new BinnieLedgerOverlay();

    protected AlvearyComponentScreen(AlvearyMenu menu, Inventory inventory, Component title, String textureName) {
        super(menu, inventory, title);
        this.texture = ResourceLocation.fromNamespaceAndPath("binniebees", "textures/gui/" + textureName + ".png");
        this.imageWidth = 176;
        this.legacyImageHeight = menu.partType().guiHeight();
        this.imageHeight = legacyImageHeight + AlvearyMenu.CONTENT_OFFSET;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 0;
        this.titleLabelY = 28;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderExpandedLegacyBackground(graphics);
    // Header Help, Info and Owner controls disabled.
    // MachineScreenWidgets.renderHeaderControls(graphics, leftPos, topPos, imageWidth);
        renderLegacyForegroundBackground(graphics, partialTick, mouseX, mouseY);
    }

    /**
      * The 1.12 texture already has a title bar, including its baked-in Info glyph.
      * Keep the original art intact below a new header strip, while reserving two
      * clean rows for the current Help/Info/Owner controls and the title.
      */
    private void renderExpandedLegacyBackground(GuiGraphics graphics) {
    // Top border and the side strips that bridge the new title row.
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, 5, 256, 256);
        graphics.blit(texture, leftPos, topPos + 5, 0, 5, 5, AlvearyMenu.HEADER_HEIGHT - 5, 256, 256);
        graphics.blit(texture, leftPos + imageWidth - 5, topPos + 5, imageWidth - 5, 5, 5,
                AlvearyMenu.HEADER_HEIGHT - 5, 256, 256);
        graphics.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5,
                topPos + AlvearyMenu.HEADER_HEIGHT, BACKGROUND);

    // Shift the 1.12 body down without copying its top border a second
    // time. This leaves one continuous frame instead of two stacked GUIs.
        graphics.blit(texture, leftPos, topPos + AlvearyMenu.HEADER_HEIGHT, 0, 5,
                imageWidth, legacyImageHeight - 5, 256, 256);

    }

    protected final int contentY(int legacyY) {
        return legacyY + AlvearyMenu.CONTENT_OFFSET;
    }

    protected void renderLegacyForegroundBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int width = font.width(title);
        graphics.drawString(font, title, (imageWidth - width) / 2, titleLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ledgers.render(graphics, font, leftPos, topPos, imageWidth, width, ledgerEntries());
        renderTooltip(graphics, mouseX, mouseY);
        if (ledgers.renderTooltip(graphics, font, mouseX, mouseY)) return;
        renderLegacyTooltips(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ledgers.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Public integration point for JEI overlays when an alveary screen is active. */
    public List<net.minecraft.client.renderer.Rect2i> ledgerAreas() {
        return ledgers.extraAreas();
    }

    private List<BinnieLedgerOverlay.Entry> ledgerEntries() {
        ArrayList<BinnieLedgerOverlay.Entry> entries = new ArrayList<>();
        String descriptionKey = "info.binniebees.alveary." + menu.partType().id();
        String tipKey = descriptionKey + ".tip";
        entries.add(BinnieLedgerOverlay.hint(menu.partType().id(),
                Component.translatable("gui.binniecore.ledger.tip", Component.translatable(tipKey)), Component.translatable("for.gui.didyouknow").append("?"),
                Component.translatable(descriptionKey)));
        if (!menu.ownerName().isEmpty()) entries.add(BinnieLedgerOverlay.owner(menu.ownerName()));

        if (menu.partType() == AlvearyPartType.FRAME && menu.getSlot(0).getItem().isEmpty()) {
            entries.add(BinnieLedgerOverlay.error("frame_missing", ForestryError.NO_RESOURCE_INVENTORY.getSprite(),
                    Component.translatable("error.binniebees.alveary.frame_missing.title"),
                    Component.translatable("error.binniebees.alveary.frame_missing.help")));
        }
        if (menu.partType() == AlvearyPartType.MUTATOR && menu.getSlot(0).getItem().isEmpty()) {
            entries.add(BinnieLedgerOverlay.error("mutagen_missing", ForestryError.NO_RESOURCE_INVENTORY.getSprite(),
                    Component.translatable("error.binniebees.alveary.mutagen_missing.title"),
                    Component.translatable("error.binniebees.alveary.mutagen_missing.help")));
        }
        if (menu.partType() == AlvearyPartType.STIMULATOR) {
            if (menu.getSlot(0).getItem().isEmpty()) {
                entries.add(BinnieLedgerOverlay.error("circuit_missing", ForestryError.NO_CIRCUIT_BOARD.getSprite(),
                        Component.translatable("error.binniebees.alveary.circuit_missing.title"),
                        Component.translatable("error.binniebees.alveary.circuit_missing.help")));
            } else if (menu.powerUsage() > 0 && menu.energyStored() < menu.powerUsage()) {
                entries.add(BinnieLedgerOverlay.error("no_power", ForestryError.NO_POWER.getSprite(),
                        Component.translatable(ForestryError.NO_POWER.getDescriptionTranslationKey()),
                        Component.translatable(ForestryError.NO_POWER.getHelpTranslationKey())));
            }
        }
        return List.copyOf(entries);
    }

    protected void renderLegacyTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    protected boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }
}
