package binnie.design.client;

import binnie.design.api.DesignedBlockKind;
import binnie.design.data.DesignLayoutCatalog;
import binnie.trees.BinnieTrees;
import binnie.botany.registry.BotanyWorldContent;
import binnie.trees.registry.BinnieTreesDesignContent;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client registration for the ordinary baked, model-data-aware Design renderer. */
@EventBusSubscriber(modid = BinnieTrees.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class DesignClientEvents {
    private DesignClientEvents() {}

    @SubscribeEvent
    public static void registerPatternModels(ModelEvent.RegisterAdditional event) {
        for (DesignedBlockKind kind : DesignedBlockKind.values()) {
            // Panel and wood share the matching 1.12 wood family.
            if (kind == DesignedBlockKind.PANEL) continue;
            for (String pattern : DesignLayoutCatalog.renderPatterns()) {
                event.register(ModelResourceLocation.standalone(DesignedBlockModel.spriteModel(kind, pattern, 0)));
                event.register(ModelResourceLocation.standalone(DesignedBlockModel.spriteModel(kind, pattern, 1)));
            }
        }
    }

    @SubscribeEvent
    public static void replaceModels(ModelEvent.ModifyBakingResult event) {
        install(event, DesignedBlockKind.WOOD, BinnieTreesDesignContent.PATTERNED_WOOD_BLOCK.get(),
                BinnieTreesDesignContent.PATTERNED_WOOD.get());
        install(event, DesignedBlockKind.PANEL, BinnieTreesDesignContent.PATTERNED_PANEL_BLOCK.get(),
                BinnieTreesDesignContent.PATTERNED_PANEL.get());
        install(event, DesignedBlockKind.GLASS, BinnieTreesDesignContent.PATTERNED_GLASS_BLOCK.get(),
                BinnieTreesDesignContent.PATTERNED_GLASS.get());
        install(event, DesignedBlockKind.CERAMIC, BotanyWorldContent.CERAMIC_TILE.get(),
                BotanyWorldContent.CERAMIC_TILE_ITEM.get());
    }

    private static void install(ModelEvent.ModifyBakingResult event, DesignedBlockKind kind,
                                binnie.design.block.DesignedBlock block, binnie.design.block.DesignedBlockItem item) {
        DesignedBlockModel model = new DesignedBlockModel(kind, event.getModels());
        for (var state : block.getStateDefinition().getPossibleStates()) {
            event.getModels().put(BlockModelShaper.stateToModelLocation(state), model);
        }
        event.getModels().put(ModelResourceLocation.inventory(item.builtInRegistryHolder().key().location()), model);
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(DesignedBlockColors::blockColor,
                BinnieTreesDesignContent.PATTERNED_WOOD_BLOCK.get(),
                BinnieTreesDesignContent.PATTERNED_PANEL_BLOCK.get(),
                BinnieTreesDesignContent.PATTERNED_GLASS_BLOCK.get(),
                BotanyWorldContent.CERAMIC_TILE.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> DesignedBlockColors.itemColor(stack, tint, DesignedBlockKind.WOOD),
                BinnieTreesDesignContent.PATTERNED_WOOD.get());
        event.register((stack, tint) -> DesignedBlockColors.itemColor(stack, tint, DesignedBlockKind.PANEL),
                BinnieTreesDesignContent.PATTERNED_PANEL.get());
        event.register((stack, tint) -> DesignedBlockColors.itemColor(stack, tint, DesignedBlockKind.GLASS),
                BinnieTreesDesignContent.PATTERNED_GLASS.get());
        event.register((stack, tint) -> DesignedBlockColors.itemColor(stack, tint, DesignedBlockKind.CERAMIC),
                BotanyWorldContent.CERAMIC_TILE_ITEM.get());
    }
}
