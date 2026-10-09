package binnie.trees.gametest;

import binnie.botany.registry.BotanyWorldContent;
import binnie.core.data.BeverageData;
import binnie.core.data.DesignData;
import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import binnie.core.network.CoreNetwork;
import binnie.core.network.ManualMachineCraftPayload;
import binnie.core.network.ManualMachineSelectionPayload;
import binnie.core.registry.BinnieDataComponents;
import binnie.design.data.DesignCatalog;
import binnie.trees.registry.BinnieTreeWoodContent;
import binnie.trees.registry.BinnieTreesContent;
import binnie.trees.registry.BinnieTreesFluids;
import binnie.trees.registry.BinnieTreesMachines;
import binnie.trees.registry.BinnieTreesDesignContent;
import binnie.trees.wood.BinnieTreeWoodType;
import forestry.api.core.machines.ICarpenterRecipe;
import forestry.api.core.machines.ISqueezerRecipe;
import forestry.api.core.machines.IStillRecipe;
import forestry.api.core.machines.fuels.BiogasEngineFuel;
import forestry.api.ForestryDataMaps;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.world.entity.player.Inventory;

/** Real registered-machine coverage for 1.12-compatible Binnie's Trees processing. */
@GameTestHolder("binniecore")
@PrefixGameTestTemplate(false)
public class BinnieTreesMachineGameTests {
    private BinnieTreesMachineGameTests() {}

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 300)
    public static void lumbermillConsumesExactWaterAndEnergyAndKeepsAllThreeOutputs(GameTestHelper helper) {
        MachineBlockEntity machine = placeMachine(helper, new BlockPos(2, 2, 2), "lumbermill");
        helper.assertTrue(machine.tank().getCapacity() == 10_000, "Lumbermill lost its source 10,000 mB water capacity");
        fillEnergy(machine);
        fill(machine, new FluidStack(Fluids.WATER, 300));
        BinnieTreeWoodContent.WoodSet elm = BinnieTreeWoodContent.get(BinnieTreeWoodType.ELM);
        machine.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, new ItemStack(elm.logItem().get()));

        helper.succeedWhen(() -> {
            ItemStack planks = machine.inventory().getStackInSlot(MachineBlockEntity.OUTPUT_SLOT);
            ItemStack bark = machine.inventory().getStackInSlot(MachineBlockEntity.SECONDARY_OUTPUT_SLOT);
            ItemStack sawdust = machine.inventory().getStackInSlot(MachineBlockEntity.TERTIARY_OUTPUT_SLOT);
            helper.assertTrue(planks.is(elm.planksItem().get()) && planks.getCount() == 4, "Lumbermill lost its plank output");
            helper.assertTrue(bark.is(BinnieTreesContent.BARK.get()) && bark.getCount() == 2, "Lumbermill lost bark byproduct");
            helper.assertTrue(sawdust.is(BinnieTreesContent.SAWDUST.get()) && sawdust.getCount() == 2, "Lumbermill lost sawdust byproduct");
            helper.assertTrue(machine.tank().getFluidAmount() == 0, "Lumbermill did not consume exactly 300 mB water");
            helper.assertTrue(machine.energy().getMaxEnergyStored() - machine.energy().getEnergyStored() == 900,
                    "Lumbermill did not consume exactly 900 FE");
        });
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 100)
    public static void lumbermillPausesBeforeOutputWhenSourceWaterRunsOut(GameTestHelper helper) {
        MachineBlockEntity machine = placeMachine(helper, new BlockPos(2, 2, 2), "lumbermill");
        fillEnergy(machine);
        fill(machine, new FluidStack(Fluids.WATER, 20));
        BinnieTreeWoodContent.WoodSet elm = BinnieTreeWoodContent.get(BinnieTreeWoodType.ELM);
        machine.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, new ItemStack(elm.logItem().get()));
        helper.startSequence().thenExecuteAfter(40, () -> {
            helper.assertTrue(machine.tank().getFluidAmount() == 0, "Lumbermill did not consume water on source ticks");
            helper.assertTrue(machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT).is(elm.logItem().get()),
                    "Lumbermill completed after its per-tick water requirement failed");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 400)
    public static void fruitPressPreservesTypedJuiceIdentityAndRejectsMixedOutput(GameTestHelper helper) {
        MachineBlockEntity press = placeMachine(helper, new BlockPos(2, 2, 2), "press");
        fillEnergy(press);
        press.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, BinnieTreesContent.FOOD.get().createStack("orange"));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(press.outputTank().getFluidAmount() == 400,
                        "Fruit Press did not produce 400 mB orange juice"))
                .thenExecute(() -> {
                    BeverageData data = press.outputTank().getFluid().get(BinnieDataComponents.BEVERAGE);
                    helper.assertTrue(data != null && data.beverage().equals("binnietrees:juice/orange"),
                            "Fruit Press output lost typed orange identity");
                    helper.assertTrue(press.energy().getMaxEnergyStored() - press.energy().getEnergyStored() == 1_000,
                            "Fruit Press did not consume exactly 1,000 FE");
                    press.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, BinnieTreesContent.FOOD.get().createStack("pear"));
                })
                .thenExecuteAfter(80, () -> {
                    helper.assertTrue(press.inventory().getStackInSlot(6).is(BinnieTreesContent.FOOD.get())
                                    && press.inventory().getStackInSlot(6).getCount() == 1,
                            "Fruit Press did not retain the protected current fruit when its typed output tank blocked it");
                    helper.assertTrue(press.outputTank().getFluidAmount() == 400,
                            "Fruit Press mixed incompatible beverage identities");
                    helper.succeed();
                });
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 1_200)
    public static void breweryFermentsJuiceAndBrewsSourceGrainRecipes(GameTestHelper helper) {
        MachineBlockEntity cider = placeMachine(helper, new BlockPos(2, 2, 2), "brewery");
        MachineBlockEntity lager = placeMachine(helper, new BlockPos(5, 2, 2), "brewery");
        fillEnergy(cider);
        fillEnergy(lager);
        fill(cider, BinnieTreesFluids.stack("juice", "apple", 1_000));
        cider.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(BinnieTreesContent.YEAST.get()));
        fill(lager, new FluidStack(Fluids.WATER, 1_000));
        lager.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, new ItemStack(BinnieTreesContent.GRAIN_BARLEY.get()));
        lager.inventory().setStackInSlot(6, new ItemStack(BinnieTreesContent.GRAIN_BARLEY.get()));
        lager.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, new ItemStack(BinnieTreesContent.HOPS.get()));
        lager.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(BinnieTreesContent.LAGER_YEAST.get()));

        helper.succeedWhen(() -> {
            helper.assertTrue(cider.outputTank().getFluidAmount() == 1_000
                            && BinnieTreesFluids.isBeverage(cider.outputTank().getFluid(), "alcohol", "apple"),
                    "Brewery did not ferment apple juice into one bucket of cider");
            helper.assertTrue(lager.outputTank().getFluidAmount() == 1_000
                            && BinnieTreesFluids.isBeverage(lager.outputTank().getFluid(), "alcohol", "lager"),
                    "Brewery did not apply the lager yeast and hops recipe");
            helper.assertTrue(cider.energy().getMaxEnergyStored() - cider.energy().getEnergyStored() == 16_000,
                    "Fermentation did not consume 16,000 FE");
            helper.assertTrue(lager.energy().getMaxEnergyStored() - lager.energy().getEnergyStored() == 16_000,
                    "Grain brewing did not consume 16,000 FE");
            helper.assertTrue(cider.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).isEmpty(),
                    "Brewery did not consume yeast");
            helper.assertTrue(lager.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT).isEmpty()
                            && lager.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT).isEmpty()
                            && lager.inventory().getStackInSlot(6).isEmpty()
                            && lager.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).isEmpty(),
                    "Brewery did not consume its locked grain, ingredient and yeast inputs");
        });
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 1_200)
    public static void breweryLocksAndPersistsItsCraftingSnapshot(GameTestHelper helper) {
        MachineBlockEntity brewery = placeMachine(helper, new BlockPos(2, 2, 2), "brewery");
        fillEnergy(brewery);
        fill(brewery, BinnieTreesFluids.stack("juice", "apple", 1_000));
        brewery.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(BinnieTreesContent.YEAST.get()));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(brewery.processData().getBoolean("BreweryLocked"), "Brewery did not lock its source crafting snapshot"))
                .thenExecute(() -> {
                    helper.assertTrue(brewery.inventory().getStackInSlot(12).is(BinnieTreesContent.YEAST.get()), "Brewery did not reserve yeast in source reserve storage");
                    brewery.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT, new ItemStack(BinnieTreesContent.LAGER_YEAST.get()));
                    CompoundTag saved = brewery.saveWithoutMetadata(helper.getLevel().registryAccess());
                    brewery.loadWithComponents(saved, helper.getLevel().registryAccess());
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(BinnieTreesFluids.isBeverage(brewery.outputTank().getFluid(), "alcohol", "apple"),
                            "Brewery changed its locked recipe after visible inputs changed or reload occurred");
                    helper.assertTrue(brewery.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).is(BinnieTreesContent.LAGER_YEAST.get()),
                            "Brewery consumed a replacement visible input instead of its reserved snapshot");
                })
                .thenExecute(helper::succeed);
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 4_200)
    public static void distilleryModePersistsAndAppliesLegacyYield(GameTestHelper helper) {
        MachineBlockEntity original = placeMachine(helper, new BlockPos(2, 2, 2), "distillery");
        helper.assertTrue(original.setMode(2), "Distillery rejected its third source-defined level");
        helper.assertTrue(!original.setMode(3), "Distillery accepted an out-of-range level");
        CompoundTag saved = original.saveWithoutMetadata(helper.getLevel().registryAccess());
        MachineBlockEntity restored = placeMachine(helper, new BlockPos(5, 2, 2), "distillery");
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.mode() == 2, "Distillery level did not survive save/reload");
        fillEnergy(restored);
        fill(restored, BinnieTreesFluids.stack("alcohol", "apple", 1_000));

        helper.succeedWhen(() -> {
            helper.assertTrue(restored.outputTank().getFluidAmount() == 200
                            && BinnieTreesFluids.isBeverage(restored.outputTank().getFluid(), "spirit", "neutral_spirit"),
                    "Triple distillation did not produce the 20% neutral-spirit yield");
            helper.assertTrue(restored.energy().getMaxEnergyStored() - restored.energy().getEnergyStored() == 7_200,
                    "Level-three distillation did not consume 2 FE/t for 3,600 ticks");
        });
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 2_600)
    public static void distilleryLocksCurrentBucketAcrossTankMutationAndReload(GameTestHelper helper) {
        MachineBlockEntity still = placeMachine(helper, new BlockPos(2, 2, 2), "distillery");
        fillEnergy(still);
        fill(still, BinnieTreesFluids.stack("alcohol", "apple", 1_000));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(still.processData().getBoolean("DistilleryLocked"),
                        "Distillery did not move its selected bucket into persisted currentFluid"))
                .thenExecute(() -> {
                    helper.assertTrue(BinnieTreesFluids.isBeverage(still.secondaryTank().getFluid(), "alcohol", "apple"),
                            "Distillery currentFluid lost its typed apple identity");
                    fill(still, BinnieTreesFluids.stack("alcohol", "barley", 1_000));
                    CompoundTag saved = still.saveWithoutMetadata(helper.getLevel().registryAccess());
                    still.loadWithComponents(saved, helper.getLevel().registryAccess());
                })
                .thenWaitUntil(() -> helper.assertTrue(BinnieTreesFluids.isBeverage(still.outputTank().getFluid(), "spirit", "apple_brandy"),
                        "Distillery recalculated its active recipe from mutated live input after reload"))
                .thenExecute(helper::succeed);
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 200)
    public static void designersUseSourcePreviewSelectionAndAdhesive(GameTestHelper helper) {
        MachineBlockEntity woodworker = placeMachine(helper, new BlockPos(1, 2, 2), "woodworker");
        MachineBlockEntity panelworker = placeMachine(helper, new BlockPos(4, 2, 2), "panelworker");
        MachineBlockEntity glassworker = placeMachine(helper, new BlockPos(7, 2, 2), "glassworker");
        MachineBlockEntity invalidWoodworker = placeMachine(helper, new BlockPos(10, 2, 2), "woodworker");
        MachineBlockEntity invalidGlassworker = placeMachine(helper, new BlockPos(13, 2, 2), "glassworker");
        BinnieTreeWoodContent.WoodSet elm = BinnieTreeWoodContent.get(BinnieTreeWoodType.ELM);
        BinnieTreeWoodContent.WoodSet pear = BinnieTreeWoodContent.get(BinnieTreeWoodType.PEAR);
        setupDesigner(woodworker, new ItemStack(elm.planksItem().get()), new ItemStack(pear.planksItem().get()));
        setupDesigner(panelworker, new ItemStack(elm.planksItem().get()), new ItemStack(pear.planksItem().get()));
        setupDesigner(glassworker, BotanyWorldContent.STAINED_GLASS_ITEM.get().createStack("red"),
                BotanyWorldContent.STAINED_GLASS_ITEM.get().createStack("royal_blue"));
        setupDesigner(invalidWoodworker, new ItemStack(Items.OAK_SLAB), new ItemStack(elm.planksItem().get()));
        setupDesigner(invalidGlassworker, new ItemStack(Items.GLASS), new ItemStack(Items.WHITE_STAINED_GLASS));
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        int octagon = DesignCatalog.DESIGNS.indexOf("octagon");
        helper.assertTrue(octagon >= 0, "Design catalog lost the source octagon layout");

        ItemStack wood = craftDesigner(helper, player, woodworker, 41, octagon);
        assertDesigned(helper, wood, BinnieTreesDesignContent.PATTERNED_WOOD.get(), "woodworker");
        helper.assertTrue(woodworker.energy().getEnergyStored() == 0,
                "Source Designer unexpectedly required Binnie processor energy");
        helper.assertTrue(woodworker.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT).isEmpty(),
                "Woodworker did not consume exactly one Wood Polish");

        ItemStack panel = craftDesigner(helper, player, panelworker, 42, octagon);
        assertDesigned(helper, panel, BinnieTreesDesignContent.PATTERNED_PANEL.get(), "panelworker");
        ItemStack glass = craftDesigner(helper, player, glassworker, 43, octagon);
        assertDesigned(helper, glass, BinnieTreesDesignContent.PATTERNED_GLASS.get(), "glassworker");
        DesignData glassDesign = glass.get(BinnieDataComponents.DESIGN);
        helper.assertTrue(glassDesign != null && glassDesign.primaryVariant().equals("red")
                        && glassDesign.secondaryVariant().equals("royal_blue"),
                "Glassworker lost Botany stained-glass colour components");

        setupDesigner(panelworker, new ItemStack(elm.planksItem().get()), new ItemStack(pear.planksItem().get()));
        ItemStack blank = craftDesigner(helper, player, panelworker, 46, DesignCatalog.DESIGNS.indexOf("blank"));
        DesignData blankDesign = blank.get(BinnieDataComponents.DESIGN);
        helper.assertTrue(blank.is(BinnieTreesDesignContent.PATTERNED_PANEL.get()) && blank.getCount() == 1
                        && blankDesign != null && blankDesign.primaryMaterial().equals(blankDesign.secondaryMaterial()),
                "Blank source design did not yield one primary-material panel");
        helper.assertTrue(panelworker.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT).is(pear.planksItem().get()),
                "Blank source design consumed its preview-only second material");

        MachineMenu invalidMenu = new MachineMenu(44, new Inventory(player), invalidWoodworker, invalidWoodworker.data());
        player.containerMenu = invalidMenu;
        helper.assertTrue(!CoreNetwork.handleManualCraft(new ManualMachineCraftPayload(44), player),
                "Woodworker accepted a non-source slab material");
        helper.assertTrue(invalidWoodworker.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT).is(Items.OAK_SLAB),
                "Invalid Woodworker input was consumed");
        MachineMenu invalidGlassMenu = new MachineMenu(45, new Inventory(player), invalidGlassworker, invalidGlassworker.data());
        player.containerMenu = invalidGlassMenu;
        helper.assertTrue(!CoreNetwork.handleManualCraft(new ManualMachineCraftPayload(45), player),
                "Glassworker accepted clear glass, which was not a legacy Design material");

        player.containerMenu = invalidMenu;
        helper.assertTrue(!CoreNetwork.handleManualSelection(new ManualMachineSelectionPayload(44, -1), player),
                "Designer accepted a negative source design index");
        player.containerMenu = invalidGlassMenu;
        helper.assertTrue(!CoreNetwork.handleManualSelection(new ManualMachineSelectionPayload(45, DesignCatalog.DESIGNS.size()), player),
                "Designer accepted an out-of-range source design index");
        CompoundTag saved = glassworker.saveWithoutMetadata(helper.getLevel().registryAccess());
        MachineBlockEntity restored = placeMachine(helper, new BlockPos(16, 2, 2), "glassworker");
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.manualSelection() == octagon,
                "Selected Designer layout did not survive save/reload");
        helper.succeed();
    }

    /**
      * Exercises the Binnie-owned ForestryCE recipe integration after a real
      * server datapack load. Forestry executes these supported recipe types;
      * Binnie supplies only the 1.12-compatible inputs, outputs and timings.
      */
    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 100)
    public static void sourceWoodPolishForestryChainLoadsWithExactAmounts(GameTestHelper helper) {
        java.util.List<BinnieTreesFluids.ProcessFluid> sourceLiquids = java.util.List.of(
                BinnieTreesFluids.RESIN, BinnieTreesFluids.TURPENTINE);
        for (BinnieTreesFluids.ProcessFluid liquid : sourceLiquids) {
            var flowing = (net.minecraft.world.level.material.FlowingFluid) liquid.fluid();
            helper.assertTrue(flowing.getBucket() == liquid.bucket(),
                    "Fixed Binnie's Trees liquid lost its legacy container behavior");
            helper.assertTrue(flowing.defaultFluidState().createLegacyBlock().getBlock() == liquid.block(),
                    "Fixed Binnie's Trees liquid lost its legacy placeable block");
            helper.assertTrue(net.neoforged.neoforge.common.CommonHooks
                            .getCraftingRemainingItem(new ItemStack(liquid.bucket()))
                            .is(net.minecraft.world.item.Items.BUCKET),
                    "Fixed Binnie's Trees liquid bucket must return an empty bucket");
        }
        BiogasEngineFuel resinFuel = BinnieTreesFluids.RESIN.stack(1).getFluidHolder().getData(ForestryDataMaps.BIOGAS_FUELS);
        helper.assertTrue(resinFuel != null && resinFuel.powerPerCycle() == 30
                        && resinFuel.burnDuration() == 10_000 && resinFuel.dissipationMultiplier() == 1,
                "Resin lost its source Biogas Engine fuel registration");
        helper.assertTrue(BinnieTreesFluids.TURPENTINE.stack(1).getFluidHolder().getData(ForestryDataMaps.BIOGAS_FUELS) == null,
                "Carpenter reagent turpentine was incorrectly made an engine fuel");

        var recipes = helper.getLevel().getRecipeManager();
        ISqueezerRecipe resin = recipes.getAllRecipesFor(FactoryRecipeTypes.SQUEEZER.type()).stream()
                .filter(recipe -> recipe.id().equals(ResourceLocation.fromNamespaceAndPath("binnietrees", "squeezer/spruce_resin")))
                .findFirst().orElseThrow(() -> new AssertionError("Missing Binnie's Trees spruce-resin Squeezer recipe")).value();
        helper.assertTrue(resin.getProcessingTime() == 10
                        && resin.getInputs().size() == 1
                        && resin.getInputs().getFirst().test(new ItemStack(Items.SPRUCE_LOG))
                        && resin.getFluidOutput().createFluidStack().is(BinnieTreesFluids.RESIN.fluid())
                        && resin.getFluidOutput().createFluidStack().getAmount() == 50
                        && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(resin.getRemnants().getItem())
                                .equals(ResourceLocation.fromNamespaceAndPath("forestry", "wood_pulp"))
                        && resin.getRemnantsChance() == 0.5F,
                "Source spruce Squeezer recipe did not retain resin, pulp, chance, or timing");

        IStillRecipe turpentine = recipes.getAllRecipesFor(FactoryRecipeTypes.STILL.type()).stream()
                .filter(recipe -> recipe.id().equals(ResourceLocation.fromNamespaceAndPath("binnietrees", "still/resin_to_turpentine")))
                .findFirst().orElseThrow(() -> new AssertionError("Missing Binnie's Trees resin-to-turpentine Still recipe")).value();
        helper.assertTrue(turpentine.getCyclesPerUnit() == 25
                        && turpentine.matches(BinnieTreesFluids.RESIN.stack(5))
                        && turpentine.getOutput().is(BinnieTreesFluids.TURPENTINE.fluid())
                        && turpentine.getOutput().getAmount() == 3,
                "Source Still recipe did not retain the 5 mB resin -> 3 mB turpentine ratio");

        ICarpenterRecipe woodWax = recipes.getAllRecipesFor(FactoryRecipeTypes.CARPENTER.type()).stream()
                .filter(recipe -> recipe.id().equals(ResourceLocation.fromNamespaceAndPath("binnietrees", "carpenter/wood_wax_turpentine")))
                .findFirst().orElseThrow(() -> new AssertionError("Missing Binnie's Trees turpentine Wood Polish Carpenter recipe")).value();
        ItemStack output = woodWax.getCraftingGridRecipe().getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(woodWax.getPackagingTime() == 25
                        && woodWax.getInputFluid().is(BinnieTreesFluids.TURPENTINE.fluid())
                        && woodWax.getInputFluid().getAmount() == 50
                        && output.is(BinnieTreesContent.WOOD_WAX.get()) && output.getCount() == 4,
                "Source Carpenter recipe did not retain its 50 mB turpentine -> four Wood Polish output");

    // Forestry 1.12's Carpenter accepted the pulpWood ore entry. Extra
    // Trees registered sawdust into that entry, so its two-sawdust paper
    // conversion was inherited instead of declared by Binnie. ForestryCE
    // narrowed its stock recipe to Forestry wood pulp; this companion data
    // recipe provides the former tag-based integration exactly.
        ICarpenterRecipe sawdustPaper = recipes.getAllRecipesFor(FactoryRecipeTypes.CARPENTER.type()).stream()
                .filter(recipe -> recipe.id().equals(ResourceLocation.fromNamespaceAndPath("binnietrees", "carpenter/sawdust_paper")))
                .findFirst().orElseThrow(() -> new AssertionError("Missing source-compatible sawdust Carpenter paper recipe")).value();
        ItemStack paper = sawdustPaper.getCraftingGridRecipe().getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(sawdustPaper.getPackagingTime() == 5
                        && sawdustPaper.getInputFluid().is(net.minecraft.world.level.material.Fluids.WATER)
                        && sawdustPaper.getInputFluid().getAmount() == 250
                        && sawdustPaper.getCraftingGridRecipe().getIngredients().size() == 2
                        && sawdustPaper.getCraftingGridRecipe().getIngredients().stream()
                                .allMatch(ingredient -> ingredient.test(new ItemStack(BinnieTreesContent.SAWDUST.get())))
                        && paper.is(Items.PAPER) && paper.getCount() == 1,
                "Binnie's Trees sawdust must retain the source Forestry Carpenter two-pulp, 250 mB water paper recipe");

        record FoodSqueezer(String item, net.minecraft.world.level.material.Fluid fluid, int amount, int time, float chance) {}
        java.util.List<FoodSqueezer> foodRecipes = java.util.List.of(
                new FoodSqueezer("crabapple", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 150, 10, 0.10F),
                new FoodSqueezer("orange", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 400, 10, 0.15F),
                new FoodSqueezer("kumquat", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 300, 10, 0.10F),
                new FoodSqueezer("lime", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 300, 10, 0.10F),
                new FoodSqueezer("wild_cherry", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 50, 20, 0.05F),
                new FoodSqueezer("sour_cherry", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 50, 20, 0.03F),
                new FoodSqueezer("black_cherry", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 50, 20, 0.05F),
                new FoodSqueezer("cherry_plum", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 100, 10, 0.60F),
                new FoodSqueezer("almond", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 80, 20, 0.05F),
                new FoodSqueezer("apricot", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 150, 10, 0.40F),
                new FoodSqueezer("grapefruit", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 500, 10, 0.15F),
                new FoodSqueezer("peach", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 150, 10, 0.40F),
                new FoodSqueezer("satsuma", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 300, 10, 0.10F),
                new FoodSqueezer("buddha_hand", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 400, 10, 0.15F),
                new FoodSqueezer("citron", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 400, 10, 0.15F),
                new FoodSqueezer("finger_lime", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 300, 10, 0.10F),
                new FoodSqueezer("key_lime", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 300, 10, 0.10F),
                new FoodSqueezer("manderin", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 400, 10, 0.10F),
                new FoodSqueezer("nectarine", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 150, 10, 0.40F),
                new FoodSqueezer("pomelo", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 300, 10, 0.10F),
                new FoodSqueezer("tangerine", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 300, 10, 0.10F),
                new FoodSqueezer("pear", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 300, 10, 0.20F),
                new FoodSqueezer("sand_pear", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 200, 10, 0.10F),
                new FoodSqueezer("hazelnut", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 150, 20, 0.05F),
                new FoodSqueezer("butternut", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 180, 20, 0.05F),
                new FoodSqueezer("beechnut", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 100, 20, 0.04F),
                new FoodSqueezer("pecan", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 50, 29, 0.02F),
                new FoodSqueezer("banana", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 100, 10, 0.30F),
                new FoodSqueezer("red_banana", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 100, 10, 0.30F),
                new FoodSqueezer("plantain", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 100, 10, 0.40F),
                new FoodSqueezer("brazil_nut", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 20, 20, 0.02F),
                new FoodSqueezer("fig", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 50, 20, 0.03F),
                new FoodSqueezer("acorn", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 50, 20, 0.03F),
                new FoodSqueezer("elderberry", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 100, 10, 0.05F),
                new FoodSqueezer("olive", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 50, 20, 0.03F),
                new FoodSqueezer("gingko_nut", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 50, 20, 0.05F),
                new FoodSqueezer("coffee", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 20, 15, 0.02F),
                new FoodSqueezer("osange_orange", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 300, 10, 0.15F),
                new FoodSqueezer("clove", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 25, 10, 0.02F),
                new FoodSqueezer("coconut", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 300, 20, 0.25F),
                new FoodSqueezer("cashew", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 150, 10, 0.15F),
                new FoodSqueezer("avocado", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 300, 10, 0.15F),
                new FoodSqueezer("nutmeg", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 50, 20, 0.10F),
                new FoodSqueezer("allspice", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 50, 20, 0.10F),
                new FoodSqueezer("chilli", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 100, 10, 0.10F),
                new FoodSqueezer("star_anise", forestry.core.platform.fluids.ForestryFluids.SEED_OIL.getFluid(), 100, 20, 0.10F),
                new FoodSqueezer("mango", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 400, 10, 0.20F),
                new FoodSqueezer("starfruit", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 300, 10, 0.10F),
                new FoodSqueezer("candlenut", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 50, 20, 0.10F),
                new FoodSqueezer("cranberry", forestry.core.platform.fluids.ForestryFluids.JUICE.getFluid(), 50, 10, 0.05F));
        for (FoodSqueezer expected : foodRecipes) {
            ISqueezerRecipe actual = recipes.getAllRecipesFor(FactoryRecipeTypes.SQUEEZER.type()).stream()
                    .filter(recipe -> recipe.id().equals(ResourceLocation.fromNamespaceAndPath(
                            "binnietrees", "squeezer/food/" + expected.item())))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing legacy food Squeezer recipe: " + expected.item())).value();
            ItemStack input = BinnieTreesContent.FOOD.get().createStack(expected.item());
            helper.assertTrue(actual.getInputs().size() == 1 && actual.getInputs().getFirst().test(input)
                            && actual.getProcessingTime() == expected.time()
                            && actual.getFluidOutput().createFluidStack().is(expected.fluid())
                            && actual.getFluidOutput().createFluidStack().getAmount() == expected.amount()
                            && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(actual.getRemnants().getItem())
                                    .equals(ResourceLocation.fromNamespaceAndPath("forestry", "mulch"))
                            && Math.abs(actual.getRemnantsChance() - expected.chance()) < 0.0001F,
                    "Legacy Squeezer values differ for Binnie's Trees food " + expected.item());
        }
        helper.succeed();
    }

    private static void setupDesigner(MachineBlockEntity machine, ItemStack primary, ItemStack secondary) {
        machine.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, primary);
        machine.inventory().setStackInSlot(MachineBlockEntity.CATALYST_SLOT, secondary);
        machine.inventory().setStackInSlot(MachineBlockEntity.REAGENT_SLOT,
                machine.spec().id().getPath().equals("glassworker")
                        ? new ItemStack(BinnieTreesContent.GLASS_FITTING.get())
                        : new ItemStack(BinnieTreesContent.WOOD_WAX.get()));
    }

    private static ItemStack craftDesigner(GameTestHelper helper, net.minecraft.server.level.ServerPlayer player,
                                           MachineBlockEntity machine, int containerId, int selection) {
        MachineMenu menu = new MachineMenu(containerId, new Inventory(player), machine, machine.data());
        player.containerMenu = menu;
        helper.assertTrue(CoreNetwork.handleManualSelection(new ManualMachineSelectionPayload(containerId, selection), player),
                "Valid Designer selection payload was rejected");
        helper.assertTrue(CoreNetwork.handleManualCraft(new ManualMachineCraftPayload(containerId), player),
                "Valid Designer preview-slot payload was rejected");
        ItemStack result = menu.getCarried().copy();
        menu.setCarried(ItemStack.EMPTY);
        return result;
    }

    private static void assertDesigned(GameTestHelper helper, ItemStack result, net.minecraft.world.item.Item expected, String name) {
        DesignData design = result.get(BinnieDataComponents.DESIGN);
        helper.assertTrue(result.is(expected) && result.getCount() == 2, name + " did not output two designed products");
        helper.assertTrue(design != null && design.design().equals("octagon")
                        && (!design.primaryMaterial().equals(design.secondaryMaterial())
                            || !design.primaryVariant().equals(design.secondaryVariant())),
                name + " did not preserve its design and paired materials");
    }


    private static MachineBlockEntity placeMachine(GameTestHelper helper, BlockPos relative, String id) {
        helper.setBlock(relative, BinnieTreesMachines.REGISTRY.block(id).get());
        if (helper.getBlockEntity(relative) instanceof MachineBlockEntity machine) return machine;
        helper.fail("Expected registered " + id + " machine block entity");
        throw new IllegalStateException("Missing machine block entity");
    }

    private static void fillEnergy(MachineBlockEntity machine) {
        int guard = 0;
        while (machine.energy().getEnergyStored() < machine.energy().getMaxEnergyStored() && guard++ < 1_000) {
            if (machine.energy().receiveEnergy(machine.energy().getMaxEnergyStored(), false) <= 0) break;
        }
        if (machine.energy().getEnergyStored() != machine.energy().getMaxEnergyStored()) {
            throw new IllegalStateException("Could not fill machine energy");
        }
    }

    private static void fill(MachineBlockEntity machine, FluidStack stack) {
        int accepted = machine.tank().fill(stack, IFluidHandler.FluidAction.EXECUTE);
        if (accepted != stack.getAmount()) throw new IllegalStateException("Could not fill machine tank");
    }
}
