package binnie.genetics.client;

import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Genetics Splicer screen. */
public final class SplicerScreen extends GeneticsMachineScreen {
    public SplicerScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderMachineFace(GuiGraphics g, float pt, int mx, int my) {
        LegacyGeneticsGui.arrow(g, "right", leftPos + 52, topPos + 62);
        LegacyGeneticsGui.arrow(g, "down", leftPos + 132, topPos + 54);
        LegacyGeneticsGui.arrow(g, "down", leftPos + 132, topPos + 96);
        LegacyGeneticsGui.arrow(g, "upleft", leftPos + 68, topPos + 86);
        LegacyGeneticsGui.splicerProgress(g, leftPos + 84, topPos + 32, 108, 102, menu.progress(), menu.maxProgress());
        renderError(g, leftPos + 218, topPos + 86);
    }
    @Override protected String validatorSprite(int slot) { return switch (slot) {
        case MachineBlockEntity.INPUT_SLOT, 10, 11, 12, 13, 14 -> "individual";
        case MachineBlockEntity.CATALYST_SLOT, 6, 7 -> "serum";
        default -> null;
    }; }
    @Override protected void highlightEnergy(GuiGraphics g) { highlight(g, 196, 64, 60, 16); }
}
