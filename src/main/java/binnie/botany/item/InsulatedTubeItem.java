package binnie.botany.item;

import binnie.botany.farming.BotanyGardenCircuit;
import binnie.botany.farming.BotanyTubeVariants;
import binnie.core.item.VariantItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Provides 1.12's Shift-expanded circuit preview for physical insulated tubes. */
public final class InsulatedTubeItem extends VariantItem {
    public InsulatedTubeItem(Item.Properties properties, String category, List<String> variants) {
        super(properties, category, variants);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<binnie.botany.farming.BotanyFarmVariants.Variant> circuits = BotanyTubeVariants.circuitsFor(value(stack));
        if (circuits.isEmpty()) {
            tooltip.add(Component.literal("<").append(Component.translatable("for.gui.noeffect"))
                    .append(Component.literal(">")).withStyle(ChatFormatting.ITALIC));
            return;
        }
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.literal("<").append(Component.translatable("for.gui.tooltip.tmi"))
                    .append(Component.literal(">")).withStyle(ChatFormatting.ITALIC));
            return;
        }

    // 1.12 grouped the same physical tube under both Forestry layouts.
        for (boolean manual : new boolean[]{false, true}) {
            String layout = manual ? "forestry.farms.manual" : "forestry.farms.managed";
            tooltip.add(Component.translatable("circuit.layout." + layout + ".usage")
                    .withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE));
            for (var circuit : circuits) if (circuit.manual() == manual) tooltip.add(BotanyGardenCircuit.statsTooltip(circuit));
        }
    }
}
