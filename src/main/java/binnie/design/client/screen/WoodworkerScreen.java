package binnie.design.client.screen;

import binnie.core.machine.MachineMenu;
import binnie.design.api.DesignedBlockKind;
import binnie.design.client.DesignerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Binnie's Trees Woodworker screen. */
public final class WoodworkerScreen extends DesignerScreen {
    public WoodworkerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, DesignedBlockKind.WOOD);
    }
}
