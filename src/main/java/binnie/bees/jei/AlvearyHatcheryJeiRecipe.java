package binnie.bees.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Dynamic queen-genome to matching larvae operation performed by the active hatchery component. */
public record AlvearyHatcheryJeiRecipe(ResourceLocation id, ItemStack queen, ItemStack larvae,
                                      int attemptDenominator) {
    public AlvearyHatcheryJeiRecipe {
        queen = queen.copy();
        larvae = larvae.copy();
        if (queen.isEmpty() || larvae.isEmpty() || attemptDenominator <= 0) {
            throw new IllegalArgumentException("Invalid alveary hatchery operation");
        }
    }
}
