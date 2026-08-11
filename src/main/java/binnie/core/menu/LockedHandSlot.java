package binnie.core.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Player-inventory slot that keeps the portable item which owns an open menu in place. */
final class LockedHandSlot extends Slot {
    private final boolean locked;

    LockedHandSlot(Inventory inventory, int index, int x, int y, boolean locked) {
        super(inventory, index, x, y);
        this.locked = locked;
    }

    @Override
    public boolean mayPickup(Player player) {
        return !locked && super.mayPickup(player);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !locked && super.mayPlace(stack);
    }
}
