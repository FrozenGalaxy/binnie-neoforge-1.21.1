package binnie.botany.machine;

import binnie.botany.BinnieBotany;
import binnie.botany.item.CeramicItem;
import binnie.botany.registry.BotanyContent;
import binnie.botany.registry.BotanyWorldContent;
import binnie.core.data.DesignData;
import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineProcessHandler;
import binnie.core.machine.MachineProcessPlan;
import binnie.core.machine.MachineProcessError;
import binnie.core.machine.MachineProcessRegistry;
import binnie.design.block.DesignedBlockItem;
import binnie.design.data.DesignCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** 1.12-style manual Tileworker workflow using two fired ceramic colours and one mortar. */
public final class BotanyMachineProcesses {
    private static boolean registered;

    private BotanyMachineProcesses() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        MachineProcessRegistry.register(ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, "tileworker"), new MachineProcessHandler() {
            @Override
            public Optional<MachineProcessPlan> createPlan(Level level, MachineBlockEntity machine) {
                return Optional.empty();
            }

            @Override
            public boolean isItemValid(MachineBlockEntity machine, int slot, ItemStack stack) {
                return switch (slot) {
                    case MachineBlockEntity.INPUT_SLOT, MachineBlockEntity.CATALYST_SLOT ->
                            stack.is(binnie.design.data.DesignerMaterialTags.CERAMIC)
                                    && stack.is(BotanyWorldContent.CERAMIC_ITEM.get());
                    case MachineBlockEntity.REAGENT_SLOT -> stack.is(BotanyContent.MORTAR.get());
                    default -> false;
                };
            }

            @Override
            public boolean isManualCrafting(MachineBlockEntity machine) {
                return true;
            }

            @Override
            public MachineProcessError diagnose(Level level, MachineBlockEntity machine) {
                ItemStack first = machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT);
                ItemStack second = machine.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT);
                ItemStack mortar = machine.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT);
                if (first.isEmpty() || second.isEmpty() || mortar.isEmpty()) return MachineProcessError.NO_ITEM;
                if (!first.is(binnie.design.data.DesignerMaterialTags.CERAMIC)
                        || !second.is(binnie.design.data.DesignerMaterialTags.CERAMIC)
                        || !first.is(BotanyWorldContent.CERAMIC_ITEM.get())
                        || !second.is(BotanyWorldContent.CERAMIC_ITEM.get())
                        || !mortar.is(BotanyContent.MORTAR.get())) return MachineProcessError.INVALID_ITEM;
                int selection = machine.manualSelection();
                if (selection < 0 || selection >= DesignCatalog.DESIGNS.size()
                        || !DesignCatalog.isSelectableDesign(DesignCatalog.DESIGNS.get(selection))) {
                    return MachineProcessError.INVALID_RECIPE;
                }
                return MachineProcessError.NONE;
            }

            @Override
            public boolean setManualSelection(MachineBlockEntity machine, int selection) {
                return selection >= 0 && selection < DesignCatalog.DESIGNS.size()
                        && DesignCatalog.isSelectableDesign(DesignCatalog.DESIGNS.get(selection));
            }

            @Override
            public ItemStack getManualPreview(MachineBlockEntity machine) {
                return product(machine);
            }

            @Override
            public ItemStack takeManualPreview(MachineBlockEntity machine, ItemStack carried) {
                ItemStack product = product(machine);
                if (product.isEmpty() || !canCarry(carried, product)) return ItemStack.EMPTY;
                boolean blank = "blank".equals(DesignCatalog.DESIGNS.get(machine.manualSelection()));
                machine.inventory().extractItem(MachineBlockEntity.INPUT_SLOT, 1, false);
                if (!blank) machine.inventory().extractItem(MachineBlockEntity.CATALYST_SLOT, 1, false);
                machine.inventory().extractItem(MachineBlockEntity.REAGENT_SLOT, 1, false);
                return product;
            }
        });
    }

    private static ItemStack product(MachineBlockEntity machine) {
        ItemStack first = machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT);
        ItemStack second = machine.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT);
        ItemStack mortar = machine.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT);
        if (!first.is(binnie.design.data.DesignerMaterialTags.CERAMIC)
                || !second.is(binnie.design.data.DesignerMaterialTags.CERAMIC)
                || !first.is(BotanyWorldContent.CERAMIC_ITEM.get()) || !second.is(BotanyWorldContent.CERAMIC_ITEM.get())
                || !mortar.is(BotanyContent.MORTAR.get())) return ItemStack.EMPTY;
        int selection = machine.manualSelection();
        if (selection < 0 || selection >= DesignCatalog.DESIGNS.size()
                || !DesignCatalog.isSelectableDesign(DesignCatalog.DESIGNS.get(selection))) return ItemStack.EMPTY;
        return tileworkerOutput(first, second, DesignCatalog.DESIGNS.get(selection));
    }

    /** Tileworker#getBlock contract shared by manual execution and JEI descriptors. */
    public static ItemStack tileworkerOutput(ItemStack first, ItemStack second, String design) {
        if (!first.is(BotanyWorldContent.CERAMIC_ITEM.get()) || !second.is(BotanyWorldContent.CERAMIC_ITEM.get())
                || !DesignCatalog.isDesign(design)) return ItemStack.EMPTY;
        boolean blank = "blank".equals(design);
        String firstColor = CeramicItem.color(first);
        String secondColor = blank ? firstColor : CeramicItem.color(second);
        ResourceLocation ceramic = BuiltInRegistries.ITEM.getKey(BotanyWorldContent.CERAMIC_ITEM.get());
        DesignData data = new DesignData(design, design, ceramic, ceramic, 0, false, firstColor, secondColor);
        DesignedBlockItem item = BotanyWorldContent.CERAMIC_TILE_ITEM.get();
        return item.create(data, blank ? 1 : 2);
    }

    private static boolean canCarry(ItemStack carried, ItemStack product) {
        return carried.isEmpty() || (ItemStack.isSameItemSameComponents(carried, product)
                && carried.getCount() + product.getCount() <= carried.getMaxStackSize());
    }
}
