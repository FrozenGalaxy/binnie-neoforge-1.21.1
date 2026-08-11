package binnie.genetics.client;

import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Genetics Analyser screen. */
public final class AnalyserScreen extends GeneticsMachineScreen {
    public AnalyserScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderMachineFace(GuiGraphics g, float pt, int mx, int my) {
        LegacyGeneticsGui.arrow(g, "right", leftPos + 54, topPos + 50);
        LegacyGeneticsGui.arrow(g, "right", leftPos + 150, topPos + 50);
        LegacyGeneticsGui.tintedPanel(g, leftPos + 72, topPos + 32, 76, 50);
        LegacyGeneticsGui.progress(g, LegacyGeneticsGui.PROCESS, leftPos + 77, topPos + 37, 0, 51, 66, 51, 66, 40, menu.progress(), menu.maxProgress());
        renderError(g, leftPos + 160, topPos + 95);
        renderCharge(g, MachineBlockEntity.REAGENT_SLOT, 63, 93, 0xFF9933FF);
    }
    @Override protected String validatorSprite(int slot) { return switch (slot) {
        case MachineBlockEntity.INPUT_SLOT, 6, 7, 8, 9, 10, 11 -> "individual";
        case MachineBlockEntity.REAGENT_SLOT -> "dye";
        default -> null;
    }; }
    @Override protected void highlightEnergy(GuiGraphics g) { highlight(g, 84, 95, 60, 16); }
}
