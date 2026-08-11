package binnie.botany.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public record TileworkerJeiRecipe(ResourceLocation id, Ingredient primary, Ingredient secondary,
                                  List<ItemStack> materialAlternatives, ItemStack mortar,
                                  List<ItemStack> outputAlternatives) {
    public TileworkerJeiRecipe {
        materialAlternatives = materialAlternatives.stream().map(ItemStack::copy).toList();
        mortar = mortar.copy();
        outputAlternatives = outputAlternatives.stream().map(ItemStack::copy).toList();
    }
}
