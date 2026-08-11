package binnie.bees.client.screen;

import binnie.bees.alveary.AlvearyMutagens;
import binnie.bees.menu.AlvearyMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class AlvearyMutatorScreen extends AlvearyComponentScreen {
    public AlvearyMutatorScreen(AlvearyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "guimutator");
    }

    @Override
    protected void renderLegacyForegroundBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        Component label = Component.translatable("binniecore.gui.possible.mutagens");
        graphics.drawString(font, label, leftPos + (imageWidth - font.width(label)) / 2, topPos + contentY(53), 0x555555, false);
        int totalWidth = AlvearyMutagens.entries().size() * 18;
        int x = leftPos + (imageWidth - totalWidth) / 2;
        for (AlvearyMutagens.Entry entry : AlvearyMutagens.entries()) {
            graphics.renderItem(entry.stack(), x + 1, topPos + contentY(66));
            x += 18;
        }
    }

    @Override
    protected void renderLegacyTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int totalWidth = AlvearyMutagens.entries().size() * 18;
        int x = (imageWidth - totalWidth) / 2;
        for (AlvearyMutagens.Entry entry : AlvearyMutagens.entries()) {
            if (inside(mouseX, mouseY, x, contentY(66), 18, 18)) {
                graphics.renderTooltip(font, List.of(
                        entry.stack().getHoverName(),
                        Component.translatable("binniecore.gui.multiplier", entry.multiplier() + "x").withStyle(ChatFormatting.GRAY)
                ), java.util.Optional.empty(), mouseX, mouseY);
                return;
            }
            x += 18;
        }
    }
}
