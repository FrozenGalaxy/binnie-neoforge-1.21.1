package binnie.genetics.jei;

import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.genetics.FlowerSpeciesType;
import binnie.botany.registry.BotanyContent;
import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.BinnieGenetics;
import binnie.genetics.api.GeneticOperations;
import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.genetics.data.GeneticAlleleData;
import binnie.genetics.data.GeneticSequenceData;
import binnie.genetics.data.GeneticSerumData;
import binnie.genetics.data.LaboratoryChargeData;
import binnie.genetics.machine.GeneticsMachineProcesses;
import binnie.genetics.registry.GeneticsContent;
import binnie.genetics.registry.GeneticsFluids;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.platform.util.SpeciesUtil;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 1.12-compatible representative operations; component-bearing examples replace a huge Cartesian recipe dump. */
public final class GeneticsJeiRecipes {
    public static final List<String> MACHINES = List.of(
            "isolator", "sequencer", "polymeriser", "inoculator", "splicer",
            "analyser", "genepool", "incubator", "acclimatiser");
    public static final int EXPECTED_EXAMPLE_COUNT = 29;

    private GeneticsJeiRecipes() {}

    public static Map<String, List<GeneticsJeiRecipe>> build() {
        LinkedHashMap<String, List<GeneticsJeiRecipe>> result = new LinkedHashMap<>();
        for (String machine : MACHINES) result.put(machine, new ArrayList<>());

        List<IFlowerSpecies> species = FlowerSpeciesType.get().getAllSpecies();
        if (species.isEmpty()) return freeze(result);
        ItemStack recipient = species.getFirst().createIndividual().createStack(FlowerLifeStage.FLOWER);
        ItemStack manipulableRecipient = species.getFirst().createIndividual().createStack(FlowerLifeStage.POLLEN);
        ItemStack donor = species.get(Math.min(1, species.size() - 1)).createIndividual().createStack(FlowerLifeStage.FLOWER);
        GeneticAlleleData active = GeneticOperations.sample(donor, FlowerChromosomes.PRIMARY_COLOR.id(), GeneticAlleleData.Origin.ACTIVE);
        GeneticAlleleData inactive = GeneticOperations.sample(donor, FlowerChromosomes.STEM.id(), GeneticAlleleData.Origin.INACTIVE);
        ItemStack activeUnsequenced = GeneticsContent.GENETIC_MATERIAL.get().sequence(GeneticSequenceData.unsequenced(active, 100));
        ItemStack inactiveUnsequenced = GeneticsContent.GENETIC_MATERIAL.get().sequence(GeneticSequenceData.unsequenced(inactive, 85));
        GeneticSerumData unchargedSerumData = new GeneticSerumData(List.of(active), 100, 0);
        GeneticSerumData unchargedArrayData = new GeneticSerumData(List.of(active, inactive), 95, 0);
        GeneticSerumData serumData = new GeneticSerumData(List.of(active), 100, 16);
        GeneticSerumData arrayData = new GeneticSerumData(List.of(active, inactive), 95, 16);
        GeneticSerumData oncePolymerisedSerumData = new GeneticSerumData(List.of(active), 100, 1);
        GeneticSerumData oncePolymerisedArrayData = new GeneticSerumData(List.of(active, inactive), 95, 1);
        ItemStack unchargedSerum = GeneticsContent.GENETIC_MATERIAL.get().serum(unchargedSerumData, false);
        ItemStack unchargedArray = GeneticsContent.GENETIC_MATERIAL.get().serum(unchargedArrayData, true);
        ItemStack serum = GeneticsContent.GENETIC_MATERIAL.get().serum(serumData, false);
        ItemStack array = GeneticsContent.GENETIC_MATERIAL.get().serum(arrayData, true);
        ItemStack oncePolymerisedSerum = GeneticsContent.GENETIC_MATERIAL.get().serum(oncePolymerisedSerumData, false);
        ItemStack oncePolymerisedArray = GeneticsContent.GENETIC_MATERIAL.get().serum(oncePolymerisedArrayData, true);
        ItemStack inoculated = GeneticOperations.applySerum(manipulableRecipient, serumData);
        ItemStack arrayInoculated = GeneticOperations.applySerum(manipulableRecipient, arrayData);
        ItemStack spliced = GeneticOperations.applySerum(manipulableRecipient, arrayData);
        ItemStack analysed = GeneticOrganismAdapters.forStack(recipient).analyze(recipient);
        ItemStack analysedSequence = activeUnsequenced.copy();
        analysedSequence.set(BinnieDataComponents.GENETIC_SEQUENCE,
                activeUnsequenced.get(BinnieDataComponents.GENETIC_SEQUENCE).analyzedCopy());
        List<ItemStack> allIndividuals = individualAlternatives(false);
        List<ItemStack> manipulableIndividuals = individualAlternatives(true);

        add(result, recipe("isolator", "chromosome", List.of(donor), List.of(new ItemStack(GeneticsContent.EMPTY_SEQUENCER.get())),
                List.of(new ItemStack(GeneticsContent.ENZYME.get())), List.of(activeUnsequenced),
                List.of(ForestryFluids.BIO_ETHANOL.getFluid(10)), List.of(),
                GeneticsMachineProcesses.ISOLATOR_OPERATION.baseDuration(), GeneticsMachineProcesses.ISOLATOR_OPERATION.energyPerTick())
                .withInputAlternatives(allIndividuals)
                .withOutputAlternatives(List.of(activeUnsequenced, inactiveUnsequenced)));

        add(result, recipe("sequencer", "active", List.of(activeUnsequenced), List.of(), List.of(new ItemStack(GeneticsContent.FLUORESCENT_DYE.get())),
                List.of(new ItemStack(GeneticsContent.EMPTY_SEQUENCER.get())), List.of(), List.of(),
                GeneticsMachineProcesses.SEQUENCER_OPERATION.baseDuration(), GeneticsMachineProcesses.SEQUENCER_OPERATION.energyPerTick()));
        add(result, recipe("sequencer", "inactive", List.of(inactiveUnsequenced), List.of(), List.of(new ItemStack(GeneticsContent.FLUORESCENT_DYE.get())),
                List.of(new ItemStack(GeneticsContent.EMPTY_SEQUENCER.get())), List.of(), List.of(),
                GeneticsMachineProcesses.SEQUENCER_OPERATION.baseDuration(), GeneticsMachineProcesses.SEQUENCER_OPERATION.energyPerTick()));

        add(result, recipe("polymeriser", "serum", List.of(unchargedSerum), List.of(),
                List.of(), List.of(oncePolymerisedSerum), List.of(GeneticsFluids.POLYMERISING_BACTERIA.stack(10), GeneticsFluids.RAW_DNA.stack(50)), List.of(),
                GeneticsMachineProcesses.POLYMERISER_OPERATION.baseDuration(), GeneticsMachineProcesses.POLYMERISER_OPERATION.energyPerTick()));
        ItemStack chargedGold = new ItemStack(Items.GOLD_NUGGET);
        chargedGold.set(BinnieDataComponents.LABORATORY_CHARGE, LaboratoryChargeData.full());
        add(result, recipe("polymeriser", "array_accelerated", List.of(unchargedArray), List.of(chargedGold),
                List.of(), List.of(oncePolymerisedArray), List.of(GeneticsFluids.POLYMERISING_BACTERIA.stack(20), GeneticsFluids.RAW_DNA.stack(100)), List.of(),
                GeneticsMachineProcesses.POLYMERISER_ACCELERATED_OPERATION.baseDuration() * 2,
                GeneticsMachineProcesses.POLYMERISER_ACCELERATED_OPERATION.energyPerTick()));

        add(result, recipe("inoculator", "flower_pollen", List.of(manipulableRecipient), List.of(serum), List.of(), List.of(inoculated),
                List.of(GeneticsFluids.VECTOR_BACTERIA.stack(15)), List.of(),
                GeneticsMachineProcesses.INOCULATOR_OPERATION.baseDuration(), GeneticsMachineProcesses.INOCULATOR_OPERATION.energyPerTick())
                .withInputAlternatives(manipulableIndividuals).withOutputAlternatives(manipulableIndividuals));
        add(result, recipe("inoculator", "array_pollen", List.of(manipulableRecipient), List.of(array),
                List.of(), List.of(arrayInoculated), List.of(GeneticsFluids.VECTOR_BACTERIA.stack(15)), List.of(),
                GeneticsMachineProcesses.INOCULATOR_OPERATION.baseDuration() * 2, GeneticsMachineProcesses.INOCULATOR_OPERATION.energyPerTick())
                .withInputAlternatives(manipulableIndividuals).withOutputAlternatives(manipulableIndividuals));

        add(result, recipe("splicer", "array", List.of(manipulableRecipient), List.of(array), List.of(), List.of(spliced), List.of(), List.of(),
                GeneticsMachineProcesses.SPLICER_OPERATION.baseDuration() * 3 / 2, GeneticsMachineProcesses.SPLICER_OPERATION.energyPerTick())
                .withInputAlternatives(manipulableIndividuals).withOutputAlternatives(manipulableIndividuals));
        add(result, recipe("analyser", "flower", List.of(recipient), List.of(), List.of(new ItemStack(GeneticsContent.DNA_DYE.get())),
                List.of(analysed), List.of(), List.of(),
                GeneticsMachineProcesses.ANALYSER_OPERATION.baseDuration(), GeneticsMachineProcesses.ANALYSER_OPERATION.energyPerTick())
                .withInputAlternatives(allIndividuals));
        add(result, recipe("analyser", "sequence", List.of(activeUnsequenced), List.of(),
                List.of(new ItemStack(GeneticsContent.DNA_DYE.get())), List.of(analysedSequence), List.of(), List.of(),
                GeneticsMachineProcesses.ANALYSER_OPERATION.baseDuration(), GeneticsMachineProcesses.ANALYSER_OPERATION.energyPerTick()));
        add(result, recipe("genepool", "flower", List.of(recipient), List.of(), List.of(new ItemStack(GeneticsContent.ENZYME.get())),
                List.of(), List.of(ForestryFluids.BIO_ETHANOL.getFluid(12)), List.of(GeneticsFluids.RAW_DNA.stack(10)),
                GeneticsMachineProcesses.GENEPOOL_OPERATION.baseDuration(), GeneticsMachineProcesses.GENEPOOL_OPERATION.energyPerTick())
                .withInputAlternatives(allIndividuals));

        addIncubator(result, "growth_medium", new ItemStack(GeneticsContent.GROWTH_MEDIUM.get()), new FluidStack(Fluids.WATER, 25), GeneticsFluids.GROWTH_MEDIUM.stack(25));
        addIncubator(result, "bacteria", new ItemStack(Items.WHEAT), GeneticsFluids.GROWTH_MEDIUM.stack(25), GeneticsFluids.BACTERIA.stack(5));
        addIncubator(result, "bacteria_growth", new ItemStack(GeneticsContent.GROWTH_MEDIUM.get()), GeneticsFluids.BACTERIA.stack(1), GeneticsFluids.BACTERIA.stack(5));
        addIncubator(result, "enzyme", new ItemStack(Items.SUGAR), GeneticsFluids.BACTERIA.stack(2), FluidStack.EMPTY,
                new ItemStack(GeneticsContent.ENZYME.get()));
        addIncubator(result, "polymerising_growth", new ItemStack(GeneticsContent.GROWTH_MEDIUM.get()), GeneticsFluids.POLYMERISING_BACTERIA.stack(1), GeneticsFluids.POLYMERISING_BACTERIA.stack(5));
        addIncubator(result, "vector_growth", new ItemStack(GeneticsContent.GROWTH_MEDIUM.get()), GeneticsFluids.VECTOR_BACTERIA.stack(1), GeneticsFluids.VECTOR_BACTERIA.stack(5));
    // 1.12 used dye metadata 15: current white dye, not bone meal.
        addIncubator(result, "polymerising", new ItemStack(Items.WHITE_DYE), GeneticsFluids.BACTERIA.stack(10), GeneticsFluids.POLYMERISING_BACTERIA.stack(10));
        addIncubator(result, "vector", new ItemStack(Items.BLAZE_POWDER), GeneticsFluids.BACTERIA.stack(10), GeneticsFluids.VECTOR_BACTERIA.stack(10));
        ItemStack larvae = SpeciesUtil.BEE_TYPE.get().getDefaultSpecies().createIndividual().createStack(BeeLifeStage.LARVAE);
        ItemStack drone = SpeciesUtil.BEE_TYPE.get().getDefaultSpecies().createIndividual().createStack(BeeLifeStage.DRONE);
        addIncubator(result, "bee_larvae", larvae, GeneticsFluids.GROWTH_MEDIUM.stack(50), FluidStack.EMPTY, drone);

        add(result, recipe("acclimatiser", "ph_acid", List.of(recipient), List.of(), List.of(new ItemStack(BotanyContent.POWDER_MULCH.get())), List.of(recipient), List.of(), List.of(),
                GeneticsMachineProcesses.INDEFINITE_OPERATION.baseDuration(), GeneticsMachineProcesses.INDEFINITE_OPERATION.energyPerTick())
                .withInputAlternatives(allIndividuals));
        add(result, recipe("acclimatiser", "ph_alkaline", List.of(recipient), List.of(), List.of(new ItemStack(BotanyContent.POWDER_ASH.get())), List.of(recipient), List.of(), List.of(),
                GeneticsMachineProcesses.INDEFINITE_OPERATION.baseDuration(), GeneticsMachineProcesses.INDEFINITE_OPERATION.energyPerTick()));
        addAcclimatiser(result, "temperature_blaze_powder", recipient, Items.BLAZE_POWDER);
        addAcclimatiser(result, "temperature_blaze_rod", recipient, Items.BLAZE_ROD);
        addAcclimatiser(result, "temperature_lava", recipient, Items.LAVA_BUCKET);
        addAcclimatiser(result, "temperature_snow", recipient, Items.SNOWBALL);
        addAcclimatiser(result, "temperature_ice", recipient, Items.ICE);
        addAcclimatiser(result, "humidity_water", recipient, Items.WATER_BUCKET);
        addAcclimatiser(result, "humidity_sand", recipient, Items.SAND);
        result.get("acclimatiser").replaceAll(recipe -> recipe.withInputAlternatives(allIndividuals)
                .withOutputAlternatives(allIndividuals));

        Map<String, List<GeneticsJeiRecipe>> frozen = freeze(result);
        int count = frozen.values().stream().mapToInt(List::size).sum();
        if (count != EXPECTED_EXAMPLE_COUNT) throw new IllegalStateException("Expected " + EXPECTED_EXAMPLE_COUNT + " JEI examples, got " + count);
        return frozen;
    }

