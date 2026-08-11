package binnie.core.machine;

import java.util.Optional;
import java.util.UUID;

import binnie.genetics.machine.GeneticsMachineInventoryLayout;
import binnie.trees.machine.BinnieTreesMachineInventoryLayout;
import binnie.genetics.registry.GeneticsFluids;
import forestry.core.platform.fluids.ForestryFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public final class MachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int CATALYST_SLOT = 1;
    public static final int REAGENT_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int SECONDARY_OUTPUT_SLOT = 4;
    public static final int TERTIARY_OUTPUT_SLOT = 5;

    private final MachineSpec spec;
    private final ItemStackHandler inventory;
    private final TrackedEnergyStorage energy;
    private final FluidTank tank;
    private final FluidTank secondaryTank;
    private final FluidTank outputTank;
    private final IFluidHandler fluidHandler;
    private final IFluidHandler inputFluidHandler;
    private final IFluidHandler outputFluidHandler;
    private final MachineSidedItemHandler inputItemHandler;
    private final MachineSidedItemHandler outputItemHandler;
    private int progress;
    private int maxProgress;
    private ResourceLocation activeRecipe;
    private int mode;
    private MachineProcessError processError = MachineProcessError.NONE;
    private MachineRedstoneMode redstoneMode = MachineRedstoneMode.IGNORED;
    private long processNonce;
    private CompoundTag processData = new CompoundTag();
    // 1.12 ComponentDesignerRecipe starts on Diamond, the third registered design.
    private int manualSelection = 2;
    @Nullable
    private UUID owner;
    @Nullable
    private String ownerName;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energy.getEnergyStored();
                case 3 -> energy.getMaxEnergyStored();
                case 4 -> tank.getFluidAmount();
                case 5 -> tank.getCapacity();
                case 6 -> secondaryTank.getFluidAmount();
                case 7 -> secondaryTank.getCapacity();
                case 8 -> outputTank.getFluidAmount();
                case 9 -> outputTank.getCapacity();
                case 10 -> mode;
                case 11 -> manualSelection;
                case 12 -> processError.ordinal();
                case 13 -> redstoneMode.ordinal();
                case 14 -> level != null && level.hasNeighborSignal(worldPosition) ? 1 : 0;
                case 15 -> energy.maxReceive();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 1) maxProgress = value;
            if (index == 11) manualSelection = value;
        }

        @Override
        public int getCount() {
            return 16;
        }
    };

    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (!(state.getBlock() instanceof MachineBlock block)) {
            throw new IllegalStateException("Machine block entity created for non-machine block " + state);
        }
        this.spec = block.spec();
        int inventorySize = Math.max(GeneticsMachineInventoryLayout.inventorySize(spec.id()), BinnieTreesMachineInventoryLayout.inventorySize(spec.id()));
        this.inventory = new ItemStackHandler(inventorySize) {
            /**
              * ItemStackHandler normally trusts the serialized {@code Size}
              * field and shrinks itself while loading. That would turn an old
              * six-slot save back into six slots and crash the new source-array
              * automation. The block's registry ID owns its layout, so load
              * old entries into the current fixed capacity instead.
              */
            @Override
            public void setSize(int ignoredSerializedSize) {
                super.setSize(inventorySize);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChangedAndSync();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                MachineProcessHandler handler = MachineProcessRegistry.get(spec.id());
                return handler == null ? slot != OUTPUT_SLOT && slot != SECONDARY_OUTPUT_SLOT && slot != TERTIARY_OUTPUT_SLOT
                        : handler.isItemValid(MachineBlockEntity.this, slot, stack);
            }
        };
        int maxTransfer = Math.max(1_000, Math.max(spec.energyPerTick() * 20, spec.energyCapacity()));
        this.energy = new TrackedEnergyStorage(spec.energyCapacity(), maxTransfer, maxTransfer, this::setChangedAndSync);
        this.tank = trackedTank(tankCapacity(0), 0);
        this.secondaryTank = trackedTank(tankCapacity(1), 1);
        this.outputTank = trackedTank(tankCapacity(2), 2);
        this.fluidHandler = new MachineFluidHandler(tank, secondaryTank, outputTank, MachineFluidHandler.Access.FULL);
        this.inputFluidHandler = new MachineFluidHandler(tank, secondaryTank, outputTank, MachineFluidHandler.Access.INPUT);
        this.outputFluidHandler = new MachineFluidHandler(tank, secondaryTank, outputTank, MachineFluidHandler.Access.OUTPUT);
        this.inputItemHandler = new MachineSidedItemHandler(inventory, inputSlots(), true, false);
        this.outputItemHandler = new MachineSidedItemHandler(inventory, outputSlots(), false, true);
    }

    private FluidTank trackedTank(int capacity, int tankIndex) {
    // The output tank is never offered to external fill operations by
    // MachineFluidHandler, but it must accept the machine's own process
    // output. Applying the input validator here blocked Incubator and
    // Genepool output production entirely.
        return new FluidTank(capacity, tankIndex == 2 ? stack -> !stack.isEmpty() : stack -> acceptsFluid(tankIndex, stack)) {
            @Override
            protected void onContentsChanged() {
                setChangedAndSync();
            }
        };
    }

    /** Machines with a single 5,000 mB vessel must not inherit the
      * generic 16,000 mB three-tank capacity. */
    private int tankCapacity(int tankIndex) {
        if ("binnietrees".equals(spec.id().getNamespace())
                && (spec.id().getPath().equals("press") || spec.id().getPath().equals("brewery"))) {
            return 5_000;
        }
        if ("binnietrees".equals(spec.id().getNamespace()) && spec.id().getPath().equals("lumbermill")) {
            return 10_000;
        }
        return spec.fluidCapacity();
    }

    /** 1.12 tank validators, retained so external pipes cannot cross-route laboratory liquids. */
    private boolean acceptsFluid(int tankIndex, FluidStack stack) {
    // Other Binnie machine packages retain their established tank rules. Genetics is
    // the package with 1.12, distinct laboratory-liquid tank roles.
        if (!"binniegenetics".equals(spec.id().getNamespace())) return true;
        if (stack.isEmpty()) return false;
        return switch (spec.id().getPath()) {
            case "isolator", "genepool" -> tankIndex == 0 && stack.is(ForestryFluids.BIO_ETHANOL.getFluid(1).getFluid());
            case "polymeriser" -> tankIndex == 0
                    ? stack.is(GeneticsFluids.POLYMERISING_BACTERIA.fluid())
                    : stack.is(GeneticsFluids.RAW_DNA.fluid());
            case "inoculator" -> tankIndex == 0 && stack.is(GeneticsFluids.VECTOR_BACTERIA.fluid());
            case "incubator" -> tankIndex == 0 && (stack.is(Fluids.WATER)
                    || stack.is(GeneticsFluids.GROWTH_MEDIUM.fluid())
                    || stack.is(GeneticsFluids.BACTERIA.fluid())
                    || stack.is(GeneticsFluids.POLYMERISING_BACTERIA.fluid())
                    || stack.is(GeneticsFluids.VECTOR_BACTERIA.fluid()));
            default -> false;
        };
    }

    private int[] inputSlots() {
        return java.util.stream.IntStream.range(0, inventory.getSlots())
                .filter(slot -> !isOutputSlot(slot))
                .toArray();
    }

    private int[] outputSlots() {
        return java.util.stream.IntStream.range(0, inventory.getSlots())
                .filter(this::isOutputSlot)
                .toArray();
    }

    private boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT || slot == SECONDARY_OUTPUT_SLOT || slot == TERTIARY_OUTPUT_SLOT
                || GeneticsMachineInventoryLayout.isOutput(spec.id(), slot);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        MachineProcessHandler handler = MachineProcessRegistry.get(machine.spec.id());
        if (handler != null) handler.beforeTick(level, machine);
        if (!machine.redstoneMode.allows(level.hasNeighborSignal(pos))) {
            machine.setProcessError(MachineProcessError.REDSTONE_PAUSED);
            return;
        }
        if (handler != null) {
            MachineProcessError diagnosis = handler.diagnose(level, machine);
            machine.setProcessError(diagnosis);
            if (handler.isManualCrafting(machine)) {
                machine.resetProgress();
                return;
            }
            if (handler.handlesIndefiniteProcess(machine)) {
                handler.tickIndefiniteProcess(level, machine);
                return;
            }
            Optional<MachineProcessPlan> dynamic = handler.createPlan(level, machine);
            if (dynamic.isPresent()) {
                machine.tickDynamic(handler, dynamic.get());
                return;
            }
            // Keep the handler's specific failure instead of replacing it with NO_RECIPE.
            if (diagnosis != MachineProcessError.NONE) {
                machine.resetProgress();
                return;
            }
        }

        ItemStack input = machine.inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            machine.setProcessError(MachineProcessError.NO_ITEM);
            machine.resetProgress();
            return;
        }
        Optional<RecipeHolder<MachineRecipe>> match = findRecipe(level, machine.spec.id(), input, machine.tank.getFluid());
        if (match.isEmpty()) {
            machine.setProcessError(MachineProcessError.NO_RECIPE);
            machine.resetProgress();
            return;
        }
        if (!machine.canAccept(match.get().value().output())) {
            machine.setProcessError(MachineProcessError.NO_SPACE);
            return;
        }

        RecipeHolder<MachineRecipe> holder = match.get();
        MachineRecipe recipe = holder.value();
        boolean processStateChanged = !holder.id().equals(machine.activeRecipe);
        if (processStateChanged) {
            machine.activeRecipe = holder.id();
            machine.progress = 0;
        }
        machine.maxProgress = recipe.duration();
        if (processStateChanged) machine.setChangedAndSync();
        if (!machine.consumeTickEnergy(recipe.energyPerTick())) {
            machine.setProcessError(MachineProcessError.NO_POWER);
            return;
        }
        machine.setProcessError(MachineProcessError.NONE);
        machine.advanceProgress();
        if (machine.progress >= machine.maxProgress) {
            machine.inventory.extractItem(INPUT_SLOT, recipe.inputCount(), false);
            if (!recipe.fluidInput().isEmpty()) machine.tank.drain(recipe.fluidInput(), IFluidHandler.FluidAction.EXECUTE);
            machine.insertOutput(OUTPUT_SLOT, recipe.output());
            machine.finishProcess();
        }
    }

    private void tickDynamic(MachineProcessHandler handler, MachineProcessPlan plan) {
        if (!canAccept(OUTPUT_SLOT, plan.output())
                || !canAccept(SECONDARY_OUTPUT_SLOT, plan.secondaryOutput())
                || !canAccept(TERTIARY_OUTPUT_SLOT, plan.tertiaryOutput())
                || (!handler.controlsFluidProgress() && !hasFluid(tank, plan.primaryFluidInput()))
                || (!handler.controlsFluidProgress() && !hasFluid(secondaryTank, plan.secondaryFluidInput()))
                || (!handler.controlsFluidProgress() && !canAcceptFluid(plan.fluidOutput()))) {
            setProcessError((!handler.controlsFluidProgress() && !canAcceptFluid(plan.fluidOutput())) ? MachineProcessError.OUTPUT_TANK_FULL : MachineProcessError.OUTPUT_BLOCKED);
            return;
        }
        boolean processStateChanged = !plan.id().equals(activeRecipe);
        boolean durationChanged = !processStateChanged && maxProgress > 0 && maxProgress != plan.duration();
        if (processStateChanged) {
            activeRecipe = plan.id();
            progress = Math.max(0, Math.min(plan.duration() - 1, handler.initialProgress(this, plan)));
            handler.onStarted(level, this, plan);
        } else if (durationChanged) {
            // 1.12 ComponentProcess stores percent progress, so a live
            // catalyst-time change must retain the completed fraction.
            progress = (int) ((long) progress * plan.duration() / maxProgress);
        }
        maxProgress = plan.duration();
        if (processStateChanged || durationChanged) setChangedAndSync();
        if (!handler.canProgress(level, this, plan)) {
            setProcessError(handler.cannotProgressError(level, this, plan));
            return;
        }
        if (!consumeTickEnergy(plan.energyPerTick())) {
            setProcessError(MachineProcessError.NO_POWER);
            return;
        }
        setProcessError(MachineProcessError.NONE);
        advanceProgress();
        handler.onProgress(level, this, plan, progress, maxProgress);
        if (progress < maxProgress) return;

        if (level != null) handler.beforeCompletion(level, this, plan);
        if (plan.inputCount() > 0) inventory.extractItem(INPUT_SLOT, plan.inputCount(), false);
        if (plan.catalystCount() > 0) inventory.extractItem(CATALYST_SLOT, plan.catalystCount(), false);
        if (plan.reagentCount() > 0) inventory.extractItem(REAGENT_SLOT, plan.reagentCount(), false);
        for (int slot : plan.extraConsumedSlots()) inventory.extractItem(slot, 1, false);
        if (!plan.catalystAfter().isEmpty()) inventory.setStackInSlot(CATALYST_SLOT, plan.catalystAfter().copy());
        if (!plan.reagentAfter().isEmpty()) inventory.setStackInSlot(REAGENT_SLOT, plan.reagentAfter().copy());
        if (!plan.primaryFluidInput().isEmpty()) tank.drain(plan.primaryFluidInput(), IFluidHandler.FluidAction.EXECUTE);
        if (!plan.secondaryFluidInput().isEmpty()) secondaryTank.drain(plan.secondaryFluidInput(), IFluidHandler.FluidAction.EXECUTE);
        insertOutput(OUTPUT_SLOT, plan.output());
        insertOutput(SECONDARY_OUTPUT_SLOT, plan.secondaryOutput());
        insertOutput(TERTIARY_OUTPUT_SLOT, plan.tertiaryOutput());
        if (!plan.fluidOutput().isEmpty() && !handler.controlsFluidOutputProgress()) outputTank.fill(plan.fluidOutput(), IFluidHandler.FluidAction.EXECUTE);
        if (level != null) handler.onCompleted(level, this, plan);
        finishProcess();
    }


    private void advanceProgress() {
        int previousSignal = comparatorOutput();
        progress++;
        setChanged();
        notifyComparatorIfChanged(previousSignal);
    }

    private void notifyComparatorIfChanged(int previousSignal) {
        if (level != null && !level.isClientSide && previousSignal != comparatorOutput()) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private boolean consumeTickEnergy(int amount) {
        if (amount == 0) return true;
        if (energy.extractEnergy(amount, true) < amount) return false;
        energy.extractEnergy(amount, false);
        return true;
    }

    private void finishProcess() {
        progress = 0;
        maxProgress = 0;
        activeRecipe = null;
        processNonce++;
        setChangedAndSync();
    }

    private static Optional<RecipeHolder<MachineRecipe>> findRecipe(Level level, ResourceLocation machineId, ItemStack input, FluidStack fluid) {
        if (input.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getAllRecipesFor(MachineRegistries.MACHINE_RECIPE_TYPE.get()).stream()
                .filter(holder -> holder.value().matches(machineId, input, fluid))
                .findFirst();
    }

    private boolean canAccept(ItemStack result) {
        return canAccept(OUTPUT_SLOT, result);
    }

    private boolean canAccept(int slot, ItemStack result) {
        if (result.isEmpty()) return true;
        ItemStack output = inventory.getStackInSlot(slot);
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private static boolean hasFluid(FluidTank tank, FluidStack required) {
        return required.isEmpty() || tank.drain(required, IFluidHandler.FluidAction.SIMULATE).getAmount() >= required.getAmount();
    }

    private boolean canAcceptFluid(FluidStack result) {
        if (result.isEmpty()) return true;
        return outputTank.fill(result, IFluidHandler.FluidAction.SIMULATE) == result.getAmount();
    }

    /** Inserts a completed process result without applying the external slot validator. */
    private void insertOutput(int slot, ItemStack result) {
        if (result.isEmpty()) return;
        ItemStack output = inventory.getStackInSlot(slot);
        if (output.isEmpty()) {
            inventory.setStackInSlot(slot, result.copy());
            return;
        }
        if (!ItemStack.isSameItemSameComponents(output, result)
                || output.getCount() + result.getCount() > output.getMaxStackSize()) {
            throw new IllegalStateException("Process completed with an incompatible output slot");
        }
        ItemStack merged = output.copy();
        merged.grow(result.getCount());
        inventory.setStackInSlot(slot, merged);
    }

    private void resetProgress() {
        if (progress != 0 || maxProgress != 0 || activeRecipe != null) {
            progress = 0;
            maxProgress = 0;
            activeRecipe = null;
            setChangedAndSync();
        }
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    /** Removes all machine inventory stacks exactly once for block-removal drops. */
    public java.util.List<ItemStack> removeAllInventoryContents() {
        java.util.List<ItemStack> contents = new java.util.ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack removed = inventory.extractItem(slot, Integer.MAX_VALUE, false);
            if (!removed.isEmpty()) contents.add(removed);
        }
        return contents;
    }

    /**
      * Starts or clears the visible state for a 1.12 processor with no finite
      * duration. Returns true on the start tick, which 1.12 indefinite
      * processors use to initialize without consuming their first power tick.
      */
    public boolean setIndefiniteProcess(ResourceLocation id, boolean active) {
        if (!active) {
            resetProgress();
            return false;
        }
        if (!id.equals(activeRecipe) || maxProgress != 1 || progress != 0) {
            activeRecipe = id;
            progress = 0;
            maxProgress = 1;
            setChangedAndSync();
            return true;
        }
        return false;
    }

    /** Uses one process tick of power without exposing storage mutation. */
    public boolean consumeProcessEnergy(int amount) {
        return consumeTickEnergy(amount);
    }

    /** Checks a machine output slot without applying its external insertion validator. */
    public boolean canAcceptProcessOutput(int slot, ItemStack result) {
        return canAccept(slot, result);
    }

    /** Checks whether the dedicated output tank can receive a process result. */
    public boolean canAcceptProcessFluid(FluidStack result) {
        return canAcceptFluid(result);
    }

    /** Inserts a validated process item result without applying an external insertion validator. */
    public void insertProcessOutput(int slot, ItemStack result) {
        insertOutput(slot, result);
    }

    public IEnergyStorage energy() {
        return energy;
    }

    public FluidTank tank() {
        return tank;
    }

    public FluidTank secondaryTank() {
        return secondaryTank;
    }

    public FluidTank outputTank() {
        return outputTank;
    }

    public IFluidHandler fluidHandler() {
        return fluidHandler;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null || side.getAxis().isHorizontal()) return fluidHandler;
        return side == Direction.DOWN ? outputFluidHandler : inputFluidHandler;
    }

    public net.neoforged.neoforge.items.IItemHandler itemHandler(@Nullable Direction side) {
        if (side == null || side.getAxis().isHorizontal()) return inventory;
        return side == Direction.DOWN ? outputItemHandler : inputItemHandler;
    }

    public MachineSpec spec() {
        return spec;
    }

    public ContainerData data() {
        return data;
    }

    public int progress() { return progress; }
    public int maxProgress() { return maxProgress; }
    public ResourceLocation activeProcess() { return activeRecipe; }
    public long processNonce() { return processNonce; }
    /** Persisted opaque state owned by the current process handler. */
    public CompoundTag processData() { return processData; }
    public void setProcessData(CompoundTag data) { processData = data.copy(); setChangedAndSync(); }
    public void clearProcessData() { if (!processData.isEmpty()) { processData = new CompoundTag(); setChangedAndSync(); } }
    public MachineProcessError processError() { return processError; }
    public void setProcessError(MachineProcessError error) {
        MachineProcessError normalized = error == null ? MachineProcessError.NONE : error;
        if (normalized != processError) {
            processError = normalized;
            setChangedAndSync();
        }
    }
    public MachineRedstoneMode redstoneMode() { return redstoneMode; }
    public boolean setRedstoneMode(int requested) {
        if (requested < 0 || requested >= MachineRedstoneMode.values().length) return false;
        MachineRedstoneMode next = MachineRedstoneMode.byId(requested);
        if (redstoneMode != next) {
            redstoneMode = next;
            setChangedAndSync();
        }
        return true;
    }

    /** Applies a validated screen action on the logical server. */
    public boolean handleScreenAction(String action) {
        if (level == null || level.isClientSide || action == null) return false;
        MachineProcessHandler handler = MachineProcessRegistry.get(spec.id());
        return handler != null && handler.handleScreenAction(level, this, action);
    }

    /** Manual controls advance finite work in machine ticks. */
    public boolean addManualProgress(int ticks) {
        if (ticks <= 0 || maxProgress <= 0 || activeRecipe == null) return false;
        progress = Math.min(maxProgress, progress + ticks);
        setChangedAndSync();
        return true;
    }

    /** Cancels only the visible progress state; handler-owned inputs remain intact. */
    public void cancelProcessProgress() { resetProgress(); }

    public int comparatorOutput() {
        if (maxProgress > 0) return Math.max(1, Math.min(15, (int) Math.ceil(progress * 15.0 / maxProgress)));
        double fullness = 0.0;
        int counted = 0;
        for (int slot : outputSlots()) {
            ItemStack stack = inventory.getStackInSlot(slot);
            fullness += stack.isEmpty() ? 0.0 : (double) stack.getCount() / Math.max(1, stack.getMaxStackSize());
            counted++;
        }
        if (outputTank.getCapacity() > 0) {
            fullness += (double) outputTank.getFluidAmount() / outputTank.getCapacity();
            counted++;
        }
        if (counted == 0 || fullness <= 0.0) return 0;
        return Math.min(15, 1 + (int) Math.floor(14.0 * fullness / counted));
    }

    public int mode() { return mode; }
    public boolean setMode(int requested) {
        MachineProcessHandler handler = MachineProcessRegistry.get(spec.id());
        int max = handler == null ? 0 : Math.max(0, handler.maxMode(this));
        if (requested < 0 || requested > max) return false;
        if (mode != requested) {
            mode = requested;
            resetProgress();
            setChangedAndSync();
        }
        return true;
    }

    public boolean isManualCrafting() {
        MachineProcessHandler handler = MachineProcessRegistry.get(spec.id());
        return handler != null && handler.isManualCrafting(this);
    }

    public int manualSelection() {
        return manualSelection;
    }

    public boolean setManualSelection(int selection) {
        MachineProcessHandler handler = MachineProcessRegistry.get(spec.id());
        if (handler == null || !handler.setManualSelection(this, selection)) return false;
        if (manualSelection != selection) {
            manualSelection = selection;
            setChangedAndSync();
        }
        return true;
    }

    /** Called only by a validated open menu; clients never supply an output stack. */
    public ItemStack takeManualPreview(ItemStack carried) {
        MachineProcessHandler handler = MachineProcessRegistry.get(spec.id());
        return handler == null ? ItemStack.EMPTY : handler.takeManualPreview(this, carried);
    }

    public ItemStack manualPreview() {
        MachineProcessHandler handler = MachineProcessRegistry.get(spec.id());
        return handler == null ? ItemStack.EMPTY : handler.getManualPreview(this);
    }
    public Optional<UUID> owner() { return Optional.ofNullable(owner); }

    /** Returns the owner's display name for the client-side user control. */
    public String ownerName() {
        if (ownerName != null && !ownerName.isBlank()) return ownerName;
        if (owner == null || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return "";
        String resolved = serverLevel.getServer().getProfileCache().get(owner)
                .map(com.mojang.authlib.GameProfile::getName)
                .orElse("");
        if (!resolved.isBlank()) {
            ownerName = resolved;
            setChangedAndSync();
        }
        return resolved;
    }

    public void setOwner(UUID owner) {
        setOwner(owner, null);
    }

    public void setOwner(UUID owner, @Nullable String ownerName) {
        if (owner == null) return;
        if (!owner.equals(this.owner) || (ownerName != null && !ownerName.equals(this.ownerName))) {
            this.owner = owner;
            if (ownerName != null && !ownerName.isBlank()) this.ownerName = ownerName;
            setChangedAndSync();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Energy", energy.serializeNBT(registries));
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("SecondaryTank", secondaryTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        if (activeRecipe != null) tag.putString("ActiveRecipe", activeRecipe.toString());
        if (owner != null) tag.putUUID("Owner", owner);
        if (ownerName != null && !ownerName.isBlank()) tag.putString("OwnerName", ownerName);
        tag.putInt("Mode", mode);
        tag.putInt("RedstoneMode", redstoneMode.ordinal());
        tag.putInt("ManualSelection", manualSelection);
        tag.putLong("ProcessNonce", processNonce);
        if (!processData.isEmpty()) tag.put("ProcessData", processData.copy());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("Energy")) energy.deserializeNBT(registries, tag.get("Energy"));
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        secondaryTank.readFromNBT(registries, tag.getCompound("SecondaryTank"));
        outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        activeRecipe = tag.contains("ActiveRecipe") ? ResourceLocation.tryParse(tag.getString("ActiveRecipe")) : null;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.contains("OwnerName") ? tag.getString("OwnerName") : null;
        mode = tag.getInt("Mode");
        redstoneMode = MachineRedstoneMode.byId(tag.getInt("RedstoneMode"));
        manualSelection = tag.contains("ManualSelection") ? tag.getInt("ManualSelection") : 2;
        processNonce = tag.getLong("ProcessNonce");
        processData = tag.contains("ProcessData") ? tag.getCompound("ProcessData").copy() : new CompoundTag();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
}
