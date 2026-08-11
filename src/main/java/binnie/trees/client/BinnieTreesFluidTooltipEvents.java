package binnie.trees.client;

import binnie.trees.BinnieTrees;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/** Game-bus tooltip styling kept separate from the mod-bus fluid registration. */
@EventBusSubscriber(modid = BinnieTrees.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class BinnieTreesFluidTooltipEvents {
    private BinnieTreesFluidTooltipEvents() {}

    @SubscribeEvent
    public static void styleFluidTooltipAttribution(RenderTooltipEvent.GatherComponents event) {
        for (int index = 0; index < event.getTooltipElements().size(); index++) {
            Either<FormattedText, TooltipComponent> element = event.getTooltipElements().get(index);
            FormattedText text = element.left().orElse(null);
            if (text != null && "Binnies: Trees".equals(text.getString())) {
                event.getTooltipElements().set(index, Either.left(Component.literal(text.getString())
                        .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC)));
            }
        }
    }
}
