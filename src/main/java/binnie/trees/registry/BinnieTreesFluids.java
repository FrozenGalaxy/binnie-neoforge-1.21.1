package binnie.trees.registry;

import binnie.core.data.BeverageData;
import binnie.trees.BinnieTrees;
import binnie.trees.beverage.LegacyBeverageCatalog;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Source-faithful fixed fluid identities for the 1.12 liquid catalog. */
public final class BinnieTreesFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, BinnieTrees.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, BinnieTrees.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BinnieTrees.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BinnieTrees.MOD_ID);
    private static final Map<String, BeverageFluid> BEVERAGES = createBeverages();

    public static final ProcessFluid RESIN = registerProcess("resin", 13_199_360);
    public static final ProcessFluid TURPENTINE = registerProcess("turpentine", 7_951_162);

    private BinnieTreesFluids() {}

    private static Map<String, BeverageFluid> createBeverages() {
        LinkedHashMap<String, BeverageFluid> result = new LinkedHashMap<>();
        for (LegacyBeverageCatalog.Definition definition : LegacyBeverageCatalog.definitions().values()) {
            // ModuleAlcohol.preInit registered exactly these four enums. MiscFluid
            // was never registered and cocktails were ItemDrink compositions.
            if (!switch (definition.category()) {
                case "juice", "alcohol", "spirit" -> true;
                default -> false;
            }) continue;
            String key = definition.category() + ":" + definition.value();
            result.put(key, new BeverageFluid(definition));
        }
        return Map.copyOf(result);
    }

    private static ProcessFluid registerProcess(String id, int tint) { return new ProcessFluid(id, tint); }

    public static FluidStack beverage(BeverageData data, int amount) {
        LegacyBeverageCatalog.Definition definition = LegacyBeverageCatalog.find(data.beverage());
        if (definition == null) throw new IllegalArgumentException("Unknown beverage " + data.beverage());
        BeverageFluid fluid = BEVERAGES.get(definition.category() + ":" + definition.value());
        if (fluid == null) throw new IllegalArgumentException("Legacy liquid was not registered: " + data.beverage());
        return fluid.stack(amount);
    }

    public static FluidStack stack(String category, String value, int amount) {
        BeverageFluid fluid = BEVERAGES.get(category + ":" + value);
        if (fluid == null) throw new IllegalArgumentException("Legacy liquid was not registered: " + category + ':' + value);
        return fluid.stack(amount);
    }

    public static BeverageData data(FluidStack stack) {
        if (stack.isEmpty()) return null;
        for (BeverageFluid beverage : BEVERAGES.values()) {
            if (beverage.fluid() == stack.getFluid()) return beverage.definition().data(stack.getAmount());
        }
        return null;
    }

    public static boolean isBeverage(FluidStack stack, String category, String value) {
        BeverageFluid expected = BEVERAGES.get(category + ":" + value);
        return expected != null && !stack.isEmpty() && stack.getFluid() == expected.fluid();
    }

    public static Iterable<BeverageFluid> beverages() { return BEVERAGES.values(); }
    public static BeverageFluid beverage(String category, String value) { return BEVERAGES.get(category + ":" + value); }

    public static void register(IEventBus bus) {
        TYPES.register(bus); FLUIDS.register(bus); BLOCKS.register(bus); ITEMS.register(bus);
    }

    public static final class BeverageFluid {
        private final LegacyBeverageCatalog.Definition definition;
        private final DeferredHolder<FluidType, FluidType> type;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        private final DeferredBlock<LiquidBlock> block;
        private final DeferredItem<BucketItem> bucket;

        private BeverageFluid(LegacyBeverageCatalog.Definition definition) {
            this.definition = definition;
            String id = definition.legacyIdentifier().toLowerCase(java.util.Locale.ROOT);
            this.type = TYPES.register(id, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId(LegacyBeverageCatalog.translationKey(definition.data(1_000)))
                    .density(1_000).viscosity(1_000).temperature(300)));
            this.source = FLUIDS.register(id, () -> new BaseFlowingFluid.Source(properties()));
            this.flowing = FLUIDS.register(id + ".flowing", () -> new BaseFlowingFluid.Flowing(properties()));
            this.block = BLOCKS.register(id, () -> new LiquidBlock(source.get(),
                    BlockBehaviour.Properties.of().liquid().noCollission().noLootTable().replaceable()));
            this.bucket = ITEMS.registerItem(id + "_bucket", properties -> new BucketItem(source.get(),
                    properties.craftRemainder(Items.BUCKET).stacksTo(1)));
        }

        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type, source, flowing).block(block).bucket(bucket)
                    .tickRate(20).slopeFindDistance(1).levelDecreasePerBlock(8);
        }
        public LegacyBeverageCatalog.Definition definition() { return definition; }
        public Fluid fluid() { return source.get(); }
        public FluidType type() { return type.get(); }
        public LiquidBlock block() { return block.get(); }
        public BucketItem bucket() { return bucket.get(); }
        public FluidStack stack(int amount) { return new FluidStack(source.get(), amount); }
        public int tint() { return definition.color(); }
    }

    /** Fixed, placeable 1.12 Binnie's Trees process liquid. */
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
            this.type = TYPES.register(id, () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.binnietrees." + id).density(1_000).viscosity(1_000).temperature(300)));
            this.source = FLUIDS.register(id, () -> new BaseFlowingFluid.Source(properties()));
            this.flowing = FLUIDS.register(id + "_flowing", () -> new BaseFlowingFluid.Flowing(properties()));
            this.block = BLOCKS.register(id, () -> new LiquidBlock(source.get(), BlockBehaviour.Properties.of().liquid().noCollission().noLootTable().replaceable()));
            this.blockItem = ITEMS.registerItem(id, properties -> new BlockItem(block.get(), properties));
            this.bucket = ITEMS.registerItem(id + "_bucket", properties -> new BucketItem(source.get(), properties.craftRemainder(Items.BUCKET).stacksTo(1)));
        }
        private BaseFlowingFluid.Properties properties() { return new BaseFlowingFluid.Properties(type, source, flowing).block(block).bucket(bucket).tickRate(20).slopeFindDistance(1).levelDecreasePerBlock(8); }
        public Fluid fluid() { return source.get(); }
        public FluidType type() { return type.get(); }
        public net.minecraft.world.level.block.Block block() { return block.get(); }
        public BucketItem bucket() { return bucket.get(); }
        public BlockItem blockItem() { return blockItem.get(); }
        public FluidStack stack(int amount) { return new FluidStack(source.get(), amount); }
        public int tint() { return tint; }
    }
}
