package binnie.botany.genetics;

import forestry.core.engine.genetics.BreedingTracker;

/** Persistent and network-synchronized flower discovery tracker. */
public final class FlowerBreedingTracker extends BreedingTracker {
    public FlowerBreedingTracker() {
        super(FlowerSpeciesTypes.FLOWER);
    }

    @Override
    public void registerPickup(forestry.api.core.genetics.ISpecies<?> species) {
        registerSpecies(species);
    }
}
