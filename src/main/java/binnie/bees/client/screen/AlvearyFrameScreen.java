package binnie.bees.client.screen;

import binnie.bees.menu.AlvearyMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AlvearyFrameScreen extends AlvearyComponentScreen {
    public AlvearyFrameScreen(AlvearyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "guiframe");
    }
}
