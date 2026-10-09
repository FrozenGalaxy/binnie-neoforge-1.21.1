package binnie.genetics.api;

import binnie.genetics.data.AlleleValue;
import binnie.genetics.data.GeneticAlleleData;
import binnie.genetics.data.GeneticSerumData;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.IChromosome;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Type-safe sample extraction, serum application, and inheritance calculations. */
public class GeneticOperations {
    private GeneticOperations() {}

    public static GeneticAlleleData sample(ItemStack stack, ResourceLocation chromosomeId, GeneticAlleleData.Origin origin) {
        GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(stack);
        IIndividual individual = adapter.read(stack);
        IChromosome<?> chromosome = requireChromosome(adapter, chromosomeId);
        Allele<?> allele = origin == GeneticAlleleData.Origin.ACTIVE
                ? individual.getGenome().getActiveAllele(chromosome)
                : individual.getGenome().getInactiveAllele(chromosome);
        return new GeneticAlleleData(adapter.id(), chromosome.id(), AlleleValue.fromJava(allele.value()), allele.dominant(), origin);
    }

    public static ItemStack applySerum(ItemStack stack, GeneticSerumData serum) {
        if (serum.charges() <= 0) throw new IllegalArgumentException("Serum has no remaining charges");
        GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(stack);
        if (!adapter.id().equals(serum.alleles().getFirst().speciesType())) {
            throw new IllegalArgumentException("Serum type does not match target organism");
        }
        IGenome genome = adapter.read(stack).getGenome();
        IdentityHashMap<IChromosome<?>, AllelePair<?>> replacements = new IdentityHashMap<>();
        for (GeneticAlleleData data : serum.alleles()) {
            IChromosome<?> chromosome = requireChromosome(adapter, data.chromosome());
            Allele<?> allele = data.toAllele(chromosome);
            replacements.put(chromosome, bothUnchecked(allele));
        }
        return adapter.replaceGenome(stack, genome.copyWithPairs(replacements));
    }

    public static ItemStack replaceAllele(ItemStack stack, GeneticAlleleData data, boolean replaceActive, boolean replaceInactive) {
        if (!replaceActive && !replaceInactive) return stack.copy();
        GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(stack);
        if (!adapter.id().equals(data.speciesType())) throw new IllegalArgumentException("Cross-species allele application rejected");
        IIndividual individual = adapter.read(stack);
        IChromosome<?> chromosome = requireChromosome(adapter, data.chromosome());
        AllelePair<?> old = individual.getGenome().getAllelePair(chromosome);
        Allele<?> replacement = data.toAllele(chromosome);
        AllelePair<?> pair = pairUnchecked(replaceActive ? replacement : old.active(), replaceInactive ? replacement : old.inactive());
        return adapter.replaceGenome(stack, individual.getGenome().copyWithPairs(Map.of(chromosome, pair)));
    }

    public static List<PunnettOutcome> punnett(ItemStack first, ItemStack second, ResourceLocation chromosomeId) {
        GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(first);
        if (!adapter.supports(second)) throw new IllegalArgumentException("Punnett parents have different organism types");
        IChromosome<?> chromosome = requireChromosome(adapter, chromosomeId);
        AllelePair<?> a = adapter.read(first).getGenome().getAllelePair(chromosome);
        AllelePair<?> b = adapter.read(second).getGenome().getAllelePair(chromosome);
        List<Allele<?>> left = List.of(a.active(), a.inactive());
        List<Allele<?>> right = List.of(b.active(), b.inactive());
        Map<String, OutcomeAccumulator> merged = new LinkedHashMap<>();
        for (Allele<?> x : left) {
            for (Allele<?> y : right) {
                AllelePair<?> pair = pairUnchecked(x, y);
                String key = pair.active().value() + "|" + pair.active().dominant() + "|" + pair.inactive().value() + "|" + pair.inactive().dominant();
                merged.computeIfAbsent(key, ignored -> new OutcomeAccumulator(pair)).count++;
            }
        }
        ArrayList<PunnettOutcome> outcomes = new ArrayList<>();
        for (OutcomeAccumulator value : merged.values()) outcomes.add(new PunnettOutcome(value.pair, value.count / 4.0));
        return List.copyOf(outcomes);
    }

    private static IChromosome<?> requireChromosome(GeneticOrganismAdapter adapter, ResourceLocation chromosomeId) {
        IChromosome<?> chromosome = adapter.speciesType().getKaryotype().getChromosome(chromosomeId);
        if (chromosome == null) throw new IllegalArgumentException("Unknown chromosome " + chromosomeId + " for " + adapter.id());
        return chromosome;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static AllelePair<?> bothUnchecked(Allele<?> allele) {
        return AllelePair.both((Allele) allele);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static AllelePair<?> pairUnchecked(Allele<?> first, Allele<?> second) {
        return AllelePair.create((Allele) first, (Allele) second);
    }

    public record PunnettOutcome(AllelePair<?> pair, double probability) {}
    private static final class OutcomeAccumulator {
        final AllelePair<?> pair;
        int count;
        OutcomeAccumulator(AllelePair<?> pair) { this.pair = pair; }
    }
}
