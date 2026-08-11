package binnie.bees.block;

import binnie.bees.alveary.AlvearyMutagens;
import binnie.bees.alveary.StimulatorCircuit;
import binnie.bees.menu.AlvearyMenu;
import forestry.api.IForestryApi;
import forestry.api.apiculture.IBeeListener;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.apiculture.hives.IHiveFrame;
import forestry.api.core.circuits.ICircuit;
import forestry.api.core.circuits.ICircuitBoard;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.api.core.multiblock.IMultiblockComponent;
import forestry.apiculture.alveary.multiblock.MultiblockLogicAlveary;
import forestry.apiculture.alveary.multiblock.TileAlveary;
import forestry.core.engine.genetics.ItemGE;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.inventory.InventoryAdapterRestricted;
import forestry.core.content.energy.EnergyTransferMode;
import forestry.core.content.energy.ForestryEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 1.12-compatible implementation of all seven Binnie's Bees alveary components. */
public final class BinnieBeesAlvearyBlockEntity extends TileAlveary
        implements IAlvearyComponent.BeeModifier<MultiblockLogicAlveary>,
        IAlvearyComponent.BeeListener<MultiblockLogicAlveary>,
        IAlvearyComponent.Active<MultiblockLogicAlveary> {
    public static final String STIMULATOR_LAYOUT = "binnie.bees.stimulator";
    public static final net.minecraft.resources.ResourceLocation STIMULATOR_SOCKET =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("binniebees", "stimulator");

    private static final int STIMULATOR_CAPACITY = 1_000;
    private static final int STIMULATOR_RECEIVE = 500;
    private static final int TRANSMISSION_CAPACITY = 1_000;
    private static final int TRANSMISSION_RECEIVE = 500;
    private static final int TRANSMISSION_MAX_OUTPUT = 500;

    private final InventoryAdapterRestricted componentInventory;
    private final ForestryEnergyStorage energy;
    @Nullable
    private UUID owner;
    @Nullable
    private String ownerName;

    private final IBeeModifier modifier = new IBeeModifier() {
        @Override
        public Vec3i modifyTerritory(IGenome genome, Vec3i value) {
            return applyFrameAndStimulator(genome, value,
                    (delegate, current) -> delegate.modifyTerritory(genome, current));
        }

        @Override
        public float modifyMutationChance(IGenome genome, IGenome mate, IMutation<IBeeSpecies> mutation, float value) {
            float result = value;
            if (partType() == AlvearyPartType.MUTATOR) {
                result = Math.min(AlvearyMutagens.multiplier(componentInventory.getItem(0)) * result, 15.0F);
            }
            IBeeModifier frame = frameModifier();
            if (frame != null) result = frame.modifyMutationChance(genome, mate, mutation, result);
            if (partType() == AlvearyPartType.STIMULATOR && isStimulatorPowered()) {
                for (StimulatorCircuit circuit : stimulatorCircuits()) {
                    result = circuit.program().modifyMutationChance(genome, mate, mutation, result);
                }
            }
            return result;
        }

        @Override
        public float modifyAging(IGenome genome, @Nullable IGenome mate, float value) {
            float result = value;
            IBeeModifier frame = frameModifier();
            if (frame != null) result = frame.modifyAging(genome, mate, result);
            if (partType() == AlvearyPartType.STIMULATOR && isStimulatorPowered()) {
                for (StimulatorCircuit circuit : stimulatorCircuits()) {
                    result = circuit.program().modifyAging(genome, mate, result);
                }
            }
            return result;
        }

        @Override
        public float modifyProductionSpeed(IGenome genome, float value) {
            return applyFrameAndStimulator(genome, value,
                    (delegate, current) -> delegate.modifyProductionSpeed(genome, current));
        }

        @Override
        public float modifyPollination(IGenome genome, float value) {
            return applyFrameAndStimulator(genome, value,
                    (delegate, current) -> delegate.modifyPollination(genome, current));
        }

        @Override
        public float modifyGeneticDecay(IGenome genome, float value) {
            return applyFrameAndStimulator(genome, value,
                    (delegate, current) -> delegate.modifyGeneticDecay(genome, current));
        }

        @Override public boolean isSealed() { return booleanModifier(IBeeModifier::isSealed); }
        @Override public boolean isSunlightSimulated() { return booleanModifier(IBeeModifier::isSunlightSimulated); }
        @Override public boolean isHellish() { return booleanModifier(IBeeModifier::isHellish); }
        @Override public boolean providesFlowers() { return booleanModifier(IBeeModifier::providesFlowers); }
        @Override public boolean isClimateFullyTolerant() { return booleanModifier(IBeeModifier::isClimateFullyTolerant); }
        @Override public boolean isAlwaysActive(IGenome genome) {
            if (partType().baseModifier().isAlwaysActive(genome)) return true;
            IBeeModifier frame = frameModifier();
            if (frame != null && frame.isAlwaysActive(genome)) return true;
            if (partType() == AlvearyPartType.STIMULATOR && isStimulatorPowered()) {
                for (StimulatorCircuit circuit : stimulatorCircuits()) {
                    if (circuit.program().isAlwaysActive(genome)) return true;
                }
            }
            return false;
        }
    };

    private final IBeeListener listener = new IBeeListener() {
        @Override
        public void wearOutEquipment(int amount) {
            if (partType() == AlvearyPartType.FRAME) wearFrame(amount);
            if (partType() == AlvearyPartType.STIMULATOR) energy.drainEnergy(stimulatorPowerUsage());
        }

        @Override
        public void onQueenDeath() {
            if (partType() == AlvearyPartType.MUTATOR && !componentInventory.getItem(0).isEmpty()) {
                componentInventory.removeItem(0, 1);
                setChanged();
            }
        }
    };

    public BinnieBeesAlvearyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, state.getBlock().getDescriptionId(), pos, state);
        AlvearyPartType part = ((BinnieBeesAlvearyBlock) state.getBlock()).partType();
        this.componentInventory = new InventoryAdapterRestricted(part.inventorySlots(), "AlvearyComponent") {
            @Override
            public boolean canSlotAccept(int slotIndex, ItemStack stack) {
                return acceptsItem(slotIndex, stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                BinnieBeesAlvearyBlockEntity.this.setChanged();
            }
        };
        int maxTransfer = part == AlvearyPartType.TRANSMISSION ? TRANSMISSION_RECEIVE : STIMULATOR_RECEIVE;
        int capacity = part == AlvearyPartType.TRANSMISSION ? TRANSMISSION_CAPACITY : STIMULATOR_CAPACITY;
        this.energy = new ForestryEnergyStorage(maxTransfer, capacity, EnergyTransferMode.RECEIVE);
    }

    public AlvearyPartType partType() {
        return ((BinnieBeesAlvearyBlock) getBlockState().getBlock()).partType();
    }

    /** The 1.12 component machine carried its creator's full GameProfile. */
    public void setOwner(UUID owner, @Nullable String ownerName) {
        if (owner == null) return;
        if (!owner.equals(this.owner) || (ownerName != null && !ownerName.equals(this.ownerName))) {
            this.owner = owner;
            if (ownerName != null && !ownerName.isBlank()) this.ownerName = ownerName;
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String ownerName() {
        if (ownerName != null && !ownerName.isBlank()) return ownerName;
        if (owner == null || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return "";
        String resolved = serverLevel.getServer().getProfileCache().get(owner)
                .map(com.mojang.authlib.GameProfile::getName)
                .orElse("");
        if (!resolved.isBlank()) setOwner(owner, resolved);
        return resolved;
    }

    @Override public IBeeModifier getBeeModifier() { return modifier; }
    @Override public IBeeListener getBeeListener() { return listener; }
    @Override public boolean allowsAutomation() { return true; }
    @Override public IInventoryAdapter getInternalInventory() { return componentInventory; }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return partType().hasGui() ? new AlvearyMenu(windowId, inventory, this) : super.createMenu(windowId, inventory, player);
    }

    public boolean acceptsItem(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= partType().inventorySlots()) return false;
        return switch (partType()) {
            case MUTATOR -> AlvearyMutagens.accepts(stack);
            case FRAME -> stack.getItem() instanceof IHiveFrame;
            case STIMULATOR -> isStimulatorBoard(stack);
            case HATCHERY -> ItemGE.getLifeStage(stack) == BeeLifeStage.LARVAE;
            default -> false;
        };
    }

    public int energyStored() { return energy.getEnergyStored(); }
    public int energyCapacity() { return energy.getMaxEnergyStored(); }
    public int stimulatorPowerUsage() {
        int total = 0;
        for (StimulatorCircuit circuit : stimulatorCircuits()) total += circuit.program().powerUsage();
        return total;
    }
    public boolean isStimulatorPowered() {
        int usage = stimulatorPowerUsage();
        return partType() == AlvearyPartType.STIMULATOR && usage > 0 && energy.getEnergyStored() >= usage;
    }

    /** True while the Transmission contains FE available for distribution. */
    public boolean isTransmissionPowered() {
        return partType() == AlvearyPartType.TRANSMISSION && energy.getEnergyStored() > 0;
    }

    @Nullable
    public IEnergyStorage getEnergyHandler(@Nullable Direction side) {
        return (partType() == AlvearyPartType.STIMULATOR || partType() == AlvearyPartType.TRANSMISSION) && !isRemoved()
                ? energy : null;
    }

    @Override
    public void updateServer(int tickCount) {
        if (partType() == AlvearyPartType.HATCHERY && level != null && level.random.nextInt(2_400) == 0
                && !getErrorLogic().hasErrors()) {
            ItemStack queenStack = getBeeInventory().getQueen();
            var individual = ItemGE.getIndividual(queenStack);
            if (individual instanceof IBee bee) {
                insertLarvae(bee.createStack(BeeLifeStage.LARVAE));
            }
        } else if (partType() == AlvearyPartType.TRANSMISSION) {
            transmitEnergy();
        }
    }

    @Override public void updateClient(int tickCount) { }

    private void insertLarvae(ItemStack larvae) {
        ItemStack remaining = larvae.copy();
        for (int slot = 0; slot < componentInventory.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = componentInventory.getItem(slot);
            if (existing.isEmpty()) {
                componentInventory.setItem(slot, remaining);
                remaining = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(existing, remaining) && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                remaining.shrink(moved);
                componentInventory.setChanged();
            }
        }
    }

    private void transmitEnergy() {
        if (energy.getEnergyStored() <= 0) return;
        List<IEnergyStorage> targets = new ArrayList<>();
        for (IMultiblockComponent component : getMultiblockLogic().getController().getComponents()) {
            if (!(component instanceof BlockEntity target) || target == this) continue;
            // ComponentTransmission queried every alveary TileEntity's
            // Energy capability from NORTH, which includes Forestry's fan and
            // heater components as well as Binnie's Bees' stimulator.
            IEnergyStorage handler = Capabilities.EnergyStorage.BLOCK.getCapability(
                    level, target.getBlockPos(), target.getBlockState(), target, Direction.NORTH);
            if (handler != null) targets.add(handler);
        }
        if (targets.isEmpty()) return;
        int output = Math.max(1, Math.min(TRANSMISSION_MAX_OUTPUT, energy.getEnergyStored() / targets.size()));
        for (IEnergyStorage target : targets) {
            int received = target.receiveEnergy(Math.min(output, energy.getEnergyStored()), false);
            if (received > 0) energy.drainEnergy(received);
            if (energy.getEnergyStored() <= 0) break;
        }
        setChanged();
    }

    private boolean isStimulatorBoard(ItemStack stack) {
        ICircuitBoard board = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(stack);
    // Unlike the old broad chipset validator, do not accept a board that
    // can never affect this module. Genetics' integrated circuit is not a
    // Forestry circuit board, and an unprogrammed Forestry board likewise
    // has no StimulatorCircuit to apply.
        if (board == null) return false;
        for (ICircuit circuit : board.getCircuits()) {
            if (circuit instanceof StimulatorCircuit) return true;
        }
        return false;
    }

    private List<StimulatorCircuit> stimulatorCircuits() {
        if (partType() != AlvearyPartType.STIMULATOR || componentInventory.getContainerSize() == 0) return List.of();
        ICircuitBoard board = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(componentInventory.getItem(0));
        if (board == null) return List.of();
        List<StimulatorCircuit> result = new ArrayList<>();
        for (ICircuit circuit : board.getCircuits()) if (circuit instanceof StimulatorCircuit stimulator) result.add(stimulator);
        return List.copyOf(result);
    }

    @Nullable
    private IBeeModifier frameModifier() {
        if (partType() != AlvearyPartType.FRAME || componentInventory.getContainerSize() == 0) return null;
        ItemStack stack = componentInventory.getItem(0);
        return stack.getItem() instanceof IHiveFrame frame ? frame.getBeeModifier(stack) : null;
    }

    private void wearFrame(int amount) {
        ItemStack stack = componentInventory.getItem(0);
        if (!(stack.getItem() instanceof IHiveFrame frame)) return;
        var individual = ItemGE.getIndividual(getBeeInventory().getQueen());
        if (!(individual instanceof IBee queen)) return;
        componentInventory.setItem(0, frame.frameUsed(this, stack, queen, amount * 5));
        setChanged();
    }

    private float applyFrameAndStimulator(IGenome genome, float value, FloatModifier modifier) {
        float result = value;
        IBeeModifier frame = frameModifier();
        if (frame != null) result = modifier.apply(frame, result);
        if (partType() == AlvearyPartType.STIMULATOR && isStimulatorPowered()) {
            for (StimulatorCircuit circuit : stimulatorCircuits()) result = modifier.apply(circuit.program(), result);
        }
        return result;
    }

    private Vec3i applyFrameAndStimulator(IGenome genome, Vec3i value, TerritoryModifier modifier) {
        Vec3i result = value;
        IBeeModifier frame = frameModifier();
        if (frame != null) result = modifier.apply(frame, result);
        if (partType() == AlvearyPartType.STIMULATOR && isStimulatorPowered()) {
            for (StimulatorCircuit circuit : stimulatorCircuits()) result = modifier.apply(circuit.program(), result);
        }
        return result;
    }

    private boolean booleanModifier(BooleanModifier check) {
        if (check.apply(partType().baseModifier())) return true;
        IBeeModifier frame = frameModifier();
        if (frame != null && check.apply(frame)) return true;
        if (partType() == AlvearyPartType.STIMULATOR && isStimulatorPowered()) {
            for (StimulatorCircuit circuit : stimulatorCircuits()) if (check.apply(circuit.program())) return true;
        }
        return false;
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        componentInventory.read(tag, registries);
        energy.read(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.contains("OwnerName") ? tag.getString("OwnerName") : null;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        componentInventory.write(tag, registries);
        energy.write(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        if (ownerName != null && !ownerName.isBlank()) tag.putString("OwnerName", ownerName);
    }

    @FunctionalInterface private interface FloatModifier { float apply(IBeeModifier modifier, float current); }
    @FunctionalInterface private interface TerritoryModifier { Vec3i apply(IBeeModifier modifier, Vec3i current); }
    @FunctionalInterface private interface BooleanModifier { boolean apply(IBeeModifier modifier); }
}
