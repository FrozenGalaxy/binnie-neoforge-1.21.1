package binnie.core.storage;

import binnie.core.data.VariantData;
import binnie.core.registry.BinnieDataComponents;
import binnie.core.registry.CoreRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Single registry item retaining all six 1.12 compartment tiers through the shared variant component. */
public final class CompartmentBlockItem extends BlockItem {
    public static final String CATEGORY = "binniecore.compartment";

    public CompartmentBlockItem(Block block, Properties properties) {
        super(block, properties.stacksTo(64));
    }

    public static ItemStack createStack(CompartmentTier tier) {
        ItemStack stack = new ItemStack(CoreRegistries.COMPARTMENT_ITEM.get());
        stack.set(BinnieDataComponents.VARIANT, new VariantData(CATEGORY, tier.getSerializedName()));
        return stack;
    }

    public static CompartmentTier tier(ItemStack stack) {
        VariantData variant = stack.get(BinnieDataComponents.VARIANT);
        return variant != null && CATEGORY.equals(variant.category()) ? CompartmentTier.byName(variant.value()) : CompartmentTier.PLAIN;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.binniecore.compartment." + tier(stack).getSerializedName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompartmentTier tier = tier(stack);
        tooltip.add(Component.translatable("tooltip.binniecore.compartment.capacity", tier.tabs(), tier.slotsPerTab(), tier.capacity())
                .withStyle(ChatFormatting.GRAY));
    }
}
