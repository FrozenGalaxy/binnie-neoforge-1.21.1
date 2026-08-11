package binnie.genetics.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** A compact, component-aware JEI example for one Genetics machine operation. */
public record GeneticsJeiRecipe(
        ResourceLocation id,
        String machine,
        List<ItemStack> inputs,
        List<ItemStack> catalysts,
        List<ItemStack> reagents,
        List<ItemStack> outputs,
        List<FluidStack> fluidInputs,
        List<FluidStack> fluidOutputs,
        int duration,
        int energyPerTick,
        Component operationTooltip,
        List<ItemStack> inputAlternatives,
        List<ItemStack> outputAlternatives
) {
    public GeneticsJeiRecipe {
        inputs = copyItems(inputs);
        catalysts = copyItems(catalysts);
        reagents = copyItems(reagents);
        outputs = copyItems(outputs);
        fluidInputs = copyFluids(fluidInputs);
        fluidOutputs = copyFluids(fluidOutputs);
        operationTooltip = operationTooltip.copy();
        inputAlternatives = copyItems(inputAlternatives);
        outputAlternatives = copyItems(outputAlternatives);
        if (machine.isBlank() || duration <= 0 || energyPerTick < 0 || (outputs.isEmpty() && fluidOutputs.isEmpty())) {
            throw new IllegalArgumentException("Invalid JEI Genetics process " + id);
        }
    }

    GeneticsJeiRecipe withInputAlternatives(List<ItemStack> alternatives) {
        return new GeneticsJeiRecipe(id, machine, inputs, catalysts, reagents, outputs,
                fluidInputs, fluidOutputs, duration, energyPerTick, operationTooltip, alternatives, outputAlternatives);
    }

    GeneticsJeiRecipe withOutputAlternatives(List<ItemStack> alternatives) {
        return new GeneticsJeiRecipe(id, machine, inputs, catalysts, reagents, outputs,
                fluidInputs, fluidOutputs, duration, energyPerTick, operationTooltip, inputAlternatives, alternatives);
    }

    private static List<ItemStack> copyItems(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    private static List<FluidStack> copyFluids(List<FluidStack> stacks) {
        return stacks.stream().map(FluidStack::copy).toList();
    }
}
