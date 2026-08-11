package binnie.genetics.machine;

import binnie.botany.registry.BotanyItemTags;
import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineProcessError;
import binnie.core.machine.MachineProcessHandler;
import binnie.core.machine.MachineProcessPlan;
import binnie.core.machine.MachineProcessRegistry;
import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.BinnieGenetics;
import binnie.genetics.api.GeneticOperations;
import binnie.genetics.api.GeneticOrganismAdapter;
import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.genetics.data.AlleleValue;
import binnie.genetics.data.GeneticAlleleData;
import binnie.genetics.data.GeneticSequenceData;
import binnie.genetics.data.GeneticSerumData;
import binnie.genetics.data.GeneticKnowledgeEntry;
import binnie.genetics.data.LaboratoryChargeData;
import binnie.genetics.tracking.GeneticKnowledgeSavedData;
import binnie.genetics.item.GeneticMaterialItem;
import binnie.genetics.registry.GeneticsContent;
import binnie.genetics.registry.GeneticsFluids;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.core.ToleranceType;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.engine.genetics.ItemGE;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

/** Dynamic, component-sensitive implementations of the Genetics laboratory pipeline. */
public final class GeneticsMachineProcesses {
    public static final OperationTiming ISOLATOR_OPERATION = new OperationTiming(4_800, 40);
    public static final OperationTiming SEQUENCER_OPERATION = new OperationTiming(19_200, 20);
    public static final OperationTiming POLYMERISER_OPERATION = new OperationTiming(2_400, 40);
    public static final OperationTiming POLYMERISER_ACCELERATED_OPERATION = new OperationTiming(480, 40);
    public static final OperationTiming INOCULATOR_OPERATION = new OperationTiming(12_000, 5);
    public static final OperationTiming SPLICER_OPERATION = new OperationTiming(1_200, 10_000);
    public static final OperationTiming ANALYSER_OPERATION = new OperationTiming(300, 30);
    public static final OperationTiming GENEPOOL_OPERATION = new OperationTiming(400, 20);
    public static final OperationTiming INDEFINITE_OPERATION = new OperationTiming(1, 2);
    private static final FluidStack EMPTY_FLUID = FluidStack.EMPTY;
    private static final int ISOLATOR_ENZYME_COST = 5_000;
    private static final int SEQUENCER_DYE_COST = 4_000;
    private static final int POLYMERISER_GOLD_COST = 4_000;
    private static final int ANALYSER_DYE_COST = 6_000;
    private static final int GENEPOOL_ENZYME_COST = 2_500;
    private static boolean registered;
    public record OperationTiming(int baseDuration, int energyPerTick) { }

