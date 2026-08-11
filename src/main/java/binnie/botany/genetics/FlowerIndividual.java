package binnie.botany.genetics;

import binnie.botany.api.genetics.IFlower;
import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.api.genetics.IFlowerSpeciesType;
import binnie.core.registry.BinnieDataComponents;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.IForestryApi;
import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.core.engine.genetics.IndividualLiving;
import forestry.core.engine.genetics.mutations.Mutation;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Full typed flower individual, including 1.12 aging, mating and color mixing. */
public final class FlowerIndividual extends IndividualLiving<IFlowerSpecies, IFlower, IFlowerSpeciesType> implements IFlower {
    public static final Codec<FlowerIndividual> CODEC = RecordCodecBuilder.create(instance -> {
        Codec<IGenome> genomeCodec = FlowerSpeciesType.get().getKaryotype().getGenomeCodec();
        return IndividualLiving.livingFields(instance, genomeCodec).and(
                FlowerState.CODEC.optionalFieldOf("flower_state", FlowerState.DEFAULT)
                        .forGetter(flower -> new FlowerState(flower.generation, flower.age, flower.wilted, flower.flowered))
        ).apply(instance, FlowerIndividual::new);
    });

    private int generation;
    private int age;
    private boolean wilted;
    private boolean flowered;

    public FlowerIndividual(IGenome genome) {
        this(genome, Optional.empty(), false, genome.getActiveValue(FlowerChromosomes.LIFESPAN),
                genome.getActiveValue(FlowerChromosomes.LIFESPAN), FlowerState.DEFAULT);
    }

    private FlowerIndividual(IGenome genome, Optional<IGenome> mate, boolean analyzed, int health, int maxHealth,
                             FlowerState state) {
        super(genome, mate, analyzed, health, maxHealth);
        this.generation = Math.max(0, state.generation());
        this.age = Mth.clamp(state.age(), 0, 15);
        this.wilted = state.wilted();
        this.flowered = state.flowered();
    }

    @Override
    protected IChromosome<Integer> getLifespanChromosome() {
        return FlowerChromosomes.LIFESPAN;
    }

    @Override
    public int getGeneration() {
        return this.generation;
    }

    @Override
    public int getAge() {
        return this.age;
    }

    @Override
    public void setAge(int age) {
        this.age = Mth.clamp(age, 0, 15);
    }

    @Override
    public boolean hasFlowered() {
        return this.flowered;
    }

    @Override
    public void setFlowered(boolean flowered) {
        this.flowered = flowered;
    }

    @Override
    public boolean isWilted() {
        return this.wilted;
    }

    @Override
    public void setWilted(boolean wilted) {
        this.wilted = wilted;
    }

    @Override
    public void age(Level level, float ageStep) {
        super.age(level, ageStep);
        if (ageStep > 0.0F) {
            int wholeSteps = Math.max(1, Mth.floor(ageStep));
            setAge(this.age + wholeSteps);
        }
        if (!isAlive() || getHealth() <= Math.max(1, getMaxHealth() / 4)) {
            this.wilted = true;
        }
    }

    @Override
    public List<IFlower> createOffspring(Level level, BlockPos pos, IFlower other, int requestedCount) {
        IGenome mateGenome = other == null ? this.mate : other.getGenome();
        int fertility = Math.max(1, this.genome.getActiveValue(FlowerChromosomes.FERTILITY));
        int count = Mth.clamp(requestedCount, 1, fertility);
        ArrayList<IFlower> offspring = new ArrayList<>(count);
        if (mateGenome == null) {
            for (int i = 0; i < count; i++) {
                FlowerIndividual clone = new FlowerIndividual(this.genome);
                clone.generation = this.generation;
                offspring.add(clone);
            }
            return List.copyOf(offspring);
        }

        for (int i = 0; i < count; i++) {
            SpeciesUtil.ISpeciesMutator mutator = (first, second) -> mutateParent(level, pos, first, second);
            FlowerIndividual child = SpeciesUtil.createOffspring(level.random, this.genome, mateGenome, mutator, FlowerIndividual::new);
            int mateGeneration = other == null ? this.generation : other.getGeneration();
            child.generation = Math.max(this.generation, mateGeneration) + 1;
            child.age = 0;
            child.wilted = false;
            child.flowered = false;
            offspring.add(child);
        }
        return List.copyOf(offspring);
    }

