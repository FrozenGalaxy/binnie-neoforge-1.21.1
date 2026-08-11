package binnie.botany.item;

import binnie.botany.block.BotanySoilBlock;
import binnie.botany.gardening.BotanyGardening;
import binnie.botany.block.SoilQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 1.12 gardening trowel: converts dirt-like ground into climate-derived Botany soil. */
public final class TrowelItem extends Item {
    public TrowelItem(Properties properties, int durability) {
        super(properties.stacksTo(1).durability(durability));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() == net.minecraft.core.Direction.DOWN) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT))) return InteractionResult.PASS;
        if (!level.getBlockState(pos.above()).canBeReplaced()) return InteractionResult.PASS;

        level.playSound(context.getPlayer(), pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!level.isClientSide) {
            level.setBlock(pos, BotanyGardening.createSoilState(level, pos, SoilQuality.SOIL), BotanySoilBlock.UPDATE_ALL);
            if (context.getPlayer() != null) {
                context.getItemInHand().hurtAndBreak(1, context.getPlayer(),
                        context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
