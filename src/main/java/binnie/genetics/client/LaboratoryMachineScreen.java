package binnie.genetics.client;

import binnie.core.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Dedicated Laboratory Machine/Lab Stand menu screen. */
public final class LaboratoryMachineScreen extends GeneticsMachineScreen {
    public LaboratoryMachineScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderMachineFace(GuiGraphics g, float pt, int mx, int my) {
    }
    @Override protected String validatorSprite(int slot) { return null; }
}
