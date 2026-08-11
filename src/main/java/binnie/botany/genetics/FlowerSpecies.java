package binnie.botany.genetics;

import binnie.botany.api.genetics.IFlower;
import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.api.genetics.IFlowerSpeciesType;
import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import forestry.api.core.TemperatureType;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.core.engine.genetics.Species;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Runtime flower species with the full default typed genome. */
public final class FlowerSpecies extends Species<IFlowerSpeciesType, IFlower> implements IFlowerSpecies {
    private final ResourceLocation flowerType;
    private final TemperatureType temperature;
    private final SoilMoisture moisture;
    private final SoilAcidity acidity;
    private final int sections;

    public FlowerSpecies(ResourceLocation id, IFlowerSpeciesType type, IGenome genome, FlowerSpeciesBuilder builder) {
        super(id, type, genome, builder);
        this.flowerType = builder.getFlowerType();
        this.temperature = builder.getTemperature();
        this.moisture = builder.getMoisture();
        this.acidity = builder.getAcidity();
        this.sections = builder.getSections();
    }

    @Override
    public ResourceLocation flowerType() {
        return this.flowerType;
    }

    @Override
    public TemperatureType temperature() {
        return this.temperature;
    }

    @Override
    public SoilMoisture moisture() {
        return this.moisture;
    }

    @Override
    public SoilAcidity acidity() {
        return this.acidity;
    }

    @Override
    public int sections() {
        return this.sections;
    }

    /**
      * Flower names were historically supplied by Botany's flower catalog instead of Forestry's
      * bee/tree/butterfly translation family. Keeping that namespace also lets FlowerItem name all
      * three flower life stages without relying on ForestryCE's built-in-only item-name helper.
    */
    @Override
    public MutableComponent getDisplayName() {
        return Component.translatable("item.binniebotany.flower." + id().getPath());
    }

    @Override
    public IFlower createIndividual(IGenome genome) {
        if (genome.getKaryotype() != getKaryotype()) {
            throw new IllegalArgumentException("Cannot create flower " + id() + " from foreign karyotype " + genome.getKaryotype().id());
        }
        return new FlowerIndividual(genome);
    }

    @Override
    public void addTooltip(IFlower individual, List<Component> tooltip) {
        if (!individual.isAnalyzed()) {
            addUnknownGenomeTooltip(tooltip);
            return;
        }
        addHybridTooltip(tooltip, individual.getGenome(), FlowerChromosomes.SPECIES, "for.flowers.hybrid");
        tooltip.add(Component.translatable("item.binniebotany.flower.age", individual.getAge()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.binniebotany.flower.temperature", this.temperature.getSerializedName(),
                individual.getGenome().getActiveValue(FlowerChromosomes.TEMPERATURE_TOLERANCE).name()).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.binniebotany.flower.moisture", this.moisture.getSerializedName(),
                individual.getGenome().getActiveValue(FlowerChromosomes.HUMIDITY_TOLERANCE).name()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.binniebotany.flower.ph", this.acidity.getSerializedName(),
                individual.getGenome().getActiveValue(FlowerChromosomes.PH_TOLERANCE).name()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.binniebotany.flower.fertility",
                individual.getGenome().getActiveValue(FlowerChromosomes.FERTILITY)).withStyle(ChatFormatting.GOLD));
        for (IChromosome<?> chromosome : getKaryotype().getChromosomes()) {
            AllelePair<?> pair = individual.getGenome().getAllelePair(chromosome);
            if (!pair.isSameAlleles()) {
                tooltip.add(Component.translatable(translationKey(chromosome, pair.active().value()))
                        .append(" / ")
                        .append(Component.translatable(translationKey(chromosome, pair.inactive().value())))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String translationKey(IChromosome chromosome, Object value) {
        return chromosome.translationKey(value);
    }
}
