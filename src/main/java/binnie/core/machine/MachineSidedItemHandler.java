package binnie.core.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Arrays;

/** Capability view that exposes only the slots valid for a machine side and operation direction. */
final class MachineSidedItemHandler implements IItemHandlerModifiable {
    private final ItemStackHandler delegate;
    private final int[] slots;
    private final boolean insert;
    private final boolean extract;

    MachineSidedItemHandler(ItemStackHandler delegate, int[] slots, boolean insert, boolean extract) {
        this.delegate = delegate;
        this.slots = Arrays.copyOf(slots, slots.length);
        this.insert = insert;
        this.extract = extract;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        // Capability consumers may probe the modifiable extension even when a
        // side is extraction-only. Reject the mutation without throwing so a
        // generic pipe cannot crash the server while respecting side access.
        if (insert) delegate.setStackInSlot(map(slot), stack);
        else map(slot); // retain the interface's required slot bounds check
    }

    @Override
    public int getSlots() {
        return slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(map(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return insert ? delegate.insertItem(map(slot), stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extract ? delegate.extractItem(map(slot), amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(map(slot));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return insert && delegate.isItemValid(map(slot), stack);
    }

    private int map(int slot) {
        if (slot < 0 || slot >= slots.length) throw new IndexOutOfBoundsException("Unknown sided slot " + slot);
        return slots[slot];
    }
}
