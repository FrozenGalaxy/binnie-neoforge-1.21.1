package binnie.trees.machine;

import binnie.core.data.BeverageData;
import binnie.core.data.DesignData;
import binnie.core.data.VariantData;
import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineProcessHandler;
import binnie.core.machine.MachineProcessPlan;
import binnie.core.machine.MachineProcessError;
import binnie.core.machine.MachineProcessRegistry;
import binnie.core.registry.BinnieDataComponents;
import binnie.design.data.DesignCatalog;
import binnie.design.data.DesignMaterialCatalog;
import binnie.trees.BinnieTrees;
import binnie.trees.beverage.LegacyBeverageCatalog;
import binnie.design.block.DesignedBlockItem;
import binnie.trees.registry.BinnieTreeWoodContent;
import binnie.trees.registry.BinnieTreesContent;
import binnie.trees.registry.BinnieTreesFluids;
import binnie.trees.registry.BinnieTreesDesignContent;
import binnie.trees.wood.BinnieTreeWoodType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Dynamic processing for the seven Binnie's Trees machines. */
public class BinnieTreesMachineProcesses {
    private static final java.util.List<FruitPressOperation> FRUIT_PRESS_OPERATIONS = createFruitPressOperations();
    private static final java.util.List<TaggedFruitPressOperation> TAGGED_FRUIT_PRESS_OPERATIONS = createTaggedFruitPressOperations();
    private static final java.util.List<BreweryFermentationOperation> BREWERY_FERMENTATIONS = createBreweryFermentations();
    private static final java.util.List<BreweryGrainOperation> BREWERY_GRAINS = createBreweryGrains();
    private static final Map<String, Distillation> DISTILLATION = distillationMappings();
    private static final java.util.List<DistillationRoute> DISTILLATION_ROUTES = createDistillationRoutes();
    private static boolean registered;

    private BinnieTreesMachineProcesses() {}

    /**
      * Immutable route data shared by the machine planner and JEI.
      */
    /** Binnie-owned routes copied by ModuleAlcohol from Forestry's
      * squeezer registry. Squeezer mulch is provenance only: FruitPressRecipe
      * itself has no item output or chance field. */
    public static java.util.List<FruitPressOperation> fruitPressOperations() { return FRUIT_PRESS_OPERATIONS; }
    public static java.util.List<TaggedFruitPressOperation> taggedFruitPressOperations() { return TAGGED_FRUIT_PRESS_OPERATIONS; }

    public static String fermentationRoute(String beverage) { return fermentationResult(beverage); }
    public static java.util.List<BreweryFermentationOperation> breweryFermentations() { return BREWERY_FERMENTATIONS; }
    public static java.util.List<BreweryGrainOperation> breweryGrainOperations() { return BREWERY_GRAINS; }

    public static java.util.List<DistillationRoute> distillationRoutes() {
        return DISTILLATION_ROUTES;
    }

    public static FluidStack beverageStack(String category, String value, int amount) {
        return beverage(category, value, amount);
    }

    public static int lumbermillDurationForAudit() { return 30; }
    public static int lumbermillEnergyForAudit() { return 900; }
    public static int lumbermillWaterDrainForAudit() { return 10; }
    public static int lumbermillWaterThresholdForAudit() { return 5; }

    public record DistillationRoute(String inputBeverage, int mode, String outputSpirit, int outputAmount) { }
    public record BeverageIdentity(String category, String value) { }
    public record BreweryFermentationOperation(String inputBeverage, String outputAlcohol) { }

    public static BeverageIdentity beverageIdentity(String id) {
        int namespace = id.indexOf(':');
        int separator = id.indexOf('/', namespace + 1);
        if (namespace < 1 || separator <= namespace + 1 || separator == id.length() - 1) {
            throw new IllegalArgumentException("Invalid beverage identity " + id);
        }
        return new BeverageIdentity(id.substring(namespace + 1, separator), id.substring(separator + 1));
    }
    public record BreweryGrainOperation(String grain, String ingredient, String yeast, String outputAlcohol) { }
    public record FruitPressOperation(String inputVariant, String juice, int outputAmount, int sourceSqueezerTime,
                                      int sourceMulchChance, boolean sourceWasSeedOil) { }
    /** Modern common-tag equivalent of ModuleAlcohol's 1.12 ore-dictionary lookup. */
    public record TaggedFruitPressOperation(ResourceLocation inputTag, String juice, int outputAmount) {
        public TagKey<net.minecraft.world.item.Item> tag() { return TagKey.create(Registries.ITEM, inputTag); }
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        register("lumbermill", BinnieTreesMachineProcesses::lumbermill, BinnieTreesMachineProcesses::lumbermillSlot, 0);
        register("press", BinnieTreesMachineProcesses::press, BinnieTreesMachineProcesses::pressSlot, 0);
        register("brewery", BinnieTreesMachineProcesses::brewery, BinnieTreesMachineProcesses::brewerySlot, 0);
        register("distillery", BinnieTreesMachineProcesses::distillery, BinnieTreesMachineProcesses::distillerySlot, 2);
        registerDesigner("woodworker");
        registerDesigner("panelworker");
        registerDesigner("glassworker");
    }

