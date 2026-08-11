package binnie.design.client.screen;

import binnie.core.machine.MachineMenu;
import binnie.design.api.DesignedBlockKind;
import binnie.design.client.DesignerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Binnie's Trees Panelworker screen. */
public final class PanelworkerScreen extends DesignerScreen {
    public PanelworkerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, DesignedBlockKind.PANEL);
    }
}
