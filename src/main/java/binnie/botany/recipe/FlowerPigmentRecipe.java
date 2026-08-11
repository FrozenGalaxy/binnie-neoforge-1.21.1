package binnie.botany.recipe;

import binnie.botany.api.genetics.IFlower;
import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.registry.BotanyContent;
import binnie.botany.item.BotanyPigments;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** The 1.12 implementation {@code PigmentRecipe}: one established flower yields its active primary-colour pigment. */
public final class FlowerPigmentRecipe extends CustomRecipe {
    public FlowerPigmentRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !result(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result(input);
    }

    private static ItemStack result(CraftingInput input) {
        IFlower flower = null;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (flower != null || !(IIndividualHandlerItem.getIndividual(stack) instanceof IFlower candidate)
                    || candidate.getAge() < 1) {
                return ItemStack.EMPTY;
            }
            flower = candidate;
        }
        if (flower == null) return ItemStack.EMPTY;
        ResourceLocation colour = flower.getGenome().getActiveValue(FlowerChromosomes.PRIMARY_COLOR);
        String id = colour.getPath();
        return binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color").contains(id)
                ? BotanyPigments.stackFor(id, BotanyContent.PIGMENT.get()) : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BotanyRecipeRegistries.FLOWER_PIGMENT.get();
    }
}
