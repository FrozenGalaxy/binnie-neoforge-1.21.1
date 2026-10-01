package binnie.genetics.tracking;

import binnie.botany.genetics.FlowerChromosomes;
import binnie.core.forestry.ForestryModules;
import binnie.botany.genetics.FlowerSpeciesTypes;
import binnie.botany.genetics.LegacyFlowerCatalog;
import binnie.genetics.api.GeneticOrganismAdapter;
import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.genetics.data.AlleleValue;
import binnie.genetics.data.GeneticKnowledgeData;
import binnie.genetics.data.GeneticKnowledgeEntry;
import binnie.genetics.data.GeneticSpeciesDiscovery;
import forestry.api.core.ToleranceType;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.core.genetics.alleles.ButterflyChromosomes;
import forestry.api.core.genetics.alleles.ForestryAlleles;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/** Builds full 1.12-style catalogs on top of ForestryCE's inline-allele model. */
public final class GeneticKnowledgeCatalog {
    private GeneticKnowledgeCatalog() {}

    /** Every registered species and every active/inactive allele represented by species defaults. */
    public static GeneticKnowledgeData registeredDefaults() {
        TreeSet<GeneticKnowledgeEntry> genes = new TreeSet<>();
        TreeSet<GeneticSpeciesDiscovery> speciesDiscoveries = new TreeSet<>();
        for (GeneticOrganismAdapter adapter : GeneticOrganismAdapters.all()) {
            for (ISpecies<?> species : adapter.speciesType().getAllSpecies()) {
                speciesDiscoveries.add(new GeneticSpeciesDiscovery(adapter.id(), species.id()));
                addGenome(genes, adapter, species.getDefaultGenome());
            }
        }
        return new GeneticKnowledgeData(genes.stream().toList(), speciesDiscoveries.stream().toList());
    }

    /**
      * The Master Gene Database starts with every species-template allele and
      * then added each breeding system's full canonical allele sets, regardless of player sequencing progress.
      */
    public static GeneticKnowledgeData masterGeneDatabase() {
        LinkedHashMap<GeneValueKey, GeneticKnowledgeEntry> genes = new LinkedHashMap<>();
        TreeSet<GeneticSpeciesDiscovery> speciesDiscoveries = new TreeSet<>();

    // Keep original order/semantics: template values are inserted first; extra allele sets only fill gaps.
        for (GeneticOrganismAdapter adapter : GeneticOrganismAdapters.all()) {
            for (ISpecies<?> species : adapter.speciesType().getAllSpecies()) {
                speciesDiscoveries.add(new GeneticSpeciesDiscovery(adapter.id(), species.id()));
                addGenome(genes, adapter, species.getDefaultGenome());
            }
        }

        addBeeExtras(genes);
        if (ForestryModules.butterfliesLoaded()) addButterflyExtras(genes);
        addTreeExtras(genes);
        addFlowerExtras(genes);
        return new GeneticKnowledgeData(genes.values().stream().toList(), speciesDiscoveries.stream().toList());
    }

    private static void addBeeExtras(Map<GeneValueKey, GeneticKnowledgeEntry> genes) {
        ResourceLocation type = ForestrySpeciesTypes.BEE;
        addAll(genes, type, BeeChromosomes.FERTILITY,
                ForestryAlleles.FERTILITY_1, ForestryAlleles.FERTILITY_2,
                ForestryAlleles.FERTILITY_3, ForestryAlleles.FERTILITY_4);
        addAll(genes, type, BeeChromosomes.POLLINATION,
                ForestryAlleles.POLLINATION_SLOWEST, ForestryAlleles.POLLINATION_SLOWER,
                ForestryAlleles.POLLINATION_SLOW, ForestryAlleles.POLLINATION_AVERAGE,
                ForestryAlleles.POLLINATION_FAST, ForestryAlleles.POLLINATION_FASTER,
                ForestryAlleles.POLLINATION_FASTEST, ForestryAlleles.POLLINATION_MAXIMUM);
        addTolerances(genes, type, BeeChromosomes.TEMPERATURE_TOLERANCE);
        addTolerances(genes, type, BeeChromosomes.HUMIDITY_TOLERANCE);
        addLifespans(genes, type, BeeChromosomes.LIFESPAN);
        addSpeeds(genes, type, BeeChromosomes.SPEED);
        addTerritories(genes, type, BeeChromosomes.TERRITORY);
        addBooleans(genes, type, BeeChromosomes.CAVE_DWELLING);
        addBooleans(genes, type, BeeChromosomes.TOLERATES_RAIN);
    }

