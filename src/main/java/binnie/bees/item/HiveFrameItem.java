package binnie.bees.item;

import binnie.core.item.VariantItem;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.hives.IHiveFrame;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import forestry.api.apiculture.genetics.IBeeSpecies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/** The five actual player-facing Binnie's Bees hive frames from 1.12. */
public final class HiveFrameItem extends VariantItem implements IHiveFrame {
    public static final List<String> VARIANTS = List.of("cocoa", "cage", "soul", "clay", "debug");
    private static final Map<String, Modifier> MODIFIERS = Map.of(
            "cocoa", new Modifier(1.50F, 1.00F, 0.75F, 1.00F),
            "cage", new Modifier(0.75F, 1.00F, 0.75F, 0.50F),
            "soul", new Modifier(0.25F, 1.50F, 0.75F, 1.00F),
            "clay", new Modifier(0.75F, 0.50F, 1.50F, 1.00F),
            "debug", new Modifier(1.00F, 1.00F, 0.0001F, 1.00F)
    );

    public HiveFrameItem(Properties properties) {
        super(properties.durability(240), "binniebees.hive_frame", VARIANTS);
    }

    @Override
    public ItemStack createStack(String value) {
        ItemStack stack = super.createStack(value);
        initialiseVariantComponents(stack, value);
        return stack;
    }

    /**
      * Vanilla's data-driven crafting result creates the item directly, so it
      * does not call {@link #createStack(String)}. Keep the
      * 1.12 frame's per-variant model and durability at the item lifecycle
      * boundary instead of making each recipe depend on fragile component JSON.
      */
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        initialiseVariantComponents(stack, value(stack));
    }

    private static void initialiseVariantComponents(ItemStack stack, String value) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(VARIANTS.indexOf(value)));
        stack.set(DataComponents.MAX_DAMAGE, maxDamage(value));
        stack.set(DataComponents.DAMAGE, 0);
    }

    @Override
    public ItemStack frameUsed(IBeeHousing housing, ItemStack frame, IBee queen, int wear) {
        if (housing.getLevel() instanceof ServerLevel server) {
            frame.hurtAndBreak(Math.max(1, wear), server, null, item -> {});
        }
        return frame.isEmpty() ? ItemStack.EMPTY : frame;
    }

    @Override
    public IBeeModifier getBeeModifier(ItemStack frame) {
        return MODIFIERS.getOrDefault(value(frame), MODIFIERS.get("cocoa"));
    }

    public static int maxDamage(String variant) {
        return "soul".equals(variant) ? 80 : 240;
    }

    /** Provides the 1.12 frame-stat tooltip instead of the generic variant catalog line. */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        switch (value(stack)) {
            case "cocoa" -> {
                modifier(tooltip, "lifespan", 0.75F, 0.25F);
                modifier(tooltip, "production", 1.50F, 5.00F);
            }
            case "cage" -> {
                modifier(tooltip, "territory", 0.50F, 0.10F);
                modifier(tooltip, "lifespan", 0.75F, 0.50F);
                modifier(tooltip, "production", 0.75F, 0.50F);
            }
            case "soul" -> {
                modifier(tooltip, "mutation", 1.50F, 5.00F);
                modifier(tooltip, "lifespan", 0.75F, 0.50F);
                modifier(tooltip, "production", 0.25F, 0.10F);
            }
            case "clay" -> {
                modifier(tooltip, "lifespan", 1.50F, 5.00F);
                modifier(tooltip, "mutation", 0.50F, 0.20F);
                modifier(tooltip, "production", 0.75F, 0.20F);
            }
            case "debug" -> modifier(tooltip, "lifespan", 0.00F, 0.00F);
            default -> throw new IllegalStateException("Unknown Binnie's Bees hive frame " + value(stack));
        }
        if (!stack.isDamaged()) tooltip.add(Component.translatable("tooltip.binniebees.frame.durability", stack.getMaxDamage()));
    }

    private static void modifier(List<Component> tooltip, String type, float multiplier, float limit) {
        String bound = limit >= 1.0F ? "max" : "min";
        tooltip.add(Component.translatable("tooltip.binniebees.frame." + type)
                .append(": ")
                .append(Component.translatable("tooltip.binniebees.frame.modifier." + bound, multiplier, limit)));
    }

    private record Modifier(float production, float mutation, float lifespan, float territory) implements IBeeModifier {
        @Override public float modifyProductionSpeed(IGenome genome, float current) { return current * production; }
        @Override public float modifyMutationChance(IGenome genome, IGenome mate, IMutation<IBeeSpecies> mutation, float current) { return current * this.mutation; }
        @Override public float modifyAging(IGenome genome, IGenome mate, float current) { return current / lifespan; }
        @Override public Vec3i modifyTerritory(IGenome genome, Vec3i current) {
            if (territory == 1.0F) return current;
            return new Vec3i(
                    Math.max(1, Math.round(current.getX() * territory)),
                    Math.max(1, Math.round(current.getY() * territory)),
                    Math.max(1, Math.round(current.getZ() * territory)));
        }
    }
}