    private GeneticsMachineProcesses() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        register("isolator", GeneticsMachineProcesses::isolator, GeneticsMachineProcesses::isolatorSlot);
        register("sequencer", GeneticsMachineProcesses::sequencer, GeneticsMachineProcesses::sequencerSlot);
        register("polymeriser", GeneticsMachineProcesses::polymeriser, GeneticsMachineProcesses::polymeriserSlot);
        register("inoculator", GeneticsMachineProcesses::inoculator, GeneticsMachineProcesses::inoculatorSlot);
        register("splicer", GeneticsMachineProcesses::splicer, GeneticsMachineProcesses::splicerSlot);
        register("analyser", GeneticsMachineProcesses::analyser, GeneticsMachineProcesses::analyserSlot);
        register("genepool", GeneticsMachineProcesses::genepool, GeneticsMachineProcesses::genepoolSlot);
        register("incubator", GeneticsMachineProcesses::noTimedPlan, GeneticsMachineProcesses::incubatorSlot);
        register("acclimatiser", GeneticsMachineProcesses::noTimedPlan, GeneticsMachineProcesses::acclimatiserSlot);
    }

    private static void register(String id, Planner planner, SlotValidator slots) {
        MachineProcessRegistry.register(ResourceLocation.fromNamespaceAndPath(BinnieGenetics.MOD_ID, id), new MachineProcessHandler() {
            @Override
            public Optional<MachineProcessPlan> createPlan(Level level, MachineBlockEntity machine) {
                try {
                    return planner.create(level, machine);
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    return Optional.empty();
                }
            }

            @Override
            public boolean isItemValid(MachineBlockEntity machine, int slot, ItemStack stack) {
                return slots.valid(machine, slot, stack) || supplementarySlot(id, machine, slot, stack);
            }

            @Override
            public void beforeTick(Level level, MachineBlockEntity machine) {
                maintainInventory(id, machine);
            }

            @Override
            public int initialProgress(MachineBlockEntity machine, MachineProcessPlan plan) {
                return GeneticsMachineProcesses.initialProgress(id, machine, plan);
            }

            @Override
            public MachineProcessError diagnose(Level level, MachineBlockEntity machine) {
                try {
                    return GeneticsMachineProcesses.diagnose(id, level, machine);
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    return MachineProcessError.INVALID_INPUT;
                }
            }

            @Override
            public void beforeCompletion(Level level, MachineBlockEntity machine, MachineProcessPlan plan) {
                GeneticsMachineProcesses.beforeCompletion(id, level, machine);
            }

            @Override
            public void onCompleted(Level level, MachineBlockEntity machine, MachineProcessPlan plan) {
                recordDiscovery(id, level, machine, plan);
            }

            @Override
            public boolean controlsFluidProgress() {
                return true;
            }

            @Override
            public boolean canProgress(Level level, MachineBlockEntity machine, MachineProcessPlan plan) {
                return GeneticsMachineProcesses.canProgress(id, machine, plan);
            }

            @Override
            public void onProgress(Level level, MachineBlockEntity machine, MachineProcessPlan plan, int progress, int maxProgress) {
                updateCarriedProgress(id, machine, progress, maxProgress);
                drainSourceFluids(id, machine, plan, progress, maxProgress);
                drainSourceCharges(id, machine, plan, progress, maxProgress);
            }

            @Override
            public boolean handlesIndefiniteProcess(MachineBlockEntity machine) {
                return id.equals("incubator") || id.equals("acclimatiser");
            }

            @Override
            public void tickIndefiniteProcess(Level level, MachineBlockEntity machine) {
                tickIndefinite(id, level, machine);
            }
        });
    }

    private static Optional<MachineProcessPlan> noTimedPlan(Level level, MachineBlockEntity machine) {
        return Optional.empty();
    }

    private static int initialProgress(String machineId, MachineBlockEntity machine, MachineProcessPlan plan) {
        if (!machineId.equals("sequencer")) return 0;
        GeneticSequenceData sequence = stack(machine, MachineBlockEntity.INPUT_SLOT).get(BinnieDataComponents.GENETIC_SEQUENCE);
        return sequence == null ? 0 : progressiveAmount(plan.duration(), sequence.sequenced(), 100);
    }

    private static void updateCarriedProgress(String machineId, MachineBlockEntity machine, int progress, int maxProgress) {
        if (!machineId.equals("sequencer")) return;
        ItemStack input = stack(machine, MachineBlockEntity.INPUT_SLOT);
        GeneticSequenceData sequence = input.get(BinnieDataComponents.GENETIC_SEQUENCE);
        if (sequence == null) return;
        int percentage = progressiveAmount(100, progress, maxProgress);
        GeneticSequenceData updatedSequence = sequence.withSequenced(percentage);
        if (updatedSequence == sequence) return;
        ItemStack updated = input.copy();
        updated.set(BinnieDataComponents.GENETIC_SEQUENCE, updatedSequence);
        machine.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, updated);
    }

    private static void beforeCompletion(String machineId, Level level, MachineBlockEntity machine) {
        if (!machineId.equals("sequencer")) return;
        updateCarriedProgress(machineId, machine, 1, 1);
        if (!(level instanceof ServerLevel serverLevel)) return;
        GeneticSequenceData sequence = stack(machine, MachineBlockEntity.INPUT_SLOT).get(BinnieDataComponents.GENETIC_SEQUENCE);
        if (sequence == null) return;
        machine.owner().ifPresent(owner -> GeneticKnowledgeSavedData.get(serverLevel)
                .discoverGene(owner, GeneticKnowledgeEntry.fromAllele(sequence.allele())));
    }

    private static MachineProcessError diagnose(String machineId, Level level, MachineBlockEntity machine) {
        return switch (machineId) {
            case "isolator" -> diagnoseIsolator(machine);
            case "sequencer" -> diagnoseSequencer(machine);
            case "polymeriser" -> diagnosePolymeriser(machine);
            case "inoculator" -> diagnoseSerumApplication(machine, false, true);
            case "splicer" -> diagnoseSerumApplication(machine, true, false);
            case "analyser" -> diagnoseAnalyser(machine);
            case "genepool" -> diagnoseGenepool(machine);
            case "incubator" -> diagnoseIncubator(machine);
            case "acclimatiser" -> diagnoseAcclimatiser(machine);
            default -> MachineProcessError.NONE;
        };
    }

    private static MachineProcessError diagnoseIsolator(MachineBlockEntity machine) {
        if (!isIndividual(stack(machine, MachineBlockEntity.INPUT_SLOT))) return MachineProcessError.NO_INDIVIDUAL;
        if (!stack(machine, MachineBlockEntity.CATALYST_SLOT).is(GeneticsContent.EMPTY_SEQUENCER.get())) {
            return MachineProcessError.NO_EMPTY_SEQUENCER;
        }
        if (!hasFluid(machine, ethanol(10), false)) return MachineProcessError.INSUFFICIENT_ETHANOL;
        ItemStack enzyme = stack(machine, MachineBlockEntity.REAGENT_SLOT);
        if (!enzyme.is(GeneticsContent.ENZYME.get()) || !hasCharge(enzyme)) return MachineProcessError.NO_ENZYME;
        return MachineProcessError.NONE;
    }

    private static MachineProcessError diagnoseSequencer(MachineBlockEntity machine) {
        GeneticSequenceData sequence = stack(machine, MachineBlockEntity.INPUT_SLOT).get(BinnieDataComponents.GENETIC_SEQUENCE);
        if (sequence == null || sequence.sequenced() >= 100) return MachineProcessError.NO_SEQUENCE;
        if (machine.owner().isEmpty()) return MachineProcessError.NO_OWNER;
        ItemStack dye = stack(machine, MachineBlockEntity.REAGENT_SLOT);
        if (!dye.is(GeneticsContent.FLUORESCENT_DYE.get()) || !hasCharge(dye)) return MachineProcessError.NO_DYE;
        if (!machine.canAcceptProcessOutput(MachineBlockEntity.OUTPUT_SLOT,
                new ItemStack(GeneticsContent.EMPTY_SEQUENCER.get()))) return MachineProcessError.OUTPUT_BLOCKED;
        return MachineProcessError.NONE;
    }

    private static MachineProcessError diagnosePolymeriser(MachineBlockEntity machine) {
        ItemStack serumStack = stack(machine, MachineBlockEntity.INPUT_SLOT);
        GeneticSerumData serum = serumStack.get(BinnieDataComponents.GENETIC_SERUM);
        if (!(isMaterial(serumStack, GeneticMaterialItem.Kind.SERUM)
                || isMaterial(serumStack, GeneticMaterialItem.Kind.SERUM_ARRAY))) return MachineProcessError.NO_SERUM;
        if (serum == null) return MachineProcessError.NO_GENE;
        if (serum.charges() >= 16) return MachineProcessError.ITEM_FILLED;
        if (!machine.tank().getFluid().is(GeneticsFluids.POLYMERISING_BACTERIA.fluid())
                || machine.tank().getFluidAmount() <= 0) return MachineProcessError.INSUFFICIENT_BACTERIA;
        if (!machine.secondaryTank().getFluid().is(GeneticsFluids.RAW_DNA.fluid())
                || machine.secondaryTank().getFluidAmount() <= 0) return MachineProcessError.INSUFFICIENT_DNA;
        ItemStack gold = stack(machine, MachineBlockEntity.CATALYST_SLOT);
        if (!gold.isEmpty() && !gold.is(Items.GOLD_NUGGET)) return MachineProcessError.INVALID_INPUT;
        return MachineProcessError.NONE;
    }

    private static MachineProcessError diagnoseSerumApplication(MachineBlockEntity machine, boolean requireArray, boolean requireVector) {
        ItemStack input = stack(machine, MachineBlockEntity.INPUT_SLOT);
        if (!isIndividual(input)) return MachineProcessError.NO_INDIVIDUAL;
        GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(input);
        if (!adapter.isDnaManipulable(input)) return MachineProcessError.INVALID_INPUT;
        ItemStack catalyst = stack(machine, MachineBlockEntity.CATALYST_SLOT);
        if (catalyst.isEmpty()) return MachineProcessError.NO_SERUM;
        if (!(catalyst.getItem() instanceof GeneticMaterialItem material)) return MachineProcessError.INVALID_SERUM;
        GeneticMaterialItem.Kind kind = material.kind(catalyst);
        if (requireArray ? kind != GeneticMaterialItem.Kind.SERUM_ARRAY
                : kind != GeneticMaterialItem.Kind.SERUM && kind != GeneticMaterialItem.Kind.SERUM_ARRAY) {
            return MachineProcessError.INVALID_SERUM;
        }
        GeneticSerumData serum = catalyst.get(BinnieDataComponents.GENETIC_SERUM);
        if (serum == null || serum.alleles().isEmpty()) return MachineProcessError.INVALID_SERUM;
        if (!serum.alleles().getFirst().speciesType().equals(adapter.id())) {
            return MachineProcessError.INVALID_SERUM_MISMATCH;
        }
        if (serum.charges() <= 0) return MachineProcessError.EMPTY_SERUM;
        if (genesToApply(input, serum) == 0) return MachineProcessError.DEFUNCT_SERUM;
        if (requireVector && (!machine.tank().getFluid().is(GeneticsFluids.VECTOR_BACTERIA.fluid())
                || machine.tank().getFluidAmount() <= 0)) return MachineProcessError.INSUFFICIENT_VECTOR;
        return MachineProcessError.NONE;
    }

    private static MachineProcessError diagnoseAnalyser(MachineBlockEntity machine) {
        ItemStack input = stack(machine, MachineBlockEntity.INPUT_SLOT);
        GeneticSequenceData sequence = input.get(BinnieDataComponents.GENETIC_SEQUENCE);
        boolean individual = isIndividual(input);
        if (!individual && sequence == null) return MachineProcessError.INVALID_INPUT;
        if ((sequence != null && sequence.analyzed())
                || (individual && GeneticOrganismAdapters.forStack(input).read(input).isAnalyzed())) {
            return MachineProcessError.ALREADY_ANALYZED;
        }
        if (machine.owner().isEmpty()) return MachineProcessError.NO_OWNER;
        ItemStack dye = stack(machine, MachineBlockEntity.REAGENT_SLOT);
        if (!dye.is(GeneticsContent.DNA_DYE.get()) || !hasCharge(dye)) return MachineProcessError.NO_DYE;
        return MachineProcessError.NONE;
    }

    private static MachineProcessError diagnoseGenepool(MachineBlockEntity machine) {
        ItemStack input = stack(machine, MachineBlockEntity.INPUT_SLOT);
        if (!isIndividual(input)) return MachineProcessError.NO_INDIVIDUAL;
        if (!hasFluid(machine, ethanol((int) Math.ceil(dnaAmount(input) * 1.2D)), false)) {
            return MachineProcessError.INSUFFICIENT_ETHANOL;
        }
        ItemStack enzyme = stack(machine, MachineBlockEntity.REAGENT_SLOT);
        if (!enzyme.is(GeneticsContent.ENZYME.get()) || !hasCharge(enzyme)) return MachineProcessError.NO_ENZYME;
        if (!machine.canAcceptProcessFluid(GeneticsFluids.RAW_DNA.stack(dnaAmount(input)))) {
            return MachineProcessError.OUTPUT_TANK_FULL;
        }
        return MachineProcessError.NONE;
    }

    private static MachineProcessError diagnoseIncubator(MachineBlockEntity machine) {
        IncubatorPlan recipe = incubatorRecipe(stack(machine, MachineBlockEntity.REAGENT_SLOT), machine.tank().getFluid());
        if (recipe == null) return MachineProcessError.INVALID_INPUT;
        if (!hasFluid(machine, recipe.fluidInput(), false)) return MachineProcessError.INSUFFICIENT_LIQUID;
        if (!machine.canAcceptProcessFluid(recipe.fluidOutput())) return MachineProcessError.OUTPUT_TANK_FULL;
        if (!machine.canAcceptProcessOutput(MachineBlockEntity.OUTPUT_SLOT, recipe.itemOutput())) {
            return MachineProcessError.OUTPUT_BLOCKED;
        }
        return MachineProcessError.NONE;
    }

    private static MachineProcessError diagnoseAcclimatiser(MachineBlockEntity machine) {
        ItemStack target = stack(machine, MachineBlockEntity.INPUT_SLOT);
        if (!isIndividual(target)) return MachineProcessError.NO_INDIVIDUAL;
        boolean hasReagent = java.util.Arrays.stream(acclimatiserReagentSlots(machine))
                .mapToObj(slot -> stack(machine, slot)).anyMatch(candidate -> !candidate.isEmpty());
        if (!hasReagent) return MachineProcessError.INVALID_INPUT;
        if (acclimatiserCandidates(target, machine).isEmpty()) return MachineProcessError.CANNOT_ACCLIMATISE;
        return MachineProcessError.NONE;
    }

    private static Optional<MachineProcessPlan> isolator(Level level, MachineBlockEntity machine) {
        ItemStack input = stack(machine, MachineBlockEntity.INPUT_SLOT);
        ItemStack vial = stack(machine, MachineBlockEntity.CATALYST_SLOT);
        ItemStack enzyme = stack(machine, MachineBlockEntity.REAGENT_SLOT);
        if (!isIndividual(input) || !vial.is(GeneticsContent.EMPTY_SEQUENCER.get()) || !isTool(enzyme, GeneticsContent.ENZYME.get())) {
            return Optional.empty();
        }
        GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(input);
        List<IChromosome<?>> chromosomes = adapter.chromosomes();
        int selector = stableHash(input, vial, enzyme, machine);
        IChromosome<?> chromosome = chromosomes.get(Math.floorMod(selector, chromosomes.size()));
        GeneticAlleleData.Origin origin = (selector & 1) == 0 ? GeneticAlleleData.Origin.ACTIVE : GeneticAlleleData.Origin.INACTIVE;
        GeneticAlleleData allele = GeneticOperations.sample(input, chromosome.id(), origin);
        ItemStack output = GeneticsContent.GENETIC_MATERIAL.get().sequence(GeneticSequenceData.unsequenced(allele, 100));
        int targetLoss = Math.floorMod(selector >>> 3, 20) == 0 ? 1 : 0;
        return Optional.of(plan("isolator", input, vial, enzyme, output, ItemStack.EMPTY,
                ISOLATOR_OPERATION.baseDuration(), ISOLATOR_OPERATION.energyPerTick(), targetLoss, 1, 0, ItemStack.EMPTY, ItemStack.EMPTY,
                ethanol(10), EMPTY_FLUID, EMPTY_FLUID));
    }

    private static Optional<MachineProcessPlan> sequencer(Level level, MachineBlockEntity machine) {
        ItemStack input = stack(machine, MachineBlockEntity.INPUT_SLOT);
        ItemStack dye = stack(machine, MachineBlockEntity.REAGENT_SLOT);
        GeneticSequenceData sequence = input.get(BinnieDataComponents.GENETIC_SEQUENCE);
        if (!isMaterial(input, GeneticMaterialItem.Kind.SEQUENCE) || sequence == null || sequence.sequenced() >= 100
                || !isTool(dye, GeneticsContent.FLUORESCENT_DYE.get()) || machine.owner().isEmpty()) return Optional.empty();
        return Optional.of(plan("sequencer", input, ItemStack.EMPTY, dye,
                new ItemStack(GeneticsContent.EMPTY_SEQUENCER.get()), ItemStack.EMPTY,
                SEQUENCER_OPERATION.baseDuration(), SEQUENCER_OPERATION.energyPerTick(), 1, 0, 0, ItemStack.EMPTY, ItemStack.EMPTY,
                EMPTY_FLUID, EMPTY_FLUID, EMPTY_FLUID));
    }

    private static Optional<MachineProcessPlan> polymeriser(Level level, MachineBlockEntity machine) {
        ItemStack input = stack(machine, MachineBlockEntity.INPUT_SLOT);
        ItemStack gold = stack(machine, MachineBlockEntity.CATALYST_SLOT);
        GeneticSerumData serum = input.get(BinnieDataComponents.GENETIC_SERUM);
        if (!(isMaterial(input, GeneticMaterialItem.Kind.SERUM) || isMaterial(input, GeneticMaterialItem.Kind.SERUM_ARRAY))
                || serum == null || serum.charges() >= 16) return Optional.empty();
        if (!gold.isEmpty() && !gold.is(Items.GOLD_NUGGET)) return Optional.empty();

        ItemStack output = input.copyWithCount(1);
        output.set(BinnieDataComponents.GENETIC_SERUM,
                new GeneticSerumData(serum.alleles(), serum.quality(), serum.charges() + 1));
        int genes = serum.alleles().size();
        boolean accelerated = gold.is(Items.GOLD_NUGGET) && hasCharge(gold);
        return Optional.of(plan("polymeriser", input, gold, ItemStack.EMPTY, output, ItemStack.EMPTY,
                (accelerated ? POLYMERISER_ACCELERATED_OPERATION.baseDuration() : POLYMERISER_OPERATION.baseDuration()) * genes,
                POLYMERISER_OPERATION.energyPerTick(),
                1, 0, 0, ItemStack.EMPTY, ItemStack.EMPTY,
                EMPTY_FLUID, EMPTY_FLUID, EMPTY_FLUID));
    }

    private static Optional<MachineProcessPlan> inoculator(Level level, MachineBlockEntity machine) {
        return serumApplication(machine, "inoculator", false);
    }

    private static Optional<MachineProcessPlan> splicer(Level level, MachineBlockEntity machine) {
        return serumApplication(machine, "splicer", true);
    }

    private static Optional<MachineProcessPlan> serumApplication(MachineBlockEntity machine, String id,
                                                                  boolean requireArray) {
        ItemStack input = stack(machine, MachineBlockEntity.INPUT_SLOT);
        ItemStack catalyst = stack(machine, MachineBlockEntity.CATALYST_SLOT);
        if (!isIndividual(input) || !(catalyst.getItem() instanceof GeneticMaterialItem material)) return Optional.empty();
        GeneticMaterialItem.Kind kind = material.kind(catalyst);
        if (requireArray ? kind != GeneticMaterialItem.Kind.SERUM_ARRAY
                : kind != GeneticMaterialItem.Kind.SERUM && kind != GeneticMaterialItem.Kind.SERUM_ARRAY) return Optional.empty();
        GeneticSerumData serum = catalyst.get(BinnieDataComponents.GENETIC_SERUM);
        if (serum == null || serum.charges() <= 0) return Optional.empty();
        GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(input);
        if (!adapter.isDnaManipulable(input) || !serum.alleles().getFirst().speciesType().equals(adapter.id())) return Optional.empty();
        int genesToUse = genesToApply(input, serum);
        if (genesToUse == 0) return Optional.empty();
        ItemStack output = GeneticOperations.applySerum(input, serum);
        if (ItemStack.isSameItemSameComponents(output, input)) return Optional.empty();
        ItemStack after = catalyst.copyWithCount(1);
        after.set(BinnieDataComponents.GENETIC_SERUM, serum.useCharge());
        int geneCount = serum.alleles().size();
        int duration = id.equals("splicer") ? splicerDuration(genesToUse) : INOCULATOR_OPERATION.baseDuration() * geneCount;
        int energy = id.equals("splicer") ? SPLICER_OPERATION.energyPerTick() : INOCULATOR_OPERATION.energyPerTick();
        return Optional.of(plan(id, input, catalyst, ItemStack.EMPTY, output, ItemStack.EMPTY,
                duration, energy, 1, 0, 0, after, ItemStack.EMPTY,
                EMPTY_FLUID, EMPTY_FLUID, EMPTY_FLUID));
    }

    private static Optional<MachineProcessPlan> analyser(Level level, MachineBlockEntity machine) {
        ItemStack input = stack(machine, MachineBlockEntity.INPUT_SLOT);
        ItemStack dye = stack(machine, MachineBlockEntity.REAGENT_SLOT);
        GeneticSequenceData sequence = input.get(BinnieDataComponents.GENETIC_SEQUENCE);
        boolean individual = isIndividual(input);
        if ((!individual && sequence == null) || !isTool(dye, GeneticsContent.DNA_DYE.get()) || machine.owner().isEmpty()) {
            return Optional.empty();
        }
        ItemStack output;
        if (sequence != null) {
            if (sequence.analyzed()) return Optional.empty();
            output = input.copyWithCount(1);
            output.set(BinnieDataComponents.GENETIC_SEQUENCE, sequence.analyzedCopy());
        } else {
            GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(input);
            if (adapter.read(input).isAnalyzed()) return Optional.empty();
            output = adapter.analyze(input);
        }
        return Optional.of(plan("analyser", input, ItemStack.EMPTY, dye, output, ItemStack.EMPTY,
                ANALYSER_OPERATION.baseDuration(), ANALYSER_OPERATION.energyPerTick(), 1, 0, 0, ItemStack.EMPTY, ItemStack.EMPTY,
                EMPTY_FLUID, EMPTY_FLUID, EMPTY_FLUID));
    }

    private static Optional<MachineProcessPlan> genepool(Level level, MachineBlockEntity machine) {
        ItemStack input = stack(machine, MachineBlockEntity.INPUT_SLOT);
        ItemStack enzyme = stack(machine, MachineBlockEntity.REAGENT_SLOT);
        if (!isIndividual(input) || !isTool(enzyme, GeneticsContent.ENZYME.get())) return Optional.empty();
        int dna = dnaAmount(input);
        return Optional.of(plan("genepool", input, ItemStack.EMPTY, enzyme, ItemStack.EMPTY, ItemStack.EMPTY,
                GENEPOOL_OPERATION.baseDuration(), GENEPOOL_OPERATION.energyPerTick(), 1, 0, 0, ItemStack.EMPTY, ItemStack.EMPTY,
                EMPTY_FLUID, EMPTY_FLUID, GeneticsFluids.RAW_DNA.stack(dna)));
    }

    /**
      * The 1.12 Incubator and Acclimatiser extend ComponentProcessIndefinate:
      * they continuously draw 2 FE/t while valid and make chance-gated work
      * attempts in place. Do not convert them to finite recipes.
      */
    private static void tickIndefinite(String id, Level level, MachineBlockEntity machine) {
        if (id.equals("incubator")) {
            tickIncubator(level, machine);
        } else if (id.equals("acclimatiser")) {
            tickAcclimatiser(level, machine);
        }
    }

    private static void tickIncubator(Level level, MachineBlockEntity machine) {
        IncubatorPlan recipe = incubatorRecipe(stack(machine, MachineBlockEntity.REAGENT_SLOT), machine.tank().getFluid());
        if (recipe == null) {
            machine.setIndefiniteProcess(machine.spec().id(), false);
            return;
        }
        boolean starting = machine.setIndefiniteProcess(machine.spec().id(), true);
        if (!hasFluid(machine, recipe.fluidInput(), false)
                || !machine.canAcceptProcessFluid(recipe.fluidOutput())
                || !machine.canAcceptProcessOutput(MachineBlockEntity.OUTPUT_SLOT, recipe.itemOutput())) {
            return;
        }
        if (starting) return;
        if (!machine.consumeProcessEnergy(2)) {
            machine.setProcessError(MachineProcessError.NO_POWER);
            return;
        }
        machine.setProcessError(MachineProcessError.NONE);
        if (level.random.nextInt(20) != 0 || level.random.nextFloat() >= recipe.tickChance()) return;

        if (!recipe.fluidInput().isEmpty()) {
            machine.tank().drain(recipe.fluidInput(), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
        if (!recipe.fluidOutput().isEmpty()) {
            machine.outputTank().fill(recipe.fluidOutput(), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
        machine.insertProcessOutput(MachineBlockEntity.OUTPUT_SLOT, recipe.itemOutput());
    // 1.12 IncubatorRecipe loses the incubator stack only on this
    // separate roll; a successful recipe normally keeps the reagent.
        if (level.random.nextFloat() < recipe.lossChance()) {
            machine.inventory().extractItem(MachineBlockEntity.REAGENT_SLOT, 1, false);
        }
    }

    private static void tickAcclimatiser(Level level, MachineBlockEntity machine) {
        ItemStack target = stack(machine, MachineBlockEntity.INPUT_SLOT);
        List<AcclimatiserCandidate> candidates = acclimatiserCandidates(target, machine);
        if (!isIndividual(target) || java.util.Arrays.stream(acclimatiserReagentSlots(machine))
                .mapToObj(slot -> stack(machine, slot)).allMatch(ItemStack::isEmpty)) {
            machine.setIndefiniteProcess(machine.spec().id(), false);
            return;
        }
        boolean starting = machine.setIndefiniteProcess(machine.spec().id(), true);
    // 1.12 canWork marks this processor active with any acclimatiser
    // stack; canProgress then reports that ineffective/bounded stacks
    // cannot alter the target and avoids draining power.
        if (candidates.isEmpty()) return;
        if (starting) return;
        if (!machine.consumeProcessEnergy(2)) {
            machine.setProcessError(MachineProcessError.NO_POWER);
            return;
        }
        machine.setProcessError(MachineProcessError.NONE);
        if (level.random.nextInt(100) != 0) return;

    // Select one valid acclimatiser at random, change the target
    // in its own slot, then consume one matching stack from the first
    // acclimatiser slot. The compact current menu exposes two such slots.
        AcclimatiserCandidate selected = candidates.get(level.random.nextInt(candidates.size()));
        ItemStack changed = acclimatise(target, selected.adjustment());
        if (ItemStack.isSameItemSameComponents(target, changed)) return;
        machine.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, changed);
        for (int slot : acclimatiserReagentSlots(machine)) {
            ItemStack candidate = stack(machine, slot);
            if (ItemStack.isSameItemSameComponents(candidate, selected.stack())) {
                machine.inventory().extractItem(slot, 1, false);
                return;
            }
        }
    }

    private static List<AcclimatiserCandidate> acclimatiserCandidates(ItemStack target, MachineBlockEntity machine) {
        if (!isIndividual(target)) return List.of();
        GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(target);
        java.util.ArrayList<AcclimatiserCandidate> candidates = new java.util.ArrayList<>();
        for (int slot : acclimatiserReagentSlots(machine)) {
            ItemStack reagent = stack(machine, slot);
            Adjustment adjustment = adjustment(adapter, reagent);
            if (adjustment != null && !ItemStack.isSameItemSameComponents(target, acclimatise(target, adjustment))) {
                candidates.add(new AcclimatiserCandidate(reagent, adjustment));
            }
        }
        return candidates;
    }

    private static ItemStack acclimatise(ItemStack input, Adjustment adjustment) {
        GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(input);
        GeneticAlleleData old = GeneticOperations.sample(input, adjustment.chromosome().id(), GeneticAlleleData.Origin.ACTIVE);
        if (!(old.value().javaValue() instanceof ToleranceType tolerance)) return input;
        ToleranceType changed = adjust(tolerance, adjustment.upDelta(), adjustment.downDelta());
        if (changed == tolerance) return input;
        GeneticAlleleData replacement = new GeneticAlleleData(adapter.id(), adjustment.chromosome().id(),
                new AlleleValue.ToleranceValue(changed), old.dominant(), GeneticAlleleData.Origin.ACTIVE);
        return GeneticOperations.replaceAllele(input, replacement, true, true);
    }

    private static IncubatorPlan incubatorRecipe(ItemStack reagent, FluidStack input) {
        if (reagent.isEmpty()) return null;
        if (ItemGE.getLifeStage(reagent) == BeeLifeStage.LARVAE && input.is(GeneticsFluids.GROWTH_MEDIUM.fluid())) {
            forestry.api.core.genetics.IIndividual larvae = ItemGE.getIndividual(reagent);
            if (larvae != null) {
                return new IncubatorPlan(EMPTY_FLUID, GeneticsFluids.GROWTH_MEDIUM.stack(50), 1.0F, 0.05F,
                        larvae.createStack(BeeLifeStage.DRONE));
            }
        }
        if (reagent.is(GeneticsContent.GROWTH_MEDIUM.get()) && input.is(net.minecraft.world.level.material.Fluids.WATER)) {
            return new IncubatorPlan(GeneticsFluids.GROWTH_MEDIUM.stack(25), new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 25), 0.2F, 1.0F);
        }
        if (reagent.is(Items.WHEAT) && input.is(GeneticsFluids.GROWTH_MEDIUM.fluid())) {
            return new IncubatorPlan(GeneticsFluids.BACTERIA.stack(5), GeneticsFluids.GROWTH_MEDIUM.stack(25), 0.2F, 1.0F);
        }
        if (reagent.is(GeneticsContent.GROWTH_MEDIUM.get()) && input.is(GeneticsFluids.BACTERIA.fluid())) {
            return new IncubatorPlan(GeneticsFluids.BACTERIA.stack(5), EMPTY_FLUID, 0.05F, 1.0F);
        }
        if (reagent.is(Items.SUGAR) && input.is(GeneticsFluids.BACTERIA.fluid())) {
            return new IncubatorPlan(EMPTY_FLUID, GeneticsFluids.BACTERIA.stack(2), 0.5F, 0.2F,
                    new ItemStack(GeneticsContent.ENZYME.get()));
        }
        if (reagent.is(GeneticsContent.GROWTH_MEDIUM.get()) && input.is(GeneticsFluids.POLYMERISING_BACTERIA.fluid())) {
            return new IncubatorPlan(GeneticsFluids.POLYMERISING_BACTERIA.stack(5), EMPTY_FLUID, 0.05F, 1.0F);
        }
        if (reagent.is(GeneticsContent.GROWTH_MEDIUM.get()) && input.is(GeneticsFluids.VECTOR_BACTERIA.fluid())) {
            return new IncubatorPlan(GeneticsFluids.VECTOR_BACTERIA.stack(5), EMPTY_FLUID, 0.05F, 1.0F);
        }
        if (reagent.is(Items.WHITE_DYE) && input.is(GeneticsFluids.BACTERIA.fluid())) {
            return new IncubatorPlan(GeneticsFluids.POLYMERISING_BACTERIA.stack(10), GeneticsFluids.BACTERIA.stack(10), 0.1F, 1.0F);
        }
        if (reagent.is(Items.BLAZE_POWDER) && input.is(GeneticsFluids.BACTERIA.fluid())) {
            return new IncubatorPlan(GeneticsFluids.VECTOR_BACTERIA.stack(10), GeneticsFluids.BACTERIA.stack(10), 0.05F, 1.0F);
        }
        return null;
    }

    private static Adjustment adjustment(GeneticOrganismAdapter adapter, ItemStack reagent) {
        List<IChromosome<?>> chromosomes = adapter.chromosomes();
        if (reagent.is(BotanyItemTags.ACID_FERTILISERS)) return findAdjustment(chromosomes, "ph_tolerance", 0, 1);
        if (reagent.is(BotanyItemTags.ALKALINE_FERTILISERS)) return findAdjustment(chromosomes, "ph_tolerance", 1, 0);
        if (reagent.is(Items.BLAZE_POWDER) || reagent.is(Items.BLAZE_ROD) || reagent.is(Items.LAVA_BUCKET)) {
            return findAdjustment(chromosomes, "temperature_tolerance", 1, 0);
        }
        if (reagent.is(Items.SNOWBALL) || reagent.is(Items.ICE)) {
            return findAdjustment(chromosomes, "temperature_tolerance", 0, 1);
        }
        if (reagent.is(Items.WATER_BUCKET)) return findAdjustment(chromosomes, "humidity_tolerance", 1, 0);
        if (reagent.is(Items.SAND)) return findAdjustment(chromosomes, "humidity_tolerance", 0, 1);
        return null;
    }

    private static Adjustment findAdjustment(List<IChromosome<?>> chromosomes, String path, int up, int down) {
        for (IChromosome<?> chromosome : chromosomes) {
            if (chromosome.id().getPath().equals(path) || chromosome.id().getPath().endsWith("/" + path)) {
                return new Adjustment(chromosome, up, down);
            }
        }
        return null;
    }

    private static ToleranceType adjust(ToleranceType old, int upDelta, int downDelta) {
        int up = Math.min(5, old.up + upDelta);
        int down = Math.min(5, old.down + downDelta);
        for (ToleranceType type : ToleranceType.values()) if (type.up == up && type.down == down) return type;
    // Forestry has no asymmetric type with both sides non-zero but unequal. Expand to the smallest symmetric range.
        if (up > 0 && down > 0) {
            int both = Math.max(up, down);
            for (ToleranceType type : ToleranceType.values()) if (type.up == both && type.down == both) return type;
        }
        return old;
    }

    private static int dnaAmount(ItemStack input) {
        GeneticOrganismAdapter adapter = GeneticOrganismAdapters.forStack(input);
        if (!adapter.id().getPath().equals("bee")) return 10;
        ILifeStage stage = adapter.lifeStage(input);
        String id = stage.getSerializedName();
        if (id.contains("drone")) return 10;
        if (id.contains("queen") || id.contains("mated")) return 50;
        return 30;
    }

    private static int genesToApply(ItemStack input, GeneticSerumData serum) {
        int changed = 0;
        for (GeneticAlleleData allele : serum.alleles()) {
            GeneticAlleleData active = GeneticOperations.sample(input, allele.chromosome(), GeneticAlleleData.Origin.ACTIVE);
            GeneticAlleleData inactive = GeneticOperations.sample(input, allele.chromosome(), GeneticAlleleData.Origin.INACTIVE);
            if (!active.value().equals(allele.value()) || !inactive.value().equals(allele.value())) changed++;
        }
        return changed;
    }

    private static int splicerDuration(int genesToUse) {
        double scale = 1.0D + (Math.max(1, genesToUse) - 1) * 0.5D;
        return (int) (SPLICER_OPERATION.baseDuration() * scale);
    }

    private static int polymeriserGeneCount(MachineBlockEntity machine) {
        GeneticSerumData serum = stack(machine, MachineBlockEntity.INPUT_SLOT).get(BinnieDataComponents.GENETIC_SERUM);
        return serum == null ? 1 : Math.max(1, serum.alleles().size());
    }

    private static int polymeriserDna(MachineBlockEntity machine) {
        return 50 * polymeriserGeneCount(machine);
    }

    private static int polymeriserBacteria(MachineBlockEntity machine) {
        return 10 * polymeriserGeneCount(machine);
    }

    private static boolean canProgress(String machineId, MachineBlockEntity machine, MachineProcessPlan plan) {
        return switch (machineId) {
            case "isolator" -> hasFluid(machine, ethanol(10), false)
                    && canDrainCharge(machine, MachineBlockEntity.REAGENT_SLOT, ISOLATOR_ENZYME_COST, machine.progress(), plan.duration());
            case "sequencer" -> canDrainCharge(machine, MachineBlockEntity.REAGENT_SLOT, SEQUENCER_DYE_COST, machine.progress(), plan.duration());
            case "polymeriser" -> canDrainProgressively(machine.tank(), GeneticsFluids.POLYMERISING_BACTERIA.stack(1), polymeriserBacteria(machine), machine.progress(), plan.duration())
                    && canDrainProgressively(machine.secondaryTank(), GeneticsFluids.RAW_DNA.stack(1), polymeriserDna(machine), machine.progress(), plan.duration());
            case "inoculator" -> canDrainProgressively(machine.tank(), GeneticsFluids.VECTOR_BACTERIA.stack(1), 15, machine.progress(), plan.duration());
            case "analyser" -> canDrainCharge(machine, MachineBlockEntity.REAGENT_SLOT, ANALYSER_DYE_COST, machine.progress(), plan.duration());
            case "genepool" -> canDrainProgressively(machine.tank(), ethanol(1), (int) Math.ceil(dnaAmount(stack(machine, MachineBlockEntity.INPUT_SLOT)) * 1.2D), machine.progress(), plan.duration())
                    && canDrainCharge(machine, MachineBlockEntity.REAGENT_SLOT, GENEPOOL_ENZYME_COST, machine.progress(), plan.duration());
            default -> true;
        };
    }

    private static void drainSourceFluids(String machineId, MachineBlockEntity machine, MachineProcessPlan plan, int progress, int maxProgress) {
        switch (machineId) {
            case "polymeriser" -> {
                drainProgressively(machine.tank(), GeneticsFluids.POLYMERISING_BACTERIA.stack(1), polymeriserBacteria(machine), progress, maxProgress);
                drainProgressively(machine.secondaryTank(), GeneticsFluids.RAW_DNA.stack(1), polymeriserDna(machine), progress, maxProgress);
            }
            case "inoculator" -> drainProgressively(machine.tank(), GeneticsFluids.VECTOR_BACTERIA.stack(1), 15, progress, maxProgress);
            case "genepool" -> drainProgressively(machine.tank(), ethanol(1),
                    (int) Math.ceil(dnaAmount(stack(machine, MachineBlockEntity.INPUT_SLOT)) * 1.2D), progress, maxProgress);
            default -> {
            }
        }
    }

    private static void drainSourceCharges(String machineId, MachineBlockEntity machine, MachineProcessPlan plan, int progress, int maxProgress) {
        switch (machineId) {
            case "isolator" -> drainCharge(machine, MachineBlockEntity.REAGENT_SLOT, ISOLATOR_ENZYME_COST, progress, maxProgress);
            case "sequencer" -> drainCharge(machine, MachineBlockEntity.REAGENT_SLOT, SEQUENCER_DYE_COST, progress, maxProgress);
            case "polymeriser" -> drainOptionalCharge(machine, MachineBlockEntity.CATALYST_SLOT,
                    POLYMERISER_GOLD_COST, progress, maxProgress);
            case "analyser" -> drainCharge(machine, MachineBlockEntity.REAGENT_SLOT, ANALYSER_DYE_COST, progress, maxProgress);
            case "genepool" -> drainCharge(machine, MachineBlockEntity.REAGENT_SLOT, GENEPOOL_ENZYME_COST, progress, maxProgress);
            default -> {
            }
        }
    }

    private static boolean canDrainProgressively(net.neoforged.neoforge.fluids.capability.templates.FluidTank tank,
                                                  FluidStack unit, int total, int progress, int maxProgress) {
        if (!tank.getFluid().is(unit.getFluid()) || tank.getFluidAmount() <= 0) return false;
        int needed = progressiveAmount(total, progress + 1, maxProgress) - progressiveAmount(total, progress, maxProgress);
        return needed == 0 || tank.drain(unit.copyWithAmount(needed), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE).getAmount() == needed;
    }

    private static void drainProgressively(net.neoforged.neoforge.fluids.capability.templates.FluidTank tank,
                                           FluidStack unit, int total, int progress, int maxProgress) {
        int amount = progressiveAmount(total, progress, maxProgress) - progressiveAmount(total, progress - 1, maxProgress);
        if (amount > 0) tank.drain(unit.copyWithAmount(amount), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
    }

    private static boolean canDrainCharge(MachineBlockEntity machine, int slot, int total, int progress, int maxProgress) {
        ItemStack stack = stack(machine, slot);
        if (stack.isEmpty()) return false;
        int needed = progressiveAmount(total, progress + 1, maxProgress) - progressiveAmount(total, progress, maxProgress);
        return totalCharge(stack) >= needed && (needed > 0 || hasCharge(stack));
    }

    private static void drainCharge(MachineBlockEntity machine, int slot, int total, int progress, int maxProgress) {
        int amount = progressiveAmount(total, progress, maxProgress) - progressiveAmount(total, progress - 1, maxProgress);
        if (amount <= 0) return;
        ItemStack updated = drainActiveCells(stack(machine, slot), amount);
        if (updated == null) throw new IllegalStateException("Laboratory charge became negative during a validated process");
        machine.inventory().setStackInSlot(slot, updated);
    }

    /** Optional catalysts accelerate only while charge remains and never block the base process. */
    private static void drainOptionalCharge(MachineBlockEntity machine, int slot, int total, int progress, int maxProgress) {
        int amount = progressiveAmount(total, progress, maxProgress) - progressiveAmount(total, progress - 1, maxProgress);
        ItemStack stack = stack(machine, slot);
        if (amount <= 0 || stack.isEmpty() || totalCharge(stack) <= 0) return;
        ItemStack updated = drainActiveCells(stack, Math.min(amount, totalCharge(stack)));
        if (updated != null) machine.inventory().setStackInSlot(slot, updated);
    }

    /**
      * Charged slots treat a stack as a queue of full cells and store only the active cell's
      * fractional charge. The component on the current stack mirrors that active-cell charge; all later
      * cells in the same stack are implicitly full. This keeps the original multi-item dye recipes
      * without duplicating or flattening their fractional machine use.
      */
    private static ItemStack drainActiveCells(ItemStack source, int amount) {
        if (source.isEmpty() || amount < 0 || totalCharge(source) < amount) return null;
        ItemStack updated = source.copy();
        int count = updated.getCount();
        int active = charge(updated);
        int remainingDrain = amount;
        while (remainingDrain > 0 && count > 0) {
            if (active <= 0) {
                count--;
                active = LaboratoryChargeData.FULL;
                continue;
            }
            int used = Math.min(active, remainingDrain);
            active -= used;
            remainingDrain -= used;
            if (active == 0) {
                count--;
                if (count > 0) active = LaboratoryChargeData.FULL;
            }
        }
        if (remainingDrain != 0) return null;
        if (count <= 0) return ItemStack.EMPTY;
        updated.setCount(count);
        if (active >= LaboratoryChargeData.FULL) updated.remove(BinnieDataComponents.LABORATORY_CHARGE);
        else updated.set(BinnieDataComponents.LABORATORY_CHARGE, new LaboratoryChargeData(active));
        return updated;
    }

    private static int totalCharge(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return charge(stack) + Math.max(0, stack.getCount() - 1) * LaboratoryChargeData.FULL;
    }

    private static int progressiveAmount(int total, int progress, int maxProgress) {
        if (maxProgress <= 0) return 0;
        return (int) ((long) total * Math.max(0, Math.min(progress, maxProgress)) / maxProgress);
    }

    private static void recordDiscovery(String machineId, Level level, MachineBlockEntity machine, MachineProcessPlan plan) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        machine.owner().ifPresent(owner -> {
            GeneticKnowledgeSavedData tracker = GeneticKnowledgeSavedData.get(serverLevel);
            if (machineId.equals("analyser") && isIndividual(plan.output())) {
                tracker.discoverIndividual(owner, GeneticOrganismAdapters.forStack(plan.output()).read(plan.output()));
            }
        });
    }

    private static boolean hasFluid(MachineBlockEntity machine, FluidStack needed, boolean secondary) {
        if (needed.isEmpty()) return true;
        return (secondary ? machine.secondaryTank() : machine.tank())
                .drain(needed, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)
                .getAmount() >= needed.getAmount();
    }

    /** Both versions use Forestry bio-ethanol. */
    private static FluidStack ethanol(int amount) {
        return ForestryFluids.BIO_ETHANOL.getFluid(amount);
    }

    private static MachineProcessPlan plan(String machine, ItemStack input, ItemStack catalyst, ItemStack reagent,
                                           ItemStack output, ItemStack secondaryOutput,
                                           int duration, int energy, int inputCount, int catalystCount, int reagentCount,
                                           ItemStack catalystAfter, ItemStack reagentAfter,
                                           FluidStack primaryFluid, FluidStack secondaryFluid, FluidStack fluidOutput) {
        int hash = stableHash(input, catalyst, reagent, null);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BinnieGenetics.MOD_ID,
                "dynamic/" + machine + "/" + Integer.toUnsignedString(hash, 16));
        return new MachineProcessPlan(id, output, secondaryOutput, ItemStack.EMPTY, duration, energy,
                inputCount, catalystCount, reagentCount, catalystAfter, reagentAfter,
                primaryFluid, secondaryFluid, fluidOutput);
    }

    private static int stableHash(ItemStack input, ItemStack catalyst, ItemStack reagent, MachineBlockEntity machine) {
        int hash = stableStackHash(input);
        hash = 31 * hash + stableStackHash(catalyst);
        hash = 31 * hash + stableStackHash(reagent);
        if (machine != null) {
            hash = 31 * hash + machine.getBlockPos().hashCode();
            hash = 31 * hash + Long.hashCode(machine.processNonce());
        }
        return hash;
    }

    private static int stackHash(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return 31 * BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode() + stack.getComponentsPatch().hashCode();
    }

    /** Active processes retain their identity while charge cells drain. */
    private static int stableStackHash(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        ItemStack stable = stack.copyWithCount(1);
        stable.remove(BinnieDataComponents.LABORATORY_CHARGE);
        GeneticSequenceData sequence = stable.get(BinnieDataComponents.GENETIC_SEQUENCE);
        if (sequence != null) stable.set(BinnieDataComponents.GENETIC_SEQUENCE, sequence.withSequenced(0));
        return stackHash(stable);
    }

    private static ItemStack stack(MachineBlockEntity machine, int slot) {
        return machine.inventory().getStackInSlot(slot);
    }

    private static boolean isTool(ItemStack stack, Item expected) {
        return stack.is(expected);
    }

    private static boolean hasCharge(ItemStack stack) {
        return totalCharge(stack) > 0;
    }

    private static int charge(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        LaboratoryChargeData charge = stack.get(BinnieDataComponents.LABORATORY_CHARGE);
        return charge == null ? LaboratoryChargeData.FULL : charge.milliCharge();
    }

    private static boolean isIndividual(ItemStack stack) {
        return !stack.isEmpty()
                && forestry.api.core.genetics.capability.IIndividualHandlerItem.isIndividual(stack)
                && GeneticOrganismAdapters.supports(stack);
    }

    private static boolean isDnaManipulable(ItemStack stack) {
        return isIndividual(stack) && GeneticOrganismAdapters.forStack(stack).isDnaManipulable(stack);
    }

    private static boolean isMaterial(ItemStack stack, GeneticMaterialItem.Kind kind) {
        return stack.getItem() instanceof GeneticMaterialItem item && item.kind(stack) == kind;
    }

    private static boolean isolatorSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return switch (slot) {
            case MachineBlockEntity.INPUT_SLOT -> isIndividual(stack);
            case MachineBlockEntity.CATALYST_SLOT -> stack.is(GeneticsContent.EMPTY_SEQUENCER.get());
            case MachineBlockEntity.REAGENT_SLOT -> stack.is(GeneticsContent.ENZYME.get());
            default -> false;
        };
    }

    private static boolean sequencerSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return switch (slot) {
            case MachineBlockEntity.INPUT_SLOT -> {
                GeneticSequenceData sequence = stack.get(BinnieDataComponents.GENETIC_SEQUENCE);
                yield isMaterial(stack, GeneticMaterialItem.Kind.SEQUENCE) && sequence != null && sequence.sequenced() < 100;
            }
            case MachineBlockEntity.REAGENT_SLOT -> stack.is(GeneticsContent.FLUORESCENT_DYE.get());
            default -> false;
        };
    }

    private static boolean polymeriserSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return switch (slot) {
            case MachineBlockEntity.INPUT_SLOT -> {
                GeneticSerumData serum = stack.get(BinnieDataComponents.GENETIC_SERUM);
                yield (isMaterial(stack, GeneticMaterialItem.Kind.SERUM)
                        || isMaterial(stack, GeneticMaterialItem.Kind.SERUM_ARRAY))
                        && serum != null && serum.charges() < 16;
            }
            case MachineBlockEntity.CATALYST_SLOT -> stack.is(Items.GOLD_NUGGET);
            default -> false;
        };
    }

    private static boolean inoculatorSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return switch (slot) {
            case MachineBlockEntity.INPUT_SLOT -> isDnaManipulable(stack);
            case MachineBlockEntity.CATALYST_SLOT -> isMaterial(stack, GeneticMaterialItem.Kind.SERUM)
                    || isMaterial(stack, GeneticMaterialItem.Kind.SERUM_ARRAY);
            default -> false;
        };
    }

    private static boolean splicerSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return switch (slot) {
            case MachineBlockEntity.INPUT_SLOT -> isDnaManipulable(stack);
            case MachineBlockEntity.CATALYST_SLOT -> isMaterial(stack, GeneticMaterialItem.Kind.SERUM_ARRAY);
            default -> false;
        };
    }

    private static boolean analyserSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return switch (slot) {
            case MachineBlockEntity.INPUT_SLOT -> isIndividual(stack) || stack.has(BinnieDataComponents.GENETIC_SEQUENCE);
            case MachineBlockEntity.REAGENT_SLOT -> stack.is(GeneticsContent.DNA_DYE.get());
            default -> false;
        };
    }

    private static boolean genepoolSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return switch (slot) {
            case MachineBlockEntity.INPUT_SLOT -> isIndividual(stack);
            case MachineBlockEntity.REAGENT_SLOT -> stack.is(GeneticsContent.ENZYME.get());
            default -> false;
        };
    }

    private static boolean incubatorSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return slot == MachineBlockEntity.REAGENT_SLOT;
    }

    private static boolean acclimatiserSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return switch (slot) {
            case MachineBlockEntity.INPUT_SLOT -> isIndividual(stack);
            case MachineBlockEntity.CATALYST_SLOT, MachineBlockEntity.REAGENT_SLOT -> {
                ItemStack target = stack(machine, MachineBlockEntity.INPUT_SLOT);
                yield isIndividual(target) && adjustment(GeneticOrganismAdapters.forStack(target), stack) != null;
            }
            default -> false;
        };
    }

    /**
      * Provides the 1.12 packages' {@code ComponentInventoryTransfer} work.
      * The active slots are the long-lived six-slot compatibility contract;
      * reserve and completed groups are the originally sized extension declared in
      * {@link GeneticsMachineInventoryLayout}.
      */
    private static void maintainInventory(String id, MachineBlockEntity machine) {
        switch (id) {
            case "isolator" -> {
                store(machine, MachineBlockEntity.OUTPUT_SLOT, GeneticsMachineInventoryLayout.Group.FINISHED);
                restockOne(machine, GeneticsMachineInventoryLayout.Group.INPUT_RESERVE, MachineBlockEntity.INPUT_SLOT);
            }
            case "sequencer" -> restockOne(machine, GeneticsMachineInventoryLayout.Group.INPUT_RESERVE, MachineBlockEntity.INPUT_SLOT);
            case "polymeriser" -> {
                recycleOrStorePolymeriserSerum(machine);
                restockOne(machine, GeneticsMachineInventoryLayout.Group.SERUM_RESERVE, MachineBlockEntity.INPUT_SLOT);
            }
            case "inoculator", "splicer" -> {
                store(machine, MachineBlockEntity.OUTPUT_SLOT, GeneticsMachineInventoryLayout.Group.FINISHED);
                ItemStack serum = stack(machine, MachineBlockEntity.CATALYST_SLOT);
                if (!serum.isEmpty() && serumCharge(serum) == 0) {
                    store(machine, MachineBlockEntity.CATALYST_SLOT, GeneticsMachineInventoryLayout.Group.SERUM_EXPENDED);
                }
                restockOne(machine, GeneticsMachineInventoryLayout.Group.INPUT_RESERVE, MachineBlockEntity.INPUT_SLOT);
                restockOne(machine, GeneticsMachineInventoryLayout.Group.SERUM_RESERVE, MachineBlockEntity.CATALYST_SLOT);
            }
            case "analyser" -> {
                store(machine, MachineBlockEntity.OUTPUT_SLOT, GeneticsMachineInventoryLayout.Group.FINISHED);
                restockOne(machine, GeneticsMachineInventoryLayout.Group.INPUT_RESERVE, MachineBlockEntity.INPUT_SLOT);
            }
            case "genepool" -> restockOne(machine, GeneticsMachineInventoryLayout.Group.INPUT_RESERVE, MachineBlockEntity.INPUT_SLOT);
            case "incubator" -> {
                store(machine, MachineBlockEntity.OUTPUT_SLOT, GeneticsMachineInventoryLayout.Group.INCUBATOR_OUTPUT);
                restockOne(machine, GeneticsMachineInventoryLayout.Group.QUEUE, MachineBlockEntity.REAGENT_SLOT);
            }
            case "acclimatiser" -> {
                ItemStack target = stack(machine, MachineBlockEntity.INPUT_SLOT);
                boolean hasReagent = java.util.Arrays.stream(acclimatiserReagentSlots(machine))
                        .mapToObj(slot -> stack(machine, slot)).anyMatch(candidate -> !candidate.isEmpty());
                if (!target.isEmpty() && hasReagent && acclimatiserCandidates(target, machine).isEmpty()) {
                    store(machine, MachineBlockEntity.INPUT_SLOT, GeneticsMachineInventoryLayout.Group.FINISHED);
                }
                restockOne(machine, GeneticsMachineInventoryLayout.Group.INPUT_RESERVE, MachineBlockEntity.INPUT_SLOT);
            }
            default -> {
            }
        }
    }

    private static boolean supplementarySlot(String machineId, MachineBlockEntity machine, int slot, ItemStack stack) {
        return GeneticsMachineInventoryLayout.slot(machine.spec().id(), slot).map(definition -> switch (definition.group()) {
            case INPUT_RESERVE -> switch (machineId) {
                case "isolator", "genepool", "acclimatiser" -> isIndividual(stack);
                case "inoculator", "splicer" -> isDnaManipulable(stack);
                case "analyser" -> isIndividual(stack) || stack.has(BinnieDataComponents.GENETIC_SEQUENCE);
                case "sequencer" -> {
                    GeneticSequenceData sequence = stack.get(BinnieDataComponents.GENETIC_SEQUENCE);
                    yield isMaterial(stack, GeneticMaterialItem.Kind.SEQUENCE) && sequence != null && sequence.sequenced() < 100;
                }
                default -> false;
            };
            case SERUM_RESERVE -> {
                boolean serumItem = isMaterial(stack, GeneticMaterialItem.Kind.SERUM)
                        || isMaterial(stack, GeneticMaterialItem.Kind.SERUM_ARRAY);
                GeneticSerumData serum = stack.get(BinnieDataComponents.GENETIC_SERUM);
                yield serumItem && (!machineId.equals("polymeriser") || serum != null && serum.charges() < 16);
            }
            case QUEUE -> !stack.isEmpty();
            case ACCLIMATISER -> isAcclimatiserReagent(stack);
            case SERUM_EXPENDED, FINISHED, INCUBATOR_OUTPUT -> false;
        }).orElse(false);
    }

    private static boolean isAcclimatiserReagent(ItemStack stack) {
        return stack.is(BotanyItemTags.ACID_FERTILISERS) || stack.is(BotanyItemTags.ALKALINE_FERTILISERS)
                || stack.is(Items.BLAZE_POWDER) || stack.is(Items.BLAZE_ROD) || stack.is(Items.LAVA_BUCKET)
                || stack.is(Items.SNOWBALL) || stack.is(Items.ICE) || stack.is(Items.WATER_BUCKET) || stack.is(Items.SAND);
    }

    private static int[] acclimatiserReagentSlots(MachineBlockEntity machine) {
        List<Integer> supplemental = GeneticsMachineInventoryLayout.slots(machine.spec().id(), GeneticsMachineInventoryLayout.Group.ACCLIMATISER);
        int[] slots = new int[2 + supplemental.size()];
        slots[0] = MachineBlockEntity.CATALYST_SLOT;
        slots[1] = MachineBlockEntity.REAGENT_SLOT;
        for (int index = 0; index < supplemental.size(); index++) slots[index + 2] = supplemental.get(index);
        return slots;
    }

    private static int serumCharge(ItemStack stack) {
        GeneticSerumData serum = stack.get(BinnieDataComponents.GENETIC_SERUM);
        return serum == null ? 0 : serum.charges();
    }

    /**
      * 1.12 Polymeriser processing adds one of sixteen charges per operation.
      * Partial serum returns to the protected process slot; only a fully charged
      * serum is moved into the finished array.
      */
    private static void recycleOrStorePolymeriserSerum(MachineBlockEntity machine) {
        ItemStack output = stack(machine, MachineBlockEntity.OUTPUT_SLOT);
        if (output.isEmpty()) return;
        GeneticSerumData serum = output.get(BinnieDataComponents.GENETIC_SERUM);
        if (serum == null || serum.charges() >= 16) {
            store(machine, MachineBlockEntity.OUTPUT_SLOT, GeneticsMachineInventoryLayout.Group.FINISHED);
            return;
        }
        if (!stack(machine, MachineBlockEntity.INPUT_SLOT).isEmpty()) return;
        machine.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, output.copy());
        machine.inventory().setStackInSlot(MachineBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
    }

    /** Moves one reserve item into an empty active processor slot. */
    private static void restockOne(MachineBlockEntity machine, GeneticsMachineInventoryLayout.Group group, int targetSlot) {
        if (!stack(machine, targetSlot).isEmpty()) return;
        for (int sourceSlot : GeneticsMachineInventoryLayout.slots(machine.spec().id(), group)) {
            ItemStack source = stack(machine, sourceSlot);
            if (source.isEmpty()) continue;
            ItemStack transferred = machine.inventory().extractItem(sourceSlot, 1, false);
            if (!transferred.isEmpty()) machine.inventory().setStackInSlot(targetSlot, transferred);
            return;
        }
    }

    /** Moves a completed stack into the first compatible output slot. */
    private static void store(MachineBlockEntity machine, int sourceSlot, GeneticsMachineInventoryLayout.Group group) {
        ItemStack remaining = stack(machine, sourceSlot);
        if (remaining.isEmpty()) return;
        for (int targetSlot : GeneticsMachineInventoryLayout.slots(machine.spec().id(), group)) {
            ItemStack target = stack(machine, targetSlot);
            if (target.isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                machine.inventory().setStackInSlot(targetSlot, remaining.copyWithCount(moved));
                remaining = remaining.copyWithCount(remaining.getCount() - moved);
            } else if (ItemStack.isSameItemSameComponents(target, remaining) && target.getCount() < target.getMaxStackSize()) {
                int moved = Math.min(remaining.getCount(), target.getMaxStackSize() - target.getCount());
                ItemStack merged = target.copy();
                merged.grow(moved);
                machine.inventory().setStackInSlot(targetSlot, merged);
                remaining = remaining.copyWithCount(remaining.getCount() - moved);
            }
            if (remaining.isEmpty()) break;
        }
        if (remaining.getCount() != stack(machine, sourceSlot).getCount()) {
            machine.inventory().setStackInSlot(sourceSlot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
        }
    }

    @FunctionalInterface private interface Planner {
        Optional<MachineProcessPlan> create(Level level, MachineBlockEntity machine);
    }
    @FunctionalInterface private interface SlotValidator {
        boolean valid(MachineBlockEntity machine, int slot, ItemStack stack);
    }
    private record Adjustment(IChromosome<?> chromosome, int upDelta, int downDelta) {}
    private record AcclimatiserCandidate(ItemStack stack, Adjustment adjustment) {}
    private record IncubatorPlan(FluidStack fluidOutput, FluidStack fluidInput, float lossChance, float tickChance, ItemStack itemOutput) {
        private IncubatorPlan(FluidStack fluidOutput, FluidStack fluidInput, float lossChance, float tickChance) {
            this(fluidOutput, fluidInput, lossChance, tickChance, ItemStack.EMPTY);
        }
    }
}
