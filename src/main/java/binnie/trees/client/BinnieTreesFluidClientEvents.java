package binnie.trees.client;

import binnie.trees.BinnieTrees;
import binnie.trees.registry.BinnieTreesFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/** Binds the original per-liquid animated sprites to the fixed Binnie's Trees liquids. */
@EventBusSubscriber(modid = BinnieTrees.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class BinnieTreesFluidClientEvents {
    private BinnieTreesFluidClientEvents() {}

    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        for (BinnieTreesFluids.BeverageFluid fluid : BinnieTreesFluids.beverages()) registerBeverage(event, fluid);
        register(event, "resin", BinnieTreesFluids.RESIN);
        register(event, "turpentine", BinnieTreesFluids.TURPENTINE);
    }

    private static void registerBeverage(RegisterClientExtensionsEvent event, BinnieTreesFluids.BeverageFluid fluid) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, "block/fluid/beverage");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return texture; }
            @Override public ResourceLocation getFlowingTexture() { return texture; }
            @Override public int getTintColor() { return 0xFF000000 | fluid.tint(); }
            @Override public int getTintColor(FluidStack stack) { return getTintColor(); }
        }, fluid.type());
    }

    private static void register(RegisterClientExtensionsEvent event, String id, BinnieTreesFluids.ProcessFluid fluid) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, "block/fluid/" + id);
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return texture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return texture;
            }

            @Override
            public int getTintColor() {
                return 0xFF000000 | fluid.tint();
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return getTintColor();
            }
        }, fluid.type());
    }
}
