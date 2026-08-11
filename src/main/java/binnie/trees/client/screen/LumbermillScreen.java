package binnie.trees.client.screen;

import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Original Lumbermill screen. */
public final class LumbermillScreen extends BinnieTreesProcessorScreen {
    public LumbermillScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Override protected void renderMachineFace(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        validator(graphics, 42, 43);
        sourceError(graphics, errorX(), errorY());
        drawTank(graphics, 16, 32, menu.fluidStack(), menu.fluid(), menu.fluidCapacity());
        renderSourcePanel(graphics, 70, 43, 66, 18, true);
        int progressWidth = menu.maxProgress() <= 0 ? 0 : 64 * menu.progress() / menu.maxProgress();
        ItemStack log = menu.sourceStack(MachineBlockEntity.INPUT_SLOT);
        ItemStack product = menu.sourceStack(MachineBlockEntity.OUTPUT_SLOT);
        for (int tile = 0; tile < 4; tile++) if (!log.isEmpty()) graphics.renderItem(log, leftPos + 71 + tile * 16, topPos + 44);
        if (!product.isEmpty() && progressWidth > 0) {
            graphics.enableScissor(leftPos + 71, topPos + 44, leftPos + 71 + progressWidth, topPos + 60);
            for (int tile = 0; tile < 4; tile++) graphics.renderItem(product, leftPos + 71 + tile * 16, topPos + 44);
            graphics.disableScissor();
        }
        long time = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        int sawY = Math.round(6.0F * (float) Math.sin(time * 0.25F));
        int sawX = Math.min(63, progressWidth);
        graphics.blit(EXTRA_TREES_GUI, leftPos + 70 + sawX, topPos + 35 + sawY, 0, 0, 6, 32, 256, 256);
        graphics.blit(EXTRA_TREES_GUI, leftPos + 72 + sawX, topPos + 35 + sawY, 2, 0, 4, 32, 256, 256);
    }

    @Override protected boolean sourceMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (inside(x, y, 42, 43, 18, 18)) return tooltip(g, Component.translatable("tooltip.binnietrees.machine.lumbermill.logs"), mouseX, mouseY);
        if (inside(x, y, 16, 32, 18, 60)) return fluidTooltip(g, "tooltip.binnietrees.machine.lumbermill.water", menu.fluidStack(), menu.fluid(), menu.fluidCapacity(), mouseX, mouseY);
        if (inside(x, y, 70, 35, 66, 32)) return progressTooltip(g, mouseX, mouseY);
        if (inside(x, y, errorX(), errorY(), 14, 14)) return errorTooltip(g, mouseX, mouseY);
        return false;
    }

    @Override protected int errorX() { return 95; }
    @Override protected int errorY() { return 73; }
    @Override protected void highlightOutput(GuiGraphics g) {
        highlight(g, 148, 43, 18, 18); highlight(g, 172, 28, 18, 18); highlight(g, 172, 58, 18, 18);
    }
    @Override protected void highlightInput(GuiGraphics g) { highlight(g, 42, 43, 18, 18); }
    @Override protected void highlightTank(GuiGraphics g, boolean output) { highlight(g, 16, 32, 18, 60); }
    @Override protected Component machineInfo() { return Component.translatable("info.binnietrees.machine.lumbermill"); }
}
