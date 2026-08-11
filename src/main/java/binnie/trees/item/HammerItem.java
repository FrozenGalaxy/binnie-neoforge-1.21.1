package binnie.trees.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Carpentry tool retained for 1.12 recipes and future hammer-aware design interactions. */
public final class HammerItem extends Item {
    public HammerItem(Properties properties, boolean durable) {
        super(properties.stacksTo(1).durability(durable ? 1562 : 251));
    }

    public boolean isActive(ItemStack stack) {
        return !stack.isEmpty() && stack.getDamageValue() < stack.getMaxDamage();
    }

    public void damageAfterUse(ItemStack stack, LivingEntity user) {
        stack.hurtAndBreak(1, user, EquipmentSlot.MAINHAND);
    }
}
