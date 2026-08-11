package binnie.core.storage;

import binnie.core.registry.CoreRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Locale;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Tier-aware tabbed storage menu. Only the selected page participates in shift-click transfer. */
public final class CompartmentMenu extends AbstractContainerMenu {
    private final CompartmentBlockEntity compartment;
    private final CompartmentTier tier;
    private int selectedTab;

    public static CompartmentMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        if (!(inventory.player.level().getBlockEntity(pos) instanceof CompartmentBlockEntity compartment)) {
            throw new IllegalStateException("Missing Binnie compartment at " + pos);
        }
        return new CompartmentMenu(containerId, inventory, compartment);
    }

    public CompartmentMenu(int containerId, Inventory playerInventory, CompartmentBlockEntity compartment) {
        super(CoreRegistries.COMPARTMENT_MENU.get(), containerId);
        this.compartment = compartment;
        this.tier = compartment.tier();
        int columns = tier.slotsPerTab() == 25 ? 5 : 10;
        for (int tab = 0; tab < tier.tabs(); tab++) {
            for (int pageSlot = 0; pageSlot < tier.slotsPerTab(); pageSlot++) {
                int absolute = tab * tier.slotsPerTab() + pageSlot;
                int x = 17 + (pageSlot % columns) * 18;
                int y = 25 + (pageSlot / columns) * 18;
                final int slotTab = tab;
                addSlot(new SlotItemHandler(compartment.backingInventory(), absolute, x, y) {
                    @Override public boolean isActive() { return selectedTab == slotTab; }
                });
            }
        }
        int playerY = tier.slotsPerTab() == 25 ? 130 : 112;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerY + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, playerY + 58));
    }

    public CompartmentTier tier() { return tier; }
    public int selectedTab() { return selectedTab; }
    public int tabCount() { return tier.tabs(); }
    public int slotsPerTab() { return tier.slotsPerTab(); }
    public String tabName(int tab) { return compartment.tabName(tab); }
    public int tabColor(int tab) { return compartment.tabColor(tab); }
    public ItemStack tabIcon(int tab) { return compartment.tabIcon(tab); }

    /** Finds the next tab containing a stack whose name or registry path matches the 1.12-style search text. */
    public int findMatchingTab(String query, int afterTab) {
        String normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return selectedTab;
        for (int offset = 1; offset <= tier.tabs(); offset++) {
            int tab = Math.floorMod(afterTab + offset, tier.tabs());
            int start = tab * tier.slotsPerTab();
            int end = start + tier.slotsPerTab();
            for (int slot = start; slot < end; slot++) {
                ItemStack stack = compartment.backingInventory().getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
                if (name.contains(normalized) || id.contains(normalized)) return tab;
            }
        }
        return -1;
    }

    public boolean selectTab(int tab) {
        if (tab < 0 || tab >= tier.tabs()) return false;
        selectedTab = tab;
        broadcastChanges();
        return true;
    }

    public boolean renameTab(int tab, String name) {
        if (tab < 0 || tab >= tier.tabs()) return false;
        compartment.setTabName(tab, name);
        return true;
    }

    public boolean cycleColor(int tab, int direction) {
        if (tab < 0 || tab >= tier.tabs() || direction == 0) return false;
        compartment.cycleTabColor(tab, direction);
        return true;
    }

    public boolean setIconFromCarried(int tab) {
        if (tab < 0 || tab >= tier.tabs()) return false;
        compartment.setTabIcon(tab, getCarried());
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        int storageSlots = tier.capacity();
        int tabStart = selectedTab * tier.slotsPerTab();
        int tabEnd = tabStart + tier.slotsPerTab();
        if (index < storageSlots) {
            if (!moveItemStackTo(source, storageSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, tabStart, tabEnd, false)) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(ContainerLevelAccess.create(compartment.getLevel(), compartment.getBlockPos()),
                player, CoreRegistries.COMPARTMENT_BLOCK.get());
    }
}
