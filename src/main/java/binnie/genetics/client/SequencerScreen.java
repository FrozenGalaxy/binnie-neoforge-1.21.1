package binnie.genetics.client;

import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Genetics Sequencer screen. */
public final class SequencerScreen extends GeneticsMachineScreen {
    public SequencerScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderMachineFace(GuiGraphics g, float pt, int mx, int my) {
        LegacyGeneticsGui.arrow(g, "right", leftPos + 62, topPos + 48);
        LegacyGeneticsGui.arrow(g, "right", leftPos + 174, topPos + 48);
        LegacyGeneticsGui.grayPanel(g, leftPos + 80, topPos + 32, 100, 52);
        LegacyGeneticsGui.sequencerReadout(g, font, leftPos + 80, topPos + 32,
                menu.getSlot(MachineBlockEntity.INPUT_SLOT).getItem(), menu.progress(), menu.maxProgress());
        renderError(g, leftPos + 178, topPos + 113);
        renderCharge(g, MachineBlockEntity.REAGENT_SLOT, 53, 111, 0xFFFF9900);
    }
    @Override protected String validatorSprite(int slot) { return switch (slot) {
        case MachineBlockEntity.INPUT_SLOT, 6, 7, 8, 9 -> "sequencer";
        case MachineBlockEntity.REAGENT_SLOT -> "dye";
        default -> null;
    }; }
    @Override protected void highlightEnergy(GuiGraphics g) { highlight(g, 86, 112, 60, 16); }
}
