package binnie.genetics.registry;

import binnie.genetics.item.GeneticMaterialItem;
import binnie.core.forestry.ForestryModules;
import binnie.genetics.item.GeneticKnowledgeItem;
import binnie.genetics.item.LaboratoryReagentItem;
import binnie.genetics.item.PunnettSquareItem;
import binnie.genetics.data.AlleleValue;
import binnie.genetics.data.GeneticAlleleData;
import binnie.genetics.data.GeneticSerumData;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.core.registry.ModuleRegistry;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

public class GeneticsContent {
    public static final ModuleRegistry CONTENT = new ModuleRegistry("binniegenetics");
    public static final DeferredItem<GeneticMaterialItem> GENETIC_MATERIAL = CONTENT.custom("genetic_material", GeneticMaterialItem::new, false);

    // 1.12-compatible laboratory consumables. Each reagent cell retains the old charged-slot fractional state.
    public static final DeferredItem<LaboratoryReagentItem> ENZYME = CONTENT.custom("enzyme", LaboratoryReagentItem::new, true);
    public static final DeferredItem<Item> EMPTY_SEQUENCER = CONTENT.custom("empty_sequencer", properties -> new Item(properties), true);
    public static final DeferredItem<LaboratoryReagentItem> FLUORESCENT_DYE = CONTENT.custom("fluorescent_dye", LaboratoryReagentItem::new, true);
    public static final DeferredItem<LaboratoryReagentItem> DNA_DYE = CONTENT.custom("dna_dye", LaboratoryReagentItem::new, true);
    public static final DeferredItem<Item> GROWTH_MEDIUM = CONTENT.simple("growth_medium");
    public static final DeferredItem<Item> LABORATORY_CASING = CONTENT.simple("laboratory_casing");
    public static final DeferredItem<Item> INTEGRATED_CIRCUIT = CONTENT.simple("integrated_circuit");
    public static final DeferredItem<Item> INTEGRATED_CPU = CONTENT.simple("integrated_cpu");
    public static final DeferredItem<Item> INTEGRATED_CASING = CONTENT.simple("integrated_casing");

