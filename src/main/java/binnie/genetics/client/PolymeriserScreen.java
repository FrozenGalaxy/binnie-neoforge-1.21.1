package binnie.genetics.client;

import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Genetics Polymeriser screen. */
public final class PolymeriserScreen extends GeneticsMachineScreen {
    public PolymeriserScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderMachineFace(GuiGraphics g, float pt, int mx, int my) {
        LegacyGeneticsGui.progress(g, LegacyGeneticsGui.PROCESS, leftPos + 68, topPos + 32, 76, 170, 76, 91, 160, 79, menu.progress(), menu.maxProgress());
        LegacyGeneticsGui.arrow(g, "right", leftPos + 34, topPos + 39);
        renderTank(g, 50, 72, menu.fluidStack(), menu.fluidCapacity(), true);
        renderTank(g, 50, 98, menu.secondaryFluidStack(), menu.secondaryFluidCapacity(), true);
        renderError(g, leftPos + 244, topPos + 97);
        renderCharge(g, 1, 139, 63, 0xFFFFCC00);
    }
    @Override protected String validatorSprite(int slot) { return switch (slot) {
        case MachineBlockEntity.INPUT_SLOT, 6, 7, 8, 9 -> "serum";
        case MachineBlockEntity.CATALYST_SLOT -> "nugget";
        default -> null;
    }; }
    @Override protected void highlightEnergy(GuiGraphics g) { highlight(g, 170, 96, 64, 16); }
    @Override protected void highlightTank(GuiGraphics g, boolean output, boolean secondary) { highlight(g, 50, secondary ? 98 : 72, 60, 18); }
    @Override protected boolean includePrimaryTankInAllInputErrors() { return true; }
    @Override protected void highlightSerumSlots(GuiGraphics g) { super.highlightSerumSlots(g); highlightSlot(g, MachineBlockEntity.INPUT_SLOT); }
    @Override protected void highlightInvalidInput(GuiGraphics g) { highlightInputSlots(g); highlightSlot(g, MachineBlockEntity.CATALYST_SLOT); }
}
