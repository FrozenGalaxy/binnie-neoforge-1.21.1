package binnie.core.client;

import binnie.core.BinnieCore;
import binnie.core.client.ponder.BinniePonderCompat;
import binnie.core.machine.MachineRegistries;
import binnie.core.registry.CoreRegistries;
import binnie.core.registry.CoreContent;
import binnie.genetics.client.LabStandRenderer;
import binnie.genetics.registry.GeneticsMachines;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import binnie.core.data.BeverageData;
import binnie.core.liquid.BinnieFluidContainerItem;
import binnie.core.registry.BinnieDataComponents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.ModList;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import binnie.core.storage.CompartmentBlockItem;

@EventBusSubscriber(modid = BinnieCore.MOD_ID, value = Dist.CLIENT)
public class BinnieCoreClientEvents {
    private BinnieCoreClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(CoreRegistries.COMPARTMENT_ITEM.get(),
                    ResourceLocation.fromNamespaceAndPath(BinnieCore.MOD_ID, "tier"),
                    (stack, level, entity, seed) -> CompartmentBlockItem.tier(stack).ordinal());
            for (BinnieFluidContainerItem item : new BinnieFluidContainerItem[]{CoreContent.GLASS.get(), CoreContent.CYLINDER.get()}) {
                ItemProperties.register(item, ResourceLocation.fromNamespaceAndPath(BinnieCore.MOD_ID, "filled"),
                        (stack, level, entity, seed) -> item.fluid(stack).isEmpty() ? 0.0F : 1.0F);
            }
            if (ModList.get().isLoaded("ponder")) {
                BinniePonderCompat.register();
            }
        });
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0 || !(stack.getItem() instanceof BinnieFluidContainerItem item)) return 0xFFFFFFFF;
            var fluid = item.fluid(stack);
            if (fluid.isEmpty()) return 0xFFFFFFFF;
            BeverageData beverage = fluid.get(BinnieDataComponents.BEVERAGE);
            return beverage != null ? 0xFF000000 | beverage.color()
                    : IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
        }, CoreContent.GLASS.get(), CoreContent.CYLINDER.get());
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GeneticsMachines.REGISTRY.blockEntityType().get(), LabStandRenderer::new);
        event.registerBlockEntityRenderer(CoreRegistries.PONDER_FLUID_PIPE_BLOCK_ENTITY.get(), PonderFluidPipeRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MachineRegistries.MACHINE_MENU.get(), MachineScreenFactory::create);
        event.register(CoreRegistries.COMPARTMENT_MENU.get(), CompartmentScreen::new);
        event.register(CoreRegistries.FIELD_KIT_MENU.get(), FieldKitScreen::new);
        event.register(CoreRegistries.GENESIS_MENU.get(), GenesisScreen::new);
    }

}
