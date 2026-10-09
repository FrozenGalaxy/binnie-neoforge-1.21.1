package binnie.genetics.client;

import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.BinnieGenetics;
import binnie.genetics.item.GeneticMaterialItem;
import binnie.genetics.registry.GeneticsContent;
import binnie.genetics.registry.GeneticsMenus;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.client.gui.screens.Screen;

@EventBusSubscriber(modid = BinnieGenetics.MOD_ID, value = Dist.CLIENT)
public class GeneticsClientEvents {
    private GeneticsClientEvents() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(GeneticsMenus.KNOWLEDGE.get(), GeneticKnowledgeScreenFactory::create);
        event.register(GeneticsMenus.PUNNETT.get(), PunnettSquareScreen::new);
    }

    /** Selects the original 1.12 item layers from the component-backed material kind. */
    @SubscribeEvent
    public static void registerMaterialModels(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(GeneticsContent.GENETIC_MATERIAL.get(),
                ResourceLocation.fromNamespaceAndPath(BinnieGenetics.MOD_ID, "material_form"),
                (stack, level, entity, seed) -> {
                    GeneticMaterialItem item = (GeneticMaterialItem) stack.getItem();
                    return switch (item.kind(stack)) {
                        case GENE -> 0.0F;
                        case SEQUENCE -> 1.0F;
                        case SERUM -> stack.has(BinnieDataComponents.GENETIC_SERUM) ? 3.0F : 2.0F;
                        case SERUM_ARRAY -> stack.has(BinnieDataComponents.GENETIC_SERUM) ? 5.0F : 4.0F;
                    };
                }));
    }

    /** Expand the 1.12 serum-array gene listing only while the player holds Shift. */
    @SubscribeEvent
    public static void expandSerumTooltip(ItemTooltipEvent event) {
        if (!Screen.hasShiftDown() || !(event.getItemStack().getItem() instanceof GeneticMaterialItem material)) return;
        var serum = event.getItemStack().get(BinnieDataComponents.GENETIC_SERUM);
        if (serum == null || material.kind(event.getItemStack()) != GeneticMaterialItem.Kind.SERUM_ARRAY) return;
    // Genetic Material has no unrelated tooltip contributors. Rebuild its
    // post-title area exactly as 1.12 GeneArrayItem did when Shift was held.
        if (event.getToolTip().size() > 1) event.getToolTip().subList(1, event.getToolTip().size()).clear();
        GeneticMaterialItem.addSerumTooltip(serum, event.getToolTip(), true);
    }
}
