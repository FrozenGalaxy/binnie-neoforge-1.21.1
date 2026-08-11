package binnie.botany.item;

import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import binnie.botany.block.BotanySoilBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;

import java.util.List;
import java.util.Map;

/** Component-backed soil item with the original ItemSoil condition tooltip. */
public final class SoilBlockItem extends BlockItem {
    public SoilBlockItem(BotanySoilBlock block, Properties properties) { super(block, properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        appendSoilTooltip(stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).properties(), tooltip);
    }

    public static void appendSoilTooltip(Map<String, String> properties, List<Component> tooltip) {
        SoilMoisture moisture = parseMoisture(properties.get(BotanySoilBlock.MOISTURE.getName()));
        SoilAcidity acidity = parseAcidity(properties.get(BotanySoilBlock.ACIDITY.getName()));
        Component conditions = Component.empty();
        if (moisture != SoilMoisture.NORMAL) {
            conditions = Component.translatable("tooltip.binniebotany.soil.moisture",
                    Component.translatable("tooltip.binniebotany.soil.moisture." + moisture.getSerializedName()));
        }
        if (acidity != SoilAcidity.NEUTRAL) {
            Component ph = Component.translatable("tooltip.binniebotany.soil.ph",
                    Component.translatable("tooltip.binniebotany.soil.ph." + acidity.getSerializedName()));
            conditions = conditions.getString().isEmpty() ? ph : conditions.copy().append(", ").append(ph);
        }
        if (!conditions.getString().isEmpty()) tooltip.add(conditions.copy().withStyle(ChatFormatting.GRAY));
        if (Boolean.parseBoolean(properties.getOrDefault(BotanySoilBlock.WEEDKILLED.getName(), "false"))) {
            tooltip.add(Component.translatable("tooltip.binniebotany.soil.weedkiller").withStyle(ChatFormatting.GREEN));
        }
    }

    /** Stable 1..18 model predicate for acidity x moisture x weedkiller state. */
    public static float modelVariant(ItemStack stack) {
        Map<String, String> properties = stack
                .getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                .properties();
        int acidity = parseAcidity(properties.get(BotanySoilBlock.ACIDITY.getName())).ordinal();
        int moisture = parseMoisture(properties.get(BotanySoilBlock.MOISTURE.getName())).ordinal();
        int weedkilled = Boolean.parseBoolean(properties.getOrDefault(
                BotanySoilBlock.WEEDKILLED.getName(), "false")) ? 9 : 0;
        return 1 + weedkilled + acidity * 3 + moisture;
    }

    private static SoilMoisture parseMoisture(String value) {
        for (SoilMoisture entry : SoilMoisture.values()) if (entry.getSerializedName().equals(value)) return entry;
        return SoilMoisture.NORMAL;
    }

    private static SoilAcidity parseAcidity(String value) {
        for (SoilAcidity entry : SoilAcidity.values()) if (entry.getSerializedName().equals(value)) return entry;
        return SoilAcidity.NEUTRAL;
    }
}
