package binnie.botany.genetics;

import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.api.genetics.IFlowerSpeciesType;
import com.mojang.serialization.Codec;
import forestry.api.IForestryApi;
import forestry.api.core.ToleranceType;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.alleles.Chromosome;
import forestry.api.core.genetics.alleles.ChromosomeFactory;
import forestry.api.core.genetics.GeneticTranslationKeys;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;

public final class FlowerChromosomes {
    private static final Codec<ToleranceType> TOLERANCE_CODEC = Codec.STRING.xmap(ToleranceType::valueOf, Enum::name);

    public static final IChromosome<ResourceLocation> SPECIES = new Chromosome<>(
            FlowerSpeciesTypes.FLOWER,
            ResourceLocation.CODEC,
            id -> GeneticTranslationKeys.createTranslationKey("allele", FlowerSpeciesTypes.FLOWER, id),
            new IChromosome.IReferenceResolver<IFlowerSpecies>() {
                @Override
                public IFlowerSpecies get(ResourceLocation id) {
                    IFlowerSpeciesType type = type();
                    IFlowerSpecies species = type.getSpeciesSafe(id);
                    return species != null ? species : type.getDefaultSpecies();
                }

                @Override
                public boolean isDominant(ResourceLocation id) {
                    return LegacyFlowerCatalog.INSTANCE.species(id).dominant();
                }
            });

    public static final IChromosome<ResourceLocation> PRIMARY_COLOR = colorChromosome("primary_color");
    public static final IChromosome<ResourceLocation> SECONDARY_COLOR = colorChromosome("secondary_color");
    public static final IChromosome<Integer> FERTILITY = ChromosomeFactory.intChromosome(id("fertility"));
    public static final IChromosome<Vec3i> TERRITORY = ChromosomeFactory.valueChromosome(id("territory"), Vec3i.CODEC,
            value -> value.getX() + "_" + value.getY() + "_" + value.getZ());
    public static final IChromosome<ResourceLocation> EFFECT = new Chromosome<>(
            id("effect"), ResourceLocation.CODEC,
            value -> GeneticTranslationKeys.createTranslationKey("allele", id("effect"), value),
            new IChromosome.IReferenceResolver<FlowerEffect>() {
                @Override
                public FlowerEffect get(ResourceLocation id) {
                    return type().getEffect(id);
                }

                @Override
                public boolean isDominant(ResourceLocation id) {
                    return type().getEffect(id).dominant();
                }
            });
    public static final IChromosome<Integer> LIFESPAN = ChromosomeFactory.intChromosome(id("lifespan"));
    public static final IChromosome<ToleranceType> TEMPERATURE_TOLERANCE = tolerance("temperature_tolerance");
    public static final IChromosome<ToleranceType> HUMIDITY_TOLERANCE = tolerance("humidity_tolerance");
    public static final IChromosome<ToleranceType> PH_TOLERANCE = tolerance("ph_tolerance");
    public static final IChromosome<Float> SAPPINESS = ChromosomeFactory.floatChromosome(id("sappiness"));
    public static final IChromosome<ResourceLocation> STEM = colorChromosome("stem");

    private FlowerChromosomes() {
    }

    private static IChromosome<ResourceLocation> colorChromosome(String name) {
        ResourceLocation chromosomeId = id(name);
        return new Chromosome<>(chromosomeId, ResourceLocation.CODEC,
                value -> GeneticTranslationKeys.createTranslationKey("allele", chromosomeId, value),
                new IChromosome.IReferenceResolver<FlowerColor>() {
                    @Override
                    public FlowerColor get(ResourceLocation id) {
                        return type().getColor(id);
                    }

                    @Override
                    public boolean isDominant(ResourceLocation id) {
                        return type().getColor(id).dominant();
                    }
                });
    }

    private static IChromosome<ToleranceType> tolerance(String name) {
        return ChromosomeFactory.valueChromosome(id(name), TOLERANCE_CODEC, ToleranceType::name);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("binniebotany", path);
    }

    private static IFlowerSpeciesType type() {
        return IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(FlowerSpeciesTypes.FLOWER, IFlowerSpeciesType.class);
    }
}
