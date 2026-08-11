package binnie.bees.menu;

import binnie.bees.block.AlvearyPartType;
import binnie.bees.block.BinnieBeesAlvearyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AlvearyMenu extends AbstractContainerMenu {
    /** Space reserved above the 1.12 window for the title-control and title rows. */
    public static final int HEADER_HEIGHT = 40;
    /** The 1.12 body begins below its five-pixel top border. */
    public static final int CONTENT_OFFSET = HEADER_HEIGHT - 5;
    private final BinnieBeesAlvearyBlockEntity alveary;
    private final int componentSlots;
    private int energyStored;
    private int energyCapacity;
    private int powerUsage;
    private int powered;
    private final String ownerName;

    public static AlvearyMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        if (!(inventory.player.level().getBlockEntity(pos) instanceof BinnieBeesAlvearyBlockEntity alveary)) {
            throw new IllegalStateException("Missing Binnie's Bees alveary component at " + pos);
        }
        return new AlvearyMenu(containerId, inventory, alveary, buffer.readUtf());
    }

    public AlvearyMenu(int containerId, Inventory playerInventory, BinnieBeesAlvearyBlockEntity alveary) {
        this(containerId, playerInventory, alveary, alveary.ownerName());
    }

    private AlvearyMenu(int containerId, Inventory playerInventory, BinnieBeesAlvearyBlockEntity alveary, String ownerName) {
        super(BinnieBeesMenus.ALVEARY.get(), containerId);
        this.alveary = alveary;
        this.componentSlots = alveary.partType().inventorySlots();
        this.ownerName = ownerName == null ? "" : ownerName;
        addComponentSlots();
        addPlayerInventory(playerInventory);
        addDataSlot(data(() -> alveary.energyStored(), value -> energyStored = value));
        addDataSlot(data(() -> alveary.energyCapacity(), value -> energyCapacity = value));
        addDataSlot(data(() -> alveary.stimulatorPowerUsage(), value -> powerUsage = value));
        addDataSlot(data(() -> alveary.isStimulatorPowered() ? 1 : 0, value -> powered = value));
    }

    private static DataSlot data(java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        return new DataSlot() {
            @Override public int get() { return getter.getAsInt(); }
            @Override public void set(int value) { setter.accept(value); }
        };
    }

    private void addComponentSlots() {
        switch (alveary.partType()) {
            case MUTATOR -> addSlot(componentSlot(0, 81, 29 + CONTENT_OFFSET));
            case FRAME -> addSlot(componentSlot(0, 80, 37 + CONTENT_OFFSET));
            // The original 256px GUI sheets bake their frames at distinct
            // positions. These are the item origins inside those frames.
            case STIMULATOR -> addSlot(componentSlot(0, 44, 37 + CONTENT_OFFSET));
            case HATCHERY -> {
                for (int slot = 0; slot < 5; slot++) addSlot(componentSlot(slot, 44 + slot * 18, 37 + CONTENT_OFFSET));
            }
            default -> { }
        }
    }

    /** Makes the container UI enforce the same 1.12 validator as automation. */
    private Slot componentSlot(int index, int x, int y) {
        return new Slot(alveary.getInternalInventory(), index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return alveary.acceptsItem(index, stack);
            }
        };
    }

    private void addPlayerInventory(Inventory playerInventory) {
    // The verified Stimulator slot is two pixels above the old source
    // layout; retain that same vertical correction for every module.
        int y = (alveary.partType() == AlvearyPartType.MUTATOR ? 84 : 63) + CONTENT_OFFSET;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + column, 8 + column * 18, y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, y + 58));
        }
    }

    public BinnieBeesAlvearyBlockEntity alveary() { return alveary; }
    public AlvearyPartType partType() { return alveary.partType(); }
    public int energyStored() { return alveary.getLevel() != null && !alveary.getLevel().isClientSide ? alveary.energyStored() : energyStored; }
    public int energyCapacity() { return alveary.getLevel() != null && !alveary.getLevel().isClientSide ? alveary.energyCapacity() : energyCapacity; }
    public int powerUsage() { return alveary.getLevel() != null && !alveary.getLevel().isClientSide ? alveary.stimulatorPowerUsage() : powerUsage; }
    public boolean powered() { return alveary.getLevel() != null && !alveary.getLevel().isClientSide ? alveary.isStimulatorPowered() : powered != 0; }
    public String ownerName() { return ownerName; }

    @Override
    public boolean stillValid(Player player) {
        return !alveary.isRemoved() && player.distanceToSqr(alveary.getBlockPos().getCenter()) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < componentSlots) {
            if (!moveItemStackTo(original, componentSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int target = 0; target < componentSlots; target++) {
                if (alveary.acceptsItem(target, original) && moveItemStackTo(original, target, target + 1, false)) {
                    moved = true;
                    break;
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, original);
        return copy;
    }
}
