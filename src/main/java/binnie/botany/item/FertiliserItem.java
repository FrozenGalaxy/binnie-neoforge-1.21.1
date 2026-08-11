package binnie.botany.item;

import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.block.BotanySoilBlock;
import binnie.botany.gardening.BotanyGardening;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class FertiliserItem extends Item {
    public enum Kind { NUTRIENT, ACID, ALKALINE }

    private final Kind kind;

    public FertiliserItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BotanySoilBlock)) return InteractionResult.PASS;

        BlockState changed = switch (kind) {
            case NUTRIENT -> {
                BotanySoilBlock soil = (BotanySoilBlock) state.getBlock();
                if (soil.quality().next() != soil.quality()) yield BotanyGardening.changeQuality(state, soil.quality().next());
                yield state.setValue(BotanySoilBlock.FERTILITY,
                        Math.min(3, state.getValue(BotanySoilBlock.FERTILITY) + 1));
            }
            case ACID -> state.setValue(BotanySoilBlock.ACIDITY, shift(state.getValue(BotanySoilBlock.ACIDITY), -1));
            case ALKALINE -> state.setValue(BotanySoilBlock.ACIDITY, shift(state.getValue(BotanySoilBlock.ACIDITY), 1));
        };
        if (changed == state) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlock(pos, changed, BotanySoilBlock.UPDATE_ALL);
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static SoilAcidity shift(SoilAcidity acidity, int delta) {
        SoilAcidity[] values = SoilAcidity.values();
        return values[Math.max(0, Math.min(values.length - 1, acidity.ordinal() + delta))];
    }
}
