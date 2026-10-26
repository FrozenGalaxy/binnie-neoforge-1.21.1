package binnie.trees.client.screen;

import binnie.core.machine.MachineMenu;
import binnie.core.machine.MachineProcessError;
import binnie.core.network.MachineScreenActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Original Fruit Press screen. */
public final class PressScreen extends BinnieTreesProcessorScreen {
    public PressScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Override protected void renderMachineFace(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        validator(graphics, 24, 52);
        sourceError(graphics, errorX(), errorY());
        drawTank(graphics, 99, 32, menu.outputFluidStack(), menu.outputFluid(), menu.outputFluidCapacity(), 2);
        graphics.blit(EXTRA_TREES_GUI, leftPos + 65, topPos + 76, 9, 52, 34, 17, 256, 256);
        if (!menu.sourceStack(6).isEmpty()) graphics.renderItem(menu.sourceStack(6), leftPos + 66, topPos + 77);
        int travel = menu.maxProgress() <= 0 ? 0 : 16 * menu.progress() / menu.maxProgress();
        graphics.blit(EXTRA_TREES_GUI, leftPos + 62, topPos + 24 + travel, 6, 0, 24, 52, 256, 256);
    }

    @Override protected boolean handleMachineClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !inside((int) mouseX - leftPos, (int) mouseY - topPos, 62, 24, 37, 69)) return false;
        int travel = menu.maxProgress() <= 0 ? 0 : 16 * menu.progress() / menu.maxProgress();
        MachineScreenActionPayload.Action action = mouseY - topPos > 76 + travel
                ? MachineScreenActionPayload.Action.CLEAR_FRUIT : MachineScreenActionPayload.Action.FRUIT_PRESS;
        PacketDistributor.sendToServer(new MachineScreenActionPayload(menu.containerId, action));
        return true;
    }

    @Override protected boolean sourceMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (inside(x, y, 24, 52, 18, 18)) return tooltip(g, Component.translatable("tooltip.binnietrees.machine.press.fruit"), mouseX, mouseY);
        if (inside(x, y, 99, 32, 18, 60)) return fluidTooltip(g, "tooltip.binnietrees.machine.press.output", menu.outputFluidStack(), menu.outputFluid(), menu.outputFluidCapacity(), mouseX, mouseY);
        if (inside(x, y, 62, 24, 37, 69)) return progressTooltip(g, mouseX, mouseY);
        if (inside(x, y, errorX(), errorY(), 14, 14)) return errorTooltip(g, mouseX, mouseY);
        return false;
    }

    @Override protected int errorX() { return 128; }
    @Override protected int errorY() { return 54; }
    @Override protected void highlightOutput(GuiGraphics g) { highlight(g, 99, 32, 18, 60); }
    @Override protected void highlightInput(GuiGraphics g) { highlight(g, 24, 52, 18, 18); }
    @Override protected void highlightTank(GuiGraphics g, boolean output) { highlight(g, 99, 32, 18, 60); }
    @Override protected boolean outputForDifferentFluid(MachineProcessError error) { return error == MachineProcessError.TANK_DIFFERENT_FLUID; }
    @Override protected int legacyProcessLength() { return 50; }
    @Override protected int legacyProcessEnergy() { return 1_000; }
    @Override protected Component machineInfo() { return Component.translatable("info.binnietrees.machine.press"); }
}
