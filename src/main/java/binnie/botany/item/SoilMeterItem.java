package binnie.botany.item;

import binnie.botany.block.BotanySoilBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Reads Botany soil type, moisture, acidity, fertility and weedkiller state without changing it. */
public final class SoilMeterItem extends Item {
    public SoilMeterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BotanySoilBlock) && level.getBlockState(pos.below()).getBlock() instanceof BotanySoilBlock) {
            pos = pos.below();
            state = level.getBlockState(pos);
        }
        if (!(state.getBlock() instanceof BotanySoilBlock soil)) return InteractionResult.PASS;
        if (!level.isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.translatable("message.binniebotany.soil_meter",
                    Component.translatable("block.binniebotany." + soil.quality().getSerializedName()),
                    Component.translatable("binniebotany.moisture." + state.getValue(BotanySoilBlock.MOISTURE).getSerializedName()),
                    Component.translatable("binniebotany.acidity." + state.getValue(BotanySoilBlock.ACIDITY).getSerializedName()),
                    state.getValue(BotanySoilBlock.FERTILITY),
                    Component.translatable(state.getValue(BotanySoilBlock.WEEDKILLED) ? "gui.yes" : "gui.no"))
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
