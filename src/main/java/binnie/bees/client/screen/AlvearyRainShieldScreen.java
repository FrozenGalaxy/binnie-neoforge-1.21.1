package binnie.bees.client.screen;

import binnie.core.client.GenericMachineScreen;
import binnie.core.machine.MachineMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Concrete Alveary Rain Shield machine screen. */
public final class AlvearyRainShieldScreen extends GenericMachineScreen {
    public AlvearyRainShieldScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
