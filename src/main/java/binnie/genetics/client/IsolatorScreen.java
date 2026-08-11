package binnie.genetics.client;

import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Genetics Isolator screen. */
public final class IsolatorScreen extends GeneticsMachineScreen {
    public IsolatorScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderMachineFace(GuiGraphics g, float pt, int mx, int my) {
        LegacyGeneticsGui.arrow(g, "right", leftPos + 62, topPos + 36);
        LegacyGeneticsGui.arrow(g, "right", leftPos + 258, topPos + 36);
        LegacyGeneticsGui.arrow(g, "up", leftPos + 258, topPos + 53);
        renderTank(g, 16, 32, menu.fluidStack(), menu.fluidCapacity());
        LegacyGeneticsGui.progress(g, LegacyGeneticsGui.PROCESS_3, leftPos + 98, topPos + 35, 0, 218, 0, 201, 142, 17, menu.progress(), menu.maxProgress());
        renderError(g, leftPos + 153, topPos + 81);
        renderCharge(g, MachineBlockEntity.REAGENT_SLOT, 99, 70, 0xFFEFEF6F);
    }
    @Override protected String validatorSprite(int slot) { return switch (slot) {
        case MachineBlockEntity.INPUT_SLOT, 6, 7, 8 -> "individual";
        case MachineBlockEntity.CATALYST_SLOT -> "sequencer";
        case MachineBlockEntity.REAGENT_SLOT -> "enzyme";
        default -> null;
    }; }
    @Override protected void highlightEnergy(GuiGraphics g) { highlight(g, 260, 130, 16, 60); }
    @Override protected void highlightTank(GuiGraphics g, boolean output, boolean secondary) { highlight(g, 16, 32, 18, 60); }
    @Override protected boolean includePrimaryTankInAllInputErrors() { return true; }
}
