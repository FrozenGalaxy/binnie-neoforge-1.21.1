package binnie.botany.jei;

import binnie.botany.api.genetics.IFlower;
import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.item.BotanyPigments;
import binnie.botany.recipe.FlowerPigmentRecipe;
import binnie.botany.registry.BotanyContent;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/** Presents the dynamic pigment recipe in JEI's normal shapeless crafting category. */
public final class FlowerPigmentCraftingExtension implements ICraftingCategoryExtension<FlowerPigmentRecipe> {
    static final String FLOWER_SLOT = "binnie_flower_pigment_input";
    static final String PIGMENT_SLOT = "binnie_flower_pigment_output";

    @Override
    public void setRecipe(RecipeHolder<FlowerPigmentRecipe> holder, IRecipeLayoutBuilder builder,
                          ICraftingGridHelper grid, IFocusGroup focuses) {
        List<BotanyJeiPlugin.PigmentPair> pairs = BotanyJeiPlugin.pigmentPairs();
        List<ItemStack> flowers = pairs.stream().map(BotanyJeiPlugin.PigmentPair::flower).toList();
        List<ItemStack> pigments = pairs.stream().map(BotanyJeiPlugin.PigmentPair::pigment).toList();

        // A one-ingredient shapeless recipe is centered by JEI in slot 4 of its 3x3 grid.
        grid.createAndSetInputs(builder, List.of(flowers), 0, 0).get(4)
                .setSlotName(FLOWER_SLOT)
                .addRichTooltipCallback((view, tooltip) -> view.getDisplayedItemStack().ifPresent(stack -> {
                    if (!(IIndividualHandlerItem.getIndividual(stack) instanceof IFlower flower)) return;
                    ResourceLocation color = flower.getGenome().getActiveValue(FlowerChromosomes.PRIMARY_COLOR);
                    tooltip.add(Component.translatable("jei.binniebotany.flower_pigment.mature").withStyle(ChatFormatting.GRAY));
                    tooltip.add(Component.translatable("jei.binniebotany.flower_pigment.primary_color",
                            Component.translatable("item.binniebotany.pigment." + color.getPath()))
                            .withStyle(ChatFormatting.YELLOW));
                }));
        grid.createAndSetOutputs(builder, pigments).setSlotName(PIGMENT_SLOT);
    }

    @Override
    public void onDisplayedIngredientsUpdate(RecipeHolder<FlowerPigmentRecipe> holder,
                                             List<IRecipeSlotDrawable> slots, IFocusGroup focuses) {
        IRecipeSlotDrawable flowerSlot = named(slots, FLOWER_SLOT);
        IRecipeSlotDrawable pigmentSlot = named(slots, PIGMENT_SLOT);
        if (flowerSlot == null || pigmentSlot == null) return;
        flowerSlot.getDisplayedItemStack().ifPresent(stack -> {
            if (!(IIndividualHandlerItem.getIndividual(stack) instanceof IFlower flower)) return;
            ResourceLocation color = flower.getGenome().getActiveValue(FlowerChromosomes.PRIMARY_COLOR);
            pigmentSlot.createDisplayOverrides().addItemStack(
                    BotanyPigments.stackFor(color.getPath(), BotanyContent.PIGMENT.get()));
        });
    }

    private static IRecipeSlotDrawable named(List<IRecipeSlotDrawable> slots, String name) {
        return slots.stream().filter(slot -> slot.getSlotName().filter(name::equals).isPresent())
                .findFirst().orElse(null);
    }
}
