package binnie.core.storage;

import binnie.core.registry.CoreRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Migration-safe 400-slot backing store with tier-limited exposure and 1.12-style tab metadata. */
public final class CompartmentBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_CAPACITY = 400;
    private static final int DEFAULT_COLOR = 0x8B6B3E;

    private final ItemStackHandler inventory = new ItemStackHandler(MAX_CAPACITY) {
        @Override
        public void setSize(int ignored) {
            super.setSize(MAX_CAPACITY);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChangedAndSync();
        }
    };
    private final CompartmentItemHandler externalInventory = new CompartmentItemHandler(inventory, this::capacity);
    private final List<String> tabNames = new ArrayList<>(8);
    private final int[] tabColors = new int[8];
    private final ItemStack[] tabIcons = new ItemStack[8];
    @Nullable private Component customName;

    public CompartmentBlockEntity(BlockPos pos, BlockState state) {
        super(CoreRegistries.COMPARTMENT_BLOCK_ENTITY.get(), pos, state);
        for (int i = 0; i < 8; i++) {
            tabNames.add("");
            tabColors[i] = DEFAULT_COLOR;
            tabIcons[i] = ItemStack.EMPTY;
        }
    }

    public CompartmentTier tier() {
        return getBlockState().hasProperty(CompartmentBlock.TIER)
                ? getBlockState().getValue(CompartmentBlock.TIER) : CompartmentTier.PLAIN;
    }

    public void setTier(CompartmentTier tier) {
        if (level != null && getBlockState().hasProperty(CompartmentBlock.TIER)
                && getBlockState().getValue(CompartmentBlock.TIER) != tier) {
            level.setBlock(worldPosition, getBlockState().setValue(CompartmentBlock.TIER, tier), 3);
        }
        setChangedAndSync();
    }

    public int capacity() { return tier().capacity(); }
    public ItemStackHandler backingInventory() { return inventory; }
    public CompartmentItemHandler externalInventory() { return externalInventory; }

    public String tabName(int tab) {
        return validTab(tab) ? tabNames.get(tab) : "";
    }

    public void setTabName(int tab, String name) {
        if (!validTab(tab)) return;
        String cleaned = name == null ? "" : name.strip();
        if (cleaned.length() > 32) cleaned = cleaned.substring(0, 32);
        tabNames.set(tab, cleaned);
        setChangedAndSync();
    }

    public int tabColor(int tab) {
        return validTab(tab) ? tabColors[tab] : DEFAULT_COLOR;
    }

    public void cycleTabColor(int tab, int direction) {
        if (!validTab(tab)) return;
        int[] palette = {0x8B6B3E, 0xB3312C, 0x3B511A, 0x253192, 0x7B2FBE, 0xD88198, 0x287697, 0xABABAB,
                0x434343, 0xC354CD, 0x41CD34, 0x6689D3, 0xDECF2A, 0xEB8844, 0xF0F0F0, 0x1D1D21};
        int current = 0;
        for (int i = 0; i < palette.length; i++) if (palette[i] == tabColors[tab]) current = i;
        tabColors[tab] = palette[Math.floorMod(current + Integer.signum(direction), palette.length)];
        setChangedAndSync();
    }

    public ItemStack tabIcon(int tab) {
        return validTab(tab) ? tabIcons[tab].copy() : ItemStack.EMPTY;
    }

    public void setTabIcon(int tab, ItemStack icon) {
        if (!validTab(tab)) return;
        tabIcons[tab] = icon.isEmpty() ? ItemStack.EMPTY : icon.copyWithCount(1);
        setChangedAndSync();
    }

    private boolean validTab(int tab) { return tab >= 0 && tab < tier().tabs(); }

    public int comparatorOutput() {
        int filled = 0;
        float fullness = 0.0F;
        for (int slot = 0; slot < capacity(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                fullness += (float) stack.getCount() / Math.min(inventory.getSlotLimit(slot), stack.getMaxStackSize());
                filled++;
            }
        }
        return filled == 0 ? 0 : 1 + Math.round(fullness / capacity() * 14.0F);
    }

    @Nullable
    public Component customName() { return customName; }

    public void setCustomName(Component name) {
        customName = name;
        setChangedAndSync();
    }

    @Override
    public Component getDisplayName() {
        return customName != null ? customName : Component.translatable("block.binniecore.compartment." + tier().getSerializedName());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CompartmentMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        ListTag names = new ListTag();
        for (String name : tabNames) names.add(StringTag.valueOf(name));
        tag.put("TabNames", names);
        tag.putIntArray("TabColors", tabColors);
        ListTag icons = new ListTag();
        for (int i = 0; i < tabIcons.length; i++) {
            CompoundTag icon = new CompoundTag();
            icon.putInt("Tab", i);
            if (!tabIcons[i].isEmpty()) icon.put("Stack", tabIcons[i].save(registries));
            icons.add(icon);
        }
        tag.put("TabIcons", icons);
        if (customName != null) tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        resetMetadata();
        if (tag.contains("Inventory")) inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        ListTag names = tag.getList("TabNames", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < Math.min(names.size(), tabNames.size()); i++) tabNames.set(i, names.getString(i));
        int[] colors = tag.getIntArray("TabColors");
        System.arraycopy(colors, 0, tabColors, 0, Math.min(colors.length, tabColors.length));
        ListTag icons = tag.getList("TabIcons", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < icons.size(); i++) {
            CompoundTag icon = icons.getCompound(i);
            int tab = icon.getInt("Tab");
            if (tab >= 0 && tab < tabIcons.length && icon.contains("Stack")) {
                tabIcons[tab] = ItemStack.parseOptional(registries, icon.getCompound("Stack"));
            }
        }
        if (tag.contains("CustomName")) customName = Component.Serializer.fromJson(tag.getString("CustomName"), registries);
    }

    private void resetMetadata() {
        customName = null;
        for (int i = 0; i < tabNames.size(); i++) {
            tabNames.set(i, "");
            tabColors[i] = DEFAULT_COLOR;
            tabIcons[i] = ItemStack.EMPTY;
        }
    }

    /** Removes all stored stacks exactly once for block-removal drops. */
    public List<ItemStack> removeAllContents() {
        List<ItemStack> contents = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack removed = inventory.extractItem(slot, Integer.MAX_VALUE, false);
            if (!removed.isEmpty()) contents.add(removed);
        }
        return contents;
    }

    public void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
