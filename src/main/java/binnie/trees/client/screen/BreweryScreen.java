package binnie.trees.client.screen;

import binnie.core.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Original Brewery screen. */
public final class BreweryScreen extends BinnieTreesProcessorScreen {
    public BreweryScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Override protected void renderMachineFace(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        validator(graphics, 16, 41); validator(graphics, 42, 32); validator(graphics, 42, 50);
        validator(graphics, 42, 68); validator(graphics, 105, 77);
        sourceError(graphics, errorX(), errorY());
        drawTank(graphics, 76, 32, menu.fluidStack(), menu.fluid(), menu.fluidCapacity());
        drawTank(graphics, 162, 32, menu.outputFluidStack(), menu.outputFluid(), menu.outputFluidCapacity());
        int x = leftPos + 110, y = topPos + 32;
        graphics.blit(EXTRA_TREES_GUI, x, y, 0, 69, 34, 39, 256, 256);
        int fermented = menu.maxProgress() <= 0 ? 0 : 32 * menu.progress() / menu.maxProgress();
        graphics.fill(x + 1, y + 6, x + 33, y + 38 - fermented, fluidColor(menu.fluidStack(), 0xFF8C6A3F));
        graphics.fill(x + 1, y + 38 - fermented, x + 33, y + 38, fluidColor(menu.outputFluidStack(), 0xFFD2A247));
        graphics.blit(EXTRA_TREES_GUI, x, y, 0, 69, 34, 39, 256, 256);
    }

    @Override protected boolean sourceMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (inside(x, y, 16, 41, 18, 18)) return tooltip(g, Component.translatable("tooltip.binnietrees.machine.brewery.ingredient"), mouseX, mouseY);
        if (inside(x, y, 42, 32, 18, 54)) return tooltip(g, Component.translatable("tooltip.binnietrees.machine.brewery.grain"), mouseX, mouseY);
        if (inside(x, y, 105, 77, 18, 18)) return tooltip(g, Component.translatable("tooltip.binnietrees.machine.brewery.yeast"), mouseX, mouseY);
        if (inside(x, y, 76, 32, 18, 60)) return fluidTooltip(g, "tooltip.binnietrees.machine.brewery.fermentable", menu.fluidStack(), menu.fluid(), menu.fluidCapacity(), mouseX, mouseY);
        if (inside(x, y, 162, 32, 18, 60)) return fluidTooltip(g, "tooltip.binnietrees.machine.brewery.fermented", menu.outputFluidStack(), menu.outputFluid(), menu.outputFluidCapacity(), mouseX, mouseY);
        if (inside(x, y, 110, 32, 34, 39)) return progressTooltip(g, mouseX, mouseY);
        if (inside(x, y, errorX(), errorY(), 14, 14)) return errorTooltip(g, mouseX, mouseY);
        return false;
    }

    @Override protected int errorX() { return 133; }
    @Override protected int errorY() { return 79; }
    @Override protected void highlightOutput(GuiGraphics g) { highlight(g, 162, 32, 18, 60); }
    @Override protected void highlightInput(GuiGraphics g) {
        highlight(g, 16, 41, 18, 18); highlight(g, 42, 32, 18, 54);
        highlight(g, 105, 77, 18, 18); highlight(g, 76, 32, 18, 60);
    }
    @Override protected void highlightTank(GuiGraphics g, boolean output) { highlight(g, output ? 162 : 76, 32, 18, 60); }
    @Override protected int legacyProcessLength() { return 800; }
    @Override protected int legacyProcessEnergy() { return 16_000; }
    @Override protected Component machineInfo() { return Component.translatable("info.binnietrees.machine.brewery"); }
}
