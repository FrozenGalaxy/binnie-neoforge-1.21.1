package binnie.botany.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import binnie.botany.block.BotanySoilBlock;

import java.util.function.Supplier;

/** A current replacement for one 1.12 metadata sub-item with a fixed placement state. */
public final class FixedGardenStateItem extends Item {
    private final Supplier<BlockState> state;

    public FixedGardenStateItem(Properties properties, Supplier<BlockState> state) {
        super(properties);
        this.state = state;
    }

    /** Provides the moisture, pH and weedkiller lines from the 1.12 soil item. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        BlockState placed = state.get();
        if (!(placed.getBlock() instanceof BotanySoilBlock)) return;

        SoilMoisture moisture = placed.getValue(BotanySoilBlock.MOISTURE);
        SoilAcidity acidity = placed.getValue(BotanySoilBlock.ACIDITY);
        Component conditions = Component.empty();
        if (moisture != SoilMoisture.NORMAL) {
            conditions = Component.translatable("tooltip.binniebotany.soil.moisture",
                    Component.translatable("tooltip.binniebotany.soil.moisture." + moisture.getSerializedName()));
        }
        if (acidity != SoilAcidity.NEUTRAL) {
            Component ph = Component.translatable("tooltip.binniebotany.soil.ph",
                    Component.translatable("tooltip.binniebotany.soil.ph." + acidity.getSerializedName()));
            conditions = conditions.getString().isEmpty() ? ph : conditions.copy().append(", ").append(ph);
        }
        if (!conditions.getString().isEmpty()) tooltip.add(conditions.copy().withStyle(ChatFormatting.GRAY));
        if (placed.getValue(BotanySoilBlock.WEEDKILLED)) {
            tooltip.add(Component.translatable("tooltip.binniebotany.soil.weedkiller").withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPlaceContext place = new BlockPlaceContext(context);
        Level level = place.getLevel();
        BlockPos pos = place.getClickedPos();
        BlockState target = state.get();
        if (!level.getBlockState(pos).canBeReplaced(place) || !target.canSurvive(level, pos)) return InteractionResult.FAIL;
        if (!level.setBlock(pos, target, 11)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            target.getBlock().setPlacedBy(level, pos, target, context.getPlayer(), context.getItemInHand());
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
        }
        var sound = target.getSoundType(level, pos, context.getPlayer()).getPlaceSound();
        level.playSound(context.getPlayer(), pos, sound, SoundSource.BLOCKS, 1.0F, 0.8F);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
