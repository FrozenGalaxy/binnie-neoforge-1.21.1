package binnie.trees.client.screen;

import binnie.core.machine.MachineMenu;
import binnie.core.network.MachineModePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Original Distillery screen. */
public final class DistilleryScreen extends BinnieTreesProcessorScreen {
    public DistilleryScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Override protected void renderMachineFace(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        sourceError(graphics, errorX(), errorY());
        drawTank(graphics, 16, 35, menu.fluidStack(), menu.fluid(), menu.fluidCapacity(), 0);
        drawTank(graphics, 114, 35, menu.outputFluidStack(), menu.outputFluid(), menu.outputFluidCapacity(), 2);
        int x = leftPos + 50, y = topPos + 32;
        graphics.blit(EXTRA_TREES_GUI, x, y, 43, 0, 58, 66, 256, 256);
        FluidStack current = menu.secondaryFluidStack();
        registerTankArea(50, 32, 18, 66, 1);
        if (!current.isEmpty()) graphics.fill(x + 1, y + 1, x + 17, y + 65, fluidColor(current, 0xFFFFFFFF));
        int flow = menu.maxProgress() <= 0 ? 0 : 38 * menu.progress() / menu.maxProgress();
        if (flow > 0) {
            graphics.enableScissor(x + 18, y, x + 18 + flow, y + 66);
            graphics.blit(EXTRA_TREES_GUI, x + 18, y, 101, 0, 38, 66, 256, 256);
            graphics.disableScissor();
        }
        graphics.blit(EXTRA_TREES_GUI, x + 40, y, 139, 0, 18, 66, 256, 256);
        int level = Math.clamp(menu.mode(), 0, 2);
        graphics.blit(EXTRA_TREES_GUI, x + 47, y + 14 + level * 15, 68, 66, 17, 7, 256, 256);
    }

    @Override protected boolean handleMachineClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !inside((int) mouseX - leftPos, (int) mouseY - topPos, 95, 40, 19, 46)) return false;
        int selected = Math.clamp((int) ((mouseY - topPos - 40) / 15), 0, 2);
        PacketDistributor.sendToServer(new MachineModePayload(menu.containerId, selected));
        return true;
    }

    @Override protected boolean sourceMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (inside(x, y, 16, 35, 18, 60)) return fluidTooltip(g, "tooltip.binnietrees.machine.distillery.distillable", menu.fluidStack(), menu.fluid(), menu.fluidCapacity(), mouseX, mouseY);
        if (inside(x, y, 114, 35, 18, 60)) return fluidTooltip(g, "tooltip.binnietrees.machine.distillery.distilled", menu.outputFluidStack(), menu.outputFluid(), menu.outputFluidCapacity(), mouseX, mouseY);
        if (inside(x, y, 50, 32, 58, 66)) return progressTooltip(g, mouseX, mouseY);
        if (inside(x, y, errorX(), errorY(), 14, 14)) return errorTooltip(g, mouseX, mouseY);
        return false;
    }

    @Override protected int errorX() { return 169; }
    @Override protected int errorY() { return 62; }
    @Override protected void highlightOutput(GuiGraphics g) { highlight(g, 114, 35, 18, 60); }
    @Override protected void highlightInput(GuiGraphics g) { highlight(g, 16, 35, 18, 60); highlight(g, 95, 40, 19, 46); }
    @Override protected void highlightTank(GuiGraphics g, boolean output) { highlight(g, output ? 114 : 16, 35, 18, 60); }
    @Override protected int legacyProcessLength() { return 2000 + 800 * Math.clamp(menu.mode(), 0, 2); }
    @Override protected int legacyProcessEnergy() { return 16_000; }
    @Override protected Component machineInfo() { return Component.translatable("info.binnietrees.machine.distillery"); }
}
