package binnie.genetics.api;

import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Typed bridge used by all Genetics machines, independent of organism family. */
public interface GeneticOrganismAdapter {
    ResourceLocation id();

    ISpeciesType<?, ?> speciesType();

    default List<IChromosome<?>> chromosomes() {
        return speciesType().getKaryotype().getChromosomes();
    }

    default boolean supports(ItemStack stack) {
        ISpeciesType<?, ?> type = IIndividualHandlerItem.getSpeciesType(stack);
        return type != null && type.id().equals(id());
    }

    default IIndividual read(ItemStack stack) {
        IIndividual individual = IIndividualHandlerItem.getIndividual(stack);
        if (individual == null || !individual.getType().id().equals(id())) {
            throw new IllegalArgumentException("Stack does not contain a " + id() + " individual");
        }
        return individual;
    }

    default ILifeStage lifeStage(ItemStack stack) {
        ILifeStage stage = IIndividualHandlerItem.getLifeStage(stack);
        if (stage == null) throw new IllegalArgumentException("Stack has no genetic life stage");
        return stage;
    }

    /** Whether the current life stage may be altered by the 1.12 Inoculator and Splicer. */
    boolean isDnaManipulable(ItemStack stack);

    default ItemStack replaceGenome(ItemStack originalStack, IGenome genome) {
        IIndividual original = read(originalStack);
        if (genome.getKaryotype() != speciesType().getKaryotype()) {
            throw new IllegalArgumentException("Genome karyotype does not belong to " + id());
        }
        IIndividual replacement = original.copyWithGenome(genome);
        replacement.setMate(original.getMate());
        if (original.isAnalyzed()) replacement.analyze();
        ItemStack result = originalStack.copyWithCount(1);
        replacement.saveToStack(result);
        return result;
    }

    default ItemStack analyze(ItemStack originalStack) {
        IIndividual individual = read(originalStack);
        individual.analyze();
        ItemStack result = originalStack.copyWithCount(1);
        individual.saveToStack(result);
        return result;
    }
}