    private static void addButterflyExtras(Map<GeneValueKey, GeneticKnowledgeEntry> genes) {
        ResourceLocation type = ForestrySpeciesTypes.BUTTERFLY;
        for (int value = 1; value <= 10; value++) {
            addValue(genes, type, ButterflyChromosomes.FERTILITY, value, value <= 2);
            addValue(genes, type, ButterflyChromosomes.METABOLISM, value, false);
        }
        addLifespans(genes, type, ButterflyChromosomes.LIFESPAN);
        addBooleans(genes, type, ButterflyChromosomes.NEVER_SLEEPS);
        addBooleans(genes, type, ButterflyChromosomes.TOLERATES_RAIN);
        addBooleans(genes, type, ButterflyChromosomes.FIREPROOF);
        addAll(genes, type, ButterflyChromosomes.SIZE,
                ForestryAlleles.SIZE_SMALLEST, ForestryAlleles.SIZE_SMALLER, ForestryAlleles.SIZE_SMALL,
                ForestryAlleles.SIZE_AVERAGE, ForestryAlleles.SIZE_LARGE, ForestryAlleles.SIZE_LARGER,
                ForestryAlleles.SIZE_LARGEST);
        addSpeeds(genes, type, ButterflyChromosomes.SPEED);
        addTolerances(genes, type, ButterflyChromosomes.TEMPERATURE_TOLERANCE);
        addTolerances(genes, type, ButterflyChromosomes.HUMIDITY_TOLERANCE);
    }

    private static void addTreeExtras(Map<GeneValueKey, GeneticKnowledgeEntry> genes) {
        ResourceLocation type = ForestrySpeciesTypes.TREE;
        addAll(genes, type, TreeChromosomes.SAPLINGS,
                ForestryAlleles.SAPLINGS_LOWEST, ForestryAlleles.SAPLINGS_LOWER, ForestryAlleles.SAPLINGS_LOW,
                ForestryAlleles.SAPLINGS_AVERAGE, ForestryAlleles.SAPLINGS_HIGH, ForestryAlleles.SAPLINGS_HIGHER,
                ForestryAlleles.SAPLINGS_HIGHEST);
        for (int value = 1; value <= 10; value++) addValue(genes, type, TreeChromosomes.GIRTH, value, false);
        addAll(genes, type, TreeChromosomes.HEIGHT,
                ForestryAlleles.HEIGHT_SMALLEST, ForestryAlleles.HEIGHT_SMALLER, ForestryAlleles.HEIGHT_SMALL,
                ForestryAlleles.HEIGHT_AVERAGE, ForestryAlleles.HEIGHT_LARGE, ForestryAlleles.HEIGHT_LARGER,
                ForestryAlleles.HEIGHT_LARGEST, ForestryAlleles.HEIGHT_GIGANTIC);
        addAll(genes, type, TreeChromosomes.MATURATION,
                ForestryAlleles.MATURATION_SLOWEST, ForestryAlleles.MATURATION_SLOWER,
                ForestryAlleles.MATURATION_SLOW, ForestryAlleles.MATURATION_AVERAGE,
                ForestryAlleles.MATURATION_FAST, ForestryAlleles.MATURATION_FASTER,
                ForestryAlleles.MATURATION_FASTEST);
        addSappiness(genes, type, TreeChromosomes.SAPPINESS);
        addAll(genes, type, TreeChromosomes.YIELD,
                ForestryAlleles.YIELD_LOWEST, ForestryAlleles.YIELD_LOWER, ForestryAlleles.YIELD_LOW,
                ForestryAlleles.YIELD_AVERAGE, ForestryAlleles.YIELD_HIGH, ForestryAlleles.YIELD_HIGHER,
                ForestryAlleles.YIELD_HIGHEST);
        addBooleans(genes, type, TreeChromosomes.FIREPROOF);
    }

    private static void addFlowerExtras(Map<GeneValueKey, GeneticKnowledgeEntry> genes) {
        ResourceLocation type = FlowerSpeciesTypes.FLOWER;
        addAll(genes, type, FlowerChromosomes.FERTILITY,
                ForestryAlleles.FERTILITY_1, ForestryAlleles.FERTILITY_2,
                ForestryAlleles.FERTILITY_3, ForestryAlleles.FERTILITY_4);
        addLifespans(genes, type, FlowerChromosomes.LIFESPAN);
        addTolerances(genes, type, FlowerChromosomes.TEMPERATURE_TOLERANCE);
        addTolerances(genes, type, FlowerChromosomes.HUMIDITY_TOLERANCE);
        addTolerances(genes, type, FlowerChromosomes.PH_TOLERANCE);
        for (LegacyFlowerCatalog.ColorDef color : LegacyFlowerCatalog.INSTANCE.colors()) {
            addValue(genes, type, FlowerChromosomes.PRIMARY_COLOR, color.id(), color.dominant());
            addValue(genes, type, FlowerChromosomes.SECONDARY_COLOR, color.id(), color.dominant());
            addValue(genes, type, FlowerChromosomes.STEM, color.id(), color.dominant());
        }
        addSappiness(genes, type, FlowerChromosomes.SAPPINESS);
        addTerritories(genes, type, FlowerChromosomes.TERRITORY);
    }

    private static void addLifespans(Map<GeneValueKey, GeneticKnowledgeEntry> genes, ResourceLocation type,
                                     IChromosome<Integer> chromosome) {
        addAll(genes, type, chromosome,
                ForestryAlleles.LIFESPAN_SHORTEST, ForestryAlleles.LIFESPAN_SHORTER,
                ForestryAlleles.LIFESPAN_SHORT, ForestryAlleles.LIFESPAN_SHORTENED,
                ForestryAlleles.LIFESPAN_NORMAL, ForestryAlleles.LIFESPAN_ELONGATED,
                ForestryAlleles.LIFESPAN_LONG, ForestryAlleles.LIFESPAN_LONGER,
                ForestryAlleles.LIFESPAN_LONGEST);
    }

