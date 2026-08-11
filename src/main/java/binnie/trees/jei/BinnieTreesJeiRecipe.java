package binnie.trees.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Immutable view of an executable Binnie's Trees process route. */
public record BinnieTreesJeiRecipe(ResourceLocation id, String machine, List<ItemStack> inputs,
                                  List<ItemStack> catalysts, List<ItemStack> reagents,
                                  List<ItemStack> outputs, List<FluidStack> fluidInputs,
                                  List<FluidStack> fluidOutputs, int duration, int energyPerTick,
                                  int mode, String detail,
                                  List<Ingredient> inputIngredients,
                                  List<List<ItemStack>> inputAlternatives,
                                  List<List<ItemStack>> outputAlternatives) {
    public BinnieTreesJeiRecipe {
        inputs = inputs.stream().map(ItemStack::copy).toList();
        catalysts = catalysts.stream().map(ItemStack::copy).toList();
        reagents = reagents.stream().map(ItemStack::copy).toList();
        outputs = outputs.stream().map(ItemStack::copy).toList();
        fluidInputs = fluidInputs.stream().map(FluidStack::copy).toList();
        fluidOutputs = fluidOutputs.stream().map(FluidStack::copy).toList();
        inputIngredients = List.copyOf(inputIngredients);
        inputAlternatives = inputAlternatives.stream()
                .map(values -> values.stream().map(ItemStack::copy).toList()).toList();
        outputAlternatives = outputAlternatives.stream()
                .map(values -> values.stream().map(ItemStack::copy).toList()).toList();
    }
}