    private static void register(String id, Planner planner, SlotValidator validator, int maxMode) {
        MachineProcessRegistry.register(ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, id), new MachineProcessHandler() {
            @Override
            public Optional<MachineProcessPlan> createPlan(Level level, MachineBlockEntity machine) {
                try {
                    if (id.equals("brewery") && machine.processData().getBoolean("BreweryLocked")) return lockedBreweryPlan(machine);
                    if (id.equals("distillery") && machine.processData().getBoolean("DistilleryLocked")) return lockedDistilleryPlan(machine);
                    return planner.create(level, machine);
                }
                catch (IllegalArgumentException | IllegalStateException ignored) { return Optional.empty(); }
            }

            @Override
            public void beforeTick(Level level, MachineBlockEntity machine) {
                if (!id.equals("press")) return;
        // ComponentInventoryTransfer restocks the protected current-fruit slot one item at a time.
                if (machine.inventory().getStackInSlot(6).isEmpty()) {
                    ItemStack moved = machine.inventory().extractItem(MachineBlockEntity.INPUT_SLOT, 1, false);
                    if (!moved.isEmpty()) machine.inventory().setStackInSlot(6, moved);
                }
            }
            @Override
            public boolean isItemValid(MachineBlockEntity machine, int slot, ItemStack stack) {
                return validator.valid(machine, slot, stack);
            }
            @Override
            public int maxMode(MachineBlockEntity machine) { return maxMode; }

            @Override
            public MachineProcessError diagnose(Level level, MachineBlockEntity machine) {
                return diagnoseTimed(id, level, machine);
            }

            @Override
            public boolean controlsFluidProgress() { return id.equals("press") || id.equals("lumbermill"); }

            @Override
            public boolean controlsFluidOutputProgress() { return id.equals("press"); }

            @Override
            public boolean handleScreenAction(Level level, MachineBlockEntity machine, String action) {
                if (!id.equals("press")) return false;
                if ("CLEAR_FRUIT".equals(action)) {
                    machine.cancelProcessProgress();
                    machine.inventory().setStackInSlot(6, ItemStack.EMPTY);
                    return true;
                }
                if (!"FRUIT_PRESS".equals(action)) return false;
        // WindowPress permits a hand press only while an item is in
        // the protected current-fruit cradle and tank output remains
        // valid. Two percentage points equal one 50-tick process tick.
                Optional<MachineProcessPlan> plan = press(level, machine);
                if (plan.isEmpty() || !canProgress(level, machine, plan.get())) return false;
                return machine.addManualProgress(1);
            }

            @Override
            public void onStarted(Level level, MachineBlockEntity machine, MachineProcessPlan plan) {
                if (id.equals("brewery")) lockBrewery(machine, plan);
                if (id.equals("distillery")) lockDistillery(machine, plan);
            }

            @Override
            public void onCompleted(Level level, MachineBlockEntity machine, MachineProcessPlan plan) {
                if (id.equals("brewery")) {
                    for (int slot = 8; slot <= 16; slot++) machine.inventory().setStackInSlot(slot, ItemStack.EMPTY);
                    machine.secondaryTank().setFluid(FluidStack.EMPTY);
                    machine.clearProcessData();
                }
                if (id.equals("distillery")) {
                    machine.secondaryTank().setFluid(FluidStack.EMPTY);
                    machine.clearProcessData();
                }
            }

            @Override
            public boolean canProgress(Level level, MachineBlockEntity machine, MachineProcessPlan plan) {
                if (id.equals("lumbermill")) return machine.tank().getFluid().is(Fluids.WATER) && machine.tank().getFluidAmount() >= 5;
                if (!id.equals("press")) return true;
                FluidStack output = plan.fluidOutput();
                FluidStack present = machine.outputTank().getFluid();
                return (present.isEmpty() || FluidStack.isSameFluidSameComponents(present, output))
                        && machine.outputTank().getCapacity() - machine.outputTank().getFluidAmount() >= 5;
            }

            @Override
            public MachineProcessError cannotProgressError(Level level, MachineBlockEntity machine, MachineProcessPlan plan) {
                return id.equals("lumbermill") ? MachineProcessError.TANK_EMPTY
                        : id.equals("press") ? MachineProcessError.OUTPUT_TANK_FULL
                        : MachineProcessError.INVALID_RECIPE;
            }

            @Override
            public void onProgress(Level level, MachineBlockEntity machine, MachineProcessPlan plan, int progress, int maxProgress) {
                if (id.equals("lumbermill")) {
                    machine.tank().drain(new FluidStack(Fluids.WATER, 10), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    return;
                }
                if (!id.equals("press")) return;
        // FruitPressLogic emits fluid in 4%-sized steps while the fruit remains in its
        // protected current-fruit slot. Use cumulative floor division so reloads do not duplicate fluid.
                int now = plan.fluidOutput().getAmount() * progress / maxProgress;
                int before = plan.fluidOutput().getAmount() * Math.max(0, progress - 1) / maxProgress;
                if (now > before) machine.outputTank().fill(plan.fluidOutput().copyWithAmount(now - before), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
        });
    }

    /**
      * 1.12 Designer packages did not consume a Design item or use power:
      * ComponentDesignerRecipe stored one selected design and its preview was
      * taken directly into the player's carried stack. Keep that protocol
      * separate from the timed processor used by the other Binnie's Trees blocks.
      */
    private static void registerDesigner(String kind) {
        MachineProcessRegistry.register(ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, kind), new MachineProcessHandler() {
            @Override
            public Optional<MachineProcessPlan> createPlan(Level level, MachineBlockEntity machine) {
                return Optional.empty();
            }

            @Override
            public boolean isItemValid(MachineBlockEntity machine, int slot, ItemStack stack) {
                return designerSlot(machine, slot, stack);
            }

            @Override
            public boolean isManualCrafting(MachineBlockEntity machine) {
                return true;
            }

            @Override
            public MachineProcessError diagnose(Level level, MachineBlockEntity machine) {
                ItemStack first = machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT);
                ItemStack second = machine.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT);
                ItemStack adhesive = machine.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT);
                if (first.isEmpty() || second.isEmpty() || adhesive.isEmpty()) return MachineProcessError.NO_ITEM;
                if (!validDesignerMaterial(kind, first) || !validDesignerMaterial(kind, second)
                        || !validDesignerAdhesive(kind, adhesive)) return MachineProcessError.INVALID_ITEM;
                int selection = machine.manualSelection();
                if (selection < 0 || selection >= DesignCatalog.DESIGNS.size()
                        || !DesignCatalog.isSelectableDesign(DesignCatalog.DESIGNS.get(selection))) {
                    return MachineProcessError.INVALID_RECIPE;
                }
                return MachineProcessError.NONE;
            }

            @Override
            public boolean setManualSelection(MachineBlockEntity machine, int selection) {
                return selection >= 0 && selection < DesignCatalog.DESIGNS.size()
                        && DesignCatalog.isSelectableDesign(DesignCatalog.DESIGNS.get(selection));
            }

            @Override
            public ItemStack getManualPreview(MachineBlockEntity machine) {
                return designerProduct(machine, kind);
            }

            @Override
            public ItemStack takeManualPreview(MachineBlockEntity machine, ItemStack carried) {
                ItemStack product = designerProduct(machine, kind);
                if (product.isEmpty() || !canCarry(carried, product)) return ItemStack.EMPTY;
                boolean blank = "blank".equals(DesignCatalog.DESIGNS.get(machine.manualSelection()));
                machine.inventory().extractItem(MachineBlockEntity.INPUT_SLOT, 1, false);
                if (!blank) machine.inventory().extractItem(MachineBlockEntity.CATALYST_SLOT, 1, false);
                machine.inventory().extractItem(MachineBlockEntity.REAGENT_SLOT, 1, false);
                return product;
            }
        });
    }

    private static MachineProcessError diagnoseTimed(String id, Level level, MachineBlockEntity machine) {
        if (id.equals("brewery") && machine.processData().getBoolean("BreweryLocked")) {
            return lockedBreweryPlan(machine).isPresent() ? MachineProcessError.NONE : MachineProcessError.INVALID_RECIPE;
        }
        if (id.equals("distillery") && machine.processData().getBoolean("DistilleryLocked")) {
            return lockedDistilleryPlan(machine).isPresent() ? MachineProcessError.NONE : MachineProcessError.INVALID_RECIPE;
        }
        return switch (id) {
            case "press" -> {
                ItemStack fruit = machine.inventory().getStackInSlot(6);
                if (fruit.isEmpty()) yield MachineProcessError.NO_ITEM;
                BeverageOutput output = juiceFor(fruit);
                if (output == null) yield MachineProcessError.NO_RECIPE;
                FluidStack expected = beverage(output.category(), output.value(), output.amount());
                FluidStack present = machine.outputTank().getFluid();
                if (!present.isEmpty() && !FluidStack.isSameFluidSameComponents(present, expected)) {
                    yield MachineProcessError.TANK_DIFFERENT_FLUID;
                }
                if (machine.outputTank().getCapacity() - machine.outputTank().getFluidAmount() < 5) {
                    yield MachineProcessError.OUTPUT_TANK_FULL;
                }
                yield MachineProcessError.NONE;
            }
            case "lumbermill" -> {
                ItemStack log = machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT);
                if (log.isEmpty()) yield MachineProcessError.NO_ITEM;
                if (lumbermillOutput(level, log).isEmpty()) yield MachineProcessError.NO_RECIPE;
                FluidStack water = machine.tank().getFluid();
                if (water.isEmpty() || water.getAmount() < 5) yield MachineProcessError.TANK_EMPTY;
                if (!water.is(Fluids.WATER)) yield MachineProcessError.TANK_DIFFERENT_FLUID;
                yield MachineProcessError.NONE;
            }
            case "distillery" -> {
                FluidStack fluid = machine.tank().getFluid();
                if (fluid.isEmpty()) yield MachineProcessError.TANK_EMPTY;
                BeverageData beverage = fluid.get(BinnieDataComponents.BEVERAGE);
                if (beverage == null) yield MachineProcessError.INVALID_RECIPE;
                if (fluid.getAmount() < 1000) yield MachineProcessError.INSUFFICIENT_LIQUID;
                Distillation route = DISTILLATION.get(beverage.beverage());
                if (route == null || route.output(Math.clamp(machine.mode(), 0, 2)) == null) {
                    yield MachineProcessError.NO_RECIPE;
                }
                yield MachineProcessError.NONE;
            }
            case "brewery" -> diagnoseBrewery(machine);
            default -> MachineProcessError.NONE;
        };
    }

    private static MachineProcessError diagnoseBrewery(MachineBlockEntity machine) {
        ItemStack ingredient = machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT);
        ItemStack yeast = machine.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT);
        if (yeast.isEmpty()) return MachineProcessError.NO_ITEM;
        if (!isYeast(yeast)) return MachineProcessError.INVALID_ITEM;
        FluidStack fluid = machine.tank().getFluid();
        if (fluid.isEmpty()) return MachineProcessError.TANK_EMPTY;
        if (fluid.getAmount() < 1000) return MachineProcessError.INSUFFICIENT_LIQUID;
        BeverageData beverage = fluid.get(BinnieDataComponents.BEVERAGE);
        if (beverage != null) {
            if (fermentationResult(beverage.beverage()) == null) return MachineProcessError.NO_RECIPE;
            if (!ingredient.isEmpty() || !breweryGrains(machine).isEmpty()) return MachineProcessError.INVALID_RECIPE;
            return MachineProcessError.NONE;
        }
        if (!fluid.is(Fluids.WATER)) return MachineProcessError.TANK_DIFFERENT_FLUID;
        int validGrains = 0;
        for (int slot : new int[]{MachineBlockEntity.CATALYST_SLOT, 6, 7}) {
            ItemStack grain = machine.inventory().getStackInSlot(slot);
            if (grain.isEmpty()) continue;
            if (grainValue(grain) == null) return MachineProcessError.INVALID_ITEM;
            validGrains++;
        }
        if (validGrains < 2) return MachineProcessError.NO_ITEM;
        return brewery(null, machine).isPresent() ? MachineProcessError.NONE : MachineProcessError.NO_RECIPE;
    }

    private static Optional<MachineProcessPlan> lumbermill(Level level, MachineBlockEntity machine) {
        ItemStack log = machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT);
        ItemStack planks = lumbermillOutput(level, log);
        if (planks.isEmpty()) return Optional.empty();
    // LumbermillLogic allows its 5 mB canProgress threshold, then drains
    // WATER_PER_TICK (10 mB) on the 1.12 tick.
    // Once a run has started, retain the plan while dry so progress pauses
    // instead of being reset by a failed fresh 300 mB recipe lookup.
        FluidStack water = new FluidStack(Fluids.WATER, 5);
        if (machine.activeProcess() == null && !hasFluid(machine.tank().getFluid(), water)) return Optional.empty();
        return Optional.of(plan("lumbermill", planks,
                new ItemStack(BinnieTreesContent.BARK.get(), 2), new ItemStack(BinnieTreesContent.SAWDUST.get(), 2),
                30, 30, 1, 0, 0, FluidStack.EMPTY, FluidStack.EMPTY, FluidStack.EMPTY));
    }

    private static Optional<MachineProcessPlan> press(Level level, MachineBlockEntity machine) {
        ItemStack fruit = machine.inventory().getStackInSlot(6);
        BeverageOutput output = juiceFor(fruit);
        if (output == null) return Optional.empty();
        return Optional.of(planWithConsumedSlots("press", 50, 20, FluidStack.EMPTY,
                beverage(output.category(), output.value(), output.amount()), 6));
    }

    private static Optional<MachineProcessPlan> brewery(Level level, MachineBlockEntity machine) {
    // Slots: ingredient 0, grain[0] 1, yeast 2, grain[1..2] 6..7.
    // The former generic implementation incorrectly made these grain/yeast/hops.
        ItemStack ingredient = machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT);
        ItemStack yeast = machine.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT);
        if (!isYeast(yeast)) return Optional.empty();
        FluidStack inputFluid = machine.tank().getFluid();
        BeverageData input = inputFluid.get(BinnieDataComponents.BEVERAGE);
        if (input != null) {
            String alcohol = fermentationResult(input.beverage());
            if (alcohol == null || inputFluid.getAmount() < 1000) return Optional.empty();
            if (!ingredient.isEmpty() || !breweryGrains(machine).isEmpty()) return Optional.empty();
            return Optional.of(planWithConsumedSlots("brewery", 800, 20, inputFluid.copyWithAmount(1000),
                    beverage("alcohol", alcohol, 1000), MachineBlockEntity.REAGENT_SLOT));
        }
        if (!inputFluid.is(Fluids.WATER) || inputFluid.getAmount() < 1000) return Optional.empty();
        int[] grainSlots = {MachineBlockEntity.CATALYST_SLOT, 6, 7};
        java.util.ArrayList<String> grains = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> occupiedGrainSlots = new java.util.ArrayList<>();
        for (int slot : grainSlots) {
            ItemStack stack = machine.inventory().getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            String value = grainValue(stack);
            if (value == null) return Optional.empty();
            grains.add(value);
            occupiedGrainSlots.add(slot);
        }
        BreweryGrainOperation operation = BREWERY_GRAINS.stream()
                .filter(candidate -> grains.stream().filter(candidate.grain()::equals).count() >= 2)
                .filter(candidate -> candidate.ingredient().isEmpty() ? ingredient.isEmpty() : ingredient.is(BinnieTreesContent.HOPS.get()))
                .filter(candidate -> candidate.yeast().equals(yeast.is(BinnieTreesContent.LAGER_YEAST.get()) ? "lager_yeast" : "yeast"))
                .findFirst().orElse(null);
        if (operation == null) return Optional.empty();
        java.util.ArrayList<Integer> consumed = new java.util.ArrayList<>(occupiedGrainSlots);
        consumed.add(MachineBlockEntity.REAGENT_SLOT);
        if (!operation.ingredient().isEmpty()) consumed.add(MachineBlockEntity.INPUT_SLOT);
        return Optional.of(planWithConsumedSlots("brewery", 800, 20, new FluidStack(Fluids.WATER, 1000),
                beverage("alcohol", operation.outputAlcohol(), 1000), consumed.stream().mapToInt(Integer::intValue).toArray()));
    }

    /** BreweryLogic locks its recipe before progress and moves its items into SLOTS_INVENTORY. */
    private static void lockBrewery(MachineBlockEntity machine, MachineProcessPlan plan) {
        if (machine.processData().getBoolean("BreweryLocked")) return;
    // 1.12 slot order is grains[0..2], ingredient, yeast; keep it in reserve[0..4].
        int[] active = {MachineBlockEntity.CATALYST_SLOT, 6, 7, MachineBlockEntity.INPUT_SLOT, MachineBlockEntity.REAGENT_SLOT};
        for (int index = 0; index < active.length; index++) {
            ItemStack stack = machine.inventory().extractItem(active[index], 1, false);
            machine.inventory().setStackInSlot(8 + index, stack);
        }
        FluidStack reserved = machine.tank().drain(plan.primaryFluidInput(), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        machine.secondaryTank().setFluid(reserved);
        BeverageData output = plan.fluidOutput().get(BinnieDataComponents.BEVERAGE);
        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        data.putBoolean("BreweryLocked", true);
        data.putInt("Duration", plan.duration());
        data.putInt("Energy", plan.energyPerTick());
        data.putInt("OutputAmount", plan.fluidOutput().getAmount());
        if (output != null) {
            data.putString("OutputBeverage", output.beverage());
            data.putInt("OutputColor", output.color());
            data.putInt("OutputAlcohol", output.alcohol());
            data.putInt("OutputVolume", output.volume());
        }
        machine.setProcessData(data);
    }

    private static Optional<MachineProcessPlan> lockedBreweryPlan(MachineBlockEntity machine) {
        net.minecraft.nbt.CompoundTag data = machine.processData();
        FluidStack reserved = machine.secondaryTank().getFluid();
        if (reserved.isEmpty() || !data.contains("OutputBeverage")) return Optional.empty();
        BeverageData outputData = new BeverageData(data.getString("OutputBeverage"), data.getInt("OutputColor"),
                data.getInt("OutputAlcohol"), data.getInt("OutputVolume"));
        FluidStack output = BinnieTreesFluids.beverage(outputData, data.getInt("OutputAmount"));
        return Optional.of(new MachineProcessPlan(ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, "dynamic/brewery"),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, data.getInt("Duration"), data.getInt("Energy"),
                0, 0, 0, ItemStack.EMPTY, ItemStack.EMPTY, FluidStack.EMPTY, FluidStack.EMPTY, output));
    }

    private static Optional<MachineProcessPlan> distillery(Level level, MachineBlockEntity machine) {
        FluidStack inputFluid = machine.tank().getFluid();
        BeverageData input = inputFluid.get(BinnieDataComponents.BEVERAGE);
        if (input == null || inputFluid.getAmount() < 1000) return Optional.empty();
        Distillation distillation = DISTILLATION.get(input.beverage());
        if (distillation == null) return Optional.empty();
        int mode = Math.max(0, Math.min(2, machine.mode()));
        DistilledOutput output = distillation.output(mode);
        if (output == null) return Optional.empty();
        return Optional.of(plan("distillery_" + mode, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                2000 + 800 * mode, 2, 0, 0, 0, inputFluid.copyWithAmount(1000), FluidStack.EMPTY,
                beverage("spirit", output.value(), output.amount())));
    }

    /**
      * DistilleryLogic drains exactly one bucket into its persisted
      * {@code currentFluid} before it starts. It must never re-read the live
      * input tank while that bucket is being distilled.
      */
    private static void lockDistillery(MachineBlockEntity machine, MachineProcessPlan plan) {
        if (machine.processData().getBoolean("DistilleryLocked")) return;
        FluidStack current = machine.tank().drain(plan.primaryFluidInput(), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (current.getAmount() != 1000) return;
        machine.secondaryTank().setFluid(current);
        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        data.putBoolean("DistilleryLocked", true);
        machine.setProcessData(data);
    }

    private static Optional<MachineProcessPlan> lockedDistilleryPlan(MachineBlockEntity machine) {
        FluidStack current = machine.secondaryTank().getFluid();
        BeverageData input = current.get(BinnieDataComponents.BEVERAGE);
        if (input == null || current.getAmount() < 1000) return Optional.empty();
        Distillation distillation = DISTILLATION.get(input.beverage());
        if (distillation == null) return Optional.empty();
        int level = Math.clamp(machine.mode(), 0, 2);
        DistilledOutput output = distillation.output(level);
        if (output == null) return Optional.empty();
        return Optional.of(plan("distillery_" + level, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                2000 + 800 * level, 2, 0, 0, 0, FluidStack.EMPTY, FluidStack.EMPTY,
                beverage("spirit", output.value(), output.amount())));
    }

    private static ItemStack designerProduct(MachineBlockEntity machine, String kind) {
        ItemStack first = machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT);
        ItemStack second = machine.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT);
        ItemStack adhesive = machine.inventory().getStackInSlot(MachineBlockEntity.REAGENT_SLOT);
        if (first.isEmpty() || second.isEmpty() || adhesive.isEmpty()) return ItemStack.EMPTY;
        if (!validDesignerMaterial(kind, first) || !validDesignerMaterial(kind, second)) return ItemStack.EMPTY;
        if (!validDesignerAdhesive(kind, adhesive)) return ItemStack.EMPTY;
        int selection = machine.manualSelection();
        if (selection < 0 || selection >= DesignCatalog.DESIGNS.size()
                || !DesignCatalog.isSelectableDesign(DesignCatalog.DESIGNS.get(selection))) return ItemStack.EMPTY;
        return designerOutput(kind, first, second, DesignCatalog.DESIGNS.get(selection));
    }

    /** Dynamic DesignerType#getBlock contract shared by manual execution and JEI descriptors. */
    public static ItemStack designerOutput(String kind, ItemStack first, ItemStack second, String design) {
        if (!DesignCatalog.isDesign(design) || !catalogDesignerMaterial(kind, first)
                || !catalogDesignerMaterial(kind, second)) return ItemStack.EMPTY;
        ResourceLocation primary = BuiltInRegistries.ITEM.getKey(first.getItem());
        ResourceLocation secondary = BuiltInRegistries.ITEM.getKey(second.getItem());
    // DesignerType#getBlock in the 1.12 release substitutes the first
    // material for the second one for Blank before it creates the stack.
    // The second input is still required for the preview, but it is not
    // represented by, or consumed for, the one-block result.
        boolean blank = "blank".equals(design);
        if (blank) {
            secondary = primary;
        }
        DesignData data = new DesignData(design, design, primary, secondary, 0, false,
                DesignMaterialCatalog.variant(first), blank ? DesignMaterialCatalog.variant(first) : DesignMaterialCatalog.variant(second));
        DesignedBlockItem resultItem = switch (kind) {
            case "woodworker" -> BinnieTreesDesignContent.PATTERNED_WOOD.get();
            case "panelworker" -> BinnieTreesDesignContent.PATTERNED_PANEL.get();
            case "glassworker" -> BinnieTreesDesignContent.PATTERNED_GLASS.get();
            default -> throw new IllegalArgumentException(kind);
        };
        return resultItem.create(data, blank ? 1 : 2);
    }

    /** Resolves Lumbermill outputs for execution and JEI. */
    public static ItemStack lumbermillOutput(ItemStack log) {
        if (log.isEmpty() || !log.is(ItemTags.LOGS)) return ItemStack.EMPTY;
        for (BinnieTreeWoodType type : BinnieTreeWoodType.values()) {
            BinnieTreeWoodContent.WoodSet set = BinnieTreeWoodContent.get(type);
            if (log.is(set.logItem().get()) || log.is(set.fireproofLogItem().get())) {
                return new ItemStack(log.is(set.fireproofLogItem().get()) ? set.fireproofPlanksItem().get() : set.planksItem().get(), 4);
            }
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(log.getItem());
        String path = id.getPath();
        String stem = path.endsWith("_log") ? path.substring(0, path.length() - 4)
                : path.endsWith("_wood") ? path.substring(0, path.length() - 5) : null;
        if (stem == null) return ItemStack.EMPTY;
        ResourceLocation planksId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), stem + "_planks");
        var item = BuiltInRegistries.ITEM.get(planksId);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, 4);
    }

    /** Runtime equivalent of the 1.12 manager's 3x3 fake crafting lookup. */
    public static ItemStack lumbermillOutput(Level level, ItemStack log) {
        ItemStack registered = lumbermillOutput(log);
        if (!registered.isEmpty()) return registered;
        if (log.isEmpty() || !log.is(ItemTags.LOGS)) return ItemStack.EMPTY;
        java.util.ArrayList<ItemStack> slots = new java.util.ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        slots.set(0, log.copyWithCount(1));
        net.minecraft.world.item.crafting.CraftingInput input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, slots);
        return level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                .map(holder -> holder.value().assemble(input, level.registryAccess()))
                .filter(stack -> stack.is(ItemTags.PLANKS)).orElse(ItemStack.EMPTY);
    }

    private static BeverageOutput juiceFor(ItemStack fruit) {
        VariantData data = fruit.get(BinnieDataComponents.VARIANT);
        if (data != null && "binnietrees.food".equals(data.category())) {
            for (FruitPressOperation operation : FRUIT_PRESS_OPERATIONS) {
                if (operation.inputVariant().equals(data.value())) return new BeverageOutput("juice", operation.juice(), operation.outputAmount());
            }
        }
        for (TaggedFruitPressOperation operation : TAGGED_FRUIT_PRESS_OPERATIONS) {
            if (fruit.is(operation.tag())) return new BeverageOutput("juice", operation.juice(), operation.outputAmount());
        }
        return null;
    }

    private static String fermentationResult(String beverage) {
        return BREWERY_FERMENTATIONS.stream().filter(route -> route.inputBeverage().equals(beverage))
                .map(BreweryFermentationOperation::outputAlcohol).findFirst().orElse(null);
    }

    private static FluidStack beverage(String category, String value, int amount) {
        BeverageData data = LegacyBeverageCatalog.get(category, value).data(amount);
        return BinnieTreesFluids.beverage(data, amount);
    }

    private static boolean lumbermillSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return slot == MachineBlockEntity.INPUT_SLOT && stack.is(ItemTags.LOGS);
    }
    private static boolean pressSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return slot == MachineBlockEntity.INPUT_SLOT && juiceFor(stack) != null;
    }
    private static boolean brewerySlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        return switch (slot) {
            case MachineBlockEntity.INPUT_SLOT -> stack.is(BinnieTreesContent.HOPS.get());
            case MachineBlockEntity.CATALYST_SLOT, 6, 7 -> grainValue(stack) != null;
            case MachineBlockEntity.REAGENT_SLOT -> isYeast(stack);
            default -> false;
        };
    }

    private static java.util.List<ItemStack> breweryGrains(MachineBlockEntity machine) {
        return java.util.List.of(machine.inventory().getStackInSlot(MachineBlockEntity.CATALYST_SLOT),
                machine.inventory().getStackInSlot(6), machine.inventory().getStackInSlot(7)).stream().filter(stack -> !stack.isEmpty()).toList();
    }
    private static boolean distillerySlot(MachineBlockEntity machine, int slot, ItemStack stack) { return false; }
    private static boolean designerSlot(MachineBlockEntity machine, int slot, ItemStack stack) {
        String kind = machine.spec().id().getPath();
        if (slot == MachineBlockEntity.REAGENT_SLOT) return validDesignerAdhesive(kind, stack);
        return (slot == MachineBlockEntity.INPUT_SLOT || slot == MachineBlockEntity.CATALYST_SLOT) && validDesignerMaterial(kind, stack);
    }

    private static boolean validDesignerAdhesive(String kind, ItemStack stack) {
        return "glassworker".equals(kind) ? stack.is(BinnieTreesContent.GLASS_FITTING.get()) : stack.is(BinnieTreesContent.WOOD_WAX.get());
    }

    private static boolean canCarry(ItemStack carried, ItemStack product) {
        return carried.isEmpty() || (ItemStack.isSameItemSameComponents(carried, product)
                && carried.getCount() + product.getCount() <= carried.getMaxStackSize());
    }

    private static boolean validDesignerMaterial(String kind, ItemStack stack) {
        return "glassworker".equals(kind)
                ? stack.is(binnie.design.data.DesignerMaterialTags.GLASS) && DesignMaterialCatalog.isGlass(stack)
                : stack.is(binnie.design.data.DesignerMaterialTags.WOOD) && DesignMaterialCatalog.isWood(stack);
    }

    private static boolean catalogDesignerMaterial(String kind, ItemStack stack) {
        return "glassworker".equals(kind) ? DesignMaterialCatalog.isGlass(stack) : DesignMaterialCatalog.isWood(stack);
    }

    private static boolean isYeast(ItemStack stack) {
        return stack.is(BinnieTreesContent.YEAST.get()) || stack.is(BinnieTreesContent.LAGER_YEAST.get());
    }

    private static String grainValue(ItemStack stack) {
        if (stack.is(BinnieTreesContent.GRAIN_BARLEY.get())) return "barley";
        if (stack.is(BinnieTreesContent.GRAIN_ROASTED.get())) return "roasted";
        if (stack.is(BinnieTreesContent.GRAIN_WHEAT.get())) return "wheat";
        if (stack.is(BinnieTreesContent.GRAIN_RYE.get())) return "rye";
        if (stack.is(BinnieTreesContent.GRAIN_CORN.get())) return "corn";
        VariantData data = stack.get(BinnieDataComponents.VARIANT);
        return data != null && "binnietrees.grain".equals(data.category()) ? data.value() : null;
    }

    private static boolean hasFluid(FluidStack present, FluidStack required) {
        return FluidStack.isSameFluidSameComponents(present, required) && present.getAmount() >= required.getAmount();
    }

    private static MachineProcessPlan plan(String id, ItemStack output, ItemStack secondary, ItemStack tertiary,
                                           int duration, int energy, int inputCount, int catalystCount, int reagentCount,
                                           FluidStack primary, FluidStack secondaryFluid, FluidStack fluidOutput) {
        return new MachineProcessPlan(ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, "dynamic/" + id),
                output, secondary, tertiary, duration, energy, inputCount, catalystCount, reagentCount,
                ItemStack.EMPTY, ItemStack.EMPTY, primary, secondaryFluid, fluidOutput);
    }

    private static MachineProcessPlan planWithConsumedSlots(String id, int duration, int energy, FluidStack primary, FluidStack output, int... slots) {
        return new MachineProcessPlan(ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, "dynamic/" + id),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, duration, energy, 0, 0, 0,
                ItemStack.EMPTY, ItemStack.EMPTY, primary, FluidStack.EMPTY, output, slots);
    }

    private static java.util.List<FruitPressOperation> createFruitPressOperations() {
    // ModuleCore 1.12 squeezer registrations and ModuleAlcohol's exact
    // crop-key filter. Seed oil output is doubled before creating a press
    // route; mulch stays on Forestry's squeezer and is never emitted here.
        return java.util.List.of(
                fruit("crabapple", "apple", 150, 10, 10, false), fruit("orange", "orange", 400, 10, 15, false),
                fruit("lime", "lime", 300, 10, 10, false), fruit("wild_cherry", "cherry", 100, 20, 5, true),
                fruit("sour_cherry", "cherry", 100, 20, 3, true), fruit("black_cherry", "cherry", 100, 20, 5, true),
                fruit("cherry_plum", "plum", 100, 10, 60, false), fruit("apricot", "apricot", 150, 10, 40, false),
                fruit("grapefruit", "grapefruit", 500, 10, 15, false), fruit("peach", "peach", 150, 10, 40, false),
                fruit("satsuma", "orange", 300, 10, 10, false), fruit("finger_lime", "lime", 300, 10, 10, false),
                fruit("key_lime", "lime", 300, 10, 10, false), fruit("manderin", "orange", 400, 10, 10, false),
                fruit("nectarine", "peach", 150, 10, 40, false), fruit("tangerine", "orange", 300, 10, 10, false),
                fruit("pear", "pear", 300, 10, 20, false), fruit("banana", "banana", 100, 10, 30, false),
                fruit("red_banana", "banana", 100, 10, 30, false), fruit("elderberry", "elderberry", 100, 10, 5, false),
                fruit("olive", "olive", 100, 20, 3, true), fruit("cranberry", "cranberry", 50, 10, 5, false));
    }

    private static FruitPressOperation fruit(String input, String juice, int amount, int squeezerTime, int mulchChance, boolean seedOil) {
        return new FruitPressOperation(input, juice, amount, squeezerTime, mulchChance, seedOil);
    }

    private static java.util.List<TaggedFruitPressOperation> createTaggedFruitPressOperations() {
        // ModuleAlcohol copied a route only when an ore-dictionary fruit also
        // had a Forestry Squeezer recipe. These common tags preserve that
        // cross-mod contract without binding Binnie to a particular crop mod.
        return java.util.List.of(
                tagged("c:fruits/apple", "apple", 200),
                tagged("c:crops/carrot", "carrot", 200),
                tagged("c:fruits/lemon", "lemon", 400),
                tagged("c:fruits/plum", "plum", 100),
                tagged("c:fruits/cherry", "cherry", 100),
                tagged("c:fruits/pineapple", "pineapple", 200),
                tagged("c:crops/tomato", "tomato", 200));
    }

    private static TaggedFruitPressOperation tagged(String tag, String juice, int amount) {
        return new TaggedFruitPressOperation(ResourceLocation.parse(tag), juice, amount);
    }

    private static java.util.List<BreweryFermentationOperation> createBreweryFermentations() {
        return java.util.List.of(
                ferment("apple", "apple"), ferment("apricot", "apricot"), ferment("banana", "banana"),
                ferment("cherry", "cherry"), ferment("elderberry", "elderberry"), ferment("peach", "peach"),
                ferment("pear", "pear"), ferment("plum", "plum"), ferment("carrot", "carrot"),
                ferment("white_grape", "white_wine"), ferment("red_grape", "red_wine"),
                ferment("lemon", "citrus"), ferment("lime", "citrus"), ferment("orange", "citrus"),
                ferment("grapefruit", "citrus"), ferment("cranberry", "cranberry"),
                ferment("pineapple", "pineapple"), ferment("tomato", "tomato"));
    }

    private static BreweryFermentationOperation ferment(String input, String output) {
        return new BreweryFermentationOperation("binnietrees:juice/" + input, output);
    }

    private static java.util.List<BreweryGrainOperation> createBreweryGrains() {
    // Matching ModuleAlcohol registration order. 1.12 BrewedGrainRecipe
    // requires at least two matching grains; an occupied third grain slot
    // may contain another registered grain and is still consumed.
        return java.util.List.of(
                grain("barley", "hops", "yeast", "ale"),
                grain("barley", "hops", "lager_yeast", "lager"),
                grain("roasted", "hops", "yeast", "stout"),
                grain("corn", "hops", "yeast", "corn_beer"),
                grain("rye", "hops", "yeast", "rye_beer"),
                grain("wheat", "hops", "yeast", "wheat_beer"),
                grain("barley", "", "yeast", "barley"),
                grain("corn", "", "yeast", "corn"),
                grain("rye", "", "yeast", "rye"),
                grain("wheat", "", "yeast", "wheat"));
    }

    private static BreweryGrainOperation grain(String grain, String ingredient, String yeast, String output) {
        return new BreweryGrainOperation(grain, ingredient, yeast, output);
    }

    private static Map<String, Distillation> distillationMappings() {
        Map<String, Distillation> map = new LinkedHashMap<>();
        distill(map, "apple", "apple_brandy", "apple_liquor");
        distill(map, "pear", "pear_brandy", "pear_liquor");
        distill(map, "apricot", "apricot_brandy", "apricot_liquor");
        distill(map, "banana", "fruit_brandy", "fruit_liquor");
        distill(map, "cherry", "cherry_brandy", "cherry_liquor");
        distill(map, "elderberry", "elderberry_brandy", "elderberry_liquor");
        distill(map, "peach", "fruit_brandy", "fruit_liquor");
        distill(map, "plum", "plum_brandy", "fruit_liquor");
        distill(map, "carrot", "fruit_brandy", "vodka");
        distill(map, "white_wine", "brandy", "brandy");
        distill(map, "red_wine", "brandy", "brandy");
        distill(map, "sparkling_wine", "brandy", "brandy");
        distill(map, "agave", "tequila", "tequila");
        distill(map, "potato", "fruit_brandy", "vodka");
        distill(map, "citrus", "citrus_brandy", "fruit_liquor");
        distill(map, "cranberry", "fruit_brandy", "fruit_liquor");
        distill(map, "pineapple", "fruit_brandy", "fruit_liquor");
        distill(map, "tomato", "fruit_brandy", "fruit_liquor");
        distill(map, "fruit", "fruit_brandy", "fruit_liquor");
        distill(map, "barley", "whiskey", "vodka");
        distill(map, "wheat", "wheat_whiskey", "vodka");
        distill(map, "rye", "rye_whiskey", "vodka");
        distill(map, "corn", "corn_whiskey", "vodka");
        return Map.copyOf(map);
    }

    /** Recipe order from ModuleAlcohol.addDistillery. Unlike the
      * runtime lookup map, this retains duplicated-but-registered routes for
      * JEI and recipe inspection. */
    private static java.util.List<DistillationRoute> createDistillationRoutes() {
        java.util.ArrayList<DistillationRoute> routes = new java.util.ArrayList<>();
        String[][] families = {
                {"apple","apple_brandy","apple_liquor"},{"pear","pear_brandy","pear_liquor"},{"apricot","apricot_brandy","apricot_liquor"},
                {"banana","fruit_brandy","fruit_liquor"},{"cherry","cherry_brandy","cherry_liquor"},{"elderberry","elderberry_brandy","elderberry_liquor"},
                {"peach","fruit_brandy","fruit_liquor"},{"plum","plum_brandy","fruit_liquor"},{"carrot","fruit_brandy","vodka"},
                {"white_wine","brandy","brandy"},{"red_wine","brandy","brandy"},{"sparkling_wine","brandy","brandy"},
                {"agave","tequila","tequila"},{"potato","fruit_brandy","vodka"},{"citrus","citrus_brandy","fruit_liquor"},
                {"cranberry","fruit_brandy","fruit_liquor"},{"pineapple","fruit_brandy","fruit_liquor"},{"tomato","fruit_brandy","fruit_liquor"},
                {"fruit","fruit_brandy","fruit_liquor"},{"barley","whiskey","vodka"},{"wheat","wheat_whiskey","vodka"},
                {"rye","rye_whiskey","vodka"},{"corn","corn_whiskey","vodka"}};
        for (String[] family : families) {
            String alcohol = "binnietrees:alcohol/" + family[0];
            String first = "binnietrees:spirit/" + family[1];
            String second = "binnietrees:spirit/" + family[2];
            routes.add(new DistillationRoute(alcohol, 0, family[1], 800));
            routes.add(new DistillationRoute(alcohol, 1, family[2], 400));
            routes.add(new DistillationRoute(alcohol, 2, "neutral_spirit", 200));
            routes.add(new DistillationRoute(first, 0, family[2], 400));
            routes.add(new DistillationRoute(first, 1, "neutral_spirit", 200));
            routes.add(new DistillationRoute(second, 0, "neutral_spirit", 400));
        }
        return java.util.List.copyOf(routes);
    }

    private static void distill(Map<String, Distillation> map, String source, String first, String second) {
        String neutral = "neutral_spirit";
        putDistillation(map, "binnietrees:alcohol/" + source, 0, first, 800);
        putDistillation(map, "binnietrees:alcohol/" + source, 1, second, 400);
        putDistillation(map, "binnietrees:alcohol/" + source, 2, neutral, 200);
        putDistillation(map, "binnietrees:spirit/" + first, 0, second, 400);
        putDistillation(map, "binnietrees:spirit/" + first, 1, neutral, 200);
        putDistillation(map, "binnietrees:spirit/" + second, 0, neutral, 400);
    }

    private static void putDistillation(Map<String, Distillation> map, String input, int mode, String output, int amount) {
        Distillation route = map.computeIfAbsent(input, key -> new Distillation());
        route.set(mode, new DistilledOutput(output, amount));
    }

    private record BeverageOutput(String category, String value, int amount) {}
    private static final class Distillation {
        private final DistilledOutput[] outputs = new DistilledOutput[3];
        private DistilledOutput output(int mode) { return outputs[mode]; }
        private void set(int mode, DistilledOutput output) { outputs[mode] = output; }
    }
    private record DistilledOutput(String value, int amount) {}
    @FunctionalInterface private interface Planner { Optional<MachineProcessPlan> create(Level level, MachineBlockEntity machine); }
    @FunctionalInterface private interface SlotValidator { boolean valid(MachineBlockEntity machine, int slot, ItemStack stack); }
}
