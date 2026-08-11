package binnie.botany.client.genetics;

import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.api.genetics.IFlower;
import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.genetics.FlowerAnalyzerLayout;
import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.genetics.FlowerSpeciesType;
import forestry.api.client.genetics.IAnalyzerGraphics;
import forestry.api.client.genetics.IAnalyzerPlugin;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpecies;
import forestry.core.platform.util.GeneticsUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
  * Current ForestryCE analyzer bridge for Binnie's 1.12 flower pages. It keeps the legacy
  * chromosome grouping while exposing soil requirements on the formerly empty third page.
  */
public final class FlowerAnalyzerPlugin implements IAnalyzerPlugin<IFlowerSpecies, IFlower> {
    private final Map<ISpecies<?>, ItemStack> iconStacks = GeneticsUtil.getIconStacks(FlowerLifeStage.FLOWER, FlowerSpeciesType.get());

    @Override
    public void drawPage1(IAnalyzerGraphics<IFlowerSpecies, IFlower> graphics, IFlower individual, ILifeStage stage, ItemStack specimen) {
        graphics.drawSpeciesIconsRow(this.iconStacks::get);
        for (var chromosome : FlowerAnalyzerLayout.firstPage()) {
            drawChromosome(graphics, chromosome);
        }
    }

    @Override
    public void drawPage2(IAnalyzerGraphics<IFlowerSpecies, IFlower> graphics, IFlower individual, ILifeStage stage, ItemStack specimen) {
        graphics.drawSpeciesIconsRow(null);
        for (var chromosome : FlowerAnalyzerLayout.secondPage()) {
            drawChromosome(graphics, chromosome);
        }
    }

    @Override
    public void drawPage3(IAnalyzerGraphics<IFlowerSpecies, IFlower> graphics, IFlower individual, ILifeStage stage, ItemStack specimen) {
        IFlowerSpecies active = individual.getGenome().resolveActive(FlowerChromosomes.SPECIES);
        IFlowerSpecies inactive = individual.getGenome().resolveInactive(FlowerChromosomes.SPECIES);

        graphics.drawSpeciesIconsRow(this.iconStacks::get);
        graphics.drawText(Component.translatable("binniebotany.analyzer.soil_requirements"));
        graphics.addLineSpacing(1);
        graphics.drawSplitLine(Component.translatable("binniebotany.analyzer.temperature"),
                temperature(active), temperature(inactive), true, null);
        graphics.drawSplitLine(Component.translatable("binniebotany.analyzer.moisture"),
                moisture(active), moisture(inactive), true, null);
        graphics.drawSplitLine(Component.translatable("binniebotany.analyzer.acidity"),
                acidity(active), acidity(inactive), true, null);
    }

    @Override
    public void drawPage4(IAnalyzerGraphics<IFlowerSpecies, IFlower> graphics, IFlower individual, ILifeStage stage, ItemStack specimen) {
        graphics.drawMutationsPage(this.iconStacks::get);
    }

    private static Component temperature(IFlowerSpecies species) {
        return Component.translatable("binniebotany.analyzer.temperature." + species.temperature().getSerializedName());
    }

    private static Component moisture(IFlowerSpecies species) {
        return Component.translatable("binniebotany.analyzer.moisture." + species.moisture().getSerializedName());
    }

    private static Component acidity(IFlowerSpecies species) {
        return Component.translatable("binniebotany.analyzer.acidity." + species.acidity().getSerializedName());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void drawChromosome(IAnalyzerGraphics<IFlowerSpecies, IFlower> graphics,
                                       forestry.api.core.genetics.alleles.IChromosome<?> chromosome) {
        graphics.drawChromosomeRow((forestry.api.core.genetics.alleles.IChromosome) chromosome);
    }
}
