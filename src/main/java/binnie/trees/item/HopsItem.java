package binnie.trees.item;

import binnie.trees.block.HopsBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Seed and crop product for the two-block hops plant. */
public final class HopsItem extends Item {
    private final DeferredBlock<HopsBlock> crop;

    public HopsItem(Properties properties, DeferredBlock<HopsBlock> crop) {
        super(properties);
        this.crop = crop;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos soilPos = context.getClickedPos();
        BlockPos lowerPos = soilPos.above();
        BlockPos upperPos = lowerPos.above();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (context.getClickedFace() != Direction.UP
                || !(level.getBlockState(soilPos).getBlock() instanceof FarmBlock)
                || !level.getBlockState(lowerPos).isAir()
                || !level.getBlockState(upperPos).isAir()
                || player != null && (!player.mayUseItemAt(lowerPos, Direction.UP, stack)
                    || !player.mayUseItemAt(upperPos, Direction.UP, stack))) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            crop.get().placeAt(level, lowerPos, 0, 3);
            SoundType sound = SoundType.CROP;
            level.playSound(null, lowerPos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            stack.consume(1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
