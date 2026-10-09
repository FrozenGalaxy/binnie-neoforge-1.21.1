package binnie.core.registry;

import binnie.core.item.FieldKitItem;
import binnie.core.item.GenesisItem;
import binnie.core.liquid.BinnieFluidContainerItem;
import binnie.core.liquid.BinnieFluidContainerType;
import binnie.trees.beverage.LegacyBeverageCatalog;
import binnie.trees.registry.BinnieTreesFluids;
import binnie.genetics.registry.GeneticsFluids;
import binnie.core.storage.CompartmentBlockItem;
import binnie.core.storage.CompartmentTier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.fluids.FluidStack;

public class CoreContent {
    public static final ModuleRegistry CONTENT = new ModuleRegistry("binniecore");
    public static final DeferredItem<FieldKitItem> FIELD_KIT = CONTENT.custom("field_kit", FieldKitItem::new, true);
    public static final DeferredItem<GenesisItem> GENESIS = CONTENT.custom("genesis", GenesisItem::new, true);
    public static final DeferredItem<BinnieFluidContainerItem> GLASS = CONTENT.custom("glass",
            properties -> new BinnieFluidContainerItem(properties, BinnieFluidContainerType.GLASS), true);
    public static final DeferredItem<BinnieFluidContainerItem> CYLINDER = CONTENT.custom("cylinder",
            properties -> new BinnieFluidContainerItem(properties, BinnieFluidContainerType.CYLINDER), true);

    private CoreContent() {}

    public static void register(IEventBus bus) {
        CONTENT.setIcon(() -> new net.minecraft.world.item.ItemStack(FIELD_KIT.get()));
        CONTENT.setDisplayItems(output -> {
            // Consolidates 1.12 Tools, Building Blocks, Materials and Apiculture assignments into Core's own tab.
            output.accept(FIELD_KIT.get());
            output.accept(GENESIS.get());
            for (CompartmentTier tier : CompartmentTier.values()) output.accept(CompartmentBlockItem.createStack(tier));
            output.accept(GLASS.get());
            output.accept(CYLINDER.get());
            for (BinnieTreesFluids.BeverageFluid beverage : BinnieTreesFluids.beverages()) {
                FluidStack fluid = beverage.stack(1_000);
                if (BinnieFluidContainerType.GLASS.showInCreative(fluid)) output.accept(GLASS.get().filled(fluid));
            }
            for (GeneticsFluids.ProcessFluid fluid : new GeneticsFluids.ProcessFluid[]{
                    GeneticsFluids.GROWTH_MEDIUM, GeneticsFluids.BACTERIA, GeneticsFluids.POLYMERISING_BACTERIA,
                    GeneticsFluids.RAW_DNA, GeneticsFluids.VECTOR_BACTERIA}) {
                FluidStack stack = fluid.stack(1_000);
                if (BinnieFluidContainerType.CYLINDER.showInCreative(stack)) output.accept(CYLINDER.get().filled(stack));
            }
        });
        CONTENT.register(bus, "itemGroup.binniecore");
    }
}
