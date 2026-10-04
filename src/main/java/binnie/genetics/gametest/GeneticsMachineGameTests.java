package binnie.genetics.gametest;

import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.api.genetics.IFlower;
import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.block.FlowerBlockEntity;
import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.genetics.FlowerSpeciesType;
import binnie.botany.genetics.LegacyFlowerCatalog;
import binnie.botany.registry.BotanyContent;
import binnie.botany.registry.BotanyWorldContent;
import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineProcessError;
import binnie.core.machine.MachineProcessRegistry;
import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.api.GeneticOperations;
import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.genetics.api.GeneticSerumEngineering;
import binnie.genetics.data.AlleleValue;
import binnie.genetics.data.GeneticAlleleData;
import binnie.genetics.data.GeneticSerumData;
import binnie.genetics.data.GeneticKnowledgeData;
import binnie.genetics.data.GeneticKnowledgeEntry;
import binnie.genetics.data.GeneticSpeciesDiscovery;
import binnie.genetics.data.GeneticSequenceData;
import binnie.genetics.data.LaboratoryChargeData;
import binnie.genetics.tracking.GeneticKnowledgeCatalog;
import binnie.genetics.tracking.GeneticKnowledgeSavedData;
import binnie.genetics.item.GeneticMaterialItem;
import binnie.genetics.machine.GeneticsMachineInventoryLayout;
import binnie.genetics.registry.GeneticsContent;
import binnie.genetics.registry.GeneticsFluids;
import binnie.genetics.registry.GeneticsMachines;
import binnie.botany.registry.BotanyMachines;
import binnie.trees.registry.BinnieTreesMachines;
import binnie.bees.block.AlvearyPartType;
import binnie.bees.registry.BinnieBeesWorldContent;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.genetics.menu.PunnettSquareMenu;
import binnie.genetics.network.GeneticsNetwork;
import binnie.genetics.network.SelectDatabaseGenePayload;
import binnie.genetics.network.SelectPunnettChromosomePayload;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.ToleranceType;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.core.genetics.alleles.ButterflyChromosomes;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.engine.genetics.ItemGE;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/** End-to-end tests for registered Genetics machines, their resources, and typed flower material. */
@GameTestHolder("binniecore")
@PrefixGameTestTemplate(false)
public final class GeneticsMachineGameTests {
    private GeneticsMachineGameTests() {}

