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
@EventBusSubscriber(modid = BinnieTrees.MOD_ID, value = Dist.CLIENT)
public final class BinnieTreesFluidClientEvents {
    private BinnieTreesFluidClientEvents() {}

    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        registerBeverage(event);
        register(event, "sap", BinnieTreesFluids.SAP);
        register(event, "resin", BinnieTreesFluids.RESIN);
        register(event, "latex", BinnieTreesFluids.LATEX);
        register(event, "turpentine", BinnieTreesFluids.TURPENTINE);
    }

    private static void registerBeverage(RegisterClientExtensionsEvent event) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, "block/fluid/beverage");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return texture; }
            @Override public ResourceLocation getFlowingTexture() { return texture; }
            @Override public int getTintColor() { return 0xFFFFFFFF; }
            @Override public int getTintColor(FluidStack stack) {
                var data = BinnieTreesFluids.data(stack);
                return data == null ? 0xFFFFFFFF : 0xFF000000 | data.color();
            }
        }, BinnieTreesFluids.beverageType());
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
        }, fluid.type());
    }
}
