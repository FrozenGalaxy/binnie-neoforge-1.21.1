package binnie.genetics.client;

import binnie.genetics.BinnieGenetics;
import binnie.genetics.registry.GeneticsFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/** Original Genetics liquid sprites for world, bucket, capsule and tank rendering. */
@EventBusSubscriber(modid = BinnieGenetics.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GeneticsFluidClientEvents {
    private GeneticsFluidClientEvents() {}

    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        register(event, "growth_medium", GeneticsFluids.GROWTH_MEDIUM);
        register(event, "bacteria", GeneticsFluids.BACTERIA);
        register(event, "polymerising_bacteria", GeneticsFluids.POLYMERISING_BACTERIA);
        register(event, "raw_dna", GeneticsFluids.RAW_DNA);
        register(event, "vector_bacteria", GeneticsFluids.VECTOR_BACTERIA);
    }

    private static void register(RegisterClientExtensionsEvent event, String id, GeneticsFluids.ProcessFluid fluid) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(BinnieGenetics.MOD_ID, "block/fluid/" + id);
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return texture; }
            @Override public ResourceLocation getFlowingTexture() { return texture; }
            @Override public int getTintColor() { return 0xFF000000 | fluid.tint(); }
            @Override public int getTintColor(FluidStack stack) { return getTintColor(); }
        }, fluid.type());
    }
}
