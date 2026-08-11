package binnie.genetics.item;

import binnie.core.data.VariantData;
import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.data.GeneticAlleleData;
import binnie.genetics.data.GeneticSequenceData;
import binnie.genetics.data.GeneticSerumData;
import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.botany.genetics.FlowerSpeciesTypes;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.alleles.IChromosome;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Component-backed gene, sequence, serum, and serum-array item. */
public final class GeneticMaterialItem extends Item {
    public enum Kind {
        GENE("gene"), SEQUENCE("sequence"), SERUM("serum"), SERUM_ARRAY("serum_array");
        private final String id;
        Kind(String id) { this.id = id; }
        public String id() { return id; }
        public static Kind byId(String id) {
            for (Kind kind : values()) if (kind.id.equals(id)) return kind;
            throw new IllegalArgumentException("Unknown genetic material kind: " + id);
        }
    }

    public GeneticMaterialItem(Properties properties) {
        super(properties);
    }

    public List<String> variants() {
        return java.util.Arrays.stream(Kind.values()).map(Kind::id).toList();
    }

    public ItemStack blank(Kind kind) {
        ItemStack stack = new ItemStack(this);
        stack.set(BinnieDataComponents.VARIANT, new VariantData("binniegenetics.material", kind.id));
        return stack;
    }

    public ItemStack gene(GeneticAlleleData data) {
        ItemStack stack = blank(Kind.GENE);
        stack.set(BinnieDataComponents.GENETIC_ALLELE, data);
        return stack;
    }

    public ItemStack sequence(GeneticSequenceData data) {
        ItemStack stack = blank(Kind.SEQUENCE);
        stack.set(BinnieDataComponents.GENETIC_SEQUENCE, data);
        return stack;
    }

    public ItemStack serum(GeneticSerumData data, boolean array) {
        ItemStack stack = blank(array ? Kind.SERUM_ARRAY : Kind.SERUM);
        stack.set(BinnieDataComponents.GENETIC_SERUM, data);
        return stack;
    }

    public Kind kind(ItemStack stack) {
        VariantData data = stack.get(BinnieDataComponents.VARIANT);
        return data == null ? Kind.GENE : Kind.byId(data.value());
    }

    @Override
    public Component getName(ItemStack stack) {
        GeneticSerumData serum = stack.get(BinnieDataComponents.GENETIC_SERUM);
        if (serum != null && (kind(stack) == Kind.SERUM || kind(stack) == Kind.SERUM_ARRAY)) {
            return Component.empty()
                    .append(systemDescriptor(serum.alleles().getFirst().speciesType()))
                    .append(" ")
                    .append(Component.translatable(getDescriptionId() + "." + kind(stack).id()));
        }
        return Component.translatable(getDescriptionId() + "." + kind(stack).id());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GeneticAlleleData gene = stack.get(BinnieDataComponents.GENETIC_ALLELE);
        GeneticSequenceData sequence = stack.get(BinnieDataComponents.GENETIC_SEQUENCE);
        GeneticSerumData serum = stack.get(BinnieDataComponents.GENETIC_SERUM);
        if (gene != null) addAlleleTooltip(gene, tooltip);
        if (sequence != null) {
            if (sequence.analyzed()) {
                addAlleleTooltip(sequence.allele(), tooltip);
            } else {
                tooltip.add(Component.translatable("tooltip.binniegenetics.sequence_unanalysed").withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.add(Component.translatable("tooltip.binniegenetics.sequence_progress", sequence.sequenced()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.binniegenetics.quality", sequence.quality()).withStyle(ChatFormatting.AQUA));
        }
        if (serum != null) {
            addSerumTooltip(serum, tooltip, false);
        }
    }

    /** Equivalent {@code ItemGene}/{@code GeneArrayItem} serum tooltip. */
    public static void addSerumTooltip(GeneticSerumData serum, List<Component> tooltip, boolean expanded) {
        tooltip.add(Component.translatable("tooltip.binniegenetics.legacy_charges", serum.charges()).withStyle(ChatFormatting.GRAY));
        List<Component> genes = serum.alleles().stream().map(GeneticMaterialItem::alleleLine).toList();
        if (genes.size() < 4 || expanded) {
            tooltip.addAll(genes);
        } else {
            tooltip.add(genes.get(0));
            tooltip.add(genes.get(1));
            tooltip.add(Component.translatable("tooltip.binniegenetics.more_genes", genes.size() - 2).withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component alleleLine(GeneticAlleleData data) {
        return Component.empty()
                .append(chromosomeName(data).copy().withStyle(ChatFormatting.GOLD))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(alleleName(data).copy().withStyle(ChatFormatting.GRAY));
    }

    @SuppressWarnings("rawtypes")
    private static Component chromosomeName(GeneticAlleleData data) {
        try {
            for (IChromosome chromosome : GeneticOrganismAdapters.get(data.speciesType()).speciesType().getKaryotype().getChromosomes()) {
                if (chromosome.id().equals(data.chromosome())) return Component.translatable(chromosome.chromosomeTranslationKey());
            }
        } catch (RuntimeException ignored) {
            // Keep the typed re1.12 id visible if a client is still receiving registries.
        }
        return Component.literal(data.chromosome().toString());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Component alleleName(GeneticAlleleData data) {
        try {
            for (IChromosome chromosome : GeneticOrganismAdapters.get(data.speciesType()).speciesType().getKaryotype().getChromosomes()) {
                if (chromosome.id().equals(data.chromosome())) {
                    return Component.translatable(chromosome.translationKey(data.value().javaValue()));
                }
            }
        } catch (RuntimeException ignored) {
            // See chromosomeName: a re1.12 id is preferable to losing data.
        }
        return Component.literal(String.valueOf(data.value().javaValue()));
    }

    private static Component systemDescriptor(net.minecraft.resources.ResourceLocation speciesType) {
        String key = speciesType.equals(ForestrySpeciesTypes.BEE) ? "apiarist"
                : speciesType.equals(ForestrySpeciesTypes.TREE) ? "arborist"
                : speciesType.equals(ForestrySpeciesTypes.BUTTERFLY) ? "lepidopterist"
                : speciesType.equals(FlowerSpeciesTypes.FLOWER) ? "botanist" : "unknown";
        return Component.translatable("tooltip.binniegenetics.system." + key);
    }

    private static void addAlleleTooltip(GeneticAlleleData data, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.binniegenetics.organism", data.speciesType().toString()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.binniegenetics.chromosome", data.chromosome().toString()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.binniegenetics.allele", data.value().javaValue().toString(), data.origin().name().toLowerCase()).withStyle(data.dominant() ? ChatFormatting.GOLD : ChatFormatting.WHITE));
    }
}
