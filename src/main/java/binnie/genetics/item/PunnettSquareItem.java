package binnie.genetics.item;

import binnie.genetics.menu.PunnettSquareMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Opens the current typed two-parent Punnett calculator. */
public final class PunnettSquareItem extends Item {
    public PunnettSquareItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean mainHand = hand == InteractionHand.MAIN_HAND;
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, menuPlayer) -> new PunnettSquareMenu(id, inventory, mainHand),
                    stack.getHoverName()), buffer -> buffer.writeBoolean(mainHand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
