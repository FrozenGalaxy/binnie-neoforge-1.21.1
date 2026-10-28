package binnie.genetics.menu;

import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.api.GeneticOrganismAdapter;
import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.genetics.api.GeneticSerumEngineering;
import binnie.genetics.data.GeneticKnowledgeData;
import binnie.genetics.data.GeneticKnowledgeEntry;
import binnie.genetics.data.LaboratoryChargeData;
import binnie.genetics.registry.GeneticsContent;
import binnie.genetics.registry.GeneticsMenus;
import binnie.genetics.tracking.GeneticKnowledgeSavedData;
import forestry.api.core.genetics.IIndividual;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;

import javax.annotation.Nullable;

/** Source-oriented menu for the gene bank, registry browser, and two-slot portable Analyst. */
public final class GeneticKnowledgeMenu extends AbstractContainerMenu {
    public static final int ANALYST_INDIVIDUAL_SLOT = 0;
    public static final int ANALYST_DYE_SLOT = 1;

    private final GeneticKnowledgeData knowledge;
    private final Mode mode;
    private final InterfaceType interfaceType;
    @Nullable private final ResourceLocation speciesTypeFilter;
    private final boolean mainHand;
    private final boolean requiresHeldItem;
    private final Player player;
    private final SimpleContainer analystSlots;
    private boolean analysing;
    private boolean analystInventoryVisible;

    public GeneticKnowledgeMenu(int containerId, Inventory inventory, GeneticKnowledgeData knowledge, Mode mode, boolean mainHand) {
        this(containerId, inventory, knowledge, mode, defaultInterface(mode), null, mainHand);
    }

    public GeneticKnowledgeMenu(int containerId, Inventory inventory, GeneticKnowledgeData knowledge, Mode mode,
                                InterfaceType interfaceType, boolean mainHand) {
        this(containerId, inventory, knowledge, mode, interfaceType, null, mainHand);
    }

    public GeneticKnowledgeMenu(int containerId, Inventory inventory, GeneticKnowledgeData knowledge, Mode mode,
                                InterfaceType interfaceType, @Nullable ResourceLocation speciesTypeFilter,
                                boolean mainHand) {
        this(containerId, inventory, knowledge, mode, interfaceType, speciesTypeFilter, mainHand, true);
    }

    public GeneticKnowledgeMenu(int containerId, Inventory inventory, GeneticKnowledgeData knowledge, Mode mode,
                                InterfaceType interfaceType, @Nullable ResourceLocation speciesTypeFilter,
                                boolean mainHand, boolean requiresHeldItem) {
        super(GeneticsMenus.KNOWLEDGE.get(), containerId);
        this.knowledge = knowledge;
        this.mode = mode;
        this.interfaceType = interfaceType;
        this.speciesTypeFilter = speciesTypeFilter;
        this.mainHand = mainHand;
        this.requiresHeldItem = requiresHeldItem;
        this.player = inventory.player;
        this.analystSlots = new SimpleContainer(2);

        if (interfaceType == InterfaceType.ANALYST) {
            loadStoredDnaDye();
            addSlot(new AnalystIndividualSlot(analystSlots, ANALYST_INDIVIDUAL_SLOT, 16, 29));
            addSlot(new AnalystDyeSlot(analystSlots, ANALYST_DYE_SLOT, 38, 29));
            addAnalystPlayerInventory(inventory);
        } else if (interfaceType == InterfaceType.GENE_DATABASE) {
            addGeneBankPlayerInventory(inventory);
        }
    }

