package binnie.trees.registry;

import binnie.core.data.BeverageData;
import binnie.core.registry.BinnieDataComponents;
import binnie.trees.BinnieTrees;
import binnie.trees.beverage.LegacyBeverageCatalog;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
  * A component-bearing carrier for the 129 1.12 beverage identities and the
  * four fixed Binnie's Trees process liquids. The fixed liquids retain the 1.12
  * fluid-block and universal-container behavior through current placeable fluid
  * blocks and buckets; the component-bearing beverage carrier remains internal.
  */
public final class BinnieTreesFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, BinnieTrees.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, BinnieTrees.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BinnieTrees.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BinnieTrees.MOD_ID);

    private static final DeferredHolder<FluidType, FluidType> BEVERAGE_TYPE = TYPES.register("beverage", () ->
            new BeverageFluidType(FluidType.Properties.create().descriptionId("fluid.binnietrees.beverage")
                    .density(1_000).viscosity(1_000).temperature(300)));
    private static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BEVERAGE = FLUIDS.register("beverage", () ->
            new BaseFlowingFluid.Source(properties()));
    private static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> BEVERAGE_FLOWING = FLUIDS.register("beverage_flowing", () ->
            new BaseFlowingFluid.Flowing(properties()));

    // BinnieTreeLiquid.java, in its original declaration order and matching RGB
    // values. Resin is used by the Forestry Squeezer -> Still ->
    // Carpenter Wood Polish production chain.
    public static final ProcessFluid SAP = register("sap", 12_481_858);
    public static final ProcessFluid RESIN = register("resin", 13_199_360);
    public static final ProcessFluid LATEX = register("latex", 14_212_041);
    public static final ProcessFluid TURPENTINE = register("turpentine", 7_951_162);

    private BinnieTreesFluids() {}

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(BEVERAGE_TYPE, BEVERAGE, BEVERAGE_FLOWING)
                .tickRate(20).slopeFindDistance(1).levelDecreasePerBlock(8);
    }

    private static ProcessFluid register(String id, int tint) {
        return new ProcessFluid(id, tint);
    }

    public static FluidStack beverage(BeverageData data, int amount) {
        FluidStack stack = new FluidStack(BEVERAGE.get(), amount);
        stack.set(BinnieDataComponents.BEVERAGE, new BeverageData(data.beverage(), data.color(), data.alcohol(), amount));
        return stack;
    }


    public static FluidStack stack(String category, String value, int amount) {
        return beverage(LegacyBeverageCatalog.get(category, value).data(Math.min(amount, 1_000)), amount);
    }

    public static BeverageData data(FluidStack stack) {
        return stack.get(BinnieDataComponents.BEVERAGE);
    }

    public static FluidType beverageType() {
        return BEVERAGE_TYPE.get();
    }

    public static Fluid beverageFluid() {
        return BEVERAGE.get();
    }

    public static boolean isBeverage(FluidStack stack, String category, String value) {
        BeverageData data = data(stack);
        return data != null && data.beverage().equals(LegacyBeverageCatalog.get(category, value).id().toString());
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private static final class BeverageFluidType extends FluidType {
        private BeverageFluidType(Properties properties) {
            super(properties);
        }

        @Override
        public String getDescriptionId(FluidStack stack) {
            BeverageData data = BinnieTreesFluids.data(stack);
            return data == null ? super.getDescriptionId(stack) : LegacyBeverageCatalog.translationKey(data);
        }
    }

    /** Fixed, 1.12 Binnie's Trees liquid. */
    public static final class ProcessFluid {
        private final DeferredHolder<FluidType, FluidType> type;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        private final DeferredBlock<LiquidBlock> block;
        private final DeferredItem<BlockItem> blockItem;
        private final DeferredItem<BucketItem> bucket;
        private final int tint;

        private ProcessFluid(String id, int tint) {
            this.tint = tint;
            this.type = TYPES.register(id, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.binnietrees." + id)
                    .density(1_000)
                    .viscosity(1_000)
                    .temperature(300)));
            this.source = FLUIDS.register(id, () -> new BaseFlowingFluid.Source(properties()));
            this.flowing = FLUIDS.register(id + "_flowing", () -> new BaseFlowingFluid.Flowing(properties()));
            this.block = BLOCKS.register(id, () -> new LiquidBlock(source.get(), BlockBehaviour.Properties.of()
                    .liquid().noCollission().noLootTable().replaceable()));
            // ManagerLiquid registered an ItemBlock under the same identifier
            // in 1.12. Keep that identity for migrated inventories/commands;
            // the current bucket is the normal player-facing container.
            this.blockItem = ITEMS.registerItem(id, properties -> new BlockItem(block.get(), properties));
            this.bucket = ITEMS.registerItem(id + "_bucket", properties -> new BucketItem(source.get(), properties
                    .craftRemainder(Items.BUCKET)
                    .stacksTo(1)));
        }

        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type, source, flowing)
                    .block(block)
                    .bucket(bucket)
                    .tickRate(20).slopeFindDistance(1).levelDecreasePerBlock(8);
        }

        public Fluid fluid() {
            return source.get();
        }

        public FluidType type() {
            return type.get();
        }

        public Block block() {
            return block.get();
        }

        public BucketItem bucket() {
            return bucket.get();
        }

        public BlockItem blockItem() {
            return blockItem.get();
        }

        public FluidStack stack(int amount) {
            return new FluidStack(source.get(), amount);
        }

        public int tint() {
            return tint;
        }
    }
}
