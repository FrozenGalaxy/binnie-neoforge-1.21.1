package binnie.core.liquid;

import binnie.core.registry.BinnieDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStackSimple;

/** Full-bucket-only component-backed handler, matching the 1.12 container semantics. */
public final class BinnieFluidContainerHandler extends FluidHandlerItemStackSimple {
    private final BinnieFluidContainerType type;

    public BinnieFluidContainerHandler(ItemStack container, BinnieFluidContainerType type) {
        super(BinnieDataComponents.FLUID_CONTENT, container, FluidType.BUCKET_VOLUME);
        this.type = type;
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        return type.accepts(fluid);
    }

    @Override
    public boolean canDrainFluidType(FluidStack fluid) {
        return type.accepts(fluid);
    }
}
