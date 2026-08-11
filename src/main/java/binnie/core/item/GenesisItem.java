package binnie.core.item;

import binnie.core.menu.GenesisMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Creative genome constructor replacing the 1.12 Genesis chromosome builder. */
public final class GenesisItem extends Item {
    public GenesisItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean mainHand = hand == InteractionHand.MAIN_HAND;
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, menuPlayer) -> new GenesisMenu(id, inventory, mainHand),
                    Component.translatable("screen.binniecore.genesis.title")), buffer -> buffer.writeBoolean(mainHand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
