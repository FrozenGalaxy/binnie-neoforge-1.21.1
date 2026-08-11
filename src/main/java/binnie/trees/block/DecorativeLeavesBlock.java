package binnie.trees.block;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/** Placeable 1.12-style decorative leaves; no saplings or fruit drops. */
public final class DecorativeLeavesBlock extends LeavesBlock {
    private final String speciesId;
    private final int color;
    public DecorativeLeavesBlock(Properties properties, String speciesId, int color) {
        super(properties);
        this.speciesId = speciesId;
        this.color = color;
    }
    public String speciesId() { return speciesId; }
    public int color() { return color; }
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.4D, 1.0D, 0.4D));
        super.entityInside(state, level, pos, entity);
    }
}
