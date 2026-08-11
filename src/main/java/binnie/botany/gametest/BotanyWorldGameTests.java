package binnie.botany.gametest;

import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.api.genetics.IFlower;
import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import binnie.botany.block.BotanySoilBlock;
import binnie.botany.block.FlowerBlockEntity;
import binnie.botany.block.GardenPlantBlock;
import binnie.botany.block.GardenPlantType;
import binnie.botany.block.SoilQuality;
import binnie.botany.block.StainedGlassBlock;
import binnie.botany.item.StainedGlassItem;
import binnie.botany.genetics.FlowerSpeciesType;
import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.genetics.LegacyFlowerCatalog;
import binnie.botany.gardening.BotanyGardening;
import binnie.botany.recipe.FlowerPigmentRecipe;
import binnie.botany.item.BotanyPigments;
import binnie.botany.registry.BotanyWorldContent;
import binnie.botany.registry.BotanyContent;
import binnie.botany.registry.BotanyMachines;
import binnie.core.data.DesignData;
import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineProcessHandler;
import binnie.core.machine.MachineProcessRegistry;
import binnie.design.data.DesignCatalog;
import binnie.core.data.VariantData;
import binnie.core.registry.BinnieDataComponents;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

@GameTestHolder("binniecore")
@PrefixGameTestTemplate(false)
public final class BotanyWorldGameTests {
    private BotanyWorldGameTests() {}

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void everyMatureFlowerColourCraftsItsExactPigment(GameTestHelper helper) {
        var species = FlowerSpeciesType.get().getDefaultSpecies();
        var recipe = new FlowerPigmentRecipe(CraftingBookCategory.MISC);
        int checked = 0;
        for (var color : LegacyFlowerCatalog.INSTANCE.colors()) {
            var pair = AllelePair.both(new Allele<>(color.id(), color.dominant()));
            var genome = species.getDefaultGenome().copyWithPairs(Map.of(FlowerChromosomes.PRIMARY_COLOR, pair));
            IFlower flower = species.createIndividual(genome);
            flower.setAge(1);
            ItemStack flowerStack = flower.createStack(FlowerLifeStage.FLOWER);
            ItemStack result = recipe.assemble(CraftingInput.of(1, 1, List.of(flowerStack)), helper.getLevel().registryAccess());
            String expected = color.id().getPath();
            ItemStack expectedStack = BotanyPigments.stackFor(expected, BotanyContent.PIGMENT.get());
            helper.assertTrue(ItemStack.isSameItemSameComponents(result, expectedStack),
                    "Flower crafted the wrong colorant for " + expected);
            checked++;
        }
        helper.assertTrue(checked == 80, "Expected all 80 legacy pigment colours");

        IFlower immature = species.createIndividual();
        ItemStack rejected = recipe.assemble(CraftingInput.of(1, 1,
                List.of(immature.createStack(FlowerLifeStage.FLOWER))), helper.getLevel().registryAccess());
        helper.assertTrue(rejected.isEmpty(), "An immature flower incorrectly crafted pigment");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void plantedFlowerRoundTripsAllGenomeAndLifecycleState(GameTestHelper helper) {
        List<IFlowerSpecies> species = FlowerSpeciesType.get().getAllSpecies();
        helper.assertTrue(species.size() == 47, "Expected all 47 flower species before world tests");
        IFlower original = species.get(31).createIndividual();
        original.setAge(7);
        original.setFlowered(true);
        original.setWilted(false);
        original.analyze();
        original.pollinate(species.get(4).createIndividual());
        ItemStack stack = original.createStack(FlowerLifeStage.FLOWER);

        BlockPos relative = new BlockPos(2, 2, 2);
        helper.setBlock(relative.below(), BotanyWorldContent.FLOWERBED.get().defaultBlockState());
        helper.setBlock(relative, BotanyWorldContent.FLOWER_BLOCK.get());
        FlowerBlockEntity placed = requireFlower(helper, relative);
        placed.setFlower(stack, null);

        CompoundTag saved = placed.saveWithoutMetadata(helper.getLevel().registryAccess());
        FlowerBlockEntity reloaded = new FlowerBlockEntity(helper.absolutePos(relative), helper.getBlockState(relative));
        reloaded.setLevel(helper.getLevel());
        reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());
        IFlower actual = reloaded.getFlower();
        helper.assertTrue(actual != null, "Reloaded flower individual was absent");
        assertFlowerEquivalent(helper, original, actual);
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void pollenTransferAndOffspringPreserveTypedGenomes(GameTestHelper helper) {
        List<IFlowerSpecies> species = FlowerSpeciesType.get().getAllSpecies();
        IFlower mother = species.get(0).createIndividual();
        mother.setAge(4);
        mother.setFlowered(true);
        IFlower father = species.get(1).createIndividual();
        father.setAge(4);
        father.setFlowered(true);

        BlockPos relative = new BlockPos(2, 2, 2);
        helper.setBlock(relative.below(), BotanyWorldContent.FLOWERBED.get().defaultBlockState());
        helper.setBlock(relative, BotanyWorldContent.FLOWER_BLOCK.get());
        FlowerBlockEntity placed = requireFlower(helper, relative);
        placed.setFlower(mother.createStack(FlowerLifeStage.FLOWER), null);
        helper.assertTrue(placed.applyPollen(father), "Valid pollen was rejected");
        IFlower pollinated = placed.getFlower();
        helper.assertTrue(pollinated != null && pollinated.getMate() != null, "Mate genome was not stored");

        List<IFlower> children = pollinated.createOffspring(helper.getLevel(), helper.absolutePos(relative), father, 3);
        helper.assertTrue(!children.isEmpty(), "Breeding produced no offspring");
        for (IFlower child : children) {
            helper.assertTrue(child.getGeneration() == 1, "Offspring generation was not incremented");
            helper.assertTrue(child.getGenome().getKaryotype() == FlowerSpeciesType.get().getKaryotype(), "Offspring has foreign karyotype");
            helper.assertTrue(child.createStack(FlowerLifeStage.SEED).getCount() == 1, "Offspring seed stack was invalid");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void soilStatePersistsAndInvalidConditionsWiltFlower(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        var soil = BotanyWorldContent.SOIL.get().defaultBlockState()
                .setValue(BotanySoilBlock.MOISTURE, SoilMoisture.DRY)
                .setValue(BotanySoilBlock.ACIDITY, SoilAcidity.ACID)
                .setValue(BotanySoilBlock.FERTILITY, 0)
                .setValue(BotanySoilBlock.WEEDKILLED, true);
        helper.setBlock(relative.below(), soil);
        helper.setBlock(relative, BotanyWorldContent.FLOWER_BLOCK.get());
        FlowerBlockEntity placed = requireFlower(helper, relative);
        IFlower flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        placed.setFlower(flower.createStack(FlowerLifeStage.FLOWER), null);
        IFlower plantedFlower = placed.getFlower();
        helper.assertTrue(plantedFlower != null, "Placed flower could not be decoded");
        plantedFlower.setHealth(1);
        plantedFlower.saveToStack(placed.getFlowerStack());

        CompoundTag savedSoil = helper.getBlockState(relative.below()).getValues().isEmpty() ? new CompoundTag() : null;
        helper.assertTrue(savedSoil == null, "Soil block state unexpectedly lost all properties");
        helper.assertTrue(helper.getBlockState(relative.below()).getValue(BotanySoilBlock.WEEDKILLED), "Weedkiller state was not stored");
        helper.succeedWhen(() -> {
            var dead = helper.getBlockState(relative);
            helper.assertTrue(dead.is(BotanyWorldContent.GARDEN_PLANT.get())
                            && dead.getValue(GardenPlantBlock.TYPE) == GardenPlantType.DEAD_FLOWER,
                    "Flower with zero-fertility soil and one health did not enter the dead-flower lifecycle");
        });
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void matureFlowerHarvestProducesAllLifeStagesAndResetsBloom(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        helper.setBlock(relative.below(), BotanyWorldContent.FLOWERBED.get().defaultBlockState());
        helper.setBlock(relative, BotanyWorldContent.FLOWER_BLOCK.get());
        FlowerBlockEntity placed = requireFlower(helper, relative);
        IFlower flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        flower.setAge(4);
        flower.setFlowered(true);
        placed.setFlower(flower.createStack(FlowerLifeStage.FLOWER), null);

        List<ItemStack> outputs = placed.harvestForAutomation();
        helper.assertTrue(outputs.stream().anyMatch(stack -> stack.is(binnie.botany.registry.BotanyContent.FLOWER.get())),
                "Harvest omitted the flower product");
        helper.assertTrue(outputs.stream().anyMatch(stack -> stack.is(binnie.botany.registry.BotanyContent.POLLEN.get())),
                "Harvest omitted pollen");
        helper.assertTrue(outputs.stream().anyMatch(stack -> stack.is(binnie.botany.registry.BotanyContent.SEED.get())),
                "Harvest omitted seeds");
        helper.assertTrue(!placed.isHarvestable(), "Harvest did not reset the bloom state");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void soilQualityTransitionsPreserveClimateAndBoundFertility(GameTestHelper helper) {
        var source = BotanyWorldContent.SOIL.get().defaultBlockState()
                .setValue(BotanySoilBlock.MOISTURE, SoilMoisture.DAMP)
                .setValue(BotanySoilBlock.ACIDITY, SoilAcidity.ACID)
                .setValue(BotanySoilBlock.FERTILITY, 1)
                .setValue(BotanySoilBlock.WEEDKILLED, true);
        var upgraded = BotanyGardening.changeQuality(source, SoilQuality.FLOWERBED);
        helper.assertTrue(upgraded.is(BotanyWorldContent.FLOWERBED.get()), "Soil did not upgrade to flowerbed");
        helper.assertTrue(upgraded.getValue(BotanySoilBlock.MOISTURE) == SoilMoisture.DAMP
                        && upgraded.getValue(BotanySoilBlock.ACIDITY) == SoilAcidity.ACID
                        && upgraded.getValue(BotanySoilBlock.WEEDKILLED),
                "Soil upgrade lost climate or weedkiller state");
        var degraded = BotanyGardening.degradeQuality(upgraded, SoilQuality.SOIL);
        helper.assertTrue(degraded.is(BotanyWorldContent.SOIL.get()), "Soil did not degrade to base soil");
        helper.assertTrue(degraded.getValue(BotanySoilBlock.FERTILITY) <= SoilQuality.SOIL.initialFertility(),
                "Degraded soil retained a higher-grade fertility charge");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void bonemealFlowersEstablishedHealthyPlant(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        helper.setBlock(relative.below(), BotanyWorldContent.FLOWERBED.get().defaultBlockState());
        helper.setBlock(relative, BotanyWorldContent.FLOWER_BLOCK.get());
        FlowerBlockEntity placed = requireFlower(helper, relative);
        IFlower flower = FlowerSpeciesType.get().getDefaultSpecies().createIndividual();
        flower.setAge(3);
        flower.setFlowered(false);
        placed.setFlower(flower.createStack(FlowerLifeStage.FLOWER), null);
        helper.assertTrue(placed.applyBonemeal(), "Healthy flower rejected bonemeal");
        helper.assertTrue(placed.isHarvestable(), "Bonemeal did not force the established plant to flower");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void stainedGlassRetainsFlowerColourForPlacementAndPickBlock(GameTestHelper helper) {
        String color = "royal_blue";
        ItemStack placedStack = BotanyWorldContent.STAINED_GLASS_ITEM.get().createStack(color);
        int colorIndex = StainedGlassItem.colorIndex(placedStack);
        helper.assertTrue(colorIndex == binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color").indexOf(color),
                "Stained glass item did not resolve its typed flower colour");

        BlockPos relative = new BlockPos(2, 2, 2);
        var state = BotanyWorldContent.STAINED_GLASS.get().defaultBlockState().setValue(StainedGlassBlock.COLOR, colorIndex);
        helper.setBlock(relative, state);
        ItemStack picked = BotanyWorldContent.STAINED_GLASS.get().getCloneItemStack(
                helper.getBlockState(relative), null, helper.getLevel(), helper.absolutePos(relative), null);
        VariantData identity = picked.get(BinnieDataComponents.VARIANT);
        helper.assertTrue(identity != null && identity.category().equals("binniebotany.flower_color") && identity.value().equals(color),
                "Pick block lost the planted glass flower-colour component");
        helper.succeed();
    }


    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void tileworkerCraftsTypedTwoColourCeramicAndConsumesExactInputs(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, BotanyMachines.REGISTRY.block("tileworker").get());
        helper.assertTrue(helper.getBlockEntity(pos) instanceof MachineBlockEntity, "Tileworker block entity was not created");
        MachineBlockEntity machine = (MachineBlockEntity) helper.getBlockEntity(pos);
        MachineProcessHandler handler = MachineProcessRegistry.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("binniebotany", "tileworker"));
        helper.assertTrue(handler != null && handler.isManualCrafting(machine), "Tileworker manual process handler was not registered");
        int diamond = DesignCatalog.DESIGNS.indexOf("diamond");
        helper.assertTrue(handler.setManualSelection(machine, diamond), "Tileworker rejected a valid design");
        machine.setManualSelection(diamond);
        machine.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, BotanyWorldContent.CERAMIC_ITEM.get().createStack("red"));
        machine.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, BotanyWorldContent.CERAMIC_ITEM.get().createStack("royal_blue"));
        machine.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(BotanyContent.MORTAR.get()));
        ItemStack preview = handler.getManualPreview(machine);
        DesignData data = preview.get(BinnieDataComponents.DESIGN);
        helper.assertTrue(preview.is(BotanyWorldContent.CERAMIC_TILE_ITEM.get()) && preview.getCount() == 2,
                "Tileworker did not preview two patterned ceramic tiles");
        helper.assertTrue(data != null && data.design().equals("diamond") && data.primaryVariant().equals("red")
                        && data.secondaryVariant().equals("royal_blue"), "Tileworker preview lost design or ceramic colours");
        ItemStack crafted = handler.takeManualPreview(machine, ItemStack.EMPTY);
        helper.assertTrue(ItemStack.isSameItemSameComponents(preview, crafted), "Tileworker crafted output differed from its preview");
        helper.assertTrue(machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT).isEmpty()
                        && machine.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT).isEmpty()
                        && machine.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).isEmpty(),
                "Tileworker did not consume exactly both ceramics and one mortar");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void tileworkerBlankPatternUsesOnlyPrimaryCeramic(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, BotanyMachines.REGISTRY.block("tileworker").get());
        MachineBlockEntity machine = (MachineBlockEntity) helper.getBlockEntity(pos);
        MachineProcessHandler handler = MachineProcessRegistry.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("binniebotany", "tileworker"));
        int blank = DesignCatalog.DESIGNS.indexOf("blank");
        machine.setManualSelection(blank);
        machine.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, BotanyWorldContent.CERAMIC_ITEM.get().createStack("white"));
        machine.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, BotanyWorldContent.CERAMIC_ITEM.get().createStack("black"));
        machine.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(BotanyContent.MORTAR.get()));
        ItemStack crafted = handler.takeManualPreview(machine, ItemStack.EMPTY);
        helper.assertTrue(crafted.getCount() == 1, "Blank Tileworker design did not yield one tile");
        helper.assertTrue(machine.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT).is(BotanyWorldContent.CERAMIC_ITEM.get()),
                "Blank Tileworker design consumed its preview-only secondary ceramic");
        helper.succeed();
    }

    private static FlowerBlockEntity requireFlower(GameTestHelper helper, BlockPos relative) {
        if (helper.getBlockEntity(relative) instanceof FlowerBlockEntity flower) return flower;
        helper.fail("Expected FlowerBlockEntity at " + relative);
        throw new IllegalStateException("missing FlowerBlockEntity");
    }

    private static void assertFlowerEquivalent(GameTestHelper helper, IFlower expected, IFlower actual) {
        helper.assertTrue(expected.getGeneration() == actual.getGeneration(), "Generation changed after reload");
        helper.assertTrue(expected.getAge() == actual.getAge(), "Age changed after reload");
        helper.assertTrue(expected.hasFlowered() == actual.hasFlowered(), "Flowered state changed after reload");
        helper.assertTrue(expected.isWilted() == actual.isWilted(), "Wilted state changed after reload");
        helper.assertTrue(expected.isAnalyzed() == actual.isAnalyzed(), "Analysis state changed after reload");
        helper.assertTrue(actual.getMate() != null, "Mate genome changed after reload");
        for (IChromosome<?> chromosome : FlowerSpeciesType.get().getKaryotype().getChromosomes()) {
            helper.assertTrue(expected.getGenome().getAllelePair(chromosome).equals(actual.getGenome().getAllelePair(chromosome)),
                    "Chromosome changed after reload: " + chromosome.id());
        }
    }
}
