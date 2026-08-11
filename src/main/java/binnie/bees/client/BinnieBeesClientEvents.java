package binnie.bees.client;

import binnie.core.item.VariantItem;
import binnie.bees.BinnieBees;
import binnie.bees.registry.BinnieBeesContent;
import binnie.bees.menu.AlvearyMenu;
import binnie.bees.menu.BinnieBeesMenus;
import binnie.bees.client.screen.AlvearyComponentScreen;
import binnie.bees.client.screen.AlvearyMutatorScreen;
import binnie.bees.client.screen.AlvearyFrameScreen;
import binnie.bees.client.screen.AlvearyStimulatorScreen;
import binnie.bees.client.screen.AlvearyHatcheryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Provides the two-pass colors used by Binnie's Bees' 1.12 products. */
@EventBusSubscriber(modid = BinnieBees.MOD_ID, value = Dist.CLIENT)
public final class BinnieBeesClientEvents {
    private BinnieBeesClientEvents() {}


    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BinnieBeesMenus.ALVEARY.get(), BinnieBeesClientEvents::createAlvearyScreen);
    }

    private static AlvearyComponentScreen createAlvearyScreen(AlvearyMenu menu, Inventory inventory, Component title) {
        return switch (menu.partType()) {
            case MUTATOR -> new AlvearyMutatorScreen(menu, inventory, title);
            case FRAME -> new AlvearyFrameScreen(menu, inventory, title);
            case STIMULATOR -> new AlvearyStimulatorScreen(menu, inventory, title);
            case HATCHERY -> new AlvearyHatcheryScreen(menu, inventory, title);
            default -> throw new IllegalStateException("Non-GUI alveary component opened a menu: " + menu.partType());
        };
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tint((VariantItem) stack.getItem(), stack, tintIndex, BinnieBeesLegacyColors.COMB),
                BinnieBeesContent.HONEY_COMB.get());
        event.register((stack, tintIndex) -> tint((VariantItem) stack.getItem(), stack, tintIndex, BinnieBeesLegacyColors.DROP),
                BinnieBeesContent.HONEY_DROP.get());
        event.register((stack, tintIndex) -> tint((VariantItem) stack.getItem(), stack, tintIndex, BinnieBeesLegacyColors.PROPOLIS),
                BinnieBeesContent.PROPOLIS.get());
    }

    private static int tint(VariantItem item, net.minecraft.world.item.ItemStack stack, int tintIndex, java.util.Map<String, int[]> colors) {
        int[] pair = colors.get(item.value(stack));
        if (pair == null || tintIndex < 0 || tintIndex >= pair.length) return 0xFFFFFFFF;
        return 0xFF000000 | pair[tintIndex];
    }
}
