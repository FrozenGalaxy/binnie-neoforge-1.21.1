package binnie.botany.genetics;

import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.api.genetics.IFlowerSpeciesType;
import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import forestry.apiimpl.plugin.SpeciesBuilder;
import forestry.api.plugin.ISpeciesBuilder;
import net.minecraft.resources.ResourceLocation;

/** Builder used to materialize the 1.12 flower catalog as typed Forestry species. */
public final class FlowerSpeciesBuilder extends SpeciesBuilder<IFlowerSpeciesType, IFlowerSpecies, FlowerSpeciesBuilder> {
    private ResourceLocation flowerType = ResourceLocation.fromNamespaceAndPath("binniebotany", "flower");
    private SoilMoisture moisture = SoilMoisture.NORMAL;
    private SoilAcidity acidity = SoilAcidity.NEUTRAL;
    private int sections = 1;

    public FlowerSpeciesBuilder(ResourceLocation id, String genus, String species) {
        super(id, genus, species);
    }

    public FlowerSpeciesBuilder setFlowerType(ResourceLocation flowerType) {
        this.flowerType = flowerType;
        return this;
    }

    public FlowerSpeciesBuilder setMoisture(SoilMoisture moisture) {
        this.moisture = moisture;
        return this;
    }

    public FlowerSpeciesBuilder setAcidity(SoilAcidity acidity) {
        this.acidity = acidity;
        return this;
    }

    public FlowerSpeciesBuilder setSections(int sections) {
        if (sections < 1) throw new IllegalArgumentException("Flower sections must be positive");
        this.sections = sections;
        return this;
    }

    public ResourceLocation getFlowerType() {
        return this.flowerType;
    }

    public SoilMoisture getMoisture() {
        return this.moisture;
    }

    public SoilAcidity getAcidity() {
        return this.acidity;
    }

    public int getSections() {
        return this.sections;
    }

    @Override
    public ISpeciesBuilder.ISpeciesFactory<IFlowerSpeciesType, IFlowerSpecies, FlowerSpeciesBuilder> createSpeciesFactory() {
        return FlowerSpecies::new;
    }
}
