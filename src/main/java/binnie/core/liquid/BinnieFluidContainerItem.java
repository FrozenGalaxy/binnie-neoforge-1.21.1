package binnie.core.liquid;

import binnie.core.data.BeverageData;
import binnie.core.registry.BinnieDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

/** Reusable Glass/Cylinder container with a full one-bucket transfer contract. */
public final class BinnieFluidContainerItem extends Item {
    private final BinnieFluidContainerType type;

    public BinnieFluidContainerItem(Properties properties, BinnieFluidContainerType type) {
        super(properties.stacksTo(16));
        this.type = type;
    }

    public BinnieFluidContainerType containerType() {
        return type;
    }

    public BinnieFluidContainerHandler createFluidHandler(ItemStack stack) {
        return new BinnieFluidContainerHandler(stack, type);
    }

    public FluidStack fluid(ItemStack stack) {
        return stack.getOrDefault(BinnieDataComponents.FLUID_CONTENT, SimpleFluidContent.EMPTY).copy();
    }

    public ItemStack filled(FluidStack fluid) {
        ItemStack stack = new ItemStack(this);
        int accepted = createFluidHandler(stack).fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        return accepted == FluidType.BUCKET_VOLUME ? stack : ItemStack.EMPTY;
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = fluid(stack);
        Component vessel = Component.translatable("item.binniecore." + type.id());
        Component contents = fluid.isEmpty()
                ? Component.translatable("tooltip.binniecore.container.empty")
                : fluidName(fluid);
        return Component.empty().append(vessel).append(" (").append(contents).append(")");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = fluid(stack);
        if (fluid.isEmpty()) return;
        tooltip.add(Component.translatable("tooltip.binniecore.container.amount", fluid.getAmount(), FluidType.BUCKET_VOLUME)
                .withStyle(ChatFormatting.GRAY));
        BeverageData beverage = beverage(fluid);
        if (beverage != null) {
            tooltip.add(Component.translatable("tooltip.binniecore.container.alcohol", beverage.alcohol())
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isDrinkable(stack) || !player.canEat(false)) return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isDrinkable(stack) ? UseAnim.DRINK : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return isDrinkable(stack) ? 32 : 0;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        FluidStack fluid = fluid(stack);
        BeverageData beverage = beverage(fluid);
        if (beverage == null || !(entity instanceof Player player)) return stack;

        if (!level.isClientSide) {
            float alcoholUnits = beverage.alcohol() / 100.0F * fluid.getAmount();
            AlcoholEffect.makeDrunk(player, alcoholUnits);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP,
                    SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }

        stack.shrink(1);
        ItemStack empty = new ItemStack(this);
        if (stack.isEmpty()) return empty;
        if (!level.isClientSide && !player.getInventory().add(empty)) player.drop(empty, false);
        return stack;
    }

    private boolean isDrinkable(ItemStack stack) {
        return beverage(fluid(stack)) != null;
    }

    private static Component fluidName(FluidStack fluid) {
        BeverageData beverage = beverage(fluid);
        if (beverage != null) {
            ResourceLocation id = ResourceLocation.tryParse(beverage.beverage());
            if (id != null && "binnietrees".equals(id.getNamespace())) {
                String[] parts = id.getPath().split("/", 2);
                if (parts.length == 2) return Component.translatable("item.binnietrees." + parts[0] + "." + parts[1]);
            }
        }
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        return Component.translatable(fluid.getDescriptionId()).withStyle(id.getNamespace().equals("minecraft")
                ? ChatFormatting.WHITE : ChatFormatting.AQUA);
    }

    private static BeverageData beverage(FluidStack fluid) {
        BeverageData component = fluid.get(BinnieDataComponents.BEVERAGE);
        return component != null ? component : binnie.trees.registry.BinnieTreesFluids.data(fluid);
    }
}
