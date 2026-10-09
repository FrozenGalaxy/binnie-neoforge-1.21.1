package binnie.trees.item;

import binnie.core.item.VariantItem;
import binnie.core.liquid.AlcoholEffect;
import binnie.core.registry.BinnieDataComponents;
import binnie.trees.registry.BinnieTreesFluids;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStackSimple;

import java.util.List;
import java.util.Map;

/** The fifteen fillable ItemDrink glass shapes from Extra Trees 1.12. */
public final class GlasswareItem extends VariantItem {
    private static final Map<String, Integer> CAPACITIES = Map.ofEntries(
            Map.entry("beer_mug", 480), Map.entry("pint", 600), Map.entry("snifter", 510),
            Map.entry("flute", 180), Map.entry("cocktail", 240), Map.entry("cordial", 60),
            Map.entry("collins", 360), Map.entry("highball", 240), Map.entry("hurricane", 450),
            Map.entry("margarita", 360), Map.entry("old_fashioned", 240), Map.entry("wine", 240),
            Map.entry("shot", 30), Map.entry("sherry", 60), Map.entry("coupe", 180));

    public GlasswareItem(Properties properties, List<String> variants) {
        // The 1.12 Fabricator emitted eight vessels per cast. Modern recipe
        // codecs reject an output count above the item's stack limit, so empty
        // vessels stack to eight; filled capability stacks remain singular.
        super(properties.stacksTo(8), "binnietrees.glassware", variants);
        if (!CAPACITIES.keySet().containsAll(variants) || variants.size() != CAPACITIES.size())
            throw new IllegalArgumentException("Glassware catalog does not match the 1.12 shapes");
    }

    public int capacity(ItemStack stack) { return CAPACITIES.get(value(stack)); }
    public FluidStack fluid(ItemStack stack) { return stack.getOrDefault(BinnieDataComponents.FLUID_CONTENT, SimpleFluidContent.EMPTY).copy(); }
    public GlasswareFluidHandler createFluidHandler(ItemStack stack) { return new GlasswareFluidHandler(stack, capacity(stack)); }
    public ItemStack filled(String shape, FluidStack fluid) {
        ItemStack stack = createStack(shape);
        int requested = Math.min(capacity(stack), fluid.getAmount());
        return createFluidHandler(stack).fill(fluid, IFluidHandler.FluidAction.EXECUTE) == requested ? stack : ItemStack.EMPTY;
    }

    @Override public Component getName(ItemStack stack) {
        FluidStack fluid = fluid(stack);
        Component glass = super.getName(stack);
        return fluid.isEmpty() ? glass : Component.translatable("tooltip.binnietrees.glassware.filled", glass,
                Component.translatable(fluid.getDescriptionId()));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (fluid(stack).isEmpty()) return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return fluid(stack).isEmpty() ? UseAnim.NONE : UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return fluid(stack).isEmpty() ? 0 : 16; }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        FluidStack contents = fluid(stack);
        var beverage = BinnieTreesFluids.data(contents);
        if (beverage == null || !(entity instanceof Player player)) return stack;
        if (!level.isClientSide) {
            AlcoholEffect.makeDrunk(player, beverage.alcohol() / 100.0F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK,
                    SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }
        ItemStack empty = createStack(value(stack));
        stack.shrink(1);
        return stack.isEmpty() ? empty : stack;
    }

    public static class GlasswareFluidHandler extends FluidHandlerItemStackSimple {
        private GlasswareFluidHandler(ItemStack container, int capacity) { super(BinnieDataComponents.FLUID_CONTENT, container, capacity); }
        @Override public boolean canFillFluidType(FluidStack fluid) {
            return getContainer().getCount() == 1 && BinnieTreesFluids.data(fluid) != null;
        }
        @Override public boolean canDrainFluidType(FluidStack fluid) {
            return getContainer().getCount() == 1 && BinnieTreesFluids.data(fluid) != null;
        }
    }
}
