package binnie.core.client.ponder;

import binnie.core.machine.MachineBlockEntity;
import binnie.core.ponder.PonderFluidPipeBlock;
import binnie.core.ponder.PonderFluidPipeBlockEntity;
import binnie.core.registry.CoreRegistries;
import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.genetics.client.GeneticsPonderGuiPreview;
import binnie.genetics.jei.GeneticsJeiRecipe;
import binnie.genetics.jei.GeneticsJeiRecipes;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.core.platform.util.SpeciesUtil;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/** Ponder scenes for Binnie's systems, paced and staged after Create's own scenes. */
public final class BinniePonderScenes {
    // Native Ponder FadeInOutInstruction adds five ticks on both ends of the requested
    // duration. Leave one extra tick after that full lifetime before starting another
    // overlay in the same visual lane, otherwise their fade tails briefly coexist.
    private static final int OVERLAY_FADE_TICKS = 5;
    private static final int OVERLAY_SEPARATION_TICKS = OVERLAY_FADE_TICKS * 2 + 1;
    private static final int ITEM_CALLOUT_TICKS = 18;
    private static final int MACHINE_EXPLANATION_TICKS = 26;
    private static final int SECTION_REVEAL_TICKS = 15;

    private BinniePonderScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Register focused scenes first: Ponder preserves insertion order per component, so
        // opening a machine now starts with that machine instead of the broad workflow overview.
        helper.forComponents(id("binniegenetics", "isolator"))
                .addStoryBoard("genetics_station", BinniePonderScenes::isolator);
        helper.forComponents(id("binniegenetics", "sequencer"))
                .addStoryBoard("genetics_station", BinniePonderScenes::sequencer);
        helper.forComponents(id("binniegenetics", "polymeriser"))
                .addStoryBoard("genetics_station", BinniePonderScenes::polymeriser);
        helper.forComponents(id("binniegenetics", "inoculator"))
                .addStoryBoard("genetics_station", BinniePonderScenes::inoculator);
        helper.forComponents(id("binniegenetics", "splicer"))
                .addStoryBoard("genetics_station", BinniePonderScenes::splicer);
        helper.forComponents(id("binniegenetics", "analyser"))
                .addStoryBoard("genetics_station", BinniePonderScenes::analyser);
        helper.forComponents(id("binniegenetics", "genepool"))
                .addStoryBoard("genetics_station", BinniePonderScenes::genePool);
        helper.forComponents(id("binniegenetics", "incubator"))
                .addStoryBoard("genetics_station", BinniePonderScenes::incubator);
        helper.forComponents(id("binniegenetics", "acclimatiser"))
                .addStoryBoard("genetics_station", BinniePonderScenes::acclimatiser);
        helper.forComponents(id("binniegenetics", "laboratory_machine"))
                .addStoryBoard("genetics_station", BinniePonderScenes::labStand);

        helper.forComponents(
                id("binniegenetics", "isolator"), id("binniegenetics", "sequencer"),
                id("binniegenetics", "polymeriser"), id("binniegenetics", "inoculator"),
                id("binniegenetics", "splicer"), id("binniegenetics", "analyser"),
                id("binniegenetics", "genepool"), id("binniegenetics", "incubator"),
                id("binniegenetics", "acclimatiser"))
                .addStoryBoard("genetics_pipeline", BinniePonderScenes::geneticsPipeline);

        helper.forComponents(
                id("binniebees", "alveary_mutator"), id("binniebees", "alveary_frame"),
                id("binniebees", "alveary_rain_shield"), id("binniebees", "alveary_lighting"),
                id("binniebees", "alveary_stimulator"), id("binniebees", "alveary_hatchery"),
                id("binniebees", "alveary_transmission"))
                .addStoryBoard("alveary_components", BinniePonderScenes::alvearyComponents);

