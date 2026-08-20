package binnie.core.ponder;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A non-spreading, renderer-backed source-fluid cube used only in Ponder scenes. */
public final class PonderFluidSourceBlock extends BaseEntityBlock {
    private static final MapCodec<PonderFluidSourceBlock> CODEC = simpleCodec(PonderFluidSourceBlock::new);

    public PonderFluidSourceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PonderFluidPipeBlockEntity(pos, state);
    }
}
