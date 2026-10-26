package binnie.genetics.client;

import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Genetics Gene Pool screen. */
public final class GenePoolScreen extends GeneticsMachineScreen {
    public GenePoolScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderMachineFace(GuiGraphics g, float pt, int mx, int my) {
        LegacyGeneticsGui.arrow(g, "right", leftPos + 80, topPos + 53);
        renderTank(g, 16, 32, menu.fluidStack(), menu.fluidCapacity(), 0);
        renderTank(g, 246, 32, menu.outputFluidStack(), menu.outputFluidCapacity(), 2);
        LegacyGeneticsGui.progress(g, LegacyGeneticsGui.PROCESS_3, leftPos + 116, topPos + 51, 64, 0, 64, 21, 130, 21, menu.progress(), menu.maxProgress());
        renderError(g, leftPos + 181, topPos + 83);
        renderCharge(g, MachineBlockEntity.REAGENT_SLOT, 140, 81, 0xFFEFEF6F);
    }
    @Override protected String validatorSprite(int slot) { return switch (slot) {
        case MachineBlockEntity.INPUT_SLOT, 6, 7, 8, 9, 10, 11 -> "individual";
        case MachineBlockEntity.REAGENT_SLOT -> "enzyme";
        default -> null;
    }; }
    @Override protected void highlightEnergy(GuiGraphics g) { highlight(g, 21, 115, 16, 60); }
    @Override protected void highlightTank(GuiGraphics g, boolean output, boolean secondary) { highlight(g, output ? 246 : 16, 32, 18, 60); }
    @Override protected boolean includePrimaryTankInAllInputErrors() { return true; }
}
