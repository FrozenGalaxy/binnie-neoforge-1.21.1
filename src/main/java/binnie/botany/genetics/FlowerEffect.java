package binnie.botany.genetics;

import binnie.botany.api.genetics.IFlower;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Server-safe flower effect. Botany 1.12 only shipped the no-op allele. */
public interface FlowerEffect {
    ResourceLocation id();

    boolean dominant();

    void tick(IFlower flower, Level level, BlockPos pos);
}
