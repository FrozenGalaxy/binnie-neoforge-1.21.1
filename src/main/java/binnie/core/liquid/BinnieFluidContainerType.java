package binnie.core.liquid;

import binnie.core.registry.BinnieDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** The two reusable one-bucket containers owned by the 1.12 Core module. */
public enum BinnieFluidContainerType {
    GLASS("glass"),
    CYLINDER("cylinder");

    private final String id;

    BinnieFluidContainerType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /**
      * Mirrors the 1.12 FluidType placement rules: Binnie's Trees liquids fit in
      * either vessel, while Genetics process liquids require the cylinder except
      * for growth medium.
      */
    public boolean accepts(FluidStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        return accepts(id, binnie.trees.registry.BinnieTreesFluids.data(stack) != null);
    }

    public boolean accepts(ResourceLocation fluidId, boolean beverage) {
        if ("binnietrees".equals(fluidId.getNamespace())) return true;
        if (!"binniegenetics".equals(fluidId.getNamespace())) return false;
        return this == CYLINDER || "growth_medium".equals(fluidId.getPath());
    }

    /** 1.12 creative-tab visibility instead of every technically valid combination. */
    public boolean showInCreative(FluidStack stack) {
        if (!accepts(stack)) return false;
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        return showInCreative(id, binnie.trees.registry.BinnieTreesFluids.data(stack) != null);
    }

    public boolean showInCreative(ResourceLocation fluidId, boolean beverage) {
        if (!accepts(fluidId, beverage)) return false;
        if (this == GLASS) return beverage;
        return "binniegenetics".equals(fluidId.getNamespace());
    }
}
