package binnie.trees.registry;

import binnie.core.item.VariantItem;
import binnie.core.forestry.ForestryModules;
import binnie.trees.item.HammerItem;
import binnie.trees.item.BinnieTreesFoodItem;
import binnie.trees.item.HopsItem;
import binnie.trees.forestry.LegacyTreeCatalog;
import binnie.trees.item.GlasswareItem;
import binnie.genetics.item.GeneticKnowledgeItem;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import binnie.core.registry.ModuleRegistry;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashMap;
import java.util.Map;

public class BinnieTreesContent {
    public static final ModuleRegistry CONTENT = new ModuleRegistry("binnietrees");
    public static final DeferredItem<VariantItem> TREE_SPECIES = CONTENT.variantsHidden("tree_species", "binnietrees.tree_species", binnie.core.catalog.LegacyCatalogs.values("binnietrees.tree_species"));
    public static final DeferredItem<VariantItem> PLANK = CONTENT.variants("plank", "binnietrees.plank", binnie.core.catalog.LegacyCatalogs.values("binnietrees.plank"));
    public static final DeferredItem<VariantItem> FORESTRY_PLANK = CONTENT.variants("forestry_plank", "binnietrees.forestry_plank", binnie.core.catalog.LegacyCatalogs.values("binnietrees.forestry_plank"));
    public static final DeferredItem<VariantItem> VANILLA_PLANK = CONTENT.variants("vanilla_plank", "binnietrees.vanilla_plank", binnie.core.catalog.LegacyCatalogs.values("binnietrees.vanilla_plank"));
    public static final DeferredItem<VariantItem> LOG = CONTENT.variants("log", "binnietrees.log", binnie.core.catalog.LegacyCatalogs.values("binnietrees.log"));
    public static final DeferredItem<BinnieTreesFoodItem> FOOD = CONTENT.custom("food",
            properties -> new BinnieTreesFoodItem(properties, binnie.core.catalog.LegacyCatalogs.values("binnietrees.food")), false);
    public static final Map<String, DeferredItem<BinnieTreesFoodItem>> FRUIT_FOODS = registerFruitFoods();
    /** Retained registry ids so stacks saved by older versions can be decoded and migrated. */
    public static final Map<String, DeferredItem<BinnieTreesFoodItem>> LEGACY_FRUIT_FOODS = registerLegacyFruitFoods();
    public static final DeferredItem<GlasswareItem> GLASSWARE = CONTENT.custom("glassware",
            properties -> new GlasswareItem(properties, binnie.core.catalog.LegacyCatalogs.values("binnietrees.glassware")), true);
    public static final DeferredItem<Item> BARK = CONTENT.simple("bark");
    public static final DeferredItem<Item> SAWDUST = CONTENT.simple("sawdust");
    public static final DeferredItem<Item> WOOD_WAX = CONTENT.simple("wood_wax");
    public static final DeferredItem<Item> GLASS_FITTING = CONTENT.simple("glass_fitting");
    public static final DeferredItem<Item> PROVEN_GEAR = CONTENT.simple("proven_gear");
    public static final DeferredItem<HammerItem> CARPENTRY_HAMMER = CONTENT.custom("carpentry_hammer",
            properties -> new HammerItem(properties, false), true);
    public static final DeferredItem<HammerItem> DURABLE_HAMMER = CONTENT.custom("durable_hammer",
            properties -> new HammerItem(properties, true), true);
    public static final DeferredItem<HopsItem> HOPS = CONTENT.custom("hops",
            properties -> new HopsItem(properties, BinnieTreesWorldContent.HOPS), true);
    public static final DeferredItem<GeneticKnowledgeItem> ARBORIST_DATABASE = CONTENT.custom("arborist_database",
            properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.DATABASE, GeneticKnowledgeMenu.InterfaceType.ARBORIST_DATABASE, ForestrySpeciesTypes.TREE), true);
    public static final DeferredItem<GeneticKnowledgeItem> MASTER_ARBORIST_DATABASE = CONTENT.custom("master_arborist_database",
            properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.MASTER_DATABASE, GeneticKnowledgeMenu.InterfaceType.ARBORIST_DATABASE, ForestrySpeciesTypes.TREE), true);
    public static final DeferredItem<GeneticKnowledgeItem> MOTH_DATABASE = CONTENT.custom("moth_database",
            properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.DATABASE, GeneticKnowledgeMenu.InterfaceType.SPECIES_DATABASE, ForestrySpeciesTypes.BUTTERFLY), true);
    public static final DeferredItem<GeneticKnowledgeItem> MASTER_MOTH_DATABASE = CONTENT.custom("master_moth_database",
            properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.MASTER_DATABASE, GeneticKnowledgeMenu.InterfaceType.SPECIES_DATABASE, ForestrySpeciesTypes.BUTTERFLY), true);
    public static final DeferredItem<Item> YEAST = CONTENT.simple("yeast");
    public static final DeferredItem<Item> LAGER_YEAST = CONTENT.simple("yeast_lager");
    public static final DeferredItem<Item> GRAIN_WHEAT = CONTENT.simple("grain_wheat");
    public static final DeferredItem<Item> GRAIN_BARLEY = CONTENT.simple("grain_barley");
    public static final DeferredItem<Item> GRAIN_RYE = CONTENT.simple("grain_rye");
    public static final DeferredItem<Item> GRAIN_CORN = CONTENT.simple("grain_corn");
    public static final DeferredItem<Item> GRAIN_ROASTED = CONTENT.simple("grain_roasted");
    static {
        CONTENT.setIcon(() -> {
            try {
                var type = forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.TREE);
                return type.getDefaultSpecies().createStack(type.getDefaultStage());
            } catch (RuntimeException ignored) { return FOOD.get().createStack(FOOD.get().variants().getFirst()); }
        });
    }

    private BinnieTreesContent() {}

    private static Map<String, DeferredItem<BinnieTreesFoodItem>> registerFruitFoods() {
        Map<String, DeferredItem<BinnieTreesFoodItem>> result = new LinkedHashMap<>();
        for (String variant : binnie.core.catalog.LegacyCatalogs.values("binnietrees.food")) {
            if (LegacyTreeCatalog.isForestryOwned(variant)) continue;
            result.put(variant, CONTENT.custom("food_" + variant,
                    properties -> new BinnieTreesFoodItem(properties, variant), false));
        }
        return Map.copyOf(result);
    }

    private static Map<String, DeferredItem<BinnieTreesFoodItem>> registerLegacyFruitFoods() {
        Map<String, DeferredItem<BinnieTreesFoodItem>> result = new LinkedHashMap<>();
        for (String variant : binnie.core.catalog.LegacyCatalogs.values("binnietrees.food")) {
            if (!LegacyTreeCatalog.isForestryOwned(variant)) continue;
            result.put(variant, CONTENT.custom("food_" + variant,
                    properties -> new BinnieTreesFoodItem(properties, variant), false));
        }
        return Map.copyOf(result);
    }

    public static ItemStack fruitStack(String variant) {
        DeferredItem<BinnieTreesFoodItem> item = FRUIT_FOODS.get(variant);
        if (item == null) throw new IllegalArgumentException("Unknown Binnie's Trees fruit " + variant);
        return item.get().createStack(variant);
    }

    public static void register(IEventBus bus) {
        CONTENT.setDisplayItems(output -> {
            try {
                var type = forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.TREE);
                for (var stage : type.getLifeStages()) for (var species : type.getAllSpecies())
                    if (species.id().getNamespace().equals("binnietrees") && !LegacyTreeCatalog.isForestryOwned(species.id().getPath())) output.accept(species.createStack(stage));
            } catch (RuntimeException ignored) {}
            if (ForestryModules.butterfliesLoaded()) try {
                var type = forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.BUTTERFLY);
                for (var stage : type.getLifeStages()) for (var species : type.getAllSpecies())
                    if (species.id().getNamespace().equals("binnietrees")) output.accept(species.createStack(stage));
            } catch (RuntimeException ignored) {}
            for (var item : BinnieTreesWorldContent.activeDecorativeLeafItems().values()) output.accept(item.get());
            for (String value : FRUIT_FOODS.keySet()) output.accept(fruitStack(value));
            BinnieTreeWoodContent.acceptCreativeItems(output);
            output.accept(SAWDUST.get());
            output.accept(BARK.get());
            output.accept(PROVEN_GEAR.get());
            output.accept(WOOD_WAX.get());
            output.accept(YEAST.get());
            output.accept(LAGER_YEAST.get());
            output.accept(GRAIN_WHEAT.get());
            output.accept(GRAIN_BARLEY.get());
            output.accept(GRAIN_RYE.get());
            output.accept(GRAIN_CORN.get());
            output.accept(GRAIN_ROASTED.get());
            output.accept(GLASS_FITTING.get());
            output.accept(HOPS.get());
            output.accept(BinnieTreesFluids.RESIN.bucket());
            output.accept(BinnieTreesFluids.TURPENTINE.bucket());
            output.accept(CARPENTRY_HAMMER.get());
            output.accept(DURABLE_HAMMER.get());
            output.accept(ARBORIST_DATABASE.get());
            output.accept(MASTER_ARBORIST_DATABASE.get());
            if (ForestryModules.butterfliesLoaded()) {
                output.accept(MOTH_DATABASE.get());
                output.accept(MASTER_MOTH_DATABASE.get());
            }
            for (String id : BinnieTreesMachines.REGISTRY.specs().keySet()) output.accept(BinnieTreesMachines.REGISTRY.block(id).get().asItem());
            for (String value : GLASSWARE.get().variants()) output.accept(GLASSWARE.get().createStack(value));
            // ItemDrink#getSubItems also supplied one filled wine glass to make
            // the tintable contents model discoverable without enumerating all
            // 92 liquid/shape combinations.
            output.accept(GLASSWARE.get().filled("wine", BinnieTreesFluids.stack("alcohol", "red_wine", 240)));
        });
        CONTENT.register(bus, "itemGroup.binnietrees");
    }

}
