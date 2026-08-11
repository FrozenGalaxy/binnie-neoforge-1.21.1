package binnie.core.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Capacity-limited automation view over the block entity's migration-safe 400-slot backing inventory. */
public final class CompartmentItemHandler implements IItemHandlerModifiable {
    private final ItemStackHandler delegate;
    private final java.util.function.IntSupplier capacity;

    public CompartmentItemHandler(ItemStackHandler delegate, java.util.function.IntSupplier capacity) {
        this.delegate = delegate;
        this.capacity = capacity;
    }

    @Override public int getSlots() { return capacity.getAsInt(); }
    @Override public ItemStack getStackInSlot(int slot) { return delegate.getStackInSlot(check(slot)); }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return delegate.insertItem(check(slot), stack, simulate); }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return delegate.extractItem(check(slot), amount, simulate); }
    @Override public int getSlotLimit(int slot) { return delegate.getSlotLimit(check(slot)); }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return delegate.isItemValid(check(slot), stack); }
    @Override public void setStackInSlot(int slot, ItemStack stack) { delegate.setStackInSlot(check(slot), stack); }

    private int check(int slot) {
        if (slot < 0 || slot >= getSlots()) throw new IndexOutOfBoundsException("Unknown compartment slot " + slot);
        return slot;
    }
}
