package binnie.core.menu;

import binnie.core.item.FieldKitItem;
import binnie.core.registry.CommonItemTags;
import binnie.core.registry.CoreRegistries;
import binnie.genetics.api.GeneticOrganismAdapter;
import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.genetics.tracking.GeneticKnowledgeSavedData;
import forestry.api.core.genetics.IIndividual;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Portable two-slot analyzer whose paper stack is persisted as Field Kit durability. */
public final class FieldKitMenu extends AbstractContainerMenu {
    public static final int INDIVIDUAL_SLOT = 0;
    public static final int PAPER_SLOT = 1;
    public static final int ANALYSIS_TICKS = 100;
    public static final int INDIVIDUAL_X = 70;
    public static final int INDIVIDUAL_Y = 58;
    public static final int PAPER_X = 208;
    public static final int PAPER_Y = 8;
    public static final int PLAYER_X = 59;
    public static final int PLAYER_Y = 142;
    public static final int HOTBAR_Y = 200;

    private final Player player;
    private final boolean mainHand;
    private final ItemStack openedKit;
    private final SimpleContainer kitInventory = new SimpleContainer(2);
    private boolean updating;
    private int analysisProgress;
    private ItemStack analysisIdentity = ItemStack.EMPTY;
    private final DataSlot analysisProgressData = new DataSlot() {
        @Override public int get() { return analysisProgress; }
        @Override public void set(int value) { analysisProgress = Math.clamp(value, 0, ANALYSIS_TICKS); }
    };

    public static FieldKitMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new FieldKitMenu(containerId, inventory, buffer.readBoolean());
    }

    public FieldKitMenu(int containerId, Inventory inventory, boolean mainHand) {
        super(CoreRegistries.FIELD_KIT_MENU.get(), containerId);
        this.player = inventory.player;
        this.mainHand = mainHand;
        this.openedKit = heldKit();
        int sheets = openedKit.isEmpty() ? 0 : FieldKitItem.remainingPaper(openedKit);
        if (sheets > 0) kitInventory.setItem(PAPER_SLOT, new ItemStack(Items.PAPER, sheets));

        addSlot(new Slot(kitInventory, INDIVIDUAL_SLOT, INDIVIDUAL_X, INDIVIDUAL_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return GeneticOrganismAdapters.supports(stack); }
            @Override public int getMaxStackSize() { return 1; }
            @Override public void setChanged() { super.setChanged(); individualChanged(); }
        });
        addSlot(new Slot(kitInventory, PAPER_SLOT, PAPER_X, PAPER_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(CommonItemTags.PAPER); }
            @Override public int getMaxStackSize() { return FieldKitItem.CAPACITY; }
            @Override public void setChanged() { super.setChanged(); inventoryChanged(); }
        });
        addPlayerInventory(inventory);
        addDataSlot(analysisProgressData);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, PLAYER_X + col * 18, PLAYER_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) {
            boolean locked = mainHand && col == inventory.selected;
            addSlot(new LockedHandSlot(inventory, col, PLAYER_X + col * 18, HOTBAR_Y, locked));
        }
    }

    private ItemStack heldKit() {
        return mainHand ? player.getMainHandItem() : player.getOffhandItem();
    }

    private void inventoryChanged() {
        if (updating || player.level().isClientSide) return;
        updating = true;
        try {
            syncPaperToKit();
            updateAnalysisCandidate(false);
        } finally {
            updating = false;
        }
    }

    private void individualChanged() {
        if (!player.level().isClientSide) {
            analysisProgress = 0;
            analysisIdentity = ItemStack.EMPTY;
        }
        inventoryChanged();
    }

    private void syncPaperToKit() {
        if (!(openedKit.getItem() instanceof FieldKitItem)) return;
        int count = kitInventory.getItem(PAPER_SLOT).getCount();
        openedKit.setDamageValue(FieldKitItem.CAPACITY - Math.clamp(count, 0, FieldKitItem.CAPACITY));
    }

    private void updateAnalysisCandidate(boolean advance) {
        ItemStack input = kitInventory.getItem(INDIVIDUAL_SLOT);
        ItemStack paper = kitInventory.getItem(PAPER_SLOT);
        if (input.isEmpty() || paper.isEmpty() || !GeneticOrganismAdapters.supports(input)) {
            resetAnalysis();
            return;
        }
        try {
            GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(input);
            IIndividual individual = adapter.read(input);
            if (individual.isAnalyzed()) {
                resetAnalysis();
                return;
            }
            if (analysisIdentity.isEmpty() || !ItemStack.isSameItemSameComponents(analysisIdentity, input)) {
                analysisIdentity = input.copyWithCount(1);
                analysisProgress = 0;
            }
            if (!advance) return;
            analysisProgress++;
            if (analysisProgress < ANALYSIS_TICKS) return;
            ItemStack analysed = adapter.analyze(input);
            IIndividual result = adapter.read(analysed);
            kitInventory.setItem(INDIVIDUAL_SLOT, analysed);
            paper.shrink(1);
            if (paper.isEmpty()) kitInventory.setItem(PAPER_SLOT, ItemStack.EMPTY);
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                GeneticKnowledgeSavedData.get(serverLevel).discoverIndividual(player.getUUID(), result);
            }
            resetAnalysis();
            syncPaperToKit();
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // Third-party malformed genetic stacks are rejected without closing the menu.
            resetAnalysis();
        }
    }

    private void resetAnalysis() {
        analysisProgress = 0;
        analysisIdentity = ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide && !updating) {
            updating = true;
            try {
                updateAnalysisCandidate(true);
            } finally {
                updating = false;
            }
        }
        super.broadcastChanges();
    }

    public Container kitInventory() { return kitInventory; }
    public int paperRemaining() { return kitInventory.getItem(PAPER_SLOT).getCount(); }
    public int analysisProgress() { return analysisProgress; }
    public boolean isAnalysing() { return analysisProgress > 0; }
    public boolean mainHand() { return mainHand; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index < 2) {
            if (!moveItemStackTo(source, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (GeneticOrganismAdapters.supports(source)) {
            if (!moveItemStackTo(source, INDIVIDUAL_SLOT, INDIVIDUAL_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (source.is(CommonItemTags.PAPER)) {
            if (!moveItemStackTo(source, PAPER_SLOT, PAPER_SLOT + 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        inventoryChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return heldKit() == openedKit && openedKit.getItem() instanceof FieldKitItem;
    }

    @Override
    public void removed(Player player) {
        syncPaperToKit();
        super.removed(player);
        if (!player.level().isClientSide) {
            ItemStack individual = kitInventory.removeItemNoUpdate(INDIVIDUAL_SLOT);
            if (!individual.isEmpty() && !player.getInventory().add(individual)) player.drop(individual, false);
            // Paper remains represented by the held kit's damage value.
            kitInventory.setItem(PAPER_SLOT, ItemStack.EMPTY);
        }
    }
}
