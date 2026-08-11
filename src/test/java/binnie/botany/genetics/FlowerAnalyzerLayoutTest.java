package binnie.botany.genetics;

import forestry.api.core.genetics.alleles.IChromosome;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ensures the current ForestryCE analyzer bridge retains every 1.12 flower chromosome. */
final class FlowerAnalyzerLayoutTest {
    @Test
    void analyzerPagesCoverTheTwelveChromosomeFlowerGenomeExactlyOnce() {
        Set<IChromosome<?>> chromosomes = new HashSet<>(FlowerAnalyzerLayout.firstPage());
        assertEquals(6, chromosomes.size());
        assertTrue(chromosomes.addAll(FlowerAnalyzerLayout.secondPage()), "Analyzer pages must not repeat a chromosome");
        assertEquals(12, chromosomes.size());
        assertTrue(chromosomes.contains(FlowerChromosomes.SPECIES));
        assertTrue(chromosomes.contains(FlowerChromosomes.PH_TOLERANCE));
        assertTrue(chromosomes.contains(FlowerChromosomes.STEM));
    }
}
