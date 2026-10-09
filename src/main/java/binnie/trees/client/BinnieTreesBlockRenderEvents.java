package binnie.trees.client;

import binnie.trees.BinnieTrees;
import binnie.trees.registry.BinnieTreesWorldContent;
import binnie.trees.registry.BinnieTreesMachines;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import binnie.trees.registry.BinnieTreesContent;

/** Client render layers for foliage-like Binnie's Trees blocks. */
@EventBusSubscriber(modid = BinnieTrees.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
@SuppressWarnings({"deprecation", "removal"}) // 1.21.1 lifecycle/render-layer compatibility API.
public class BinnieTreesBlockRenderEvents {
    private BinnieTreesBlockRenderEvents() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(BinnieTreesContent.GLASSWARE.get(),
                    ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, "glassware_model"),
                    (stack, level, entity, seed) -> {
                        var item = BinnieTreesContent.GLASSWARE.get();
                        int shape = item.variants().indexOf(item.value(stack));
                        return shape * 2.0F + (item.fluid(stack).isEmpty() ? 0.0F : 1.0F);
                    });
            for (var hedge : BinnieTreesWorldContent.hedges().values()) {
                ItemBlockRenderTypes.setRenderLayer(hedge.get(), RenderType.cutout());
            }
            for (var leaves : BinnieTreesWorldContent.decorativeLeaves().values()) {
                ItemBlockRenderTypes.setRenderLayer(leaves.get(), RenderType.cutoutMipped());
            }
            for (String id : BinnieTreesMachines.REGISTRY.specs().keySet()) {
                ItemBlockRenderTypes.setRenderLayer(BinnieTreesMachines.REGISTRY.block(id).get(), RenderType.cutout());
            }
        });
    }
}
