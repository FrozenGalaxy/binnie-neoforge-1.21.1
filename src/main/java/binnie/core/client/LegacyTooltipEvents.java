package binnie.core.client;

import binnie.core.BinnieCore;
import binnie.core.registry.BinnieDataComponents;
import binnie.core.data.VariantData;
import binnie.core.storage.CompartmentBlockItem;
import binnie.core.tooltip.LegacyTooltipDescriptions;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Adds Binnie's item-description tooltips on the client. */
@EventBusSubscriber(modid = BinnieCore.MOD_ID, value = Dist.CLIENT)
public class LegacyTooltipEvents {
    private static final ResourceLocation COMPARTMENT = ResourceLocation.fromNamespaceAndPath("binniecore", "compartment");

    private LegacyTooltipEvents() {
    }

    @SubscribeEvent
    public static void addLegacyDescriptions(ItemTooltipEvent event) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());

        LegacyTooltipDescriptions.alwaysVisibleDescription(itemId).ifPresent(key -> {
            Component text = Component.translatable(key);
            // The two Binnie's Trees master databases were dark purple in 1.12;
            // the Master Bee Dictionary and deprecated mortar used normal text.
            if (itemId.getNamespace().equals("binnietrees")) text = text.copy().withStyle(ChatFormatting.DARK_PURPLE);
            event.getToolTip().add(text);
        });

        if (!Screen.hasShiftDown()) return;

        if (COMPARTMENT.equals(itemId)) {
            VariantData data = event.getItemStack().get(BinnieDataComponents.VARIANT);
            String tier = data != null && CompartmentBlockItem.CATEGORY.equals(data.category()) ? data.value() : "plain";
            event.getToolTip().add(Component.translatable(LegacyTooltipDescriptions.compartmentDescription(tier)).withStyle(ChatFormatting.GRAY));
            return;
        }

        LegacyTooltipDescriptions.shiftDescription(itemId)
                .ifPresent(key -> event.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.GRAY)));
    }
}