    /** Proves external pipes discover and fill the normal NeoForge sided fluid capability. */
    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void machineFluidCapabilityAcceptsExternalInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 2);
        MachineBlockEntity machine = placeMachine(helper, pos, "isolator");
        IFluidHandler handler = Capabilities.FluidHandler.BLOCK.getCapability(helper.getLevel(), pos,
                machine.getBlockState(), machine, Direction.NORTH);
        helper.assertTrue(handler != null, "Machine did not expose a sided NeoForge fluid capability");
        helper.assertTrue(handler.fill(ForestryFluids.BIO_ETHANOL.getFluid(125), IFluidHandler.FluidAction.EXECUTE) == 125,
                "External fluid handler rejected a valid input fill");
        helper.assertTrue(machine.tank().getFluidAmount() == 125, "External input did not reach the primary tank");
        helper.succeed();
    }

    /**
      * Exercises every real laboratory fluid route through the public NeoForge
      * capability. This catches the class of defect where a process can work
      * in a direct unit setup but a player cannot pipe its required liquid in
      * or take a produced liquid out.
      */
    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void everyLaboratoryFluidRouteIsPlayerAccessible(GameTestHelper helper) {
        MachineBlockEntity isolator = placeMachine(helper, new BlockPos(1, 2, 2), "isolator");
        MachineBlockEntity genepool = placeMachine(helper, new BlockPos(3, 2, 2), "genepool");
        MachineBlockEntity polymeriser = placeMachine(helper, new BlockPos(5, 2, 2), "polymeriser");
        MachineBlockEntity inoculator = placeMachine(helper, new BlockPos(7, 2, 2), "inoculator");
        MachineBlockEntity incubator = placeMachine(helper, new BlockPos(9, 2, 2), "incubator");
        MachineBlockEntity sequencer = placeMachine(helper, new BlockPos(11, 2, 2), "sequencer");

        helper.assertTrue(!incubator.getBlockState().canOcclude(),
                "Genetics machine occlusion hid the wall behind its cutout model");
        helper.assertTrue(!BotanyMachines.REGISTRY.block("tileworker").get().defaultBlockState().canOcclude(),
                "Botany tileworker occluded wall faces behind its cutout model");
        helper.assertTrue(!BinnieTreesMachines.REGISTRY.block("woodworker").get().defaultBlockState().canOcclude(),
                "Trees machines occluded wall faces behind their cutout models");
        helper.assertTrue(BinnieBeesWorldContent.alveary(AlvearyPartType.FRAME).get().defaultBlockState().canOcclude(),
                "Full-cube alveary blocks unexpectedly lost normal occlusion");
        helper.assertTrue(isolator.fluidHandler().fill(ForestryFluids.BIO_ETHANOL.getFluid(10), IFluidHandler.FluidAction.EXECUTE) == 10,
                "Isolator did not accept Bio Ethanol from a pipe");
        helper.assertTrue(genepool.fluidHandler().fill(ForestryFluids.BIO_ETHANOL.getFluid(12), IFluidHandler.FluidAction.EXECUTE) == 12,
                "Genepool did not accept Bio Ethanol from a pipe");
        helper.assertTrue(polymeriser.fluidHandler().fill(GeneticsFluids.POLYMERISING_BACTERIA.stack(10), IFluidHandler.FluidAction.EXECUTE) == 10,
                "Polymeriser did not route Polymerising Bacteria to its upper input tank");
        helper.assertTrue(polymeriser.fluidHandler().fill(GeneticsFluids.RAW_DNA.stack(50), IFluidHandler.FluidAction.EXECUTE) == 50,
                "Polymeriser did not route Raw DNA to its lower input tank");
        helper.assertTrue(polymeriser.fluidHandler().fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 0,
                "Polymeriser accepted an invalid liquid into a source-defined tank");
        helper.assertTrue(inoculator.fluidHandler().fill(GeneticsFluids.VECTOR_BACTERIA.stack(15), IFluidHandler.FluidAction.EXECUTE) == 15,
                "Inoculator did not accept Vector Bacteria from a pipe");
        helper.assertTrue(incubator.fluidHandler().fill(new FluidStack(Fluids.WATER, 25), IFluidHandler.FluidAction.EXECUTE) == 25,
                "Incubator did not accept Water from a pipe");
        helper.assertTrue(incubator.fluidHandler().drain(25, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "Incubator let a pipe extract recipe input Water before processing it");
        helper.assertTrue(incubator.fluidHandler().drain(new FluidStack(Fluids.WATER, 25), IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "Incubator let a pipe extract recipe input Water by fluid type before processing it");
        helper.assertTrue(incubator.tank().getFluidAmount() == 25,
                "Incubator did not retain the Water required by its recipe");
        helper.assertTrue(sequencer.fluidHandler().fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 0,
                "Sequencer unexpectedly accepted a fluid despite having no source tank");

        helper.assertTrue(incubator.outputTank().fill(GeneticsFluids.GROWTH_MEDIUM.stack(25), IFluidHandler.FluidAction.EXECUTE) == 25,
                "Incubator could not store a process-produced Growth Medium output");
        helper.assertTrue(incubator.fluidHandler().drain(25, IFluidHandler.FluidAction.EXECUTE).is(GeneticsFluids.GROWTH_MEDIUM.fluid()),
                "Incubator could not expose a process-produced output to an external pipe");
        helper.succeed();
    }

    /** The real creative/Forestry queen route used by the client smoke test. */
    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 400)
    public static void isolatorProcessesARealForestryQueen(GameTestHelper helper) {
        MachineBlockEntity isolator = placeMachine(helper, new BlockPos(2, 2, 2), "isolator");
        ItemStack queen = SpeciesUtil.BEE_TYPE.get().getDefaultSpecies().createIndividual().createStack(BeeLifeStage.QUEEN);
        fillEnergy(isolator);
        fillPrimary(isolator, ForestryFluids.BIO_ETHANOL.getFluid(10));
        isolator.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, queen);
        isolator.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, new ItemStack(GeneticsContent.EMPTY_SEQUENCER.get()));
        isolator.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(GeneticsContent.ENZYME.get()));
        assertPlanReady(helper, isolator, "creative Forestry queen");
        helper.startSequence().thenExecute(() -> {
            advanceMachine(helper, isolator, 4_800, "Isolator did not complete a real Forestry queen");
            GeneticSequenceData sequence = takeOutput(isolator).get(BinnieDataComponents.GENETIC_SEQUENCE);
            helper.assertTrue(sequence != null && sequence.sequenced() == 0
                            && sequence.allele().speciesType().equals(ForestrySpeciesTypes.BEE),
                    "Queen Isolator output was not an unsequenced typed bee vial");
            helper.assertTrue(isolator.tank().getFluidAmount() == 0,
                    "Queen Isolator did not consume exactly 10 mB Bio Ethanol");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 1_800)
    public static void flowerPipelineUsesRealConsumablesFluidsAndPlants(GameTestHelper helper) {
        MachineBlockEntity isolator = placeMachine(helper, new BlockPos(1, 2, 2), "isolator");
        MachineBlockEntity sequencer = placeMachine(helper, new BlockPos(3, 2, 2), "sequencer");
        MachineBlockEntity polymeriser = placeMachine(helper, new BlockPos(5, 2, 2), "polymeriser");
        MachineBlockEntity inoculator = placeMachine(helper, new BlockPos(7, 2, 2), "inoculator");
        fillEnergy(isolator, sequencer, polymeriser, inoculator);
        fillPrimary(isolator, ForestryFluids.BIO_ETHANOL.getFluid(10));
        fillPrimary(polymeriser, GeneticsFluids.POLYMERISING_BACTERIA.stack(10));
        fillSecondary(polymeriser, GeneticsFluids.RAW_DNA.stack(50));
        fillPrimary(inoculator, GeneticsFluids.VECTOR_BACTERIA.stack(15));
        sequencer.setOwner(UUID.fromString("7b4e30c3-3c8e-43bf-8160-6b2e8f3fe57a"));
        isolator.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, new ItemStack(GeneticsContent.EMPTY_SEQUENCER.get()));
        isolator.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(GeneticsContent.ENZYME.get()));
        ItemStack donor = findDonorWithDistinctRecipient(helper, isolator);
        isolator.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, donor);
        assertPlanReady(helper, isolator, "isolator");

        helper.startSequence()
                .thenExecute(() -> {
                    advanceMachine(helper, isolator, 4_800, "Isolator did not complete at its source duration");
                    helper.assertTrue(isolator.tank().getFluidAmount() == 0, "Isolator did not consume exactly 10 mB ethanol");
                    helper.assertTrue(isolator.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT).isEmpty(), "Empty vial was not consumed");
                    helper.assertTrue(charge(isolator.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT)) == 5_000,
                            "Isolator did not consume the source half-cell enzyme charge");
                    ItemStack sequenceVial = takeOutput(isolator);
                    GeneticSequenceData isolatedSequence = sequenceVial.get(BinnieDataComponents.GENETIC_SEQUENCE);
                    GeneticAlleleData data = isolatedSequence == null ? null : isolatedSequence.allele();
                    helper.assertTrue(isolatedSequence != null && isolatedSequence.sequenced() == 0 && data != null,
                            "Isolator output lost its unsequenced typed allele data");
                    IChromosome<?> extractedChromosome = FlowerSpeciesType.get().getKaryotype().getChromosome(data.chromosome());
                    helper.assertTrue(extractedChromosome != null, "Isolator selected an unknown flower chromosome");
                    ItemStack recipientStack = findDifferentRecipient(helper, data, extractedChromosome);
                    IFlower recipient = requireFlower(helper, recipientStack, "Selected recipient");
                    IChromosome<?> unrelated = extractedChromosome == FlowerChromosomes.FERTILITY
                            ? FlowerChromosomes.LIFESPAN : FlowerChromosomes.FERTILITY;
                    AllelePair<?> unrelatedBefore = recipient.getGenome().getAllelePair(unrelated);
                    sequencer.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, sequenceVial);
                    sequencer.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(GeneticsContent.FLUORESCENT_DYE.get()));

                    advanceMachine(helper, sequencer, 19_200, "Sequencer did not complete at its source duration");
                    helper.assertTrue(charge(sequencer.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT)) == 6_000,
                            "Sequencer did not consume the source 0.4 fluorescent-dye charge");
                    helper.assertTrue(sequencer.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT)
                            .is(GeneticsContent.EMPTY_SEQUENCER.get()), "Sequencer did not return an empty vial");
                    helper.assertTrue(sequencer.inventory().getStackInSlot(MachineBlockEntity.SECONDARY_OUTPUT_SLOT).isEmpty(),
                            "Sequencer retained a completed sequence vial instead of consuming it");
                    ItemStack selectedSerum = GeneticSerumEngineering.applyGene(
                            GeneticsContent.GENETIC_MATERIAL.get().blank(GeneticMaterialItem.Kind.SERUM),
                            GeneticKnowledgeEntry.fromAllele(data));
                    helper.assertTrue(!selectedSerum.isEmpty(), "Database gene selection did not create an uncharged serum");
                    polymeriser.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, selectedSerum);
                    polymeriser.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, new ItemStack(Items.GOLD_NUGGET));

                    advanceMachine(helper, polymeriser, 480, "Polymeriser did not complete with its charged source catalyst");
                    helper.assertTrue(polymeriser.tank().getFluidAmount() == 0, "Polymeriser did not consume exactly 10 mB Polymerising Bacteria");
                    helper.assertTrue(polymeriser.secondaryTank().getFluidAmount() == 0, "Polymeriser did not consume exactly 50 mB Raw DNA");
                    helper.assertTrue(charge(polymeriser.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT)) == 6_000,
                            "Polymeriser did not retain the source partially charged gold catalyst");
                    ItemStack expectedSerum = takeOutput(polymeriser);
                    GeneticSerumData serumData = expectedSerum.get(BinnieDataComponents.GENETIC_SERUM);
                    helper.assertTrue(serumData != null && serumData.charges() == 1,
                            "Polymeriser did not add exactly one of the source sixteen charges");
                    inoculator.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, recipientStack);
                    inoculator.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, expectedSerum);

                    advanceMachine(helper, inoculator, 12_000, "Inoculator did not complete at its source duration");
                    helper.assertTrue(inoculator.tank().getFluidAmount() == 0, "Inoculator did not consume exactly 15 mB vector bacteria");
                    ItemStack modifiedStack = takeOutput(inoculator);
                    IFlower modified = requireFlower(helper, modifiedStack, "Inoculator output");
                    GeneticSerumData serum = expectedSerum.get(BinnieDataComponents.GENETIC_SERUM);
                    helper.assertTrue(serum != null, "Expected serum data disappeared");
                    Object expectedValue = serum.alleles().getFirst().value().javaValue();
                    AllelePair<?> changed = modified.getGenome().getAllelePair(extractedChromosome);
                    helper.assertTrue(changed.active().value().equals(expectedValue) && changed.inactive().value().equals(expectedValue),
                            "Target chromosome was not inoculated into both allele states");
                    IChromosome<?> unchangedChromosome = extractedChromosome == FlowerChromosomes.FERTILITY
                            ? FlowerChromosomes.LIFESPAN : FlowerChromosomes.FERTILITY;
                    helper.assertTrue(modified.getGenome().getAllelePair(unchangedChromosome).equals(unrelatedBefore),
                            "Inoculation corrupted an unrelated chromosome");
                    GeneticSerumData remaining = inoculator.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT)
                            .get(BinnieDataComponents.GENETIC_SERUM);
                    helper.assertTrue(remaining != null && remaining.charges() == 0, "Inoculator did not consume one serum charge");
                    helper.assertTrue(modified.isAnalyzed() == requireFlower(helper, recipientStack, "Recipient").isAnalyzed(),
                            "Inoculation changed analysis state");

                    BlockPos plantedPos = new BlockPos(9, 2, 2);
                    helper.setBlock(plantedPos.below(), BotanyWorldContent.FLOWERBED.get());
                    helper.setBlock(plantedPos, BotanyWorldContent.FLOWER_BLOCK.get());
                    FlowerBlockEntity planted = requirePlanted(helper, plantedPos);
                    planted.setFlower(modified.createStack(FlowerLifeStage.FLOWER), null);
                    CompoundTag saved = planted.saveWithoutMetadata(helper.getLevel().registryAccess());
                    FlowerBlockEntity restored = new FlowerBlockEntity(helper.absolutePos(plantedPos), helper.getBlockState(plantedPos));
                    restored.setLevel(helper.getLevel());
                    restored.loadWithComponents(saved, helper.getLevel().registryAccess());
                    IFlower afterReload = restored.getFlower();
                    helper.assertTrue(afterReload != null && afterReload.getGenome().getAllelePair(extractedChromosome).equals(changed),
                            "Modified planted genome changed after save/reload");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 400)
    public static void crossSpeciesSerumIsRejectedByActualInoculator(GameTestHelper helper) {
        ItemStack flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual().createStack(FlowerLifeStage.POLLEN);
        GeneticAlleleData beeAllele = new GeneticAlleleData(ForestrySpeciesTypes.BEE,
                ResourceLocation.fromNamespaceAndPath("forestry", "fertility"),
                new AlleleValue.IntValue(2), true, GeneticAlleleData.Origin.ACTIVE);
        ItemStack wrongSerum = GeneticsContent.GENETIC_MATERIAL.get().serum(new GeneticSerumData(List.of(beeAllele), 100, 16), false);
        MachineBlockEntity inoculator = placeMachine(helper, new BlockPos(2, 2, 2), "inoculator");
        fillEnergy(inoculator);
        fillPrimary(inoculator, GeneticsFluids.VECTOR_BACTERIA.stack(15));
        inoculator.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, flower.copy());
        inoculator.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, wrongSerum.copy());
        helper.startSequence().thenExecuteAfter(40, () -> {
            helper.assertTrue(inoculator.progress() == 0, "Cross-species serum started processing");
            helper.assertTrue(inoculator.processError() == MachineProcessError.INVALID_SERUM_MISMATCH,
                    "Cross-species serum did not expose the source mismatch diagnostic; got " + inoculator.processError());
            helper.assertTrue(inoculator.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT).isEmpty(), "Cross-species serum produced output");
            helper.assertTrue(inoculator.tank().getFluidAmount() == 15, "Rejected serum consumed vector bacteria");
            helper.assertTrue(ItemStack.isSameItemSameComponents(flower, inoculator.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT)), "Rejected target was consumed");
            helper.assertTrue(ItemStack.isSameItemSameComponents(wrongSerum, inoculator.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT)), "Rejected serum was consumed");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 400)
    public static void inoculatorRejectsNonManipulableFlowerStage(GameTestHelper helper) {
        IFlower flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        ItemStack matureFlower = flower.createStack(FlowerLifeStage.FLOWER);
        GeneticAlleleData allele = GeneticOperations.sample(matureFlower,
                FlowerChromosomes.STEM.id(), GeneticAlleleData.Origin.ACTIVE);
        ItemStack serum = GeneticsContent.GENETIC_MATERIAL.get().serum(
                new GeneticSerumData(List.of(allele), 100, 16), false);
        MachineBlockEntity inoculator = placeMachine(helper, new BlockPos(2, 2, 2), "inoculator");
        fillEnergy(inoculator);
        fillPrimary(inoculator, GeneticsFluids.VECTOR_BACTERIA.stack(15));
        inoculator.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, matureFlower.copy());
        inoculator.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, serum.copy());
        helper.startSequence().thenExecuteAfter(40, () -> {
            helper.assertTrue(inoculator.progress() == 0, "Mature flower started an Inoculator process");
            helper.assertTrue(inoculator.processError() == MachineProcessError.INVALID_INPUT,
                    "Mature flower did not expose the source life-stage diagnostic; got " + inoculator.processError());
            helper.assertTrue(inoculator.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT).isEmpty(),
                    "Rejected mature flower produced an output");
            helper.assertTrue(inoculator.tank().getFluidAmount() == 15,
                    "Rejected mature flower consumed Vector Bacteria");
            helper.assertTrue(ItemStack.isSameItemSameComponents(matureFlower,
                            inoculator.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT)),
                    "Rejected mature flower was consumed or changed");
            helper.assertTrue(ItemStack.isSameItemSameComponents(serum,
                            inoculator.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT)),
                    "Rejected mature flower consumed serum charge");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 600)
    public static void acclimatiserChangesOnlyPhToleranceUsingReagent(GameTestHelper helper) {
        IFlower original = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        ItemStack target = original.createStack(FlowerLifeStage.FLOWER);
        AllelePair<?> oldPh = original.getGenome().getAllelePair(FlowerChromosomes.PH_TOLERANCE);
        AllelePair<?> oldHumidity = original.getGenome().getAllelePair(FlowerChromosomes.HUMIDITY_TOLERANCE);
        MachineBlockEntity acclimatiser = placeMachine(helper, new BlockPos(2, 2, 2), "acclimatiser");
        fillEnergy(acclimatiser);
        acclimatiser.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, target);
        acclimatiser.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(BotanyContent.ACID_FERTILISER.get(), 64));
        assertIndefiniteHandler(helper, acclimatiser, "acclimatiser");
        helper.startSequence().thenExecute(() -> {
            int initialEnergy = acclimatiser.energy().getEnergyStored();
            advanceUntil(helper, acclimatiser, 5_000,
                    () -> !requireFlower(helper, acclimatiser.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT), "Acclimatiser target")
                            .getGenome().getAllelePair(FlowerChromosomes.PH_TOLERANCE).equals(oldPh),
                    "Acclimatiser did not make its source one-in-100-tick attempt");
            IFlower changed = requireFlower(helper, acclimatiser.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT), "Acclimatiser target");
            helper.assertTrue(!changed.getGenome().getAllelePair(FlowerChromosomes.PH_TOLERANCE).equals(oldPh), "pH tolerance did not change");
            helper.assertTrue(changed.getGenome().getAllelePair(FlowerChromosomes.HUMIDITY_TOLERANCE).equals(oldHumidity), "Humidity tolerance was corrupted");
            helper.assertTrue(acclimatiser.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).getCount() == 63,
                    "Acclimatiser did not consume exactly one successful reagent");
            helper.assertTrue(acclimatiser.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT).isEmpty(),
                    "Indefinite Acclimatiser incorrectly emitted a timed-process output");
            helper.assertTrue(acclimatiser.energy().getEnergyStored() < initialEnergy,
                    "Acclimatiser did not consume source 2 FE/t while active");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 800)
    public static void acclimatiserSupportsTemperatureAndHumidityReagents(GameTestHelper helper) {
        IFlower temperatureOriginal = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        IFlower humidityOriginal = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        AllelePair<?> oldTemperature = temperatureOriginal.getGenome().getAllelePair(FlowerChromosomes.TEMPERATURE_TOLERANCE);
        AllelePair<?> oldHumidity = humidityOriginal.getGenome().getAllelePair(FlowerChromosomes.HUMIDITY_TOLERANCE);
        MachineBlockEntity temperature = placeMachine(helper, new BlockPos(2, 2, 2), "acclimatiser");
        MachineBlockEntity humidity = placeMachine(helper, new BlockPos(5, 2, 2), "acclimatiser");
        temperature.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, temperatureOriginal.createStack(FlowerLifeStage.FLOWER));
        temperature.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(Items.BLAZE_POWDER, 64));
        humidity.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, humidityOriginal.createStack(FlowerLifeStage.FLOWER));
        humidity.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(Items.SAND, 64));
        assertIndefiniteHandler(helper, temperature, "temperature acclimatiser");
        assertIndefiniteHandler(helper, humidity, "humidity acclimatiser");

        helper.startSequence().thenExecute(() -> {
            boolean temperatureChanged = false;
            boolean humidityChanged = false;
            for (int tick = 0; tick < 5_000 && (!temperatureChanged || !humidityChanged); tick++) {
                fillEnergy(temperature, humidity);
                if (!temperatureChanged) {
                    MachineBlockEntity.serverTick(helper.getLevel(), temperature.getBlockPos(), temperature.getBlockState(), temperature);
                    temperatureChanged = !requireFlower(helper, temperature.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT), "Temperature acclimatiser target")
                            .getGenome().getAllelePair(FlowerChromosomes.TEMPERATURE_TOLERANCE).equals(oldTemperature);
                }
                if (!humidityChanged) {
                    MachineBlockEntity.serverTick(helper.getLevel(), humidity.getBlockPos(), humidity.getBlockState(), humidity);
                    humidityChanged = !requireFlower(helper, humidity.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT), "Humidity acclimatiser target")
                            .getGenome().getAllelePair(FlowerChromosomes.HUMIDITY_TOLERANCE).equals(oldHumidity);
                }
            }
            helper.assertTrue(temperatureChanged, "Blaze Powder did not change flower temperature tolerance");
            helper.assertTrue(humidityChanged, "Sand did not change flower humidity tolerance");
            helper.assertTrue(temperature.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).getCount() == 63,
                    "Temperature acclimatiser did not consume exactly one successful reagent; got "
                            + temperature.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).getCount());
            helper.assertTrue(humidity.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).getCount() == 63,
                    "Humidity acclimatiser did not consume exactly one successful reagent; got "
                            + humidity.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).getCount());
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 800)
    public static void sourceLaboratoryReserveAndCompletedGroupsMoveRealStacks(GameTestHelper helper) {
        MachineBlockEntity isolator = placeMachine(helper, new BlockPos(2, 2, 2), "isolator");
        ItemStack flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual().createStack(FlowerLifeStage.FLOWER);
        ResourceLocation isolatorId = isolator.spec().id();
        int isolatorReserve = GeneticsMachineInventoryLayout.slots(isolatorId, GeneticsMachineInventoryLayout.Group.INPUT_RESERVE).getFirst();
        int isolatorFinished = GeneticsMachineInventoryLayout.slots(isolatorId, GeneticsMachineInventoryLayout.Group.FINISHED).getFirst();
        helper.assertTrue(isolator.inventory().getSlots() == 15, "Isolator did not receive its source-sized inventory");
        isolator.inventory().setStackInSlot(isolatorReserve, flower);

        MachineBlockEntity incubator = placeMachine(helper, new BlockPos(5, 2, 2), "incubator");
        ResourceLocation incubatorId = incubator.spec().id();
        int queue = GeneticsMachineInventoryLayout.slots(incubatorId, GeneticsMachineInventoryLayout.Group.QUEUE).getFirst();
        int incubatorOutput = GeneticsMachineInventoryLayout.slots(incubatorId, GeneticsMachineInventoryLayout.Group.INCUBATOR_OUTPUT).getFirst();
        incubator.inventory().setStackInSlot(queue, new ItemStack(GeneticsContent.GROWTH_MEDIUM.get()));

        MachineBlockEntity acclimatiser = placeMachine(helper, new BlockPos(8, 2, 2), "acclimatiser");
        ResourceLocation acclimatiserId = acclimatiser.spec().id();
        int thirdReagent = GeneticsMachineInventoryLayout.slots(acclimatiserId, GeneticsMachineInventoryLayout.Group.ACCLIMATISER).getFirst();
        IFlower original = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        acclimatiser.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, original.createStack(FlowerLifeStage.FLOWER));
        acclimatiser.inventory().setStackInSlot(thirdReagent, new ItemStack(BotanyContent.ACID_FERTILISER.get(), 4));

        helper.startSequence().thenExecute(() -> {
            MachineBlockEntity.serverTick(helper.getLevel(), isolator.getBlockPos(), isolator.getBlockState(), isolator);
            helper.assertTrue(ItemStack.isSameItemSameComponents(flower, isolator.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT)),
                    "Isolator did not restock its source reserve into the active target");
            ItemStack result = GeneticsContent.GENETIC_MATERIAL.get().gene(GeneticOperations.sample(flower,
                    FlowerChromosomes.STEM.id(), GeneticAlleleData.Origin.ACTIVE));
            isolator.inventory().setStackInSlot(MachineBlockEntity.OUTPUT_SLOT, result);
            MachineBlockEntity.serverTick(helper.getLevel(), isolator.getBlockPos(), isolator.getBlockState(), isolator);
            helper.assertTrue(ItemStack.isSameItemSameComponents(result, isolator.inventory().getStackInSlot(isolatorFinished)),
                    "Isolator did not store its completed sample in the source finished array");
            helper.assertTrue(isolator.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT).isEmpty(),
                    "Isolator retained a completed sample after source storage transfer");

            MachineBlockEntity.serverTick(helper.getLevel(), incubator.getBlockPos(), incubator.getBlockState(), incubator);
            helper.assertTrue(incubator.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).is(GeneticsContent.GROWTH_MEDIUM.get()),
                    "Incubator did not restock its queued input into the active incubator slot");
            ItemStack enzyme = new ItemStack(GeneticsContent.ENZYME.get());
            incubator.inventory().setStackInSlot(MachineBlockEntity.OUTPUT_SLOT, enzyme);
            MachineBlockEntity.serverTick(helper.getLevel(), incubator.getBlockPos(), incubator.getBlockState(), incubator);
            helper.assertTrue(incubator.inventory().getStackInSlot(incubatorOutput).is(GeneticsContent.ENZYME.get()),
                    "Incubator did not store an item recipe result in its source output array");

            fillEnergy(acclimatiser);
            for (int tick = 0; tick < 5_000 && acclimatiser.inventory().getStackInSlot(thirdReagent).getCount() == 4; tick++) {
                fillEnergy(acclimatiser);
                MachineBlockEntity.serverTick(helper.getLevel(), acclimatiser.getBlockPos(), acclimatiser.getBlockState(), acclimatiser);
            }
            helper.assertTrue(acclimatiser.inventory().getStackInSlot(thirdReagent).getCount() == 3,
                    "Acclimatiser did not consume a successful source third reagent slot");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 400)
    public static void legacySixSlotMachineSaveMigratesToSourceSizedInventory(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        MachineBlockEntity original = placeMachine(helper, relative, "genepool");
        ItemStack flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual().createStack(FlowerLifeStage.FLOWER);
        original.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, flower);

        helper.startSequence().thenExecute(() -> {
            CompoundTag saved = original.saveWithoutMetadata(helper.getLevel().registryAccess());
            // This is the matching ItemStackHandler size written by every
            // pre-source-array Binnie machine save.
            saved.getCompound("Inventory").putInt("Size", 6);
            BlockPos absolute = helper.absolutePos(relative);
            helper.getLevel().removeBlockEntity(absolute);
            var created = original.getType().create(absolute, helper.getBlockState(relative));
            helper.assertTrue(created instanceof MachineBlockEntity, "Could not recreate legacy Genepool block entity");
            MachineBlockEntity restored = (MachineBlockEntity) created;
            restored.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.getLevel().setBlockEntity(restored);
            helper.assertTrue(restored.inventory().getSlots() == 12,
                    "Legacy six-slot Genepool save shrank the source-sized inventory during load");
            helper.assertTrue(ItemStack.isSameItemSameComponents(flower, restored.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT)),
                    "Legacy compatibility-slot contents changed during source-array migration");
            MachineBlockEntity.serverTick(helper.getLevel(), restored.getBlockPos(), restored.getBlockState(), restored);
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 700)
    public static void machineProcessSurvivesBlockEntityReload(GameTestHelper helper) {
        IFlower flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        GeneticAlleleData gene = GeneticOperations.sample(flower.createStack(FlowerLifeStage.FLOWER), FlowerChromosomes.STEM.id(), GeneticAlleleData.Origin.INACTIVE);
        ItemStack geneStack = GeneticsContent.GENETIC_MATERIAL.get().sequence(GeneticSequenceData.unsequenced(gene, 100));
        BlockPos relative = new BlockPos(2, 2, 2);
        MachineBlockEntity[] active = {placeMachine(helper, relative, "sequencer")};
        active[0].setOwner(UUID.fromString("32f3d229-1370-4bc1-b155-c089a11bb4ed"));
        fillEnergy(active[0]);
        active[0].inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, geneStack);
        active[0].inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(GeneticsContent.FLUORESCENT_DYE.get()));
        assertPlanReady(helper, active[0], "sequencer");
        int[] progressBeforeReload = {0};
        helper.startSequence()
                .thenExecute(() -> {
                    advanceTicks(helper, active[0], 250);
                    helper.assertTrue(active[0].progress() >= 250, "Sequencer never began processing");
                    progressBeforeReload[0] = active[0].progress();
                    int expectedCharge = LaboratoryChargeData.FULL - 4_000 * progressBeforeReload[0] / 19_200;
                    helper.assertTrue(charge(active[0].inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT)) == expectedCharge,
                            "Sequencer dye did not drain with source progress before save/reload");
                    CompoundTag saved = active[0].saveWithoutMetadata(helper.getLevel().registryAccess());
                    BlockPos absolute = helper.absolutePos(relative);
                    helper.getLevel().removeBlockEntity(absolute);
                    var created = active[0].getType().create(absolute, helper.getBlockState(relative));
                    helper.assertTrue(created instanceof MachineBlockEntity, "Could not recreate sequencer block entity");
                    MachineBlockEntity restored = (MachineBlockEntity) created;
                    restored.loadWithComponents(saved, helper.getLevel().registryAccess());
                    helper.getLevel().setBlockEntity(restored);
                    active[0] = restored;
                    helper.assertTrue(restored.progress() == progressBeforeReload[0], "Progress changed during reload");
                    GeneticSequenceData restoredSequence = restored.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT)
                            .get(BinnieDataComponents.GENETIC_SEQUENCE);
                    helper.assertTrue(restoredSequence != null && restoredSequence.allele().equals(gene)
                                    && restoredSequence.sequenced() == 100 * progressBeforeReload[0] / 19_200,
                            "Partial sequence state changed during reload");
                    helper.assertTrue(restored.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).is(GeneticsContent.FLUORESCENT_DYE.get()), "Dye changed during reload");
                    helper.assertTrue(charge(restored.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT)) == expectedCharge,
                            "Partial Sequencer dye charge changed during save/reload");
                    advanceMachine(helper, restored, 19_200, "Reloaded sequencer did not resume through its source duration");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 600)
    public static void sequencerProgressivelyDrainsPartialDyeAndPausesWithoutReset(GameTestHelper helper) {
        IFlower flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        GeneticAlleleData gene = GeneticOperations.sample(flower.createStack(FlowerLifeStage.FLOWER),
                FlowerChromosomes.STEM.id(), GeneticAlleleData.Origin.ACTIVE);
        MachineBlockEntity sequencer = placeMachine(helper, new BlockPos(2, 2, 2), "sequencer");
        sequencer.setOwner(UUID.fromString("9e38ccae-682b-4910-874e-bfbe725ee7ac"));
        sequencer.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT,
                GeneticsContent.GENETIC_MATERIAL.get().sequence(GeneticSequenceData.unsequenced(gene, 100)));
        ItemStack dye = new ItemStack(GeneticsContent.FLUORESCENT_DYE.get());
        dye.set(BinnieDataComponents.LABORATORY_CHARGE, new LaboratoryChargeData(50));
        sequencer.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, dye);

        helper.startSequence().thenExecute(() -> {
            advanceTicks(helper, sequencer, 300);
            int pausedProgress = sequencer.progress();
            helper.assertTrue(pausedProgress > 0 && charge(sequencer.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT)) == 0,
                    "Sequencer did not consume its partial dye cell during source progress");
            advanceTicks(helper, sequencer, 20);
            helper.assertTrue(sequencer.progress() == pausedProgress,
                    "Sequencer reset or advanced after its progressive dye charge was exhausted");
            GeneticSequenceData partial = sequencer.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT)
                    .get(BinnieDataComponents.GENETIC_SEQUENCE);
            helper.assertTrue(partial != null && partial.sequenced() == 100 * pausedProgress / 19_200,
                    "Sequencer did not preserve its partial percentage while paused for dye");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 700)
    public static void genepoolConsumesIndividualEthanolAndEnzymeToProduceRawDna(GameTestHelper helper) {
        MachineBlockEntity genepool = placeMachine(helper, new BlockPos(2, 2, 2), "genepool");
        fillEnergy(genepool);
        genepool.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT,
                FlowerSpeciesType.get().getDefaultSpecies().createIndividual().createStack(FlowerLifeStage.FLOWER));
        genepool.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(GeneticsContent.ENZYME.get()));
        fillPrimary(genepool, ForestryFluids.BIO_ETHANOL.getFluid(12));
        assertPlanReady(helper, genepool, "genepool");
        helper.succeedWhen(() -> {
            helper.assertTrue(genepool.outputTank().getFluid().is(GeneticsFluids.RAW_DNA.fluid()) && genepool.outputTank().getFluidAmount() == 10,
                    "Genepool did not produce exactly 10 mB Raw DNA for a flower");
            helper.assertTrue(genepool.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT).isEmpty(), "Genepool did not destroy its input");
            helper.assertTrue(genepool.tank().getFluidAmount() == 0, "Genepool did not consume exactly 12 mB ethanol");
            helper.assertTrue(charge(genepool.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT)) == 7_500,
                    "Genepool did not consume the source quarter-cell enzyme charge");
        });
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 800)
    public static void polymeriserDrainsFluidsProgressivelyAndResumesAfterRefill(GameTestHelper helper) {
        IFlower flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        GeneticAlleleData allele = GeneticOperations.sample(flower.createStack(FlowerLifeStage.FLOWER),
                FlowerChromosomes.STEM.id(), GeneticAlleleData.Origin.ACTIVE);
        MachineBlockEntity polymeriser = placeMachine(helper, new BlockPos(2, 2, 2), "polymeriser");
        polymeriser.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT,
                GeneticsContent.GENETIC_MATERIAL.get().serum(new GeneticSerumData(List.of(allele), 100, 0), false));
        ItemStack depletedGold = new ItemStack(Items.GOLD_NUGGET);
        depletedGold.set(BinnieDataComponents.LABORATORY_CHARGE, new LaboratoryChargeData(0));
        polymeriser.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, depletedGold);
        fillPrimary(polymeriser, GeneticsFluids.POLYMERISING_BACTERIA.stack(1));
        fillSecondary(polymeriser, GeneticsFluids.RAW_DNA.stack(1));
        assertPlanReady(helper, polymeriser, "polymeriser with a partial initial fluid supply");

        helper.startSequence().thenExecute(() -> {
            // Raw DNA drains 50 mB across 2,400 ticks, so its 1 mB initial
            // supply is exhausted first. Bacteria remains in the upper tank
            // until its later 240-tick first drain.
            advanceTicks(helper, polymeriser, 95);
            helper.assertTrue(polymeriser.secondaryTank().getFluidAmount() == 0,
                    "Polymeriser did not make its first progressive Raw DNA drain");
            boolean paused = false;
            for (int tick = 0; tick < 48; tick++) {
                int before = polymeriser.progress();
                advanceTicks(helper, polymeriser, 1);
                if (polymeriser.progress() == before) {
                    paused = true;
                    break;
                }
            }
            helper.assertTrue(paused,
                    "Polymeriser did not pause when its next progressive Raw DNA drain was unavailable");
            helper.assertTrue(polymeriser.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT).getCount() == 1,
                    "Polymeriser consumed input while paused for a progressive fluid refill");
            helper.assertTrue(charge(polymeriser.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT)) == 0,
                    "Depleted optional gold incorrectly blocked or regained charge");
            fillPrimary(polymeriser, GeneticsFluids.POLYMERISING_BACTERIA.stack(9));
            fillSecondary(polymeriser, GeneticsFluids.RAW_DNA.stack(49));
            advanceMachine(helper, polymeriser, 2_400,
                    "Polymeriser did not resume and complete after its source fluids were restored");
            helper.assertTrue(polymeriser.tank().getFluidAmount() == 0 && polymeriser.secondaryTank().getFluidAmount() == 0,
                    "Polymeriser did not consume exact total source fluids after a refill");
            GeneticSerumData partial = polymeriser.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT)
                    .get(BinnieDataComponents.GENETIC_SERUM);
            helper.assertTrue(partial != null && partial.charges() == 1,
                    "Polymeriser did not add exactly one serum charge per operation");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 200)
    public static void polymeriserCyclesUntilFullAndStoresOnlySixteenChargeSerum(GameTestHelper helper) {
        IFlower flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        GeneticAlleleData allele = GeneticOperations.sample(flower.createStack(FlowerLifeStage.FLOWER),
                FlowerChromosomes.STEM.id(), GeneticAlleleData.Origin.ACTIVE);
        MachineBlockEntity polymeriser = placeMachine(helper, new BlockPos(2, 2, 2), "polymeriser");
        polymeriser.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT,
                GeneticsContent.GENETIC_MATERIAL.get().serum(new GeneticSerumData(List.of(allele), 100, 0), false));
        fillPrimary(polymeriser, GeneticsFluids.POLYMERISING_BACTERIA.stack(160));
        fillSecondary(polymeriser, GeneticsFluids.RAW_DNA.stack(800));

        List<Integer> finished = GeneticsMachineInventoryLayout.slots(polymeriser.spec().id(),
                GeneticsMachineInventoryLayout.Group.FINISHED);
        advanceUntilPowered(helper, polymeriser, 38_420,
                () -> finished.stream().map(slot -> polymeriser.inventory().getStackInSlot(slot))
                        .map(stack -> stack.get(BinnieDataComponents.GENETIC_SERUM))
                        .anyMatch(serum -> serum != null && serum.charges() == 16),
                "Polymeriser did not recycle its serum through sixteen source charging operations");
        helper.assertTrue(polymeriser.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT).isEmpty()
                        && polymeriser.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT).isEmpty(),
                "Polymeriser left a fully charged serum in an active process slot");
        helper.assertTrue(polymeriser.tank().getFluidAmount() == 0 && polymeriser.secondaryTank().getFluidAmount() == 0,
                "Sixteen Polymeriser operations did not consume 160 mB bacteria and 800 mB Raw DNA");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 600)
    public static void analyserUsesDyeAndPersistsFlowerAnalysis(GameTestHelper helper) {
        MachineBlockEntity analyser = placeMachine(helper, new BlockPos(2, 2, 2), "analyser");
        analyser.setOwner(UUID.fromString("6a9786ce-cab8-459d-badc-c7e00cb5c1da"));
        fillEnergy(analyser);
        analyser.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT,
                FlowerSpeciesType.get().getDefaultSpecies().createIndividual().createStack(FlowerLifeStage.FLOWER));
        analyser.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(GeneticsContent.DNA_DYE.get()));
        helper.succeedWhen(() -> {
            assertOutput(helper, analyser, "Analyser did not finish");
            ItemStack analyzedStack = takeOutput(analyser);
            IFlower analyzed = requireFlower(helper, analyzedStack, "Analyser output");
            helper.assertTrue(analyzed.isAnalyzed(), "Analyser output was not marked analyzed");
            ItemStack roundTrip = analyzedStack.copy();
            helper.assertTrue(requireFlower(helper, roundTrip, "Copied analyzed flower").isAnalyzed(), "Analyzed state did not survive stack copy");
            helper.assertTrue(charge(analyser.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT)) == 4_000,
                    "Analyser did not consume the source 0.6 DNA-dye charge");
        });
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 600)
    public static void analyserRevealsCompletedSequenceWithoutChangingGene(GameTestHelper helper) {
        ItemStack flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual().createStack(FlowerLifeStage.FLOWER);
        GeneticAlleleData allele = GeneticOperations.sample(flower,
                FlowerChromosomes.STEM.id(), GeneticAlleleData.Origin.ACTIVE);
        ItemStack sequence = GeneticsContent.GENETIC_MATERIAL.get().sequence(
                new GeneticSequenceData(allele, 73, 100, false));
        MachineBlockEntity analyser = placeMachine(helper, new BlockPos(2, 2, 2), "analyser");
        analyser.setOwner(UUID.fromString("118d4cce-1e92-4bc2-98e1-1e23c7bb5b39"));
        fillEnergy(analyser);
        analyser.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, sequence);
        analyser.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT,
                new ItemStack(GeneticsContent.DNA_DYE.get()));
        helper.succeedWhen(() -> {
            assertOutput(helper, analyser, "Analyser did not finish sequence analysis");
            GeneticSequenceData analyzed = takeOutput(analyser).get(BinnieDataComponents.GENETIC_SEQUENCE);
            helper.assertTrue(analyzed != null && analyzed.analyzed(),
                    "Analyser did not reveal the completed sequence");
            helper.assertTrue(analyzed.allele().equals(allele) && analyzed.quality() == 73 && analyzed.sequenced() == 100,
                    "Analyser changed sequence identity, quality, or progress");
            helper.assertTrue(charge(analyser.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT)) == 4_000,
                    "Sequence analysis did not consume the source 0.6 DNA-dye charge");
        });
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 600)
    public static void incubatorConvertsWaterAndGrowthMediumIntoProcessFluid(GameTestHelper helper) {
        MachineBlockEntity incubator = placeMachine(helper, new BlockPos(2, 2, 2), "incubator");
        fillEnergy(incubator);
        fillPrimary(incubator, new FluidStack(Fluids.WATER, 25));
        incubator.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(GeneticsContent.GROWTH_MEDIUM.get(), 64));
        assertIndefiniteHandler(helper, incubator, "incubator");
        helper.startSequence().thenExecute(() -> {
            int initialEnergy = incubator.energy().getEnergyStored();
            advanceUntil(helper, incubator, 2_000,
                    () -> incubator.outputTank().getFluid().is(GeneticsFluids.GROWTH_MEDIUM.fluid()),
                    "Incubator did not pass its source one-in-20 chance gate");
            helper.assertTrue(incubator.outputTank().getFluid().is(GeneticsFluids.GROWTH_MEDIUM.fluid()) && incubator.outputTank().getFluidAmount() == 25,
                    "Incubator did not produce Growth Medium fluid");
            helper.assertTrue(incubator.tank().getFluidAmount() == 0, "Incubator did not consume water");
            int remaining = incubator.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).getCount();
            helper.assertTrue(remaining == 63 || remaining == 64,
                    "Incubator ignored the source separate 20% reagent-loss roll: " + remaining);
            helper.assertTrue(incubator.energy().getEnergyStored() < initialEnergy,
                    "Incubator did not consume source 2 FE/t while waiting for a chance roll");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 600)
    public static void incubatorUsesLegacySugarEnzymeAndWhiteDyeBacteriaRoutes(GameTestHelper helper) {
        MachineBlockEntity sugar = placeMachine(helper, new BlockPos(2, 2, 2), "incubator");
        MachineBlockEntity dye = placeMachine(helper, new BlockPos(5, 2, 2), "incubator");
        fillEnergy(sugar, dye);
        fillPrimary(sugar, GeneticsFluids.BACTERIA.stack(2));
        fillPrimary(dye, GeneticsFluids.BACTERIA.stack(10));
        sugar.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(Items.SUGAR, 64));
        dye.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(Items.WHITE_DYE, 64));
        assertIndefiniteHandler(helper, sugar, "incubator sugar/enzyme");
        assertIndefiniteHandler(helper, dye, "incubator white dye/polymerising bacteria");

        MachineBlockEntity invalid = placeMachine(helper, new BlockPos(8, 2, 2), "incubator");
        fillEnergy(invalid);
        fillPrimary(invalid, GeneticsFluids.BACTERIA.stack(10));
        invalid.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(Items.BONE_MEAL));
        helper.startSequence().thenExecute(() -> {
            int invalidEnergy = invalid.energy().getEnergyStored();
            advanceTicks(helper, invalid, 100);
            helper.assertTrue(invalid.activeProcess() == null && invalid.energy().getEnergyStored() == invalidEnergy,
                    "Incubator retained the non-source bone-meal polymerising-bacteria route");
            advanceUntil(helper, sugar, 5_000,
                    () -> sugar.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT).is(GeneticsContent.ENZYME.get()),
                    "Sugar plus Bacteria did not produce the legacy chance-gated Enzyme item");
            advanceUntil(helper, dye, 2_000,
                    () -> dye.outputTank().getFluid().is(GeneticsFluids.POLYMERISING_BACTERIA.fluid()),
                    "White dye plus Bacteria did not produce Polymerising Bacteria");
            helper.assertTrue(sugar.tank().getFluidAmount() == 0,
                    "Sugar/Enzyme route did not consume exactly 2 mB Bacteria");
            helper.assertTrue(dye.outputTank().getFluidAmount() == 10,
                    "White dye/Bacteria route did not produce exactly 10 mB Polymerising Bacteria");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 600)
    public static void incubatorConvertsBeeLarvaeIntoSameGenomeDrone(GameTestHelper helper) {
        MachineBlockEntity incubator = placeMachine(helper, new BlockPos(2, 2, 2), "incubator");
        fillEnergy(incubator);
        ItemStack larvae = SpeciesUtil.BEE_TYPE.get().getDefaultSpecies().createIndividual().createStack(BeeLifeStage.LARVAE);
        fillPrimary(incubator, GeneticsFluids.GROWTH_MEDIUM.stack(50));
        incubator.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, larvae);
        assertIndefiniteHandler(helper, incubator, "incubator larvae");
        helper.startSequence().thenExecute(() -> {
            advanceUntil(helper, incubator, 2_000,
                    () -> !incubator.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT).isEmpty(),
                    "Incubator did not convert bee larvae on the source chance loop");
            ItemStack drone = incubator.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT);
            helper.assertTrue(ItemGE.getLifeStage(drone) == BeeLifeStage.DRONE,
                    "Incubator larvae route did not produce a ForestryCE drone");
            IIndividual larvalIndividual = ItemGE.getIndividual(larvae);
            IIndividual droneIndividual = ItemGE.getIndividual(drone);
            helper.assertTrue(larvalIndividual != null && droneIndividual != null
                            && larvalIndividual.getGenome().equals(droneIndividual.getGenome()),
                    "Incubator larvae route did not retain the larval genome");
            helper.assertTrue(incubator.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).isEmpty(),
                    "Incubator larvae route did not apply its source 100% loss chance");
            helper.assertTrue(incubator.tank().getFluidAmount() == 0,
                    "Incubator larvae route did not consume exactly 50 mB Growth Medium");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 1_500)
    public static void splicerAppliesMultiGeneArrayAndConsumesOneCharge(GameTestHelper helper) {
        FlowerPair pair = distinctPrimaryColorFlowers(helper);
        ItemStack target = pair.recipient().createStack(FlowerLifeStage.POLLEN);
        GeneticAlleleData color = GeneticOperations.sample(pair.donor().createStack(FlowerLifeStage.FLOWER), FlowerChromosomes.PRIMARY_COLOR.id(), GeneticAlleleData.Origin.ACTIVE);
        GeneticAlleleData stem = GeneticOperations.sample(pair.donor().createStack(FlowerLifeStage.FLOWER), FlowerChromosomes.STEM.id(), GeneticAlleleData.Origin.ACTIVE);
        ItemStack array = GeneticsContent.GENETIC_MATERIAL.get().serum(new GeneticSerumData(List.of(color, stem), 100, 16), true);
        MachineBlockEntity splicer = placeMachine(helper, new BlockPos(2, 2, 2), "splicer");
        fillEnergy(splicer);
        splicer.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, target);
        splicer.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, array);
        helper.startSequence().thenExecute(() -> {
            advanceMachine(helper, splicer, 1_800, "Splicer did not complete at its source scaled duration");
            IFlower changed = requireFlower(helper, takeOutput(splicer), "Splicer output");
            helper.assertTrue(changed.getGenome().getActiveValue(FlowerChromosomes.PRIMARY_COLOR).equals(color.value().javaValue()), "Splicer missed primary color");
            helper.assertTrue(changed.getGenome().getActiveValue(FlowerChromosomes.STEM).equals(stem.value().javaValue()), "Splicer missed stem");
            GeneticSerumData remaining = splicer.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT).get(BinnieDataComponents.GENETIC_SERUM);
            helper.assertTrue(remaining != null && remaining.charges() == 15, "Splicer did not consume one array charge");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 900)
    public static void analyserPersistsAllFlowerChromosomeDiscoveries(GameTestHelper helper) {
        UUID owner = UUID.fromString("9fba7b35-40ec-4df8-920e-f1c1c06fbf95");
        MachineBlockEntity analyser = placeMachine(helper, new BlockPos(2, 2, 2), "analyser");
        analyser.setOwner(owner);
        fillEnergy(analyser);
        analyser.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT,
                FlowerSpeciesType.get().getDefaultSpecies().createIndividual().createStack(FlowerLifeStage.FLOWER));
        analyser.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(GeneticsContent.DNA_DYE.get()));
        helper.succeedWhen(() -> {
            assertOutput(helper, analyser, "Analyser did not finish discovery test");
            GeneticKnowledgeData knowledge = GeneticKnowledgeSavedData.get(helper.getLevel()).get(owner);
            Set<ResourceLocation> chromosomes = knowledge.genes().stream()
                    .filter(gene -> gene.speciesType().equals(FlowerSpeciesType.get().id()))
                    .map(gene -> gene.chromosome())
                    .collect(Collectors.toSet());
            helper.assertTrue(chromosomes.size() == 12,
                    "Analyser recorded " + chromosomes.size() + "/12 flower chromosomes: " + chromosomes);
            helper.assertTrue(knowledge.species().stream().anyMatch(discovery ->
                            discovery.speciesType().equals(FlowerSpeciesType.get().id())),
                    "Analyser did not record flower species discovery");
        });
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 200)
    public static void punnettMenuAndPayloadRejectInvalidSelections(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(GeneticsContent.PUNNETT_SQUARE.get()));
        PunnettSquareMenu menu = new PunnettSquareMenu(41, new Inventory(player), true);
        player.containerMenu = menu;
        FlowerPair pair = distinctPrimaryColorFlowers(helper);
        menu.parents().setItem(PunnettSquareMenu.FIRST_PARENT_SLOT, pair.donor().createStack(FlowerLifeStage.FLOWER));
        menu.parents().setItem(PunnettSquareMenu.SECOND_PARENT_SLOT, pair.recipient().createStack(FlowerLifeStage.FLOWER));
        menu.slotsChanged(menu.parents());

        helper.assertTrue(menu.chromosomeIds().size() == 12, "Punnett menu did not expose all 12 flower chromosomes");
        helper.assertTrue(GeneticsNetwork.handleSelection(new SelectPunnettChromosomePayload(41, 11), player),
                "Valid serverbound chromosome selection was rejected");
        helper.assertTrue(menu.selectedChromosomeIndex() == 11, "Valid selection did not mutate the authoritative menu");
        helper.assertTrue(!menu.outcomes().isEmpty(), "Punnett menu produced no deterministic outcomes");
        helper.assertTrue(!GeneticsNetwork.handleSelection(new SelectPunnettChromosomePayload(40, 0), player),
                "Wrong-container payload was accepted");
        helper.assertTrue(!GeneticsNetwork.handleSelection(new SelectPunnettChromosomePayload(41, -1), player),
                "Negative chromosome index was accepted");
        helper.assertTrue(!GeneticsNetwork.handleSelection(new SelectPunnettChromosomePayload(41, 12), player),
                "Out-of-range chromosome index was accepted");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 200)
    public static void databasePayloadAppliesSequencedGeneToCarriedSerum(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(GeneticsContent.DATABASE.get()));
        ItemStack flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual().createStack(FlowerLifeStage.FLOWER);
        GeneticAlleleData allele = GeneticOperations.sample(flower, FlowerChromosomes.STEM.id(), GeneticAlleleData.Origin.ACTIVE);
        GeneticKnowledgeData knowledge = GeneticKnowledgeData.EMPTY.withGene(GeneticKnowledgeEntry.fromAllele(allele));
        GeneticKnowledgeMenu menu = new GeneticKnowledgeMenu(61, new Inventory(player), knowledge,
                GeneticKnowledgeMenu.Mode.DATABASE, true);
        player.containerMenu = menu;
        menu.setCarried(GeneticsContent.GENETIC_MATERIAL.get().blank(GeneticMaterialItem.Kind.SERUM));

        helper.assertTrue(GeneticsNetwork.handleDatabaseSelection(new SelectDatabaseGenePayload(61, 0), player),
                "Database rejected a valid carried blank serum");
        GeneticSerumData serum = menu.getCarried().get(BinnieDataComponents.GENETIC_SERUM);
        helper.assertTrue(serum != null && serum.charges() == 0 && serum.alleles().size() == 1,
                "Database did not create the source uncharged one-gene serum");
        helper.assertTrue(!GeneticsNetwork.handleDatabaseSelection(new SelectDatabaseGenePayload(60, 0), player),
                "Database accepted a payload for another container");
        helper.assertTrue(!GeneticsNetwork.handleDatabaseSelection(new SelectDatabaseGenePayload(61, 1), player),
                "Database accepted an out-of-range gene index");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 200)
    public static void portableAnalystRefusesUnanalyzedSpecimenWithoutDnaDye(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(GeneticsContent.ANALYST.get()));
        GeneticKnowledgeMenu menu = new GeneticKnowledgeMenu(63, new Inventory(player), GeneticKnowledgeData.EMPTY,
                GeneticKnowledgeMenu.Mode.ANALYST, true);
        player.containerMenu = menu;
        GeneticKnowledgeData before = GeneticKnowledgeSavedData.get(helper.getLevel()).get(player.getUUID());
        ItemStack specimen = FlowerSpeciesType.get().getDefaultSpecies().createIndividual().createStack(FlowerLifeStage.FLOWER);
        menu.analystSlots().setItem(GeneticKnowledgeMenu.ANALYST_INDIVIDUAL_SLOT, specimen);
        menu.slotsChanged(menu.analystSlots());

        ItemStack unchanged = menu.analystSlots().getItem(GeneticKnowledgeMenu.ANALYST_INDIVIDUAL_SLOT);
        helper.assertTrue(!GeneticOrganismAdapters.forStack(unchanged).read(unchanged).isAnalyzed(),
                "Portable Analyst analyzed a specimen without DNA Dye");
        helper.assertTrue(menu.analystSlots().getItem(GeneticKnowledgeMenu.ANALYST_DYE_SLOT).isEmpty(),
                "Portable Analyst unexpectedly created or consumed DNA Dye");
        GeneticKnowledgeData after = GeneticKnowledgeSavedData.get(helper.getLevel()).get(player.getUUID());
        helper.assertTrue(after.equals(before),
                "Portable Analyst changed discoveries without completing analysis");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 200)
    public static void portableAnalystConsumesDyeAndDiscoversGenome(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(GeneticsContent.ANALYST.get()));
        GeneticKnowledgeMenu menu = new GeneticKnowledgeMenu(62, new Inventory(player), GeneticKnowledgeData.EMPTY,
                GeneticKnowledgeMenu.Mode.ANALYST, true);
        player.containerMenu = menu;
        ItemStack specimen = FlowerSpeciesType.get().getDefaultSpecies().createIndividual().createStack(FlowerLifeStage.FLOWER);
        menu.analystSlots().setItem(GeneticKnowledgeMenu.ANALYST_DYE_SLOT, new ItemStack(GeneticsContent.DNA_DYE.get()));
        menu.analystSlots().setItem(GeneticKnowledgeMenu.ANALYST_INDIVIDUAL_SLOT, specimen);
        menu.slotsChanged(menu.analystSlots());

        ItemStack analysed = menu.analystSlots().getItem(GeneticKnowledgeMenu.ANALYST_INDIVIDUAL_SLOT);
        helper.assertTrue(GeneticOrganismAdapters.forStack(analysed).read(analysed).isAnalyzed(),
                "Portable Analyst did not mark the specimen analysed");
        helper.assertTrue(menu.analystSlots().getItem(GeneticKnowledgeMenu.ANALYST_DYE_SLOT).isEmpty(),
                "Portable Analyst did not consume one DNA Dye cell");
        GeneticKnowledgeData knowledge = GeneticKnowledgeSavedData.get(helper.getLevel()).get(player.getUUID());
        helper.assertTrue(knowledge.genes().size() >= 12 && !knowledge.species().isEmpty(),
                "Portable Analyst did not register the analysed genome and species");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 200)
    public static void masterGeneDatabaseContainsEveryLegacyAllele(GameTestHelper helper) {
        GeneticKnowledgeData defaults = GeneticKnowledgeCatalog.registeredDefaults();
        GeneticKnowledgeData catalog = GeneticKnowledgeCatalog.masterGeneDatabase();
        helper.assertTrue(GeneticKnowledgeMenu.Mode.MASTER_DATABASE.exposesCompleteGeneCatalog(
                        GeneticKnowledgeMenu.InterfaceType.GENE_DATABASE),
                "Master Gene Database did not bypass the sequenced-gene tracker");
        helper.assertTrue(catalog.genes().size() > defaults.genes().size(),
                "Master Gene Database did not add the original breeding-system allele sets");

        for (var adapter : GeneticOrganismAdapters.all()) {
            for (ISpecies<?> species : adapter.speciesType().getAllSpecies()) {
                for (IChromosome<?> chromosome : adapter.chromosomes()) {
                    AllelePair<?> pair = species.getDefaultGenome().getAllelePair(chromosome);
                    helper.assertTrue(containsGeneValue(catalog, adapter.id(), chromosome, pair.active().value()),
                            "Master Gene Database omitted active default " + adapter.id() + "/" + chromosome.id());
                    helper.assertTrue(containsGeneValue(catalog, adapter.id(), chromosome, pair.inactive().value()),
                            "Master Gene Database omitted inactive default " + adapter.id() + "/" + chromosome.id());
                }
            }
        }
        for (ToleranceType tolerance : ToleranceType.values()) {
            helper.assertTrue(containsGeneValue(catalog, ForestrySpeciesTypes.BEE,
                    BeeChromosomes.TEMPERATURE_TOLERANCE, tolerance), "Missing bee tolerance " + tolerance);
        }
        helper.assertTrue(containsGeneValue(catalog, ForestrySpeciesTypes.BEE,
                BeeChromosomes.POLLINATION, 99), "Missing maximum bee pollination");
        for (int value = 1; value <= 10; value++) {
            if (binnie.core.forestry.ForestryModules.butterfliesLoaded()) {
                helper.assertTrue(containsGeneValue(catalog, ForestrySpeciesTypes.BUTTERFLY,
                        ButterflyChromosomes.METABOLISM, value), "Missing butterfly metabolism " + value);
            }
            helper.assertTrue(containsGeneValue(catalog, ForestrySpeciesTypes.TREE,
                    TreeChromosomes.GIRTH, value), "Missing tree girth " + value);
        }
        for (var color : LegacyFlowerCatalog.INSTANCE.colors()) {
            helper.assertTrue(containsGeneValue(catalog, binnie.botany.genetics.FlowerSpeciesTypes.FLOWER,
                    FlowerChromosomes.PRIMARY_COLOR, color.id()), "Missing primary flower color " + color.id());
            helper.assertTrue(containsGeneValue(catalog, binnie.botany.genetics.FlowerSpeciesTypes.FLOWER,
                    FlowerChromosomes.SECONDARY_COLOR, color.id()), "Missing secondary flower color " + color.id());
            helper.assertTrue(containsGeneValue(catalog, binnie.botany.genetics.FlowerSpeciesTypes.FLOWER,
                    FlowerChromosomes.STEM, color.id()), "Missing stem flower color " + color.id());
        }
        long unique = catalog.genes().stream()
                .map(gene -> gene.speciesType() + "|" + gene.chromosome() + "|" + gene.value())
                .distinct().count();
        helper.assertTrue(unique == catalog.genes().size(), "Master Gene Database contains duplicate allele values");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 200)
    public static void masterRegistryCatalogContainsEveryLiveSpecies(GameTestHelper helper) {
        GeneticKnowledgeData catalog = GeneticKnowledgeCatalog.registeredDefaults();
        int expected = 0;
        for (var adapter : GeneticOrganismAdapters.all()) {
            for (ISpecies<?> species : adapter.speciesType().getAllSpecies()) {
                expected++;
                GeneticSpeciesDiscovery discovery = new GeneticSpeciesDiscovery(adapter.id(), species.id());
                helper.assertTrue(catalog.knows(discovery),
                        "Master Registry omitted live species " + discovery.speciesType() + "/" + discovery.species());
            }
        }
        helper.assertTrue(catalog.species().size() == expected,
                "Master Registry catalog size " + catalog.species().size() + " did not equal live species count " + expected);
        helper.assertTrue(GeneticKnowledgeMenu.Mode.MASTER_REGISTRY.exposesCompleteSpeciesCatalog(
                        GeneticKnowledgeMenu.InterfaceType.REGISTRY),
                "Master Registry did not enable complete live species access");
        helper.assertTrue(GeneticKnowledgeMenu.Mode.MASTER_REGISTRY.exposesCompleteMutationCatalog(
                        GeneticKnowledgeMenu.InterfaceType.REGISTRY),
                "Master Registry did not enable complete mutation access");
        helper.succeed();
    }

    private static boolean containsGeneValue(GeneticKnowledgeData catalog, ResourceLocation speciesType,
                                             IChromosome<?> chromosome, Object value) {
        AlleleValue encoded = AlleleValue.fromJava(value);
        return catalog.genes().stream().anyMatch(gene -> gene.speciesType().equals(speciesType)
                && gene.chromosome().equals(chromosome.id()) && gene.value().equals(encoded));
    }

    private static ItemStack findDonorWithDistinctRecipient(GameTestHelper helper, MachineBlockEntity isolator) {
        var handler = MachineProcessRegistry.get(isolator.spec().id());
        helper.assertTrue(handler != null, "No Isolator process handler registered");
        for (IFlowerSpecies species : FlowerSpeciesType.get().getAllSpecies()) {
            ItemStack donor = species.createIndividual().createStack(FlowerLifeStage.FLOWER);
            isolator.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, donor);
            var plan = handler.createPlan(helper.getLevel(), isolator);
            if (plan.isEmpty()) continue;
            GeneticSequenceData sequence = plan.get().output().get(BinnieDataComponents.GENETIC_SEQUENCE);
            GeneticAlleleData allele = sequence == null ? null : sequence.allele();
            if (allele == null) continue;
            IChromosome<?> chromosome = FlowerSpeciesType.get().getKaryotype().getChromosome(allele.chromosome());
            if (chromosome == null) continue;
            for (IFlowerSpecies recipientSpecies : FlowerSpeciesType.get().getAllSpecies()) {
                IFlower recipient = recipientSpecies.createIndividual();
                AllelePair<?> pair = recipient.getGenome().getAllelePair(chromosome);
                Object isolated = allele.value().javaValue();
                if (!pair.active().value().equals(isolated) || !pair.inactive().value().equals(isolated)) {
                    return donor;
                }
            }
        }
        helper.fail("No flower donor produced an isolatable allele with a distinct natural recipient");
        throw new IllegalStateException("No viable Isolator donor");
    }

    private static ItemStack findDifferentRecipient(GameTestHelper helper, GeneticAlleleData allele, IChromosome<?> chromosome) {
        for (IFlowerSpecies species : FlowerSpeciesType.get().getAllSpecies()) {
            IFlower candidate = species.createIndividual();
            AllelePair<?> pair = candidate.getGenome().getAllelePair(chromosome);
            if (!pair.active().value().equals(allele.value().javaValue()) || !pair.inactive().value().equals(allele.value().javaValue())) {
                return candidate.createStack(FlowerLifeStage.POLLEN);
            }
        }
        helper.fail("No recipient differs from isolated allele for " + chromosome.id());
        throw new IllegalStateException("No distinct recipient");
    }

    private static FlowerPair distinctPrimaryColorFlowers(GameTestHelper helper) {
        List<IFlowerSpecies> species = FlowerSpeciesType.get().getAllSpecies();
        for (IFlowerSpecies donorSpecies : species) {
            IFlower donor = donorSpecies.createIndividual();
            Object donorColor = donor.getGenome().getActiveValue(FlowerChromosomes.PRIMARY_COLOR);
            for (IFlowerSpecies recipientSpecies : species) {
                IFlower recipient = recipientSpecies.createIndividual();
                if (!donorColor.equals(recipient.getGenome().getActiveValue(FlowerChromosomes.PRIMARY_COLOR))) return new FlowerPair(donor, recipient);
            }
        }
        helper.fail("No two flower species have distinct primary colors");
        throw new IllegalStateException("No distinct flower colors");
    }

    private static MachineBlockEntity placeMachine(GameTestHelper helper, BlockPos relative, String id) {
        helper.setBlock(relative, GeneticsMachines.REGISTRY.block(id).get());
        if (helper.getBlockEntity(relative) instanceof MachineBlockEntity machine) return machine;
        helper.fail("Expected registered " + id + " machine block entity at " + relative);
        throw new IllegalStateException("Missing machine block entity");
    }

    private static void fillEnergy(MachineBlockEntity... machines) {
        for (MachineBlockEntity machine : machines) {
            int guard = 0;
            while (machine.energy().getEnergyStored() < machine.energy().getMaxEnergyStored() && guard++ < 1_000) {
                if (machine.energy().receiveEnergy(machine.energy().getMaxEnergyStored(), false) <= 0) break;
            }
            if (machine.energy().getEnergyStored() != machine.energy().getMaxEnergyStored()) throw new IllegalStateException("Could not fill machine energy");
        }
    }

    /**
      * Executes the production server ticker directly. This keeps a source-length
      * test bounded without changing a machine's duration or process code.
      */
    private static void advanceMachine(GameTestHelper helper, MachineBlockEntity machine, int maximumTicks, String message) {
        for (int tick = 0; tick < maximumTicks; tick++) {
            fillEnergy(machine);
            MachineBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
            if (!machine.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT).isEmpty()) return;
        }
        assertOutput(helper, machine, message);
        throw new IllegalStateException(message);
    }

    private static void advanceTicks(GameTestHelper helper, MachineBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            fillEnergy(machine);
            MachineBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
        }
    }

    private static void advanceUntil(GameTestHelper helper, MachineBlockEntity machine, int maximumTicks,
                                     BooleanSupplier complete, String message) {
        for (int tick = 0; tick < maximumTicks; tick++) {
            MachineBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
            if (complete.getAsBoolean()) return;
        }
        helper.fail(message);
        throw new IllegalStateException(message);
    }

    private static void advanceUntilPowered(GameTestHelper helper, MachineBlockEntity machine, int maximumTicks,
                                            BooleanSupplier complete, String message) {
        for (int tick = 0; tick < maximumTicks; tick++) {
            fillEnergy(machine);
            MachineBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
            if (complete.getAsBoolean()) return;
        }
        helper.fail(message);
        throw new IllegalStateException(message);
    }

    private static void fillPrimary(MachineBlockEntity machine, FluidStack fluid) {
        int accepted = machine.tank().fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        if (accepted != fluid.getAmount()) throw new IllegalStateException("Could not fill primary tank with " + fluid);
    }

    private static void fillSecondary(MachineBlockEntity machine, FluidStack fluid) {
        int accepted = machine.secondaryTank().fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        if (accepted != fluid.getAmount()) throw new IllegalStateException("Could not fill secondary tank with " + fluid);
    }

    private static int charge(ItemStack stack) {
        LaboratoryChargeData charge = stack.get(BinnieDataComponents.LABORATORY_CHARGE);
        return charge == null ? LaboratoryChargeData.FULL : charge.milliCharge();
    }

    private static void assertOutput(GameTestHelper helper, MachineBlockEntity machine, String message) {
        helper.assertTrue(!machine.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT).isEmpty(),
                message + " [progress=" + machine.progress() + "/" + machine.maxProgress() + ", energy=" + machine.energy().getEnergyStored()
                        + ", active=" + machine.activeProcess() + ", input=" + machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT)
                        + ", catalyst=" + machine.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT)
                        + ", reagent=" + machine.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT) + "]");
    }

    private static void assertPlanReady(GameTestHelper helper, MachineBlockEntity machine, String id) {
        var handler = MachineProcessRegistry.get(machine.spec().id());
        helper.assertTrue(handler != null, "No dynamic process handler registered for " + machine.spec().id());
        helper.assertTrue(handler.createPlan(helper.getLevel(), machine).isPresent(),
                "Dynamic process plan rejected for " + id + " input=" + machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT)
                        + " catalyst=" + machine.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT)
                        + " reagent=" + machine.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT));
    }

    private static void assertIndefiniteHandler(GameTestHelper helper, MachineBlockEntity machine, String id) {
        var handler = MachineProcessRegistry.get(machine.spec().id());
        helper.assertTrue(handler != null && handler.handlesIndefiniteProcess(machine),
                "No source-indefinite handler registered for " + id);
    }

    private static ItemStack takeOutput(MachineBlockEntity machine) {
        ItemStack result = machine.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT).copy();
        machine.inventory().setStackInSlot(MachineBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        return result;
    }

    private static IFlower requireFlower(GameTestHelper helper, ItemStack stack, String label) {
        IIndividual individual = ItemGE.getIndividual(stack);
        if (individual instanceof IFlower flower) return flower;
        helper.fail(label + " did not contain a flower genome");
        throw new IllegalStateException("Missing flower individual");
    }

    private static FlowerBlockEntity requirePlanted(GameTestHelper helper, BlockPos relative) {
        if (helper.getBlockEntity(relative) instanceof FlowerBlockEntity flower) return flower;
        helper.fail("Expected planted FlowerBlockEntity at " + relative);
        throw new IllegalStateException("Missing planted flower");
    }

    private record FlowerPair(IFlower donor, IFlower recipient) {}
}