    private static void addSpeeds(Map<GeneValueKey, GeneticKnowledgeEntry> genes, ResourceLocation type,
                                  IChromosome<Float> chromosome) {
        addAll(genes, type, chromosome,
                ForestryAlleles.SPEED_SLOWEST, ForestryAlleles.SPEED_SLOWER, ForestryAlleles.SPEED_SLOW,
                ForestryAlleles.SPEED_NORMAL, ForestryAlleles.SPEED_FAST, ForestryAlleles.SPEED_FASTER,
                ForestryAlleles.SPEED_FASTEST);
    }

    private static void addTerritories(Map<GeneValueKey, GeneticKnowledgeEntry> genes, ResourceLocation type,
                                       IChromosome<net.minecraft.core.Vec3i> chromosome) {
        addAll(genes, type, chromosome,
                ForestryAlleles.TERRITORY_AVERAGE, ForestryAlleles.TERRITORY_LARGE,
                ForestryAlleles.TERRITORY_LARGER, ForestryAlleles.TERRITORY_LARGEST);
    }

    private static void addSappiness(Map<GeneValueKey, GeneticKnowledgeEntry> genes, ResourceLocation type,
                                     IChromosome<Float> chromosome) {
        addAll(genes, type, chromosome,
                ForestryAlleles.SAPPINESS_LOWEST, ForestryAlleles.SAPPINESS_LOWER, ForestryAlleles.SAPPINESS_LOW,
                ForestryAlleles.SAPPINESS_AVERAGE, ForestryAlleles.SAPPINESS_HIGH, ForestryAlleles.SAPPINESS_HIGHER,
                ForestryAlleles.SAPPINESS_HIGHEST);
    }

    private static void addTolerances(Map<GeneValueKey, GeneticKnowledgeEntry> genes, ResourceLocation type,
                                      IChromosome<ToleranceType> chromosome) {
        for (ToleranceType tolerance : ToleranceType.values()) {
            boolean dominant = tolerance == ToleranceType.BOTH_1 || tolerance == ToleranceType.UP_1 || tolerance == ToleranceType.DOWN_1;
            addValue(genes, type, chromosome, tolerance, dominant);
        }
    }

    private static void addBooleans(Map<GeneValueKey, GeneticKnowledgeEntry> genes, ResourceLocation type,
                                    IChromosome<Boolean> chromosome) {
        addAllele(genes, type, chromosome, ForestryAlleles.TRUE);
        addAllele(genes, type, chromosome, ForestryAlleles.FALSE);
    }

    @SafeVarargs
    private static <V> void addAll(Map<GeneValueKey, GeneticKnowledgeEntry> genes, ResourceLocation type,
                                   IChromosome<V> chromosome, Allele<V>... alleles) {
        for (Allele<V> allele : alleles) addAllele(genes, type, chromosome, allele);
    }

    private static <V> void addAllele(Map<GeneValueKey, GeneticKnowledgeEntry> genes, ResourceLocation type,
                                      IChromosome<V> chromosome, Allele<V> allele) {
        addValue(genes, type, chromosome, allele.value(), allele.dominant());
    }

    private static void addValue(Map<GeneValueKey, GeneticKnowledgeEntry> genes, ResourceLocation type,
                                 IChromosome<?> chromosome, Object value, boolean dominant) {
        AlleleValue encoded = AlleleValue.fromJava(value);
        GeneValueKey key = new GeneValueKey(type, chromosome.id(), encoded);
        genes.putIfAbsent(key, new GeneticKnowledgeEntry(type, chromosome.id(), encoded, dominant));
    }

    private static void addGenome(TreeSet<GeneticKnowledgeEntry> genes, GeneticOrganismAdapter adapter, IGenome genome) {
        for (IChromosome<?> chromosome : adapter.chromosomes()) {
            AllelePair<?> pair = genome.getAllelePair(chromosome);
            genes.add(new GeneticKnowledgeEntry(adapter.id(), chromosome.id(),
                    AlleleValue.fromJava(pair.active().value()), pair.active().dominant()));
            genes.add(new GeneticKnowledgeEntry(adapter.id(), chromosome.id(),
                    AlleleValue.fromJava(pair.inactive().value()), pair.inactive().dominant()));
        }
    }

    private static void addGenome(Map<GeneValueKey, GeneticKnowledgeEntry> genes, GeneticOrganismAdapter adapter, IGenome genome) {
        for (IChromosome<?> chromosome : adapter.chromosomes()) {
            AllelePair<?> pair = genome.getAllelePair(chromosome);
            addValue(genes, adapter.id(), chromosome, pair.active().value(), pair.active().dominant());
            addValue(genes, adapter.id(), chromosome, pair.inactive().value(), pair.inactive().dominant());
        }
    }

    private record GeneValueKey(ResourceLocation speciesType, ResourceLocation chromosome, AlleleValue value) {}
}
