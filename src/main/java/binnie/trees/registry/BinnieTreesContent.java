package binnie.trees.registry;

import binnie.core.item.VariantItem;
import binnie.trees.beverage.BeverageItem;
import binnie.trees.item.HammerItem;
import binnie.trees.item.BinnieTreesFoodItem;
import binnie.trees.item.HopsItem;
import binnie.genetics.item.GeneticKnowledgeItem;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import binnie.core.registry.ModuleRegistry;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import java.util.List;

public final class BinnieTreesContent {
    public static final ModuleRegistry CONTENT = new ModuleRegistry("binnietrees");
    public static final DeferredItem<VariantItem> TREE_SPECIES = CONTENT.variantsHidden("tree_species", "binnietrees.tree_species", binnie.core.catalog.LegacyCatalogs.values("binnietrees.tree_species"));
    public static final DeferredItem<VariantItem> PLANK = CONTENT.variants("plank", "binnietrees.plank", binnie.core.catalog.LegacyCatalogs.values("binnietrees.plank"));
    public static final DeferredItem<VariantItem> FORESTRY_PLANK = CONTENT.variants("forestry_plank", "binnietrees.forestry_plank", binnie.core.catalog.LegacyCatalogs.values("binnietrees.forestry_plank"));
    public static final DeferredItem<VariantItem> VANILLA_PLANK = CONTENT.variants("vanilla_plank", "binnietrees.vanilla_plank", binnie.core.catalog.LegacyCatalogs.values("binnietrees.vanilla_plank"));
    public static final DeferredItem<VariantItem> LOG = CONTENT.variants("log", "binnietrees.log", binnie.core.catalog.LegacyCatalogs.values("binnietrees.log"));
    public static final DeferredItem<BinnieTreesFoodItem> FOOD = CONTENT.custom("food",
            properties -> new BinnieTreesFoodItem(properties, binnie.core.catalog.LegacyCatalogs.values("binnietrees.food")), false);
    public static final DeferredItem<VariantItem> GLASSWARE = CONTENT.variants("glassware", "binnietrees.glassware", binnie.core.catalog.LegacyCatalogs.values("binnietrees.glassware"));
    public static final DeferredItem<BeverageItem> ALCOHOL = beverage("alcohol", "binnietrees.alcohol");
    public static final DeferredItem<BeverageItem> COCKTAIL = beverage("cocktail", "binnietrees.cocktail");
    public static final DeferredItem<BeverageItem> JUICE = beverage("juice", "binnietrees.juice");
    public static final DeferredItem<BeverageItem> LIQUEUR = beverage("liqueur", "binnietrees.liqueur");
    public static final DeferredItem<BeverageItem> MISC_FLUID = beverage("misc_fluid", "binnietrees.misc_fluid");
    public static final DeferredItem<BeverageItem> SPIRIT = beverage("spirit", "binnietrees.spirit");
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


    private static DeferredItem<BeverageItem> beverage(String category, String catalogCategory) {
        List<String> values = binnie.core.catalog.LegacyCatalogs.values(catalogCategory);
        DeferredItem<BeverageItem> item = CONTENT.custom(category,
                properties -> new BeverageItem(properties, catalogCategory, category, values), false);
        return item;
    }

    private BinnieTreesContent() {}
    public static void register(IEventBus bus) {
        CONTENT.setDisplayItems(output -> {
            try {
                var type = forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.TREE);
                for (var stage : type.getLifeStages()) for (var species : type.getAllSpecies())
                    if (species.id().getNamespace().equals("binnietrees")) output.accept(species.createStack(stage));
            } catch (RuntimeException ignored) {}
            try {
                var type = forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.BUTTERFLY);
                for (var stage : type.getLifeStages()) for (var species : type.getAllSpecies())
                    if (species.id().getNamespace().equals("binnietrees")) output.accept(species.createStack(stage));
            } catch (RuntimeException ignored) {}
            for (var item : BinnieTreesWorldContent.decorativeLeafItems().values()) output.accept(item.get());
            BinnieTreesFoodItem food = FOOD.get();
            for (String value : binnie.core.catalog.LegacyCatalogs.values("binnietrees.food")) output.accept(food.createStack(value));
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
            output.accept(BinnieTreesFluids.SAP.bucket());
            output.accept(BinnieTreesFluids.RESIN.bucket());
            output.accept(BinnieTreesFluids.LATEX.bucket());
            output.accept(BinnieTreesFluids.TURPENTINE.bucket());
            output.accept(CARPENTRY_HAMMER.get());
            output.accept(DURABLE_HAMMER.get());
            output.accept(ARBORIST_DATABASE.get());
            output.accept(MASTER_ARBORIST_DATABASE.get());
            output.accept(MOTH_DATABASE.get());
            output.accept(MASTER_MOTH_DATABASE.get());
            for (String id : BinnieTreesMachines.REGISTRY.specs().keySet()) output.accept(BinnieTreesMachines.REGISTRY.block(id).get().asItem());
            for (String value : GLASSWARE.get().variants()) output.accept(GLASSWARE.get().createStack(value));
            acceptBeverages(output, ALCOHOL.get(), binnie.core.catalog.LegacyCatalogs.values("binnietrees.alcohol"));
            acceptBeverages(output, COCKTAIL.get(), binnie.core.catalog.LegacyCatalogs.values("binnietrees.cocktail"));
            acceptBeverages(output, JUICE.get(), binnie.core.catalog.LegacyCatalogs.values("binnietrees.juice"));
            acceptBeverages(output, LIQUEUR.get(), binnie.core.catalog.LegacyCatalogs.values("binnietrees.liqueur"));
            acceptBeverages(output, MISC_FLUID.get(), binnie.core.catalog.LegacyCatalogs.values("binnietrees.misc_fluid"));
            acceptBeverages(output, SPIRIT.get(), binnie.core.catalog.LegacyCatalogs.values("binnietrees.spirit"));
        });
        CONTENT.register(bus, "itemGroup.binnietrees");
    }

    private static void acceptBeverages(net.minecraft.world.item.CreativeModeTab.Output output, BeverageItem item, List<String> values) {
        for (String value : values) output.accept(item.createStack(value));
    }
}
