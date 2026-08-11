package binnie.trees.registry;

import binnie.core.item.VariantItem;
import binnie.trees.item.HammerItem;
import binnie.trees.item.BinnieTreesFoodItem;
import binnie.trees.item.HopsItem;
import binnie.trees.item.GlasswareItem;
import binnie.genetics.item.GeneticKnowledgeItem;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import binnie.core.registry.ModuleRegistry;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

public final class BinnieTreesContent {
    public static final ModuleRegistry CONTENT = new ModuleRegistry("binnietrees");
    public static final DeferredItem<VariantItem> TREE_SPECIES = CONTENT.variantsHidden("tree_species", "binnietrees.tree_species", binnie.core.catalog.LegacyCatalogs.values("binnietrees.tree_species"));
    public static final DeferredItem<VariantItem> PLANK = CONTENT.variants("plank", "binnietrees.plank", binnie.core.catalog.LegacyCatalogs.values("binnietrees.plank"));
    public static final DeferredItem<VariantItem> FORESTRY_PLANK = CONTENT.variants("forestry_plank", "binnietrees.forestry_plank", binnie.core.catalog.LegacyCatalogs.values("binnietrees.forestry_plank"));
    public static final DeferredItem<VariantItem> VANILLA_PLANK = CONTENT.variants("vanilla_plank", "binnietrees.vanilla_plank", binnie.core.catalog.LegacyCatalogs.values("binnietrees.vanilla_plank"));
    public static final DeferredItem<VariantItem> LOG = CONTENT.variants("log", "binnietrees.log", binnie.core.catalog.LegacyCatalogs.values("binnietrees.log"));
    public static final DeferredItem<BinnieTreesFoodItem> FOOD = CONTENT.custom("food",
            properties -> new BinnieTreesFoodItem(properties, binnie.core.catalog.LegacyCatalogs.values("binnietrees.food")), false);
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
            output.accept(BinnieTreesFluids.RESIN.bucket());
            output.accept(BinnieTreesFluids.TURPENTINE.bucket());
            output.accept(CARPENTRY_HAMMER.get());
            output.accept(DURABLE_HAMMER.get());
            output.accept(ARBORIST_DATABASE.get());
            output.accept(MASTER_ARBORIST_DATABASE.get());
            output.accept(MOTH_DATABASE.get());
            output.accept(MASTER_MOTH_DATABASE.get());
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