    private static void addIncubator(Map<String, List<GeneticsJeiRecipe>> result, String name, ItemStack reagent, FluidStack input, FluidStack output) {
        addIncubator(result, name, reagent, input, output, ItemStack.EMPTY);
    }

    private static void addIncubator(Map<String, List<GeneticsJeiRecipe>> result, String name, ItemStack reagent,
                                     FluidStack input, FluidStack output, ItemStack itemOutput) {
        add(result, recipe("incubator", name, List.of(), List.of(), List.of(reagent),
                itemOutput.isEmpty() ? List.of() : List.of(itemOutput),
                input.isEmpty() ? List.of() : List.of(input), output.isEmpty() ? List.of() : List.of(output),
                GeneticsMachineProcesses.INDEFINITE_OPERATION.baseDuration(), GeneticsMachineProcesses.INDEFINITE_OPERATION.energyPerTick()));
    }

    private static void addAcclimatiser(Map<String, List<GeneticsJeiRecipe>> result, String name,
                                        ItemStack recipient, net.minecraft.world.item.Item reagent) {
        add(result, recipe("acclimatiser", name, List.of(recipient), List.of(), List.of(new ItemStack(reagent)),
                List.of(recipient), List.of(), List.of(),
                GeneticsMachineProcesses.INDEFINITE_OPERATION.baseDuration(), GeneticsMachineProcesses.INDEFINITE_OPERATION.energyPerTick()));
    }

