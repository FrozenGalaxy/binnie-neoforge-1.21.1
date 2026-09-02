package binnie.botany.farming;

import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import forestry.agriculture.multifarm.circuits.MultifarmCircuit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** The original Botany garden circuit, including its compact tube-tooltip line. */
public final class BotanyGardenCircuit extends MultifarmCircuit {
    private final BotanyFarmVariants.Variant variant;

    public BotanyGardenCircuit(String id, ResourceLocation farmType, BotanyFarmVariants.Variant variant) {
        super(id, farmType, variant.manual());
        this.variant = variant;
    }

    @Override
    public void addTooltip(List<Component> tooltip) {
        tooltip.add(statsTooltip(variant));
    }

    public static Component statsTooltip(BotanyFarmVariants.Variant variant) {
        MutableComponent line = Component.literal(" - Flowers (");
        line.append(Component.translatable("binniebotany.moisture").withStyle(ChatFormatting.GRAY));
        line.append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
        line.append(Component.translatable("binniebotany.moisture." + variant.moisture().getSerializedName())
                .withStyle(moistureColor(variant.moisture())));
        line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
        if (variant.acidity() != null) {
            line.append(Component.translatable("binniebotany.ph").withStyle(ChatFormatting.GRAY));
            line.append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
            line.append(Component.translatable("binniebotany.acidity." + variant.acidity().getSerializedName())
                    .withStyle(acidityColor(variant.acidity())));
            line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
        }
        line.append(Component.translatable(variant.fertilised()
                        ? "tooltip.binniebotany.tube.fertilized"
                        : "tooltip.binniebotany.tube.unfertilized")
                .withStyle(variant.fertilised() ? ChatFormatting.GREEN : ChatFormatting.RED));
        return line.append(Component.literal(")").withStyle(ChatFormatting.GRAY));
    }

    private static ChatFormatting moistureColor(SoilMoisture moisture) {
        return switch (moisture) {
            case DRY -> ChatFormatting.YELLOW;
            case NORMAL -> ChatFormatting.WHITE;
            case DAMP -> ChatFormatting.DARK_BLUE;
        };
    }

    private static ChatFormatting acidityColor(SoilAcidity acidity) {
        return switch (acidity) {
            case ACID -> ChatFormatting.RED;
            case NEUTRAL -> ChatFormatting.WHITE;
            case ALKALINE -> ChatFormatting.AQUA;
        };
    }
}
