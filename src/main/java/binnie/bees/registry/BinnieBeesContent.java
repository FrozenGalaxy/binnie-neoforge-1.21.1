package binnie.bees.registry;

import binnie.core.item.VariantItem;
import binnie.bees.item.IndustrialFrameItem;
import binnie.bees.item.HiveFrameItem;
import binnie.core.registry.ModuleRegistry;
import binnie.genetics.item.GeneticKnowledgeItem;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.trees.forestry.LegacyHeightMutationCondition;
import forestry.api.ForestryRegistries;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.MutationConditionType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BinnieBeesContent {
    public static final ModuleRegistry CONTENT = new ModuleRegistry("binniebees");
    private static final DeferredRegister<MutationConditionType<?>> MUTATION_CONDITION_TYPES =
        DeferredRegister.create(ForestryRegistries.Keys.MUTATION_CONDITION_TYPE, "binnietrees");
    public static final DeferredHolder<MutationConditionType<?>, MutationConditionType<LegacyHeightMutationCondition>> HEIGHT_MUTATION_CONDITION =
        MUTATION_CONDITION_TYPES.register("height", () -> LegacyHeightMutationCondition.TYPE);
    public static final DeferredItem<VariantItem> BEE_SPECIES = CONTENT.variantsHidden("bee_species", "binniebees.bee_species", binnie.core.catalog.LegacyCatalogs.values("binniebees.bee_species"));
    public static final DeferredItem<VariantItem> BEE_EFFECT = CONTENT.variantsHidden("bee_effect", "binniebees.bee_effect", binnie.core.catalog.LegacyCatalogs.values("binniebees.bee_effect"));
    public static final DeferredItem<VariantItem> HONEY_COMB = CONTENT.variants("honey_comb", "binniebees.honey_comb", binnie.core.catalog.LegacyCatalogs.values("binniebees.honey_comb"));
    /** Current fallback for Forestry 1.12's removed Quartz comb variant. */
    public static final DeferredItem<Item> QUARTZ_COMB = CONTENT.simple("quartz_comb");
    public static final List<String> MISC_PRODUCT_IDS = List.of(
            "scented_gear", "diamond_shard", "emerald_shard", "ruby_shard", "sapphire_shard", "lapis_shard",
            "iron_dust", "gold_dust", "silver_dust", "platinum_dust", "copper_dust", "tin_dust", "nickel_dust",
            "lead_dust", "zinc_dust", "titanium_dust", "tungsten_dust", "radioactive_dust", "coal_dust",
            "clay_dust", "yellorium_dust", "blutonium_dust", "cyanite_dust");
    public static final Map<String, DeferredItem<Item>> MISC_PRODUCTS = createMiscProducts();
    public static final DeferredItem<VariantItem> HONEY_DROP = CONTENT.variants("honey_drop", "binniebees.honey_drop", binnie.core.catalog.LegacyCatalogs.values("binniebees.honey_drop"));
    public static final DeferredItem<VariantItem> PROPOLIS = CONTENT.variants("propolis", "binniebees.propolis", binnie.core.catalog.LegacyCatalogs.values("binniebees.propolis"));
    public static final DeferredItem<HiveFrameItem> HIVE_FRAME = CONTENT.custom("hive_frame", HiveFrameItem::new, false);
    /** 1.12 industrial frame programs remain registered for the alveary frame machine, but are not player-tab hive frames. */
    public static final DeferredItem<IndustrialFrameItem> INDUSTRIAL_FRAME = CONTENT.custom("industrial_frame",
            properties -> new IndustrialFrameItem(properties, binnie.core.catalog.LegacyCatalogs.values("binniebees.industrial_frame")), false);
    static {
        CONTENT.setIcon(() -> {
            try {
                var type = forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.BEE);
                return type.getDefaultSpecies().createStack(type.getDefaultStage());
            } catch (RuntimeException ignored) { return HONEY_COMB.get().createStack(HONEY_COMB.get().variants().getFirst()); }
        });
    }
    public static final DeferredItem<VariantItem> HIVE = CONTENT.variantsHidden("hive", "binniebees.hive_type", binnie.core.catalog.LegacyCatalogs.values("binniebees.hive_type"));
    public static final DeferredItem<VariantItem> ALVEARY_COMPONENT = CONTENT.variantsHidden("alveary_component", "binniebees.alveary_component", binnie.core.catalog.LegacyCatalogs.values("binniebees.alveary_component"));
    public static final DeferredItem<GeneticKnowledgeItem> BEE_DATABASE = CONTENT.custom("bee_database",
            properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.DATABASE, GeneticKnowledgeMenu.InterfaceType.SPECIES_DATABASE, ForestrySpeciesTypes.BEE), false);
    public static final DeferredItem<GeneticKnowledgeItem> MASTER_BEE_DATABASE = CONTENT.custom("master_bee_database",
            properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.MASTER_DATABASE, GeneticKnowledgeMenu.InterfaceType.SPECIES_DATABASE, ForestrySpeciesTypes.BEE), false);
    private static Map<String, DeferredItem<Item>> createMiscProducts() {
        Map<String, DeferredItem<Item>> result = new LinkedHashMap<>();
        for (String id : MISC_PRODUCT_IDS) result.put(id, CONTENT.simple(id));
        return Map.copyOf(result);
    }
    private BinnieBeesContent() {}
    public static void register(IEventBus bus) {
        CONTENT.setDisplayItems(output -> {
            try {
                var type = forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.BEE);
                for (var stage : type.getLifeStages()) for (var species : type.getAllSpecies())
                    if (species.id().getNamespace().equals("binniebees")) output.accept(species.createStack(stage));
            } catch (RuntimeException ignored) {}
            for (String value : HONEY_COMB.get().variants()) output.accept(HONEY_COMB.get().createStack(value));
            output.accept(QUARTZ_COMB.get());
            for (String id : MISC_PRODUCT_IDS) output.accept(MISC_PRODUCTS.get(id).get());
            for (String value : HONEY_DROP.get().variants()) output.accept(HONEY_DROP.get().createStack(value));
            for (String value : PROPOLIS.get().variants()) output.accept(PROPOLIS.get().createStack(value));
            for (String value : HiveFrameItem.VARIANTS) output.accept(HIVE_FRAME.get().createStack(value));
            output.accept(BEE_DATABASE.get());
            output.accept(MASTER_BEE_DATABASE.get());
            for (var type : binnie.bees.block.BinnieBeeHiveBlock.HiveType.values()) output.accept(BinnieBeesWorldContent.hive(type).get().asItem());
            for (var type : binnie.bees.block.AlvearyPartType.values()) output.accept(BinnieBeesWorldContent.alveary(type).get().asItem());
        });
        CONTENT.register(bus, "itemGroup.binniebees");
        MUTATION_CONDITION_TYPES.register(bus);
    }
}
