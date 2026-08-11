package binnie.genetics.client;

import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Genetics Acclimatiser screen. */
public final class AcclimatiserScreen extends GeneticsMachineScreen {
    public AcclimatiserScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderMachineFace(GuiGraphics g, float pt, int mx, int my) {
        LegacyGeneticsGui.progress(g, LegacyGeneticsGui.PROCESS_3, leftPos + 65, topPos + 28, 0, 64, 0, 64, 64, 64, menu.progress(), menu.maxProgress());
        renderError(g, leftPos + 181, topPos + 83);
    }
    @Override protected String validatorSprite(int slot) { return switch (slot) {
        case MachineBlockEntity.INPUT_SLOT, 6, 7, 8, 9 -> "individual";
        default -> null;
    }; }
    @Override protected void highlightEnergy(GuiGraphics g) { highlight(g, 21, 115, 16, 60); }
    @Override protected void highlightInvalidInput(GuiGraphics g) { highlightAcclimatiserReagents(g); }
}
