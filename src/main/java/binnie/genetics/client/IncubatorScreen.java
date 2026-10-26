package binnie.genetics.client;

import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Genetics Incubator screen. */
public final class IncubatorScreen extends GeneticsMachineScreen {
    public IncubatorScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderMachineFace(GuiGraphics g, float pt, int mx, int my) {
        LegacyGeneticsGui.arrow(g, "right", leftPos + 72, topPos + 45);
        LegacyGeneticsGui.arrow(g, "right", leftPos + 122, topPos + 45);
        renderTank(g, 16, 32, menu.fluidStack(), menu.fluidCapacity(), 0);
        renderTank(g, 164, 32, menu.outputFluidStack(), menu.outputFluidCapacity(), 2);
        LegacyGeneticsGui.progress(g, LegacyGeneticsGui.PROCESS, leftPos + 80, topPos + 38, 0, 91, 38, 91, 38, 32, menu.progress(), menu.maxProgress());
        renderError(g, leftPos + 91, topPos + 82);
    }
    @Override protected String validatorSprite(int slot) { return null; }
    @Override protected void highlightEnergy(GuiGraphics g) { highlight(g, 198, 35, 16, 54); }
    @Override protected void highlightTank(GuiGraphics g, boolean output, boolean secondary) { highlight(g, output ? 164 : 16, 32, 18, 60); }
    @Override protected boolean includePrimaryTankInAllInputErrors() { return true; }
    @Override protected void highlightInvalidInput(GuiGraphics g) { highlightSlot(g, MachineBlockEntity.REAGENT_SLOT); }
}