    @Nullable
    private static ImmutableList<AllelePair<?>> mutateParent(Level level, BlockPos pos, IGenome parentOne, IGenome parentTwo) {
        IGenome firstGenome;
        IGenome secondGenome;
        IFlowerSpecies firstSpecies;
        IFlowerSpecies secondSpecies;
        if (level.random.nextBoolean()) {
            firstGenome = parentOne;
            secondGenome = parentTwo;
            firstSpecies = parentOne.resolveActive(FlowerChromosomes.SPECIES);
            secondSpecies = parentTwo.resolveInactive(FlowerChromosomes.SPECIES);
        } else {
            firstGenome = parentTwo;
            secondGenome = parentOne;
            firstSpecies = parentTwo.resolveActive(FlowerChromosomes.SPECIES);
            secondSpecies = parentOne.resolveInactive(FlowerChromosomes.SPECIES);
        }

        ArrayList<AllelePair<?>> changed = new ArrayList<>(firstGenome.getAllelePairs());
        boolean didColorMutation = false;
        didColorMutation |= mutateColor(level, firstGenome, secondGenome, FlowerChromosomes.PRIMARY_COLOR, changed);
        didColorMutation |= mutateColor(level, firstGenome, secondGenome, FlowerChromosomes.SECONDARY_COLOR, changed);
        didColorMutation |= mutateColor(level, firstGenome, secondGenome, FlowerChromosomes.STEM, changed);

        IClimateProvider climate = IForestryApi.INSTANCE.getClimateManager().createClimateProvider(level, pos);
        IFlowerSpeciesType flowerType = (IFlowerSpeciesType) firstSpecies.getType();
        for (IMutation<IFlowerSpecies> mutation : flowerType.getMutations().getCombinationsShuffled(firstSpecies, secondSpecies, level.random)) {
            float chance = Mutation.getChance(mutation, level, pos, firstGenome, secondGenome, climate);
            if (chance > 0.0F && level.random.nextFloat() < chance) {
                return mutation.getResultAlleles();
            }
        }
        return didColorMutation ? ImmutableList.copyOf(changed) : null;
    }

    private static boolean mutateColor(Level level, IGenome firstGenome, IGenome secondGenome,
                                       IChromosome<ResourceLocation> chromosome, List<AllelePair<?>> result) {
        ResourceLocation first = firstGenome.getActiveValue(chromosome);
        ResourceLocation second = secondGenome.getActiveValue(chromosome);
        if (first.equals(second)) return false;
        LegacyFlowerCatalog.ColorMixDef mix = LegacyFlowerCatalog.INSTANCE.colorMix(first, second);
        if (mix == null || level.random.nextFloat() * 100.0F >= mix.chance()) return false;
        int index = firstGenome.getKaryotype().getChromosomes().indexOf(chromosome);
        FlowerColor color = FlowerSpeciesType.get().getColor(mix.result());
        result.set(index, AllelePair.both(new Allele<>(color.id(), color.dominant())));
        return true;
    }

    @Override
    protected void copyPropertiesTo(IFlower other) {
        super.copyPropertiesTo(other);
        FlowerIndividual flower = (FlowerIndividual) other;
        flower.generation = this.generation;
        flower.age = this.age;
        flower.wilted = this.wilted;
        flower.flowered = this.flowered;
    }

    @Override
    protected void savePropertiesToStack(ItemStack stack) {
        super.savePropertiesToStack(stack);
        stack.set(BinnieDataComponents.FLOWER_GENERATION, this.generation);
        stack.set(BinnieDataComponents.FLOWER_AGE, this.age);
        stack.set(BinnieDataComponents.FLOWER_WILTED, this.wilted);
        stack.set(BinnieDataComponents.FLOWER_FLOWERED, this.flowered);
        if (stack.is(binnie.botany.registry.BotanyContent.FLOWER.get())) {
            String type = getSpecies().flowerType().getPath();
            int index = binnie.botany.client.FlowerItemModels.index(type);
            if (index >= 0) stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(index + 1));
        }
    }

    @Override
    public void loadPropertiesFromStack(ItemStack stack) {
        super.loadPropertiesFromStack(stack);
        this.generation = Math.max(0, stack.getOrDefault(BinnieDataComponents.FLOWER_GENERATION, 0));
        this.age = Mth.clamp(stack.getOrDefault(BinnieDataComponents.FLOWER_AGE, 0), 0, 15);
        this.wilted = stack.getOrDefault(BinnieDataComponents.FLOWER_WILTED, false);
        this.flowered = stack.has(BinnieDataComponents.FLOWER_FLOWERED)
                ? stack.getOrDefault(BinnieDataComponents.FLOWER_FLOWERED, false)
                : this.age > 1;
    }
    private record FlowerState(int generation, int age, boolean wilted, boolean flowered) {
        private static final FlowerState DEFAULT = new FlowerState(0, 0, false, false);
        private static final Codec<FlowerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("generation", 0).forGetter(FlowerState::generation),
                Codec.INT.optionalFieldOf("age", 0).forGetter(FlowerState::age),
                Codec.BOOL.optionalFieldOf("wilted", false).forGetter(FlowerState::wilted),
                Codec.BOOL.optionalFieldOf("flowered", false).forGetter(FlowerState::flowered)
        ).apply(instance, FlowerState::new));
    }

}
