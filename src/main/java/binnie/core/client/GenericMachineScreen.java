package binnie.core.client;

import binnie.core.machine.MachineMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Generic fallback used only by machines without a variant-specific screen implementation. */
public class GenericMachineScreen extends MachineScreen {
    public GenericMachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
