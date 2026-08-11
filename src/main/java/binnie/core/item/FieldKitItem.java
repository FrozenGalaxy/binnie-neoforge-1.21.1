package binnie.core.item;

import binnie.core.menu.FieldKitMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Portable 1.12-style field kit. Paper is stored as inverse item damage. */
public final class FieldKitItem extends Item {
    public static final int CAPACITY = 64;

    public FieldKitItem(Properties properties) {
        super(properties.stacksTo(1).durability(CAPACITY));
    }

    public static int remainingPaper(ItemStack stack) {
        return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            boolean mainHand = hand == InteractionHand.MAIN_HAND;
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new FieldKitMenu(containerId, inventory, mainHand),
                    stack.getHoverName()), buffer -> buffer.writeBoolean(mainHand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int remaining = remainingPaper(stack);
        Component paper = remaining == 0
                ? Component.translatable("tooltip.binniecore.field_kit.empty")
                : Component.translatable("tooltip.binniecore.field_kit.uses", remaining, stack.getMaxDamage());
        tooltip.add(paper.copy().withStyle(remaining == 0 ? ChatFormatting.RED : ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