    public static GeneticKnowledgeMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Mode mode = buffer.readEnum(Mode.class);
        InterfaceType interfaceType = buffer.readEnum(InterfaceType.class);
        ResourceLocation speciesTypeFilter = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        boolean mainHand = buffer.readBoolean();
        boolean requiresHeldItem = buffer.readBoolean();
        GeneticKnowledgeData data = GeneticKnowledgeData.STREAM_CODEC.decode(buffer);
        return new GeneticKnowledgeMenu(containerId, inventory, data, mode, interfaceType, speciesTypeFilter, mainHand, requiresHeldItem);
    }

    /** 1.12 Gene Bank player inventory: all 36 slots in a six-by-six matrix. */
    private void addGeneBankPlayerInventory(Inventory inventory) {
        for (int row = 0; row < KnowledgeUiContract.GENE_DATABASE_INVENTORY_ROWS; row++) {
            for (int column = 0; column < KnowledgeUiContract.GENE_DATABASE_INVENTORY_COLUMNS; column++) {
                int inventoryIndex = column + row * KnowledgeUiContract.GENE_DATABASE_INVENTORY_COLUMNS;
                addSlot(new Slot(inventory, inventoryIndex,
                        KnowledgeUiContract.GENE_DATABASE_INVENTORY_X + column * 18,
                        KnowledgeUiContract.GENE_DATABASE_INVENTORY_Y + row * 18));
            }
        }
    }

    private void addAnalystPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new AnalystPlayerSlot(inventory, column + row * 9 + 9,
                        KnowledgeUiContract.ANALYST_INVENTORY_X + column * 18,
                        KnowledgeUiContract.ANALYST_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new AnalystPlayerSlot(inventory, column,
                    KnowledgeUiContract.ANALYST_INVENTORY_X + column * 18,
                    KnowledgeUiContract.ANALYST_INVENTORY_Y + 58));
        }
    }

    /** Client-only drawer state used by {@link AnalystPlayerSlot#isActive()}. */
    public void setAnalystInventoryVisible(boolean visible) {
        analystInventoryVisible = visible;
    }

    public boolean isAnalystInventoryVisible() {
        return analystInventoryVisible;
    }

    public GeneticKnowledgeData knowledge() {
        return knowledge;
    }

    public Mode mode() {
        return mode;
    }

    public InterfaceType interfaceType() {
        return interfaceType;
    }

    @Nullable
    public ResourceLocation speciesTypeFilter() {
        return speciesTypeFilter;
    }

    public Container analystSlots() {
        return analystSlots;
    }

    /** Server-authoritative 1.12 gene-bank click action. */
    public boolean selectGene(int index) {
        if (interfaceType != InterfaceType.GENE_DATABASE || index < 0 || index >= knowledge.genes().size()) return false;
        GeneticKnowledgeEntry gene = knowledge.genes().get(index);
        ItemStack result = GeneticSerumEngineering.applyGene(getCarried(), gene);
        if (result.isEmpty()) return false;
        setCarried(result);
        broadcastChanges();
        return true;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == analystSlots) {
            tryAnalyse();
            storeDnaDye();
        }
    }

    private ItemStack analystItem() {
        return mainHand ? player.getMainHandItem() : player.getOffhandItem();
    }

    private void loadStoredDnaDye() {
        NonNullList<ItemStack> stored = NonNullList.withSize(1, ItemStack.EMPTY);
        analystItem().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(stored);
        ItemStack dye = stored.getFirst();
        if (dye.is(GeneticsContent.DNA_DYE.get())) analystSlots.setItem(ANALYST_DYE_SLOT, dye.copy());
    }

    private void storeDnaDye() {
        if (player.level().isClientSide || interfaceType != InterfaceType.ANALYST) return;
        ItemStack holder = analystItem();
        if (!mode.matches(holder)) return;
        NonNullList<ItemStack> stored = NonNullList.withSize(1, ItemStack.EMPTY);
        stored.set(0, analystSlots.getItem(ANALYST_DYE_SLOT).copy());
        holder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stored));
        player.getInventory().setChanged();
    }

    private void tryAnalyse() {
        if (analysing || interfaceType != InterfaceType.ANALYST || player.level().isClientSide) return;
        ItemStack input = analystSlots.getItem(ANALYST_INDIVIDUAL_SLOT);
        ItemStack dye = analystSlots.getItem(ANALYST_DYE_SLOT);
        if (!hasRequiredDnaDye(input, dye)) return;
        LaboratoryChargeData charge = dye.get(BinnieDataComponents.LABORATORY_CHARGE);
        if (charge != null && charge.milliCharge() == 0) return;

        GeneticOrganismAdapter adapter;
        IIndividual individual;
        try {
            adapter = GeneticOrganismAdapters.forStack(input);
            individual = adapter.read(input);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return;
        }
        if (individual.isAnalyzed()) return;

        analysing = true;
        try {
            ItemStack analysed = adapter.analyze(input);
            IIndividual analysedIndividual = adapter.read(analysed);
            analystSlots.setItem(ANALYST_INDIVIDUAL_SLOT, analysed);
            analystSlots.removeItem(ANALYST_DYE_SLOT, 1);
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                GeneticKnowledgeSavedData.get(serverLevel).discoverIndividual(player.getUUID(), analysedIndividual);
            }
            broadcastChanges();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            // Malformed third-party genetic stacks must not crash a menu packet handler.
        } finally {
            analysing = false;
        }
    }

    /** Analysis never starts from a specimen alone; a DNA Dye must be present in slot 1. */
    private boolean hasRequiredDnaDye(ItemStack input, ItemStack dye) {
        return GeneticOrganismAdapters.supports(input) && dye.is(GeneticsContent.DNA_DYE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();

        if (interfaceType == InterfaceType.ANALYST) {
            if (index < 2) {
                if (!moveItemStackTo(source, 2, slots.size(), true)) return ItemStack.EMPTY;
            } else if (GeneticOrganismAdapters.supports(source)) {
                if (!moveItemStackTo(source, ANALYST_INDIVIDUAL_SLOT, ANALYST_INDIVIDUAL_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (source.is(GeneticsContent.DNA_DYE.get())) {
                if (!moveItemStackTo(source, ANALYST_DYE_SLOT, ANALYST_DYE_SLOT + 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!requiresHeldItem) return true;
        ItemStack held = mainHand ? player.getMainHandItem() : player.getOffhandItem();
        return !held.isEmpty() && mode.matches(held) && held.getItem() instanceof binnie.genetics.item.GeneticKnowledgeItem item
                && item.interfaceType() == interfaceType;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && interfaceType == InterfaceType.ANALYST) {
            storeDnaDye();
            analystSlots.removeItemNoUpdate(ANALYST_DYE_SLOT);
            clearContainer(player, analystSlots);
        }
    }

    private static InterfaceType defaultInterface(Mode mode) {
        if (mode == Mode.ANALYST) return InterfaceType.ANALYST;
        if (mode.isRegistry()) return InterfaceType.REGISTRY;
        return InterfaceType.GENE_DATABASE;
    }

    public enum InterfaceType {
        ANALYST,
        REGISTRY,
        GENE_DATABASE,
        ARBORIST_DATABASE,
        SPECIES_DATABASE
    }

    public enum Mode {
        DATABASE(false, "database"),
        MASTER_DATABASE(true, "master_database"),
        REGISTRY(false, "registry"),
        MASTER_REGISTRY(true, "master_registry"),
        ANALYST(false, "analyst");

        private final boolean master;
        private final String itemId;

        Mode(boolean master, String itemId) {
            this.master = master;
            this.itemId = itemId;
        }

        public boolean master() {
            return master;
        }

        public String itemId() {
            return itemId;
        }

        public boolean isDatabase() {
            return this == DATABASE || this == MASTER_DATABASE;
        }

        public boolean isRegistry() {
            return this == REGISTRY || this == MASTER_REGISTRY;
        }

    /**
          * Original 1.12 master registries bypassed the player's breeding tracker and queried
          * each active breeding system's full species collection directly.
          */
        public boolean exposesCompleteSpeciesCatalog(InterfaceType interfaceType) {
            return this == MASTER_REGISTRY && interfaceType == InterfaceType.REGISTRY;
        }

    /** Master Registry mutation pages never replace entries with unknown placeholders. */
        public boolean exposesCompleteMutationCatalog(InterfaceType interfaceType) {
            return this == MASTER_REGISTRY && interfaceType == InterfaceType.REGISTRY;
        }

    /** The original Master Gene Database bypasses the per-player sequenced-gene tracker. */
        public boolean exposesCompleteGeneCatalog(InterfaceType interfaceType) {
            return this == MASTER_DATABASE && interfaceType == InterfaceType.GENE_DATABASE;
        }

        public boolean matches(ItemStack stack) {
            return stack.getItem() instanceof binnie.genetics.item.GeneticKnowledgeItem item && item.mode() == this;
        }
    }

    private class AnalystPlayerSlot extends Slot {
        private AnalystPlayerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            // The server keeps the slots valid for packet handling. The client hides and disables
            // them while the original bottom ControlSlide is collapsed or moving.
            return !player.level().isClientSide || analystInventoryVisible;
        }
    }

    private class AnalystIndividualSlot extends Slot {
        private AnalystIndividualSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return GeneticOrganismAdapters.supports(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            tryAnalyse();
        }
    }

    private class AnalystDyeSlot extends Slot {
        private AnalystDyeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(GeneticsContent.DNA_DYE.get());
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            tryAnalyse();
        }
    }
}
