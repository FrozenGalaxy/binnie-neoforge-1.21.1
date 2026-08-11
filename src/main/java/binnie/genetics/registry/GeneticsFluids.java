package binnie.genetics.registry;

import binnie.genetics.BinnieGenetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** 1.12-compatible Genetics process liquids with standard world blocks and bucket containers. */
public final class GeneticsFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, BinnieGenetics.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, BinnieGenetics.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BinnieGenetics.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BinnieGenetics.MOD_ID);

    public static final ProcessFluid GROWTH_MEDIUM = register("growth_medium", 0xEBE3B5);
    public static final ProcessFluid BACTERIA = register("bacteria", 0xD8B901);
    public static final ProcessFluid POLYMERISING_BACTERIA = register("polymerising_bacteria", 0xAE9DC4);
    public static final ProcessFluid RAW_DNA = register("raw_dna", 0xE639E9);
    public static final ProcessFluid VECTOR_BACTERIA = register("vector_bacteria", 0xF38DFE);

    private GeneticsFluids() {}

    private static ProcessFluid register(String id, int tint) {
        return new ProcessFluid(id, tint);
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static final class ProcessFluid {
        private final int tint;
        private final DeferredHolder<FluidType, FluidType> type;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        private final DeferredBlock<LiquidBlock> block;
        private final DeferredItem<BucketItem> bucket;

        private ProcessFluid(String id, int tint) {
            this.tint = tint;
            this.type = TYPES.register(id, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.binniegenetics." + id)
                    .density(1_000)
                    .viscosity(1_000)
                    .temperature(300)));
            this.source = FLUIDS.register(id, () -> new BaseFlowingFluid.Source(properties()));
            this.flowing = FLUIDS.register(id + "_flowing", () -> new BaseFlowingFluid.Flowing(properties()));
            this.block = BLOCKS.register(id, () -> new LiquidBlock(source.get(), BlockBehaviour.Properties.of()
                    .liquid().noCollission().noLootTable().replaceable()));
            this.bucket = ITEMS.registerItem(id + "_bucket", properties -> new BucketItem(source.get(),
                    properties.craftRemainder(Items.BUCKET).stacksTo(1)));
        }

        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type, source, flowing).block(block).bucket(bucket)
                    .tickRate(20).slopeFindDistance(1).levelDecreasePerBlock(8);
        }

        public Fluid fluid() {
            return source.get();
        }

        public FluidType type() {
            return type.get();
        }

        public int tint() {
            return tint;
        }

        public LiquidBlock block() { return block.get(); }
        public BucketItem bucket() { return bucket.get(); }

        public FluidStack stack(int amount) {
            return new FluidStack(source.get(), amount);
        }
    }
}
