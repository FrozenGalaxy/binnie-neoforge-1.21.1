package binnie.core.machine;

import binnie.genetics.machine.GeneticsMachineLayout;
import binnie.genetics.machine.GeneticsMachineInventoryLayout;
import binnie.trees.machine.BinnieTreesMachineInventoryLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;

public final class MachineMenu extends AbstractContainerMenu {
    private final MachineBlockEntity machine;
    private final ContainerData data;
    private final int machineSlots;
    private final String ownerName;

    public static MachineMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        if (!(inventory.player.level().getBlockEntity(pos) instanceof MachineBlockEntity machine)) {
            throw new IllegalStateException("Missing Binnie machine at " + pos);
        }
        String ownerName = buffer.isReadable() ? buffer.readUtf() : "";
        return new MachineMenu(containerId, inventory, machine, new SimpleContainerData(16), ownerName);
    }

    public MachineMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        this(containerId, playerInventory, machine, data, machine.ownerName());
    }

    private MachineMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data, String ownerName) {
        super(MachineRegistries.MACHINE_MENU.get(), containerId);
        this.machine = machine;
        this.data = data;
        this.machineSlots = machine.inventory().getSlots();
        this.ownerName = ownerName == null ? "" : ownerName;
        boolean manualDesigner = machine.isManualCrafting();
        GeneticsMachineLayout.Layout geneticsLayout = GeneticsMachineLayout.forMachine(machine.spec().id()).orElse(null);
        MachineUiLayout sourceLayout = MachineUiLayout.forMachine(machine.spec().id()).orElse(null);
        int inputX = manualDesigner ? 22 : geneticsLayout == null ? 26 : geneticsLayout.inputX();
        int inputY = 35;
        int catalystX = geneticsLayout == null ? 62 : geneticsLayout.catalystX();
        int catalystY = 35;
        int reagentX = manualDesigner ? 42 : geneticsLayout == null ? 80 : geneticsLayout.reagentX();
        int reagentY = manualDesigner ? 65 : geneticsLayout == null ? 58 : geneticsLayout.reagentY();
        int outputX = manualDesigner ? -1_000 : geneticsLayout == null ? 116 : geneticsLayout.outputX();
        int outputY = 35;
        int secondaryX = manualDesigner ? -1_000 : geneticsLayout == null ? 140 : geneticsLayout.secondaryX();
        int secondaryY = 35;
        int tertiaryX = manualDesigner ? -1_000 : geneticsLayout == null ? 140 : geneticsLayout.tertiaryX();
        int tertiaryY = 58;
        if (geneticsLayout != null && !manualDesigner) {
            inputY = geneticsLayout.inputY();
            catalystY = geneticsLayout.catalystY();
            outputY = geneticsLayout.outputY();
            secondaryY = geneticsLayout.secondaryY();
            tertiaryY = geneticsLayout.tertiaryY();
        } else if (sourceLayout != null && !manualDesigner) {
            MachineUiLayout.Point point = sourceLayout.input().itemArea();
            inputX = point.x(); inputY = point.y();
            point = sourceLayout.catalyst().itemArea();
            catalystX = point.x(); catalystY = point.y();
            point = sourceLayout.reagent().itemArea();
            reagentX = point.x(); reagentY = point.y();
            point = sourceLayout.output().itemArea();
            outputX = point.x(); outputY = point.y();
            point = sourceLayout.secondaryOutput().itemArea();
            secondaryX = point.x(); secondaryY = point.y();
            point = sourceLayout.tertiaryOutput().itemArea();
            tertiaryX = point.x(); tertiaryY = point.y();
        }
        addSlot(new SlotItemHandler(machine.inventory(), MachineBlockEntity.INPUT_SLOT, inputX, inputY));
        addSlot(new SlotItemHandler(machine.inventory(), MachineBlockEntity.CATALYST_SLOT, catalystX, catalystY));
        addSlot(new SlotItemHandler(machine.inventory(), MachineBlockEntity.REAGENT_SLOT, reagentX, reagentY));
        addSlot(new SlotItemHandler(machine.inventory(), MachineBlockEntity.OUTPUT_SLOT, outputX, outputY) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new SlotItemHandler(machine.inventory(), MachineBlockEntity.SECONDARY_OUTPUT_SLOT, secondaryX, secondaryY) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new SlotItemHandler(machine.inventory(), MachineBlockEntity.TERTIARY_OUTPUT_SLOT, tertiaryX, tertiaryY) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (GeneticsMachineInventoryLayout.SlotDefinition definition : GeneticsMachineInventoryLayout.extraSlots(machine.spec().id())) {
            addSlot(new SlotItemHandler(machine.inventory(), definition.slot(), definition.x(), definition.y()) {
                @Override public boolean mayPlace(ItemStack stack) { return definition.mode() == GeneticsMachineInventoryLayout.SlotMode.INPUT && super.mayPlace(stack); }
                @Override public boolean mayPickup(Player player) { return definition.mode() == GeneticsMachineInventoryLayout.SlotMode.OUTPUT; }
            });
        }
        for (BinnieTreesMachineInventoryLayout.SlotDefinition definition : BinnieTreesMachineInventoryLayout.extraSlots(machine.spec().id())) {
            addSlot(new SlotItemHandler(machine.inventory(), definition.slot(), definition.x(), definition.y()) {
                @Override public boolean mayPlace(ItemStack stack) { return definition.mode() == BinnieTreesMachineInventoryLayout.Mode.INPUT && super.mayPlace(stack); }
                @Override public boolean mayPickup(Player player) { return definition.mode() != BinnieTreesMachineInventoryLayout.Mode.PROCESS_RESERVE; }
            });
        }
        addPlayerInventory(playerInventory, manualDesigner, geneticsLayout, sourceLayout);
        addDataSlots(data);
    }

    private void addPlayerInventory(Inventory inventory, boolean manualDesigner, GeneticsMachineLayout.Layout geneticsLayout,
                                    MachineUiLayout sourceLayout) {
        int left = manualDesigner ? 14 : geneticsLayout == null ? 8 : geneticsLayout.playerX();
        int top = manualDesigner ? 96 : geneticsLayout == null ? 84 : geneticsLayout.playerY();
        if (sourceLayout != null && !manualDesigner && geneticsLayout == null) {
            MachineUiLayout.Point player = sourceLayout.playerInventory().itemArea();
            left = player.x();
            top = player.y();
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, left + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, left + col * 18, top + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(original, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()), player, machine.getBlockState().getBlock());
    }

    public net.minecraft.resources.ResourceLocation machineId() { return machine.spec().id(); }
    public int machineSlotCount() { return machineSlots; }

    public int progress() { return data.get(0); }
    public int maxProgress() { return data.get(1); }
    public int energy() { return data.get(2); }
    public int maxEnergy() { return data.get(3); }
    public int maxEnergyInput() { return data.get(15); }
    public int fluid() { return data.get(4); }
    public int fluidCapacity() { return data.get(5); }
    /** Client-synchronised primary tank identity for 1.12 GUI rendering. */
    public FluidStack fluidStack() { return machine.tank().getFluid(); }
    public int secondaryFluid() { return data.get(6); }
    public int secondaryFluidCapacity() { return data.get(7); }
    /** Client-synchronised secondary tank identity for 1.12 GUI rendering. */
    public FluidStack secondaryFluidStack() { return machine.secondaryTank().getFluid(); }
    public int outputFluid() { return data.get(8); }
    public int outputFluidCapacity() { return data.get(9); }
    /** Client-synchronised output tank identity for 1.12 GUI rendering. */
    public FluidStack outputFluidStack() { return machine.outputTank().getFluid(); }
    public int mode() { return data.get(10); }
    public boolean setMode(int mode) { return machine.setMode(mode); }
    public boolean isManualCrafting() { return machine.isManualCrafting(); }
    public int manualSelection() { return data.get(11); }
    public MachineProcessError processError() { return MachineProcessError.byId(data.get(12)); }
    public MachineRedstoneMode redstoneMode() { return MachineRedstoneMode.byId(data.get(13)); }
    public boolean redstonePowered() { return data.get(14) != 0; }
    public boolean setRedstoneMode(int mode) { return machine.setRedstoneMode(mode); }
    public boolean setManualSelection(int selection) { return machine.setManualSelection(selection); }
    public boolean handleScreenAction(binnie.core.network.MachineScreenActionPayload.Action action) {
        return machine.handleScreenAction(action.name());
    }
    /** Identity shown by the retained 1.12 CraftGUI user control. */
    public String ownerName() { return ownerName; }
    public ItemStack manualPreview() { return machine.manualPreview(); }
    /** Source-only hidden process slots (for example Fruit Press's current-fruit cradle). */
    public ItemStack sourceStack(int slot) { return slot >= 0 && slot < machine.inventory().getSlots() ? machine.inventory().getStackInSlot(slot) : ItemStack.EMPTY; }
    public boolean takeManualPreview() {
        ItemStack product = machine.takeManualPreview(getCarried());
        if (product.isEmpty()) return false;
        ItemStack carried = getCarried();
        if (carried.isEmpty()) setCarried(product);
        else {
            carried.grow(product.getCount());
            setCarried(carried);
        }
        broadcastChanges();
        return true;
    }
}