    private static GeneticsJeiRecipe recipe(String machine, String name, List<ItemStack> inputs, List<ItemStack> catalysts,
                                             List<ItemStack> reagents, List<ItemStack> outputs, List<FluidStack> fluidsIn,
                                             List<FluidStack> fluidsOut, int duration, int energy) {
        return new GeneticsJeiRecipe(ResourceLocation.fromNamespaceAndPath(BinnieGenetics.MOD_ID, "jei/" + machine + "/" + name),
                machine, inputs, catalysts, reagents, outputs, fluidsIn, fluidsOut, duration, energy,
                operationTooltip(machine, name), List.of(), List.of());
    }

    /**
      * Tags cannot encode Forestry genome and life-stage components. Supplying
      * real stacks makes JEI cycle exactly the executable individual family.
      */
    @SuppressWarnings("rawtypes")
    private static List<ItemStack> individualAlternatives(boolean manipulableOnly) {
        ArrayList<ItemStack> result = new ArrayList<>();
        for (var adapter : GeneticOrganismAdapters.all()) {
            ISpeciesType type = adapter.speciesType();
            for (Object speciesObject : type.getAllSpecies()) {
                ISpecies species = (ISpecies) speciesObject;
                for (Object stageObject : type.getLifeStages()) {
                    ItemStack stack = species.createStack((forestry.api.core.genetics.ILifeStage) stageObject);
                    if (!stack.isEmpty() && (!manipulableOnly || adapter.isDnaManipulable(stack))) result.add(stack);
                }
            }
        }
        return List.copyOf(result);
    }

    private static Component operationTooltip(String machine, String name) {
        if (machine.equals("incubator")) {
            return Component.translatable("jei.binniegenetics.incubator.contract." + name);
        }
        return Component.translatable("jei.binniegenetics." + machine + ".contract");
    }

    private static void add(Map<String, List<GeneticsJeiRecipe>> result, GeneticsJeiRecipe recipe) {
        result.get(recipe.machine()).add(recipe);
    }

    private static Map<String, List<GeneticsJeiRecipe>> freeze(Map<String, List<GeneticsJeiRecipe>> input) {
        LinkedHashMap<String, List<GeneticsJeiRecipe>> result = new LinkedHashMap<>();
        input.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return Map.copyOf(result);
    }
}
