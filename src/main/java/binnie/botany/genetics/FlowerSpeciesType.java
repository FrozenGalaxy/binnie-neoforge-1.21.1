package binnie.botany.genetics;

import binnie.botany.api.genetics.IFlower;
import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.api.genetics.IFlowerSpeciesType;
import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import forestry.api.IForestryApi;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.core.ToleranceType;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IKaryotype;
import forestry.api.plugin.IForestryPlugin;
import forestry.api.plugin.ISpeciesTypeBuilder;
import forestry.core.engine.genetics.BreedingTracker;
import forestry.core.engine.genetics.MutationManager;
import forestry.core.engine.genetics.SpeciesType;
import forestry.core.engine.genetics.mutations.Mutation;
import forestry.core.engine.genetics.root.BreedingTrackerManager;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** ForestryCE custom species type implementing the full Binnie flower karyotype and catalog. */
public final class FlowerSpeciesType extends SpeciesType<IFlowerSpecies, IFlower> implements IFlowerSpeciesType {
    @Nullable
    private ImmutableMap<ResourceLocation, FlowerColor> colors;
    @Nullable
    private ImmutableMap<ResourceLocation, FlowerEffect> effects;

    public FlowerSpeciesType(IKaryotype karyotype, ISpeciesTypeBuilder builder) {
        super(FlowerSpeciesTypes.FLOWER, karyotype, builder);
    }

    public static IFlowerSpeciesType get() {
        return IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(FlowerSpeciesTypes.FLOWER, IFlowerSpeciesType.class);
    }

    @Override
    public FlowerColor getColor(ResourceLocation id) {
        return requireValue(this.colors, id, "flower color");
    }

    @Nullable
    @Override
    public FlowerColor getColorSafe(ResourceLocation id) {
        return valueSafe(this.colors, id);
    }

    @Override
    public FlowerEffect getEffect(ResourceLocation id) {
        return requireValue(this.effects, id, "flower effect");
    }

    @Nullable
    @Override
    public FlowerEffect getEffectSafe(ResourceLocation id) {
        return valueSafe(this.effects, id);
    }

    @Override
    public ImmutableMap<ResourceLocation, IFlowerSpecies> handleSpeciesRegistration(List<IForestryPlugin> plugins) {
        ImmutableMap.Builder<ResourceLocation, FlowerColor> colors = ImmutableMap.builderWithExpectedSize(LegacyFlowerCatalog.INSTANCE.colors().size());
        for (LegacyFlowerCatalog.ColorDef color : LegacyFlowerCatalog.INSTANCE.colors()) {
            colors.put(color.id(), new FlowerColor(color.id(), color.rgb(), color.wiltedRgb(), color.dominant()));
        }
        this.colors = colors.buildOrThrow();
        this.effects = ImmutableMap.of(NoFlowerEffect.ID, NoFlowerEffect.INSTANCE);

        LinkedHashMap<ResourceLocation, IFlowerSpecies> species = new LinkedHashMap<>();
        for (LegacyFlowerCatalog.SpeciesDef definition : LegacyFlowerCatalog.INSTANCE.species()) {
            FlowerSpeciesBuilder builder = new FlowerSpeciesBuilder(definition.id(), definition.genus(), definition.species())
                    .setDominant(definition.dominant())
                    .setAuthority(definition.authority())
                    .setTemperature(TemperatureType.valueOf(definition.temperature().toUpperCase(Locale.ROOT)))
                    .setHumidity(toHumidity(definition.moisture()))
                    .setMoisture(SoilMoisture.byName(definition.moisture()))
                    .setAcidity(SoilAcidity.byName(definition.acidity()))
                    .setFlowerType(definition.type())
                    .setSections(definition.sections())
                    .setEscritoireColor(TextColor.fromRgb(getColor(definition.allele("primary").resourceValue()).rgb()));
            builder.setGenome(genome -> setGenome(definition, genome));
            IGenome genome = builder.buildGenome(this.karyotype.createGenomeBuilder());
            IFlowerSpecies value = builder.createSpeciesFactory().create(definition.id(), this, genome, builder);
            if (species.put(definition.id(), value) != null) {
                throw new IllegalStateException("Duplicate flower species " + definition.id());
            }
        }
        return ImmutableMap.copyOf(species);
    }

