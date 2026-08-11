package binnie.botany.registry;

import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.genetics.FlowerItem;
import binnie.botany.genetics.FlowerSpeciesType;
import binnie.botany.genetics.FlowerSpeciesTypes;
import binnie.core.item.VariantItem;
import binnie.botany.item.FertiliserItem;
import binnie.botany.item.SoilMeterItem;
import binnie.botany.item.TrowelItem;
import binnie.botany.item.WeedkillerItem;
import binnie.botany.item.InsulatedTubeItem;
import binnie.botany.item.FlowerColorVariantItem;
import binnie.botany.item.BotanyPigments;
import binnie.genetics.item.GeneticKnowledgeItem;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.core.registry.ModuleRegistry;
import forestry.core.engine.genetics.ItemGE;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import forestry.core.content.backpacks.BackpackDefinition;
import forestry.core.content.backpacks.BackpackFilterNaturalist;
import forestry.core.content.backpacks.items.NaturalistBackpackItem;

public final class BotanyContent {
    public static final ModuleRegistry CONTENT = new ModuleRegistry("binniebotany");
    public static final DeferredItem<FlowerItem> FLOWER = CONTENT.custom("flower", properties -> new FlowerItem(properties, FlowerLifeStage.FLOWER), false);
    public static final DeferredItem<FlowerItem> POLLEN = CONTENT.custom("pollen", properties -> new FlowerItem(properties, FlowerLifeStage.POLLEN), false);
    public static final DeferredItem<FlowerItem> SEED = CONTENT.custom("seed", properties -> new FlowerItem(properties, FlowerLifeStage.SEED), false);
    public static final DeferredItem<FlowerColorVariantItem> PIGMENT = CONTENT.custom("pigment",
            properties -> new FlowerColorVariantItem(properties, "binniebotany.flower_color",
                    BotanyPigments.customColors(), "item.binniebotany.pigment.name"), false);
    public static final DeferredItem<net.minecraft.world.item.Item> MORTAR = CONTENT.simple("mortar");
    public static final DeferredItem<VariantItem> CERAMIC_PATTERN = CONTENT.variantsHidden("ceramic_pattern", "binniebotany.ceramic_pattern", binnie.core.catalog.LegacyCatalogs.values("binniebotany.ceramic_pattern"));
    public static final DeferredItem<FlowerColorVariantItem> CLAY = CONTENT.custom("clay",
            properties -> new FlowerColorVariantItem(properties, "binniebotany.flower_color",
                    binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color"), "item.binniebotany.clay.name"), false);
    public static final DeferredItem<FertiliserItem> POWDER_ASH = CONTENT.custom("powder_ash", properties -> new FertiliserItem(properties, FertiliserItem.Kind.ALKALINE), false);
    public static final DeferredItem<FertiliserItem> POWDER_PULP = CONTENT.custom("powder_pulp", properties -> new FertiliserItem(properties, FertiliserItem.Kind.ALKALINE), false);
    public static final DeferredItem<FertiliserItem> POWDER_MULCH = CONTENT.custom("powder_mulch", properties -> new FertiliserItem(properties, FertiliserItem.Kind.ACID), false);
    public static final DeferredItem<FertiliserItem> POWDER_SULPHUR = CONTENT.custom("powder_sulphur", properties -> new FertiliserItem(properties, FertiliserItem.Kind.ACID), false);
    public static final DeferredItem<FertiliserItem> POWDER_FERTILISER = CONTENT.custom("powder_fertiliser", properties -> new FertiliserItem(properties, FertiliserItem.Kind.NUTRIENT), false);
    public static final DeferredItem<FertiliserItem> POWDER_COMPOST = CONTENT.custom("powder_compost", properties -> new FertiliserItem(properties, FertiliserItem.Kind.NUTRIENT), false);
    public static final DeferredItem<FertiliserItem> FERTILISER = CONTENT.custom("fertiliser", properties -> new FertiliserItem(properties, FertiliserItem.Kind.NUTRIENT), true);
    public static final DeferredItem<FertiliserItem> ACID_FERTILISER = CONTENT.custom("acid_fertiliser", properties -> new FertiliserItem(properties, FertiliserItem.Kind.ACID), true);
    public static final DeferredItem<FertiliserItem> ALKALINE_FERTILISER = CONTENT.custom("alkaline_fertiliser", properties -> new FertiliserItem(properties, FertiliserItem.Kind.ALKALINE), true);
    public static final DeferredItem<WeedkillerItem> WEEDKILLER = CONTENT.custom("weedkiller", WeedkillerItem::new, true);
    public static final DeferredItem<TrowelItem> TROWEL_WOOD = CONTENT.custom("trowel_wood", properties -> new TrowelItem(properties, 59), true);
    public static final DeferredItem<TrowelItem> TROWEL_STONE = CONTENT.custom("trowel_stone", properties -> new TrowelItem(properties, 131), true);
    public static final DeferredItem<TrowelItem> TROWEL_IRON = CONTENT.custom("trowel_iron", properties -> new TrowelItem(properties, 250), true);
    public static final DeferredItem<TrowelItem> TROWEL_GOLD = CONTENT.custom("trowel_gold", properties -> new TrowelItem(properties, 32), true);
    public static final DeferredItem<TrowelItem> TROWEL_DIAMOND = CONTENT.custom("trowel_diamond", properties -> new TrowelItem(properties, 1561), true);
    public static final DeferredItem<SoilMeterItem> SOIL_METER = CONTENT.custom("soil_meter", SoilMeterItem::new, true);
    public static final DeferredItem<GeneticKnowledgeItem> BOTANIST_DATABASE = CONTENT.custom("botanist_database",
            properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.DATABASE, GeneticKnowledgeMenu.InterfaceType.SPECIES_DATABASE, FlowerSpeciesTypes.FLOWER), true);
    public static final DeferredItem<GeneticKnowledgeItem> BOTANIST_DATABASE_MASTER = CONTENT.custom("botanist_database_master",
            properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.MASTER_DATABASE, GeneticKnowledgeMenu.InterfaceType.SPECIES_DATABASE, FlowerSpeciesTypes.FLOWER), true);
    private static final BackpackDefinition BOTANIST_BACKPACK_DEFINITION = new BackpackDefinition(0xf6e83e, 0xffffff,
            new BackpackFilterNaturalist(FlowerSpeciesTypes.FLOWER));
    public static final DeferredItem<NaturalistBackpackItem> BOTANIST_BACKPACK = CONTENT.custom("botanist_backpack",
            properties -> new NaturalistBackpackItem(FlowerSpeciesTypes.FLOWER, BOTANIST_BACKPACK_DEFINITION,
                    properties.stacksTo(1).component(net.minecraft.core.component.DataComponents.CONTAINER,
                            net.minecraft.world.item.component.ItemContainerContents.EMPTY)), true);
    public static final List<String> INSULATED_TUBE_VARIANTS = binnie.botany.farming.BotanyTubeVariants.allIds();
    public static final DeferredItem<InsulatedTubeItem> INSULATED_TUBE = CONTENT.custom("insulated_tube",
            properties -> new InsulatedTubeItem(properties, "binniebotany.insulated_tube", INSULATED_TUBE_VARIANTS), false);