    public static final DeferredItem<GeneticKnowledgeItem> DATABASE = CONTENT.custom("database", properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.DATABASE), true);
    public static final DeferredItem<GeneticKnowledgeItem> MASTER_DATABASE = CONTENT.custom("master_database", properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.MASTER_DATABASE), true);
    public static final DeferredItem<GeneticKnowledgeItem> REGISTRY = CONTENT.custom("registry", properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.REGISTRY), true);
    public static final DeferredItem<GeneticKnowledgeItem> MASTER_REGISTRY = CONTENT.custom("master_registry", properties -> new GeneticKnowledgeItem(properties, GeneticKnowledgeMenu.Mode.MASTER_REGISTRY), true);
    public static final DeferredItem<GeneticKnowledgeItem> ANALYST = CONTENT.custom("analyst", properties ->
            new GeneticKnowledgeItem(properties.component(net.minecraft.core.component.DataComponents.CONTAINER,
                    net.minecraft.world.item.component.ItemContainerContents.EMPTY), GeneticKnowledgeMenu.Mode.ANALYST), true);
    public static final DeferredItem<PunnettSquareItem> PUNNETT_SQUARE = CONTENT.custom("punnett_square", PunnettSquareItem::new, true);
    static {
        CONTENT.setIcon(() -> new ItemStack(INTEGRATED_CPU.get()));
        CONTENT.setDisplayItems(output -> {
            GeneticMaterialItem material = GENETIC_MATERIAL.get();
            // 1.12 ModuleCore order: serum, serum array, knowledge tools, misc components, sequence, then machines.
            legacySerumStacks(false).forEach(output::accept);
            legacySerumStacks(true).forEach(output::accept);
            output.accept(DATABASE.get());
            output.accept(ANALYST.get());
            output.accept(REGISTRY.get());
            output.accept(MASTER_REGISTRY.get());
            output.accept(MASTER_DATABASE.get());
            output.accept(PUNNETT_SQUARE.get());
            output.accept(LABORATORY_CASING.get());
            output.accept(DNA_DYE.get());
            output.accept(FLUORESCENT_DYE.get());
            output.accept(ENZYME.get());
            output.accept(GROWTH_MEDIUM.get());
            output.accept(GeneticsFluids.GROWTH_MEDIUM.bucket());
            output.accept(GeneticsFluids.BACTERIA.bucket());
            output.accept(GeneticsFluids.POLYMERISING_BACTERIA.bucket());
            output.accept(GeneticsFluids.RAW_DNA.bucket());
            output.accept(GeneticsFluids.VECTOR_BACTERIA.bucket());
            output.accept(EMPTY_SEQUENCER.get());
            output.accept(material.blank(GeneticMaterialItem.Kind.GENE));
            output.accept(material.blank(GeneticMaterialItem.Kind.SEQUENCE));
            output.accept(INTEGRATED_CIRCUIT.get());
            output.accept(INTEGRATED_CPU.get());
            output.accept(INTEGRATED_CASING.get());
            acceptLegacySequenceExample(output);
            for (String legacyId : binnie.core.catalog.LegacyCatalogs.values("binniegenetics.machine")) {
                String runtimeId = GeneticsMachines.runtimeId(legacyId);
                output.accept(GeneticsMachines.REGISTRY.block(runtimeId).get().asItem());
            }
        });
    }

    /**
      * Dynamic counterparts to 1.12's {@code ItemSerum#getSubItems} and
      * {@code ItemSerumArray#getSubItems}. These must be available to both the
      * creative tab and JEI: component-bearing serum stacks are not discoverable
      * from the bare registry item alone.
      */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<ItemStack> legacySerumStacks(boolean arrays) {
        GeneticMaterialItem material = GENETIC_MATERIAL.get();
        java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
        try {
            // ItemSerum#getSubItems and ItemSerumArray#getSubItems exposed one
            // representative for each of Binnie's four laboratory roots, not a
            // Cartesian list of every allele of every species.
            var types = new java.util.ArrayList<>(List.of(ForestrySpeciesTypes.BEE, ForestrySpeciesTypes.TREE));
            if (ForestryModules.butterfliesLoaded()) types.add(ForestrySpeciesTypes.BUTTERFLY);
            types.add(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("binniebotany", "flower"));
            for (var id : types) {
                var type = forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(id);
                var genome = type.getDefaultSpecies().getDefaultGenome();
                java.util.ArrayList<GeneticAlleleData> genes = new java.util.ArrayList<>();
                for (var chromosomeObject : genome.getKaryotype().getChromosomes()) {
                    forestry.api.core.genetics.alleles.IChromosome chromosome =
                            (forestry.api.core.genetics.alleles.IChromosome) chromosomeObject;
                    forestry.api.core.genetics.alleles.Allele allele = genome.getActiveAllele(chromosome);
                    genes.add(new GeneticAlleleData(type.id(), chromosome.id(), AlleleValue.fromJava(allele.value()),
                            allele.dominant(), GeneticAlleleData.Origin.ACTIVE));
                }
                if (genes.isEmpty()) continue;
                List<GeneticAlleleData> payload = arrays ? List.copyOf(genes) : List.of(genes.getFirst());
                addDistinct(stacks, material.serum(new GeneticSerumData(payload, 100, 16), arrays));
            }
        } catch (RuntimeException ignored) {
            // JEI/the creative tab may be queried before Forestry registration finishes.
        }
        return List.copyOf(stacks);
    }

    private static void addDistinct(List<ItemStack> stacks, ItemStack candidate) {
        if (stacks.stream().noneMatch(existing -> ItemStack.isSameItemSameComponents(existing, candidate))) {
            stacks.add(candidate);
        }
    }

    private static void acceptLegacySequenceExample(net.minecraft.world.item.CreativeModeTab.Output output) {
        try {
            var bee = forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.BEE);
            var species = bee.getDefaultSpecies();
            var chromosome = bee.getKaryotype().getSpeciesChromosome();
            var allele = species.getDefaultGenome().getActiveAllele(chromosome);
            GeneticAlleleData data = new GeneticAlleleData(bee.id(), chromosome.id(), AlleleValue.fromJava(allele.value()), allele.dominant(), GeneticAlleleData.Origin.ACTIVE);
            output.accept(GENETIC_MATERIAL.get().sequence(new binnie.genetics.data.GeneticSequenceData(data, 0, 100, false)));
        } catch (RuntimeException ignored) {}
    }

    private GeneticsContent() {}
    public static void register(IEventBus bus) { CONTENT.register(bus, "itemGroup.binniegenetics"); }
}
