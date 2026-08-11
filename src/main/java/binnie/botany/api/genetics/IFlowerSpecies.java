package binnie.botany.api.genetics;

import forestry.api.core.TemperatureType;
import forestry.api.core.genetics.ISpecies;
import net.minecraft.resources.ResourceLocation;

/** Public current species contract for Binnie Botany flowers. */
public interface IFlowerSpecies extends ISpecies<IFlower> {
    ResourceLocation flowerType();

    TemperatureType temperature();

    SoilMoisture moisture();

    SoilAcidity acidity();

    int sections();
}