    static {
        CONTENT.setIcon(() -> BotanyWorldContent.LEGACY_PLANT_ITEMS.isEmpty()
                ? new ItemStack(FERTILISER.get()) : new ItemStack(BotanyWorldContent.LEGACY_PLANT_ITEMS.getFirst().get()));
        CONTENT.setDisplayItems(output -> {
            // 1.12 Gardening registration order: five plant states, then six 9-state soil families.
            for (var item : BotanyWorldContent.LEGACY_PLANT_ITEMS) output.accept(item.get());
            // Display the real component-bearing block items. The old fixed-ID
            // aliases remain registry-internal, but cannot participate in the
            // component-sensitive gardening recipes or JEI usage graph.
            for (var quality : binnie.botany.block.SoilQuality.values()) {
                var item = switch (quality) {
                    case SOIL -> BotanyWorldContent.SOIL_ITEM.get();
                    case LOAM -> BotanyWorldContent.LOAM_ITEM.get();
                    case FLOWERBED -> BotanyWorldContent.FLOWERBED_ITEM.get();
                };
                for (var moisture : binnie.botany.api.genetics.SoilMoisture.values()) {
                    for (var acidity : binnie.botany.api.genetics.SoilAcidity.values()) {
                        output.accept(BotanyWorldContent.soilStack(item, moisture, acidity,
                                quality.initialFertility(), false));
                        output.accept(BotanyWorldContent.soilStack(item, moisture, acidity,
                                quality.initialFertility(), true));
                    }
                }
            }
            output.accept(SOIL_METER.get());
            output.accept(TROWEL_WOOD.get());
            output.accept(TROWEL_STONE.get());
            output.accept(TROWEL_IRON.get());
            output.accept(TROWEL_DIAMOND.get());
            output.accept(TROWEL_GOLD.get());
            output.accept(POWDER_ASH.get());
            output.accept(POWDER_PULP.get());
            output.accept(POWDER_MULCH.get());
            output.accept(POWDER_SULPHUR.get());
            output.accept(POWDER_FERTILISER.get());
            output.accept(POWDER_COMPOST.get());
            output.accept(MORTAR.get());
            output.accept(WEEDKILLER.get());

            output.accept(BOTANIST_BACKPACK.get());
            try {
                var type = FlowerSpeciesType.get();
                for (var stage : FlowerLifeStage.values()) {
                    ArrayList<ItemStack> stacks = new ArrayList<>();
                    ItemGE.addCreativeItems(stage, stacks, true, type);
                    for (ItemStack stack : stacks) output.accept(stack);
                }
            } catch (RuntimeException ignored) {}

            for (String color : binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color")) {
                output.accept(BotanyWorldContent.CERAMIC_ITEM.get().createStack(color));
            }
            for (String color : binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color")) {
                output.accept(BotanyWorldContent.STAINED_GLASS_ITEM.get().createStack(color));
            }
            binnie.botany.registry.BotanyCreativeParity.acceptCeramicDesigns(output);
            binnie.botany.registry.BotanyCreativeParity.acceptCeramicBricks(output);
            for (String color : BotanyPigments.customColors()) output.accept(PIGMENT.get().createStack(color));
            for (String color : binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color")) output.accept(CLAY.get().createStack(color));
            output.accept(BOTANIST_DATABASE.get());
            output.accept(BOTANIST_DATABASE_MASTER.get());
            for (String id : binnie.botany.farming.BotanyTubeVariants.physicalIds()) output.accept(INSULATED_TUBE.get().createStack(id));
            output.accept(BotanyMachines.REGISTRY.block("tileworker").get().asItem());
        });
    }

    private BotanyContent() {}
    public static void register(IEventBus bus) { CONTENT.register(bus, "itemGroup.binniebotany"); }
}
