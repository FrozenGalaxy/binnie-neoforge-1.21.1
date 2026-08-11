package binnie.genetics.menu;

import binnie.genetics.api.GeneticOperations;
import binnie.genetics.api.GeneticOrganismAdapter;
import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.genetics.registry.GeneticsMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import forestry.api.core.genetics.alleles.AllelePair;

import java.util.List;

/** Two-parent, type-safe Punnett-square container using the current ForestryCE karyotypes. */
public final class PunnettSquareMenu extends AbstractContainerMenu {
    public static final int FIRST_PARENT_SLOT = 0;
    public static final int SECOND_PARENT_SLOT = 1;
    private final SimpleContainer parents;
    private final boolean mainHand;
    private int selectedChromosome;

    public PunnettSquareMenu(int containerId, Inventory playerInventory, boolean mainHand) {
        this(containerId, playerInventory, new SimpleContainer(2), mainHand);
    }

    private PunnettSquareMenu(int containerId, Inventory playerInventory, SimpleContainer parents, boolean mainHand) {
        super(GeneticsMenus.PUNNETT.get(), containerId);
        this.parents = parents;
        this.mainHand = mainHand;
        addSlot(new GeneticParentSlot(parents, FIRST_PARENT_SLOT, 31, 35));
        addSlot(new GeneticParentSlot(parents, SECOND_PARENT_SLOT, 31, 83));
        addPlayerInventory(playerInventory);
        addDataSlot(new DataSlot() {
            @Override public int get() { return selectedChromosome; }
            @Override public void set(int value) { selectedChromosome = value; }
        });
    }

    public static PunnettSquareMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PunnettSquareMenu(containerId, inventory, buffer.readBoolean());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 110 + column * 18, 138 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 110 + column * 18, 196));
    }

    public Container parents() {
        return parents;
    }

    public boolean hasCompatibleParents() {
        ItemStack first = parents.getItem(FIRST_PARENT_SLOT);
        ItemStack second = parents.getItem(SECOND_PARENT_SLOT);
        if (first.isEmpty() || second.isEmpty()) return false;
        try {
            return GeneticOrganismAdapters.forStack(first).supports(second);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public List<ResourceLocation> chromosomeIds() {
        if (!hasCompatibleParents()) return List.of();
        GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(parents.getItem(FIRST_PARENT_SLOT));
        return adapter.chromosomes().stream().map(chromosome -> chromosome.id()).toList();
    }

    public int selectedChromosomeIndex() {
        return selectedChromosome;
    }

    public ResourceLocation selectedChromosomeId() {
        List<ResourceLocation> chromosomes = chromosomeIds();
        if (chromosomes.isEmpty()) return null;
        int index = Math.clamp(selectedChromosome, 0, chromosomes.size() - 1);
        return chromosomes.get(index);
    }

    public List<GeneticOperations.PunnettOutcome> outcomes() {
        ResourceLocation chromosome = selectedChromosomeId();
        if (chromosome == null) return List.of();
        return GeneticOperations.punnett(parents.getItem(FIRST_PARENT_SLOT), parents.getItem(SECOND_PARENT_SLOT), chromosome);
    }

    public AllelePair<?> parentPair(int parent) {
        ResourceLocation chromosome = selectedChromosomeId();
        if (chromosome == null || parent < 0 || parent > 1) return null;
        ItemStack stack = parents.getItem(parent);
        try {
            GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(stack);
            var type = adapter.chromosomes().stream().filter(value -> value.id().equals(chromosome)).findFirst().orElse(null);
            return type == null ? null : adapter.read(stack).getGenome().getAllelePair(type);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /** Server-authoritative selection; invalid indices or mismatched parents are rejected without mutation. */
    public boolean selectChromosome(int index) {
        List<ResourceLocation> chromosomes = chromosomeIds();
        if (index < 0 || index >= chromosomes.size()) return false;
        selectedChromosome = index;
        broadcastChanges();
        return true;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        List<ResourceLocation> chromosomes = chromosomeIds();
        if (chromosomes.isEmpty()) selectedChromosome = 0;
        else if (selectedChromosome >= chromosomes.size()) selectedChromosome = chromosomes.size() - 1;
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index < 2) {
            if (!moveItemStackTo(source, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!forestry.api.core.genetics.capability.IIndividualHandlerItem.isIndividual(source)
                    || !moveItemStackTo(source, 0, 2, false)) return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack held = mainHand ? player.getMainHandItem() : player.getOffhandItem();
        return held.getItem() instanceof binnie.genetics.item.PunnettSquareItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) clearContainer(player, parents);
    }

    private static final class GeneticParentSlot extends Slot {
        GeneticParentSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return forestry.api.core.genetics.capability.IIndividualHandlerItem.isIndividual(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
