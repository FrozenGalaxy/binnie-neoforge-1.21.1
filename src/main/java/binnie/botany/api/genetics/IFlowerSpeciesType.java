package binnie.botany.api.genetics;

import binnie.botany.genetics.FlowerColor;
import binnie.botany.genetics.FlowerEffect;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.resources.ResourceLocation;

/** Public current replacement for the 1.12 IFlowerRoot. */
public interface IFlowerSpeciesType extends ISpeciesType<IFlowerSpecies, IFlower> {
    FlowerColor getColor(ResourceLocation id);

    FlowerColor getColorSafe(ResourceLocation id);

    FlowerEffect getEffect(ResourceLocation id);

    FlowerEffect getEffectSafe(ResourceLocation id);
}
