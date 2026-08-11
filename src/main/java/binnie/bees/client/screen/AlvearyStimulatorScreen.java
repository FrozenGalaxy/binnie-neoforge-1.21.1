package binnie.bees.client.screen;

import binnie.core.client.MachineScreenWidgets;
import binnie.bees.menu.AlvearyMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class AlvearyStimulatorScreen extends AlvearyComponentScreen {
    public AlvearyStimulatorScreen(AlvearyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "guistimulator");
    }

    @Override
    protected void renderLegacyForegroundBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    // The 1.12 widget was y=29, but this preserved PNG positions the
    // visual circuit slot nine pixels below the old Window layout. Keep
    // the bar in that same corrected visual row.
		// (GuiGraphics graphics, int x, int y, int width, int height, int stored, int capacity)

        MachineScreenWidgets.energyBar(graphics, leftPos + 70, topPos + contentY(37), 72, 16,
                menu.energyStored(), menu.energyCapacity());
    }

    @Override
    protected void renderLegacyTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, 44, contentY(37), 18, 18) && menu.getSlot(0).getItem().isEmpty()) {
            graphics.renderTooltip(font, Component.translatable("tooltip.binniebees.stimulator.circuit_slot"), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, 70, contentY(38), 72, 15)) {
            graphics.renderTooltip(font, List.of(
                    Component.translatable("tooltip.binniebees.energy", menu.energyStored(), menu.energyCapacity()),
                    Component.translatable("tooltip.binniebees.stimulator.usage", menu.powerUsage())
            ), java.util.Optional.empty(), mouseX, mouseY);
        }
    }
}
