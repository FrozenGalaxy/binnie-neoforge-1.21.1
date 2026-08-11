package binnie.design.block;

import binnie.core.data.DesignData;
import binnie.core.registry.BinnieDataComponents;
import binnie.design.api.DesignedBlockKind;
import binnie.design.data.DesignCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class DesignedBlockItem extends BlockItem {
    private final DesignedBlockKind kind;

    public DesignedBlockItem(DesignedBlock block, Properties properties, DesignedBlockKind kind) {
        super(block, properties);
        this.kind = kind;
    }

    public ItemStack create(DesignData design, int count) {
        ItemStack stack = new ItemStack(this, count);
        stack.set(BinnieDataComponents.DESIGN, DesignCatalog.validate(design));
        return stack;
    }

    public DesignedBlockKind kind() {
        return kind;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack,
                                                  net.minecraft.world.level.block.state.BlockState state) {
        boolean changed = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        DesignData data = stack.get(BinnieDataComponents.DESIGN);
        if (data != null && level.getBlockEntity(pos) instanceof DesignedBlockEntity designed) {
            designed.setData(data);
            return true;
        }
        return changed;
    }

    @Override
    public Component getName(ItemStack stack) {
        DesignData data = stack.get(BinnieDataComponents.DESIGN);
        return data == null ? super.getName(stack) : Component.translatable(getDescriptionId() + ".designed", data.design());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DesignData data = stack.get(BinnieDataComponents.DESIGN);
        if (data == null) return;
        tooltip.add(Component.translatable("tooltip.binniedesign.pattern", data.pattern()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(data.primaryMaterial() + " + " + data.secondaryMaterial()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.binniedesign.transform", data.rotation(), data.mirrored()).withStyle(ChatFormatting.DARK_AQUA));
    }
}
