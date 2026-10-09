package binnie.bees.block;

import forestry.apiculture.hives.HiveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Marker entity used by Forestry's spectacles renderer for Binnie's wild hives. */
public final class BinnieBeeHiveBlockEntity extends HiveBlockEntity {
    public BinnieBeeHiveBlockEntity(BlockEntityType<? extends HiveBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
