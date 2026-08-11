package binnie.botany.genetics;

import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.api.genetics.IFlower;
import binnie.botany.api.genetics.IFlowerSpeciesType;
import binnie.botany.block.FlowerBlockEntity;
import binnie.botany.registry.BotanyWorldContent;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.core.engine.genetics.ItemGE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Item representation for flower, pollen and seed life stages. */
public final class FlowerItem extends ItemGE {
    private final FlowerLifeStage flowerStage;

    public FlowerItem(Item.Properties properties, FlowerLifeStage stage) {
        super(properties, stage);
        this.flowerStage = stage;
    }

    @Override
    protected ISpecies<?> getSpecies(ItemStack stack) {
        return getIndividualFromComponent(stack).getSpecies();
    }

    @Override
    public ISpeciesType<?, ?> getType() {
        return FlowerSpeciesType.get();
    }

    public IFlowerSpeciesType flowerType() {
        return FlowerSpeciesType.get();
    }

    /**
      * ForestryCE's ItemGE naming helper knows only Forestry's three built-in
      * organism families. Flowers used their own display-name contract in 1.12, so retain it here
      * instead of passing a Botany species through the built-in-only helper.
      */
    @Override
    public Component getName(ItemStack stack) {
        IFlower flower = getIndividualFromComponent(stack) instanceof IFlower value ? value : null;
        if (flower == null) {
            return super.getName(stack);
        }

        Component species = flower.getSpecies().getDisplayName();
        return switch (this.flowerStage) {
            case FLOWER -> species;
            case POLLEN, SEED -> Component.translatable("item.binniebotany.grammar." + this.flowerStage.getSerializedName(), species);
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK || !(getIndividualFromComponent(stack) instanceof IFlower flower)) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos pos = hit.getBlockPos();
        if (flowerStage == FlowerLifeStage.POLLEN) {
            if (!(level.getBlockEntity(pos) instanceof FlowerBlockEntity planted) || !planted.isHarvestable()) {
                return InteractionResultHolder.pass(stack);
            }
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (planted.applyPollen(flower)) {
                if (!player.isCreative()) stack.shrink(1);
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, hand, hit));
        BlockState hitState = level.getBlockState(pos);
        if (!hitState.canBeReplaced(context)) pos = pos.relative(hit.getDirection());
        if (!level.getBlockState(pos).canBeReplaced(context)
                || !BotanyWorldContent.FLOWER_BLOCK.get().defaultBlockState().canSurvive(level, pos)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, BotanyWorldContent.FLOWER_BLOCK.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof FlowerBlockEntity planted) {
                planted.setFlower(stack, player.getUUID());
            }
            if (!player.isCreative()) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
