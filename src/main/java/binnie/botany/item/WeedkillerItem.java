package binnie.botany.item;

import binnie.botany.block.BotanySoilBlock;
import binnie.botany.block.GardenPlantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class WeedkillerItem extends Item {
    public WeedkillerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        BlockPos plantPos = null;
        if (state.getBlock() instanceof GardenPlantBlock) {
            plantPos = pos;
            pos = pos.below();
            state = level.getBlockState(pos);
        } else if (level.getBlockState(pos.above()).getBlock() instanceof GardenPlantBlock) {
            plantPos = pos.above();
        }
        if (!(state.getBlock() instanceof BotanySoilBlock) || (state.getValue(BotanySoilBlock.WEEDKILLED) && plantPos == null)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(BotanySoilBlock.WEEDKILLED, true), BotanySoilBlock.UPDATE_ALL);
            if (plantPos != null) level.destroyBlock(plantPos, false);
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
