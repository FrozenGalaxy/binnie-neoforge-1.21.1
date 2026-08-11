package binnie.core.registry;

import binnie.core.BinnieCore;
import binnie.core.data.BeverageData;
import binnie.core.data.DesignData;
import binnie.core.data.GenomeData;
import binnie.core.data.VariantData;
import binnie.genetics.data.GeneticAlleleData;
import binnie.genetics.data.GeneticSequenceData;
import binnie.genetics.data.GeneticSerumData;
import binnie.genetics.data.GeneticKnowledgeData;
import binnie.genetics.data.LaboratoryChargeData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BinnieDataComponents {
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, BinnieCore.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<VariantData>> VARIANT = COMPONENTS.registerComponentType("variant", builder -> builder.persistent(VariantData.CODEC).networkSynchronized(VariantData.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GenomeData>> GENOME = COMPONENTS.registerComponentType("genome", builder -> builder.persistent(GenomeData.CODEC).networkSynchronized(GenomeData.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DesignData>> DESIGN = COMPONENTS.registerComponentType("design", builder -> builder.persistent(DesignData.CODEC).networkSynchronized(DesignData.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BeverageData>> BEVERAGE = COMPONENTS.registerComponentType("beverage", builder -> builder.persistent(BeverageData.CODEC).networkSynchronized(BeverageData.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT = COMPONENTS.registerComponentType("fluid_content", builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FLOWER_GENERATION = COMPONENTS.registerComponentType("flower_generation", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FLOWER_AGE = COMPONENTS.registerComponentType("flower_age", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FLOWER_WILTED = COMPONENTS.registerComponentType("flower_wilted", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FLOWER_FLOWERED = COMPONENTS.registerComponentType("flower_flowered", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GeneticAlleleData>> GENETIC_ALLELE = COMPONENTS.registerComponentType("genetic_allele", builder -> builder.persistent(GeneticAlleleData.CODEC).networkSynchronized(GeneticAlleleData.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GeneticSequenceData>> GENETIC_SEQUENCE = COMPONENTS.registerComponentType("genetic_sequence", builder -> builder.persistent(GeneticSequenceData.CODEC).networkSynchronized(GeneticSequenceData.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GeneticSerumData>> GENETIC_SERUM = COMPONENTS.registerComponentType("genetic_serum", builder -> builder.persistent(GeneticSerumData.CODEC).networkSynchronized(GeneticSerumData.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LaboratoryChargeData>> LABORATORY_CHARGE = COMPONENTS.registerComponentType("laboratory_charge", builder -> builder.persistent(LaboratoryChargeData.CODEC).networkSynchronized(LaboratoryChargeData.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GeneticKnowledgeData>> GENETIC_KNOWLEDGE = COMPONENTS.registerComponentType("genetic_knowledge", builder -> builder.persistent(GeneticKnowledgeData.CODEC).networkSynchronized(GeneticKnowledgeData.STREAM_CODEC).cacheEncoding());
    private BinnieDataComponents() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
