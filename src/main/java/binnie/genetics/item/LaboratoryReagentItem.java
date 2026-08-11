package binnie.genetics.item;

import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.data.LaboratoryChargeData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A one-cell laboratory reagent with 1.12-compatible fractional charge. */
public final class LaboratoryReagentItem extends Item {
    public LaboratoryReagentItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LaboratoryChargeData charge = stack.get(BinnieDataComponents.LABORATORY_CHARGE);
        int percent = (charge == null ? LaboratoryChargeData.FULL : charge.milliCharge()) / 100;
        tooltip.add(Component.translatable("tooltip.binniegenetics.reagent_charge", percent).withStyle(ChatFormatting.YELLOW));
    }
}
