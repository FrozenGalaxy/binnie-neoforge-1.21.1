package binnie.botany.genetics;

import forestry.api.core.genetics.alleles.IChromosome;

import java.util.List;

/**
  * 1.12 Flower analyzer pagination. The two chromosome pages cover the
  * entire twelve-chromosome flower genome exactly once; the client renderer consumes this layout.
  */
public final class FlowerAnalyzerLayout {
    private static final List<IChromosome<?>> FIRST_PAGE = List.of(
            FlowerChromosomes.SPECIES,
            FlowerChromosomes.SAPPINESS,
            FlowerChromosomes.TERRITORY,
            FlowerChromosomes.FERTILITY,
            FlowerChromosomes.EFFECT,
            FlowerChromosomes.LIFESPAN);
    private static final List<IChromosome<?>> SECOND_PAGE = List.of(
            FlowerChromosomes.PRIMARY_COLOR,
            FlowerChromosomes.SECONDARY_COLOR,
            FlowerChromosomes.STEM,
            FlowerChromosomes.TEMPERATURE_TOLERANCE,
            FlowerChromosomes.HUMIDITY_TOLERANCE,
            FlowerChromosomes.PH_TOLERANCE);

    private FlowerAnalyzerLayout() {
    }

    public static List<IChromosome<?>> firstPage() {
        return FIRST_PAGE;
    }

    public static List<IChromosome<?>> secondPage() {
        return SECOND_PAGE;
    }
}
