package binnie.botany.genetics;

import binnie.botany.api.genetics.IFlower;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public enum NoFlowerEffect implements FlowerEffect {
    INSTANCE;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("binniebotany", "none");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public boolean dominant() {
        return true;
    }

    @Override
    public void tick(IFlower flower, Level level, BlockPos pos) {
    // 1.12 behavior: the only shipped flower effect had no action.
    }
}