        helper.forComponents(
                id("binniebotany", "flower"), id("binniebotany", "pollen"),
                id("binniebotany", "seed"), id("binniebotany", "soil_meter"))
                .addStoryBoard("botany_breeding", BinniePonderScenes::botanyBreeding);
    }

    public static void geneticsPipeline(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("genetics_pipeline", "Genetics Machine Workflow");
        scene.configureBasePlate(0, 0, 11);
        scene.scaleSceneView(0.64F);

        Map<String, List<GeneticsJeiRecipe>> examples = GeneticsJeiRecipes.build();
        GeneticsJeiRecipe isolator = recipe(examples, "isolator", "chromosome");
        GeneticsJeiRecipe sequencer = recipe(examples, "sequencer", "active");
        GeneticsJeiRecipe polymeriser = recipe(examples, "polymeriser", "serum");
        GeneticsJeiRecipe inoculator = recipe(examples, "inoculator", "flower_pollen");

        Selection floor = util.select().layer(0);
        Selection wall = util.select().fromTo(0, 1, 7, 10, 3, 7);
        Selection support = util.select().fromTo(1, 1, 5, 10, 1, 6);
        scene.world().showSection(floor, Direction.UP);
        scene.idle(5);
        scene.world().showSection(wall, Direction.NORTH);
        scene.idle(6);

        text(scene, util, util.grid().at(1, 1, 2), 65,
                "The main Genetics workflow starts with an organism. First, isolate one of its genes, sequence it, put that gene into a Serum, charge the Serum, then apply it to another organism.");
        scene.world().showSection(util.select().fromTo(0, 1, 2, 2, 1, 2), Direction.DOWN);
        scene.idle(65 + OVERLAY_SEPARATION_TICKS);
        showInputs(scene, util, util.grid().at(1, 1, 2), first(isolator.inputs()), first(isolator.catalysts()),
                first(isolator.reagents()), fluidBucket(isolator.fluidInputs(), 0));
        showFluidInputs(scene, util, util.grid().at(1, 1, 2), isolator.fluidInputs());
        result(scene, util, util.grid().at(2, 1, 2), first(isolator.outputs()));
        scene.idle(35);

        scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 1, 2), Direction.WEST);
        scene.idle(8);
        text(scene, util, util.grid().at(3, 1, 2), 60,
                "The Isolator uses an Empty Sequencer to take one gene from the organism. The result is an unsequenced Sequence. The Sequencer reads that Sequence with Fluorescent Dye and returns the Empty Sequencer.");
        scene.overlay().showLine(PonderPalette.GREEN, util.vector().centerOf(2, 1, 2), util.vector().centerOf(3, 1, 2), 45);
        scene.idle(60 + OVERLAY_SEPARATION_TICKS);
        showInputs(scene, util, util.grid().at(3, 1, 2), first(sequencer.inputs()), first(sequencer.reagents()));
        result(scene, util, util.grid().at(4, 1, 2), first(sequencer.outputs()));
        scene.idle(35);

        scene.world().showSection(util.select().fromTo(5, 1, 2, 6, 1, 2), Direction.WEST);
        scene.idle(8);
        text(scene, util, util.grid().at(5, 1, 2), 65,
                "Once a gene has been discovered, it can be put into a Serum or Serum Array. The Polymeriser charges it with Polymerising Bacteria and Raw DNA so it can be used in the next machines.");
        scene.idle(65 + OVERLAY_SEPARATION_TICKS);
        showInputs(scene, util, util.grid().at(5, 1, 2), first(polymeriser.inputs()),
                fluidBucket(polymeriser.fluidInputs(), 0), fluidBucket(polymeriser.fluidInputs(), 1));
        showFluidInput(scene, util, util.grid().at(5, 1, 2), polymeriser.fluidInputs().getFirst(), true);
        result(scene, util, util.grid().at(6, 1, 2), first(polymeriser.outputs()));
        scene.idle(40);

        scene.world().showSection(util.select().fromTo(7, 1, 2, 10, 1, 2), Direction.WEST);
        scene.idle(8);
        text(scene, util, util.grid().at(8, 1, 2), 65,
                "The Inoculator uses Vector Bacteria to apply a charged Serum to an organism. Flowers are modified as Pollen, not as grown Flowers.");
        scene.overlay().showLine(PonderPalette.GREEN, util.vector().centerOf(6, 1, 2), util.vector().centerOf(10, 1, 2), 55);
        scene.idle(65 + OVERLAY_SEPARATION_TICKS);
        showInputs(scene, util, util.grid().at(7, 1, 2), first(inoculator.inputs()), first(inoculator.catalysts()),
                fluidBucket(inoculator.fluidInputs(), 0));
        showFluidInputs(scene, util, util.grid().at(7, 1, 2), inoculator.fluidInputs());
        result(scene, util, util.grid().at(10, 1, 2), first(inoculator.outputs()));
        scene.idle(45);

        scene.world().showSection(support, Direction.UP);
        scene.idle(12);
        showFluidConnection(scene, util, util.grid().at(5, 1, 5), util.grid().at(5, 1, 2),
                recipe(examples, "genepool", "flower").fluidOutputs().getFirst());
        scene.rotateCameraY(28);
        text(scene, util, util.grid().at(6, 1, 5), 70,
                "The other machines handle the rest of the lab work. The Analyser reveals genetic information, the Gene Pool makes Raw DNA, the Incubator makes lab fluids, and the Acclimatiser changes climate tolerances.");
        scene.idle(75);
        scene.markAsFinished();
    }

    public static void isolator(SceneBuilder scene, SceneBuildingUtil util) {
        GeneticsJeiRecipe example = recipe("isolator", "chromosome");
        setupStation(scene, util, "isolator", "Using the Isolator");
        BlockPos machine = machinePos(util);
        machineExplanation(scene, util, machine, id("binniegenetics", "isolator"), example,
                "Put an organism, an Empty Sequencer and Enzyme into the Isolator, then supply Bioethanol. It takes one active or inactive gene and produces an unsequenced Sequence.");
        showInputs(scene, util, machine, first(example.inputs()), first(example.catalysts()), first(example.reagents()),
                fluidBucket(example.fluidInputs(), 0));
        showFluidInputs(scene, util, machine, example.fluidInputs());
        text(scene, util, machine, 65,
                "The organism can be lost while the gene is being extracted. If it is valuable breeding stock, keep a spare before putting it into the Isolator.");
        moveThroughStation(scene, util, first(example.inputs()), first(example.outputs()));
        scene.idle(70);
        scene.markAsFinished();
    }

    public static void sequencer(SceneBuilder scene, SceneBuildingUtil util) {
        GeneticsJeiRecipe example = recipe("sequencer", "active");
        setupStation(scene, util, "sequencer", "Using the Sequencer");
        BlockPos machine = machinePos(util);
        machineExplanation(scene, util, machine, id("binniegenetics", "sequencer"), example,
                "The Sequencer reads the unsequenced Sequence made by the Isolator. Add Fluorescent Dye and it works through the sample until the gene has been identified.");
        showInputs(scene, util, machine, first(example.inputs()), first(example.reagents()), first(example.catalysts()));
        text(scene, util, machine, 65,
                "When it finishes, the discovered gene is recorded for you and the Empty Sequencer is returned. A Database can then be used to view the genes you have discovered.");
        moveThroughStation(scene, util, first(example.inputs()), first(example.outputs()));
        scene.idle(70);
        scene.markAsFinished();
    }

    public static void polymeriser(SceneBuilder scene, SceneBuildingUtil util) {
        GeneticsJeiRecipe example = recipe("polymeriser", "array_accelerated");
        setupStation(scene, util, "polymeriser", "Charging Serums with the Polymeriser");
        BlockPos machine = machinePos(util);
        machineExplanation(scene, util, machine, id("binniegenetics", "polymeriser"), example,
                "The Polymeriser adds charges to a Serum or Serum Array. Supply Polymerising Bacteria and Raw DNA, then put the Serum into the machine. A charged Gold Nugget can be added to speed it up.");
        showInputsWithDescription(scene, util, machine,
                "a Serum Array together with Polymerising Bacteria and Raw DNA; the Charged Gold Nugget is an optional accelerator",
                first(example.inputs()), first(example.catalysts()),
                fluidBucket(example.fluidInputs(), 0), fluidBucket(example.fluidInputs(), 1));
        showFluidInputs(scene, util, machine, example.fluidInputs());
        text(scene, util, machine, 65,
                "Each run adds one charge to the Serum. A Serum can hold up to sixteen charges, and using it in another machine spends a charge.");
        moveThroughStation(scene, util, first(example.inputs()), first(example.outputs()));
        scene.idle(70);
        scene.markAsFinished();
    }

    public static void inoculator(SceneBuilder scene, SceneBuildingUtil util) {
        GeneticsJeiRecipe example = recipe("inoculator", "flower_pollen");
        setupStation(scene, util, "inoculator", "Applying Genes with the Inoculator");
        BlockPos machine = machinePos(util);
        machineExplanation(scene, util, machine, id("binniegenetics", "inoculator"), example,
                "The Inoculator applies the genes stored in a charged Serum to an organism. Flowers have to be modified as Pollen, not as grown Flowers.");
        showInputs(scene, util, machine, first(example.inputs()), first(example.catalysts()),
                fluidBucket(example.fluidInputs(), 0));
        showFluidInputs(scene, util, machine, example.fluidInputs());
        text(scene, util, machine, 65,
                "Supply Vector Bacteria, the organism and the Serum. When it finishes, the organism comes back with the new gene and the Serum loses one charge.");
        moveThroughStation(scene, util, first(example.inputs()), first(example.outputs()));
        scene.idle(70);
        scene.markAsFinished();
    }

    public static void splicer(SceneBuilder scene, SceneBuildingUtil util) {
        GeneticsJeiRecipe example = recipe("splicer", "array");
        setupStation(scene, util, "splicer", "Applying Gene Arrays with the Splicer");
        BlockPos machine = machinePos(util);
        machineExplanation(scene, util, machine, id("binniegenetics", "splicer"), example,
                "The Splicer applies a charged Serum Array to an organism. Like the Inoculator, flowers have to be used as Pollen.");
        showInputs(scene, util, machine, first(example.inputs()), first(example.catalysts()));
        text(scene, util, machine, 65,
                "It does not need Vector Bacteria and works much faster than the Inoculator, but it uses a lot more FE.");
        moveThroughStation(scene, util, first(example.inputs()), first(example.outputs()));
        scene.idle(70);
        scene.markAsFinished();
    }

    public static void analyser(SceneBuilder scene, SceneBuildingUtil util) {
        GeneticsJeiRecipe example = recipe("analyser", "sequence");
        setupStation(scene, util, "analyser", "Analysing Genetic Material");
        BlockPos machine = machinePos(util);
        machineExplanation(scene, util, machine, id("binniegenetics", "analyser"), example,
                "The Analyser reveals genetic information that has not been analysed yet. Put in an organism or an unanalysed Sequence together with DNA Dye.");
        showInputs(scene, util, machine, first(example.inputs()), first(example.reagents()));
        text(scene, util, machine, 65,
                "The same item comes back analysed. Its genetic information can then be read instead of being hidden.");
        moveThroughStation(scene, util, first(example.inputs()), first(example.outputs()));
        scene.idle(70);
        scene.markAsFinished();
    }

    public static void genePool(SceneBuilder scene, SceneBuildingUtil util) {
        GeneticsJeiRecipe example = recipe("genepool", "flower");
        setupStation(scene, util, "genepool", "Producing Raw DNA");
        BlockPos machine = machinePos(util);
        machineExplanation(scene, util, machine, id("binniegenetics", "genepool"), example,
                "The Gene Pool breaks down an organism to make Raw DNA. It needs Enzyme and Bioethanol to process the organism.");
        showInputs(scene, util, machine, first(example.inputs()), first(example.reagents()),
                fluidBucket(example.fluidInputs(), 0));
        showFluidInputs(scene, util, machine, example.fluidInputs());
        text(scene, util, machine, 60,
                "The organism is consumed. Raw DNA collects in the output tank and is mainly used to charge Serums in the Polymeriser.");
        showFluidOutputs(scene, util, machine, example.fluidOutputs());
        controls(scene, util, util.grid().at(5, 1, 2), fluidBucket(example.fluidOutputs(), 0));
        scene.idle(65);
        scene.markAsFinished();
    }

    public static void incubator(SceneBuilder scene, SceneBuildingUtil util) {
        Map<String, List<GeneticsJeiRecipe>> examples = GeneticsJeiRecipes.build();
        GeneticsJeiRecipe enzyme = recipe(examples, "incubator", "enzyme");
        GeneticsJeiRecipe medium = recipe(examples, "incubator", "growth_medium");
        GeneticsJeiRecipe bacteria = recipe(examples, "incubator", "bacteria");
        GeneticsJeiRecipe polymerising = recipe(examples, "incubator", "polymerising");
        GeneticsJeiRecipe vector = recipe(examples, "incubator", "vector");

        setupStation(scene, util, "incubator", "Making Genetics Fluids");
        BlockPos machine = machinePos(util);
        machineExplanation(scene, util, machine, id("binniegenetics", "incubator"), enzyme,
                "The Incubator makes several fluids used by the Genetics machines. Give it the right fluid and reagent, and it will keep processing while both are available.");

        showItemOutputRoute(scene, util, machine);
        showInputs(scene, util, machine, first(enzyme.reagents()), fluidBucket(enzyme.fluidInputs(), 0));
        showFluidInputs(scene, util, machine, enzyme.fluidInputs());
        text(scene, util, machine, 50,
                "Sugar and Bacteria make Enzyme.");
        controls(scene, util, util.grid().at(5, 1, 2), first(enzyme.outputs()));
        scene.idle(50 + OVERLAY_SEPARATION_TICKS);

        hideItemOutputRoute(scene, util, machine);
        showInputs(scene, util, machine, first(medium.reagents()), fluidBucket(medium.fluidInputs(), 0));
        updateFluidInputs(scene, machine, medium.fluidInputs());
        showFluidOutputs(scene, util, machine, medium.fluidOutputs());
        text(scene, util, machine, 50,
                "A Growth Medium item and Water make Growth Medium fluid.");
        controls(scene, util, util.grid().at(5, 1, 2), fluidBucket(medium.fluidOutputs(), 0));
        scene.idle(50 + OVERLAY_SEPARATION_TICKS);

        showInputs(scene, util, machine, first(bacteria.reagents()), fluidBucket(bacteria.fluidInputs(), 0));
        updateFluidInputs(scene, machine, bacteria.fluidInputs());
        updateFluidOutputs(scene, machine, bacteria.fluidOutputs());
        text(scene, util, machine, 50,
                "Wheat and Growth Medium fluid produce Bacteria.");
        controls(scene, util, util.grid().at(5, 1, 2), fluidBucket(bacteria.fluidOutputs(), 0));
        scene.idle(50 + OVERLAY_SEPARATION_TICKS);

        showInputs(scene, util, machine, first(polymerising.reagents()), fluidBucket(polymerising.fluidInputs(), 0));
        updateFluidInputs(scene, machine, polymerising.fluidInputs());
        updateFluidOutputs(scene, machine, polymerising.fluidOutputs());
        controls(scene, util, util.grid().at(5, 1, 2), fluidBucket(polymerising.fluidOutputs(), 0));
        scene.idle(ITEM_CALLOUT_TICKS + OVERLAY_SEPARATION_TICKS);
        showInputs(scene, util, machine, first(vector.reagents()), fluidBucket(vector.fluidInputs(), 0));
        updateFluidInputs(scene, machine, vector.fluidInputs());
        updateFluidOutputs(scene, machine, vector.fluidOutputs());
        controls(scene, util, util.grid().at(5, 1, 2), fluidBucket(vector.fluidOutputs(), 0));
        text(scene, util, machine, 65,
                "White Dye turns Bacteria into Polymerising Bacteria. Blaze Powder turns Bacteria into Vector Bacteria. Both fluids are used later when charging or applying genes.");
        scene.idle(70);
        scene.markAsFinished();
    }

    public static void acclimatiser(SceneBuilder scene, SceneBuildingUtil util) {
        Map<String, List<GeneticsJeiRecipe>> examples = GeneticsJeiRecipes.build();
        GeneticsJeiRecipe acid = recipe(examples, "acclimatiser", "ph_acid");
        GeneticsJeiRecipe alkaline = recipe(examples, "acclimatiser", "ph_alkaline");
        GeneticsJeiRecipe lava = recipe(examples, "acclimatiser", "temperature_lava");
        GeneticsJeiRecipe snow = recipe(examples, "acclimatiser", "temperature_snow");
        GeneticsJeiRecipe water = recipe(examples, "acclimatiser", "humidity_water");
        GeneticsJeiRecipe sand = recipe(examples, "acclimatiser", "humidity_sand");

        setupStation(scene, util, "acclimatiser", "Changing Climate Tolerances");
        BlockPos machine = machinePos(util);
        machineExplanation(scene, util, machine, id("binniegenetics", "acclimatiser"), acid,
                "The Acclimatiser changes an organism's pH, temperature and humidity tolerances. Put the organism in with a reagent for the tolerance you want to change.");
        showInputsWithDescription(scene, util, machine,
                "any genetic individual together with either Powdered Mulch or Powdered Ash",
                first(acid.inputs()), first(acid.reagents()), first(alkaline.reagents()));
        text(scene, util, machine, 55,
                "Powdered Mulch pushes pH tolerance toward acidic conditions. Powdered Ash pushes it toward alkaline conditions.");
        scene.idle(55 + OVERLAY_SEPARATION_TICKS);

        showInputsWithDescription(scene, util, machine,
                "any genetic individual with one matching reagent: Lava or Snow for temperature, Water or Sand for humidity",
                first(acid.inputs()), first(lava.reagents()), first(snow.reagents()),
                first(water.reagents()), first(sand.reagents()));
        text(scene, util, machine, 60,
                "Lava and Snow change temperature tolerance, while Water and Sand change humidity tolerance in opposite directions.");
        scene.idle(60 + OVERLAY_SEPARATION_TICKS);
        controls(scene, util, util.grid().at(5, 1, 2), first(acid.outputs()));
        text(scene, util, util.grid().at(5, 1, 2), 60,
                "The organism itself is the output. Its tolerance genes are changed instead of creating a separate item.");
        scene.idle(65);
        scene.markAsFinished();
    }

    public static void labStand(SceneBuilder scene, SceneBuildingUtil util) {
        setupStation(scene, util, "laboratory_machine", "Using the Lab Stand");
        BlockPos machine = machinePos(util);
        text(scene, util, machine, 60,
                "The Lab Stand holds Binnie's genetics Databases and displays the mounted Database on top.");
        ItemStack database = item("binniegenetics", "database");
        scene.overlay().showControls(util.vector().topOf(machine), Pointing.DOWN, 40)
                .rightClick().withItem(database);
        scene.idle(60 + OVERLAY_SEPARATION_TICKS);
        scene.world().modifyBlockEntity(machine, MachineBlockEntity.class,
                be -> be.inventory().setStackInSlot(MachineBlockEntity.INPUT_SLOT, database.copy()));
        scene.effects().indicateSuccess(machine);
        text(scene, util, machine, 60,
                "Right-click the Lab Stand with a compatible Database to place it on the stand.");
        scene.idle(65);
        scene.overlay().showControls(util.vector().topOf(machine), Pointing.DOWN, 40).rightClick();
        text(scene, util, machine, 65,
                "Right-click a Lab Stand with a Database on it to open that Database without carrying it around.");
        scene.idle(75);
        scene.markAsFinished();
    }

    public static void alvearyComponents(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("alveary_components", "Binnie's Alveary Upgrades");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.76F);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);

        Selection alveary = util.select().fromTo(3, 1, 3, 5, 3, 5);
        Selection cap = util.select().fromTo(3, 4, 3, 5, 4, 5);
        Selection workshop = util.select().fromTo(1, 1, 1, 7, 1, 7).substract(alveary);
        scene.world().showSection(workshop, Direction.DOWN);
        scene.idle(8);
        for (int y = 1; y <= 3; y++) {
            scene.world().showSection(util.select().fromTo(3, y, 3, 5, y, 5), Direction.DOWN);
            scene.idle(6);
        }
        scene.world().showSection(cap, Direction.DOWN);
        scene.idle(12);

        BlockPos slot = util.grid().at(4, 2, 3);
        text(scene, util, slot, 60,
                "Binnie's Alveary blocks replace normal blocks in the outside walls of a Forestry Alveary. The Alveary still uses the same 3×3×3 body and slab roof.");
        scene.idle(65);

        component(scene, util, slot, "alveary_mutator", 55,
                "The Mutator consumes mutagen items to increase the chance of bee mutations. The bonus has a limit, so adding more does not increase it forever.");
        component(scene, util, slot, "alveary_frame", 55,
                "The Frame Housing holds one Forestry frame and applies that frame's effects to the Alveary. The frame loses durability while the Alveary runs.");
        component(scene, util, slot, "alveary_rain_shield", 50,
                "The Rain Shield lets bees that normally stop in the rain keep working through bad weather.");
        component(scene, util, slot, "alveary_lighting", 50,
                "The Lighting block provides artificial light so bees that need daylight can keep working when the light is too low.");
        component(scene, util, slot, "alveary_stimulator", 60,
                "The Stimulator takes a compatible circuit board and applies its bee modifiers while it has power.");
        component(scene, util, slot, "alveary_hatchery", 55,
                "The Hatchery can produce larvae while a healthy queen is working and stores the larvae inside.");
        component(scene, util, slot, "alveary_transmission", 60,
                "The Transmission block takes FE and shares it with Alveary components that need power.");
        scene.markAsFinished();
    }

    public static void botanyBreeding(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("botany_breeding", "Breeding Flowers");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.78F);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(1, 1, 8, 7, 3, 8), Direction.NORTH);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 7, 1, 7), Direction.DOWN);
        scene.idle(12);

        BlockPos left = util.grid().at(2, 1, 3);
        BlockPos right = util.grid().at(6, 1, 3);
        BlockPos child = util.grid().at(4, 1, 6);
        controls(scene, util, left, item("binniebotany", "flower"));
        scene.idle(34);
        controls(scene, util, right, item("binniebotany", "flower"));
        scene.idle(34);
        text(scene, util, left, 65,
                "Binnie's flowers have genes just like bees and trees. Two flowers can look the same but still carry different active and inactive genes, so the parents you use matter.");
        scene.idle(70);

        ElementLink<EntityElement> pollen = scene.world().createItemEntity(util.vector().topOf(left).add(0, 0.6, 0),
                util.vector().of(0.10, 0.18, 0), item("binniebotany", "pollen"));
        scene.idle(18);
        scene.world().modifyEntity(pollen, Entity::discard);
        scene.overlay().showControls(util.vector().topOf(right), Pointing.DOWN, 35)
                .rightClick().withItem(item("binniebotany", "pollen"));
        text(scene, util, right, 65,
                "Pollen carries the genes of the flower it came from. Use it on another mature flower to pollinate that flower.");
        scene.idle(70);

        controls(scene, util, child, item("binniebotany", "seed"));
        text(scene, util, child, 65,
                "Pollinated flowers can produce Seeds with genes from both parents. Grow the Seeds and keep the offspring with the traits you want.");
        scene.idle(70);

        int soilMeterCalloutTicks = 24;
        scene.overlay().showControls(util.vector().topOf(child), Pointing.DOWN, soilMeterCalloutTicks)
                .rightClick().withItem(item("binniebotany", "soil_meter"));
        scene.idle(soilMeterCalloutTicks + OVERLAY_SEPARATION_TICKS);
        controls(scene, util, util.grid().at(2, 1, 6), new ItemStack(Items.WATER_BUCKET));
        scene.idle(34);
        controls(scene, util, child, item("binniebotany", "acid_fertiliser"));
        scene.idle(34);
        text(scene, util, child, 70,
                "Flower soil tracks moisture, acidity, fertility and weedkiller. Use the Soil Meter to check it, then water or fertilise the soil as needed.");
        scene.idle(80);
        scene.markAsFinished();
    }

    private static void setupStation(SceneBuilder scene, SceneBuildingUtil util, String machineId, String title) {
        scene.title(machineId, title);
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.88F);
        BlockPos machine = machinePos(util);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(0, 1, 5, 6, 3, 5), Direction.NORTH);
        scene.idle(5);
        scene.world().setBlock(machine, block("binniegenetics", machineId).defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(1, 1, 2, 2, 1, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(machine), Direction.DOWN);
        // x=4 is the optional output pipe. Keep it out of the station section so it can
        // be introduced exactly once by showFluidOutputs instead of being redrawn over
        // an already-visible schematic block. Fluid-output machines also own x=5,
        // where their destination tank replaces the template's item chest.
        if (!machineId.equals("genepool") && !machineId.equals("incubator")) {
            // Item-output stations keep the template's source-faithful hopper feeding
            // the destination chest. Fluid-output stations reserve both positions for
            // their directional pipe and glass tank instead.
            scene.world().showSection(util.select().fromTo(4, 1, 2, 5, 1, 2), Direction.DOWN);
        }
        scene.idle(6);
        // The centre of this row is the optional south-side source tank. As with the
        // output line, its own reveal helper owns that position.
        scene.world().showSection(util.select().position(2, 1, 4), Direction.DOWN);
        scene.world().showSection(util.select().position(4, 1, 4), Direction.DOWN);
        scene.idle(10);
        scene.effects().indicateSuccess(machine);
        scene.overlay().showOutline(PonderPalette.GREEN, machineId, util.select().position(machine), 25);
        scene.idle(8);
    }

    /** Pair the compact real GUI with its short introductory explanation. */
    private static void machineExplanation(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos,
                                           ResourceLocation machineId, GeneticsJeiRecipe recipe, String text) {
        GeneticsPonderGuiPreview.show(scene, machineId, recipe, MACHINE_EXPLANATION_TICKS);
        scene.overlay().showText(MACHINE_EXPLANATION_TICKS)
                .placeNearTarget()
                .text(text)
                .pointAt(util.vector().topOf(pos))
                .attachKeyFrame();
        scene.idle(MACHINE_EXPLANATION_TICKS + OVERLAY_SEPARATION_TICKS);
    }

    private static void moveThroughStation(SceneBuilder scene, SceneBuildingUtil util, ItemStack inputStack, ItemStack output) {
        BlockPos input = util.grid().at(1, 1, 2);
        BlockPos machine = machinePos(util);
        BlockPos result = util.grid().at(5, 1, 2);
        if (!inputStack.isEmpty()) {
            ElementLink<EntityElement> entity = scene.world().createItemEntity(util.vector().topOf(input).add(0, 0.3, 0),
                    util.vector().of(0.14, 0.12, 0), inputStack);
            scene.idle(12);
            scene.world().modifyEntity(entity, Entity::discard);
        }
        scene.effects().indicateSuccess(machine);
        scene.idle(8);
        if (!output.isEmpty()) {
            scene.addKeyframe();
            scene.overlay().showControls(util.vector().topOf(result), Pointing.DOWN, 28).withItem(output);
        }
    }

    /**
     * Present one complete recipe at a glance instead of cycling through disconnected
     * callouts. The primary input sits above up to four centred supporting inputs;
     * additional inputs continue in centred rows below it.
     */
    private static void showInputs(SceneBuilder scene, SceneBuildingUtil util, BlockPos machine, ItemStack... inputs) {
        showInputsWithDescription(scene, util, machine, null, inputs);
    }

    private static void showInputsWithDescription(SceneBuilder scene, SceneBuildingUtil util, BlockPos machine,
                                                  String description, ItemStack... inputs) {
        List<ItemStack> visibleInputs = new ArrayList<>();
        for (ItemStack input : inputs) {
            if (!input.isEmpty()) visibleInputs.add(input);
        }
        if (visibleInputs.isEmpty()) return;

        int duration = 42;
        scene.addKeyframe();
        var machineTop = util.vector().topOf(machine);

        if (visibleInputs.size() == 1) {
            scene.overlay().showControls(machineTop.add(0, 0.45, 0), Pointing.DOWN, duration)
                    .withItem(ponderInputIcon(visibleInputs.getFirst()));
        } else {
            scene.overlay().showControls(machineTop.add(0, 1.05, 0), Pointing.DOWN, duration)
                    .withItem(ponderInputIcon(visibleInputs.getFirst()));
            int supportingCount = visibleInputs.size() - 1;
            for (int index = 0; index < supportingCount; index++) {
                int row = index / 3;
                int rowStart = row * 3;
                int rowCount = Math.min(3, supportingCount - rowStart);
                int column = index - rowStart;
                double xOffset = (column - (rowCount - 1) / 2.0) * 0.9;
                double yOffset = 0.08 - row * 0.82;
                // Equal X offsets alone form a staircase in Ponder's isometric camera.
                // The inverse Z component cancels that vertical projection, producing
                // a genuinely horizontal row on screen.
                scene.overlay().showControls(machineTop.add(xOffset, yOffset, -xOffset), Pointing.DOWN, duration)
                        .withItem(ponderInputIcon(visibleInputs.get(index + 1)));
            }
        }

        String requirement = description == null ? describeRequiredInputs(visibleInputs) : description;
        scene.overlay().showText(duration)
                .placeNearTarget()
                .sharedText("required_inputs", requirement)
                .pointAt(machineTop.add(0, 0.45, 0))
                .attachKeyFrame();
        scene.idle(duration + OVERLAY_SEPARATION_TICKS);
    }

    /** Use one stable, recognisable specimen for every generic organism input. */
    private static ItemStack ponderInputIcon(ItemStack input) {
        if (!GeneticOrganismAdapters.supports(input)) return input;
        return SpeciesUtil.BEE_TYPE.get().getDefaultSpecies().createIndividual().createStack(BeeLifeStage.QUEEN);
    }

    private static String ponderInputName(ItemStack input) {
        if (GeneticOrganismAdapters.supports(input)) return "Any genetic individual";
        var containedFluid = FluidUtil.getFluidContained(input);
        if (containedFluid.isPresent() && !containedFluid.get().isEmpty()) {
            return containedFluid.get().getHoverName().getString();
        }
        String name = ponderItemName(input);
        return name.endsWith(" Bucket") ? name.substring(0, name.length() - " Bucket".length()) : name;
    }

    private static String describeRequiredInputs(List<ItemStack> inputs) {
        List<String> names = inputs.stream().map(BinniePonderScenes::ponderInputName).toList();
        if (names.size() == 1) return names.getFirst();
        return names.getFirst() + " together with " + naturalJoin(names.subList(1, names.size()));
    }

    private static String naturalJoin(List<String> values) {
        if (values.size() == 1) return values.getFirst();
        if (values.size() == 2) return values.getFirst() + " and " + values.get(1);
        return String.join(", ", values.subList(0, values.size() - 1)) + ", and " + values.getLast();
    }

    private static void component(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos, String id, int duration,
                                  String description) {
        scene.world().setBlock(pos, block("binniebees", id).defaultBlockState(), true);
        scene.effects().indicateSuccess(pos);
        scene.overlay().showControls(util.vector().topOf(pos), Pointing.DOWN, 28)
                .withItem(item("binniebees", id));
        scene.overlay().showOutlineWithText(util.select().position(pos), duration)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .text(description);
        scene.idle(duration + 5);
    }

    private static void controls(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty())
            scene.overlay().showControls(util.vector().topOf(pos), Pointing.DOWN, ITEM_CALLOUT_TICKS).withItem(stack);
    }

    private static void result(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        scene.overlay().showControls(util.vector().topOf(pos), Pointing.DOWN, ITEM_CALLOUT_TICKS).withItem(stack);
        scene.overlay().showText(ITEM_CALLOUT_TICKS)
                .placeNearTarget()
                .sharedText("process_output", ponderItemName(stack))
                .pointAt(util.vector().topOf(pos))
                .attachKeyFrame();
        scene.idle(ITEM_CALLOUT_TICKS + OVERLAY_SEPARATION_TICKS);
    }

    private static void text(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos, int duration, String text) {
        scene.overlay().showText(duration)
                .placeNearTarget()
                .text(text)
                .pointAt(util.vector().topOf(pos))
                .attachKeyFrame();
    }

    /**
     * Ponder captions describe the item kind, not a component-bearing specimen's
     * dynamically generated allele name. The latter may still contain a translation
     * key while storyboards are registered during resource reload.
     */
    private static String ponderItemName(ItemStack stack) {
        String descriptionId = stack.getItem().getDescriptionId();
        String translated = stack.getItem().getDescription().getString();
        if (!translated.equals(descriptionId) && !translated.contains(".item.")) return translated;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String[] words = id.getPath().split("_");
        StringBuilder fallback = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!fallback.isEmpty()) fallback.append(' ');
            fallback.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return fallback.toString();
    }

    private static BlockPos machinePos(SceneBuildingUtil util) {
        return util.grid().at(3, 1, 2);
    }

    private static GeneticsJeiRecipe recipe(String machine, String name) {
        return recipe(GeneticsJeiRecipes.build(), machine, name);
    }

    private static GeneticsJeiRecipe recipe(Map<String, List<GeneticsJeiRecipe>> examples, String machine, String name) {
        String suffix = "/" + name;
        return examples.getOrDefault(machine, List.of()).stream()
                .filter(recipe -> recipe.id().getPath().endsWith(suffix))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing Genetics JEI example " + machine + "/" + name));
    }

    private static ItemStack first(List<ItemStack> stacks) {
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.getFirst();
    }

    private static ItemStack fluidBucket(List<FluidStack> fluids, int index) {
        if (index >= fluids.size() || fluids.get(index).isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(fluids.get(index).getFluid().getBucket());
    }

    private static void showFluidInputs(SceneBuilder scene, SceneBuildingUtil util, BlockPos machine,
                                        List<FluidStack> fluids) {
        if (!fluids.isEmpty()) showFluidInput(scene, util, machine, fluids.getFirst(), true);
        if (fluids.size() > 1) showFluidInput(scene, util, machine, fluids.get(1), false);
    }

    /**
     * The source tank is deliberately renderer-backed rather than real fluid, so water
     * and other inputs cannot spread or tick while the Ponder scene is playing.
     */
    private static void showFluidInput(SceneBuilder scene, SceneBuildingUtil util, BlockPos machine,
                                       FluidStack fluid, boolean fromSouth) {
        if (fluid.isEmpty()) return;
        Direction flow = fromSouth ? Direction.NORTH : Direction.SOUTH;
        BlockPos pipe = machine.relative(flow.getOpposite());
        BlockPos source = pipe.relative(flow.getOpposite());
        showFluidSource(scene, util, source, fluid);
        showFluidPipe(scene, util, pipe, flow, fluid);
    }

    private static void showFluidOutputs(SceneBuilder scene, SceneBuildingUtil util, BlockPos machine,
                                         List<FluidStack> fluids) {
        if (fluids.isEmpty() || fluids.getFirst().isEmpty()) return;
        FluidStack output = fluids.getFirst();
        showFluidPipe(scene, util, machine.relative(Direction.EAST), Direction.EAST, output);
        showFluidTank(scene, util, machine.relative(Direction.EAST, 2), Direction.WEST, output);
    }

    /** Reveal the item-output hopper and its destination chest from the station template. */
    private static void showItemOutputRoute(SceneBuilder scene, SceneBuildingUtil util, BlockPos machine) {
        scene.world().showSection(util.select().fromTo(machine.relative(Direction.EAST),
                machine.relative(Direction.EAST, 2)), Direction.WEST);
        scene.idle(SECTION_REVEAL_TICKS);
    }

    /**
     * The Incubator first demonstrates its item recipe, then its fluid recipes. Remove
     * the item route before the same two cells become the output pipe and glass tank.
     */
    private static void hideItemOutputRoute(SceneBuilder scene, SceneBuildingUtil util, BlockPos machine) {
        scene.world().hideSection(util.select().fromTo(machine.relative(Direction.EAST),
                machine.relative(Direction.EAST, 2)), Direction.EAST);
        scene.idle(SECTION_REVEAL_TICKS);
    }

    /** Change an already-revealed input line without replaying its section animation. */
    private static void updateFluidInputs(SceneBuilder scene, BlockPos machine, List<FluidStack> fluids) {
        if (!fluids.isEmpty() && !fluids.getFirst().isEmpty()) {
            updateFluidInput(scene, machine, fluids.getFirst(), true);
        }
        if (fluids.size() > 1 && !fluids.get(1).isEmpty()) {
            updateFluidInput(scene, machine, fluids.get(1), false);
        }
    }

    private static void updateFluidInput(SceneBuilder scene, BlockPos machine, FluidStack fluid, boolean fromSouth) {
        Direction flow = fromSouth ? Direction.NORTH : Direction.SOUTH;
        BlockPos pipe = machine.relative(flow.getOpposite());
        BlockPos source = pipe.relative(flow.getOpposite());
        setDisplayedFluid(scene, source, fluid);
        setDisplayedFluid(scene, pipe, fluid);
    }

    private static void updateFluidOutputs(SceneBuilder scene, BlockPos machine, List<FluidStack> fluids) {
        if (!fluids.isEmpty() && !fluids.getFirst().isEmpty()) {
            setDisplayedFluid(scene, machine.relative(Direction.EAST), fluids.getFirst());
            setDisplayedFluid(scene, machine.relative(Direction.EAST, 2), fluids.getFirst());
        }
    }

    /** Draw a straight, correctly directed fluid link between aligned machines. */
    private static void showFluidConnection(SceneBuilder scene, SceneBuildingUtil util, BlockPos from,
                                            BlockPos to, FluidStack fluid) {
        if (fluid.isEmpty()) return;
        Direction direction;
        int distance;
        if (from.getX() == to.getX() && from.getY() == to.getY()) {
            direction = to.getZ() < from.getZ() ? Direction.NORTH : Direction.SOUTH;
            distance = Math.abs(to.getZ() - from.getZ());
        } else if (from.getZ() == to.getZ() && from.getY() == to.getY()) {
            direction = to.getX() < from.getX() ? Direction.WEST : Direction.EAST;
            distance = Math.abs(to.getX() - from.getX());
        } else {
            throw new IllegalArgumentException("Ponder fluid pipes must connect aligned machines");
        }
        BlockPos cursor = from;
        for (int step = 1; step < distance; step++) {
            cursor = cursor.relative(direction);
            showFluidPipe(scene, util, cursor, direction, fluid);
        }
    }

    private static void showFluidSource(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos, FluidStack fluid) {
        showFluidTank(scene, util, pos, Direction.DOWN, fluid);
    }

    private static void showFluidTank(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos,
                                      Direction revealFrom, FluidStack fluid) {
        scene.world().setBlock(pos, CoreRegistries.PONDER_FLUID_SOURCE.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(pos), revealFrom);
        scene.idle(SECTION_REVEAL_TICKS);
        setDisplayedFluid(scene, pos, fluid);
    }

    private static void showFluidPipe(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos,
                                      Direction direction, FluidStack fluid) {
        PonderFluidPipeBlock pipe = CoreRegistries.PONDER_FLUID_PIPE.get();
        scene.world().setBlock(pos, pipe.defaultBlockState()
                .setValue(PonderFluidPipeBlock.FACING, direction)
                .setValue(PonderFluidPipeBlock.ARROWS, true), false);
        scene.world().showSection(util.select().position(pos), direction.getOpposite());
        scene.idle(SECTION_REVEAL_TICKS);
        setDisplayedFluid(scene, pos, fluid);
    }

    private static void setDisplayedFluid(SceneBuilder scene, BlockPos pos, FluidStack fluid) {
        FluidStack displayed = fluid.copy();
        scene.world().modifyBlockEntity(pos, PonderFluidPipeBlockEntity.class,
                blockEntity -> blockEntity.setDisplayedFluid(displayed));
    }

    private static ItemStack item(String namespace, String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(id(namespace, path)));
    }

    private static Block block(String namespace, String path) {
        return BuiltInRegistries.BLOCK.get(id(namespace, path));
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
