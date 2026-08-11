package binnie.genetics.client;

import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Genetics Inoculator screen. */
public final class InoculatorScreen extends GeneticsMachineScreen {
    public InoculatorScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderMachineFace(GuiGraphics g, float pt, int mx, int my) {
        LegacyGeneticsGui.arrow(g, "right", leftPos + 52, topPos + 32);
        LegacyGeneticsGui.arrow(g, "right", leftPos + 88, topPos + 32);
        LegacyGeneticsGui.arrow(g, "upleft", leftPos + 194, topPos + 52);
        LegacyGeneticsGui.arrow(g, "leftdown", leftPos + 194, topPos + 68);
        renderTank(g, 16, 66, menu.fluidStack(), menu.fluidCapacity());
        LegacyGeneticsGui.progress(g, LegacyGeneticsGui.PROCESS_2, leftPos + 34, topPos + 56, 0, 72, 0, 0, 142, 72, menu.progress(), menu.maxProgress());
        renderError(g, leftPos + 161, topPos + 118);
    }
    @Override protected String validatorSprite(int slot) { return switch (slot) {
        case MachineBlockEntity.INPUT_SLOT, 10, 11, 12, 13, 14 -> "individual";
        case MachineBlockEntity.CATALYST_SLOT, 6, 7 -> "serum";
        default -> null;
    }; }
    @Override protected void highlightEnergy(GuiGraphics g) { highlight(g, 91, 118, 60, 16); }
    @Override protected void highlightTank(GuiGraphics g, boolean output, boolean secondary) { highlight(g, 16, 66, 18, 60); }
    @Override protected boolean includePrimaryTankInAllInputErrors() { return true; }
}