    private static void setGenome(LegacyFlowerCatalog.SpeciesDef definition, forestry.api.plugin.IGenomeBuilder genome) {
        genome.set(FlowerChromosomes.SPECIES, new Allele<>(definition.id(), definition.dominant()));
        setReference(genome, FlowerChromosomes.PRIMARY_COLOR, definition.allele("primary"));
        setReference(genome, FlowerChromosomes.SECONDARY_COLOR, definition.allele("secondary"));
        setData(genome, FlowerChromosomes.FERTILITY, definition.allele("fertility").intValue(), definition.allele("fertility").dominant());
        int[] territory = definition.allele("territory").intArray();
        setData(genome, FlowerChromosomes.TERRITORY, new Vec3i(territory[0], territory[1], territory[2]), definition.allele("territory").dominant());
        setReference(genome, FlowerChromosomes.EFFECT, definition.allele("effect"));
        setData(genome, FlowerChromosomes.LIFESPAN, definition.allele("lifespan").intValue(), definition.allele("lifespan").dominant());
        setData(genome, FlowerChromosomes.TEMPERATURE_TOLERANCE,
                ToleranceType.valueOf(definition.allele("temperature_tolerance").stringValue()), definition.allele("temperature_tolerance").dominant());
        setData(genome, FlowerChromosomes.HUMIDITY_TOLERANCE,
                ToleranceType.valueOf(definition.allele("humidity_tolerance").stringValue()), definition.allele("humidity_tolerance").dominant());
        setData(genome, FlowerChromosomes.PH_TOLERANCE,
                ToleranceType.valueOf(definition.allele("ph_tolerance").stringValue()), definition.allele("ph_tolerance").dominant());
        setData(genome, FlowerChromosomes.SAPPINESS, definition.allele("sappiness").floatValue(), definition.allele("sappiness").dominant());
        setReference(genome, FlowerChromosomes.STEM, definition.allele("stem"));
        genome.setRemainingDefault();
    }

    private static void setReference(forestry.api.plugin.IGenomeBuilder genome,
                                     forestry.api.core.genetics.alleles.IChromosome<ResourceLocation> chromosome,
                                     LegacyFlowerCatalog.AlleleDef allele) {
        genome.set(chromosome, new Allele<>(allele.resourceValue(), allele.dominant()));
    }

    private static <V> void setData(forestry.api.plugin.IGenomeBuilder genome,
                                    forestry.api.core.genetics.alleles.IChromosome<V> chromosome, V value, boolean dominant) {
        genome.set(chromosome, new Allele<>(value, dominant));
    }

    private static HumidityType toHumidity(String moisture) {
        return switch (SoilMoisture.byName(moisture)) {
            case DRY -> HumidityType.ARID;
            case NORMAL -> HumidityType.NORMAL;
            case DAMP -> HumidityType.DAMP;
        };
    }

    @Override
    public void onSpeciesRegistered(ImmutableMap<ResourceLocation, IFlowerSpecies> allSpecies) {
        super.onSpeciesRegistered(allSpecies);
        ImmutableList.Builder<forestry.api.core.genetics.IMutation<IFlowerSpecies>> mutations =
                ImmutableList.builderWithExpectedSize(LegacyFlowerCatalog.INSTANCE.mutations().size());
        for (LegacyFlowerCatalog.MutationDef definition : LegacyFlowerCatalog.INSTANCE.mutations()) {
            IFlowerSpecies first = getSpecies(definition.first());
            IFlowerSpecies second = getSpecies(definition.second());
            IFlowerSpecies result = getSpecies(definition.result());
            mutations.add(new Mutation<>(this, first, second, result, Map.of(), definition.chance() / 100.0F, List.of()));
        }
        setMutations(new MutationManager<>(mutations.build()));
    }

    @Override
    public boolean isMember(IIndividual individual) {
        return individual instanceof IFlower;
    }

    @Override
    public IBreedingTracker getBreedingTracker(LevelAccessor level, @Nullable GameProfile profile) {
        return BreedingTrackerManager.INSTANCE.getTracker(this, level, profile);
    }

    @Override
    public String getBreedingTrackerFile(@Nullable GameProfile profile) {
        return "BotanistTracker." + (profile == null ? "common" : profile.getId());
    }

    @Override
    public IBreedingTracker createBreedingTracker() {
        return new FlowerBreedingTracker();
    }

    @Override
    public IBreedingTracker createBreedingTracker(CompoundTag nbt) {
        FlowerBreedingTracker tracker = new FlowerBreedingTracker();
        tracker.load(nbt);
        return tracker;
    }

    @Override
    public void initializeBreedingTracker(IBreedingTracker tracker, @Nullable Level world, @Nullable GameProfile profile) {
        if (tracker instanceof BreedingTracker breedingTracker) {
            breedingTracker.setLevel(world);
            breedingTracker.setUsername(profile);
        }
    }

    @Override
    public Codec<? extends IFlower> getIndividualCodec() {
        return FlowerIndividual.CODEC;
    }

    @Override
    public float getResearchSuitability(IFlowerSpecies species, ItemStack stack) {
        return stack.getItem() instanceof FlowerItem ? 1.0F : super.getResearchSuitability(species, stack);
    }

    @Override
    public List<ItemStack> getResearchBounty(IFlowerSpecies species, Level level, GameProfile researcher, IFlower individual, int bountyLevel) {
        List<ItemStack> bounty = super.getResearchBounty(species, level, researcher, individual, bountyLevel);
        if (level.random.nextFloat() < bountyLevel / 16.0F) {
            bounty.add(individual.createStack(binnie.botany.api.genetics.FlowerLifeStage.POLLEN));
        }
        return bounty;
    }
}
