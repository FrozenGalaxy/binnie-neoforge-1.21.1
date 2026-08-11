package binnie.bees.jei;

import binnie.bees.block.AlvearyPartType;
import binnie.bees.registry.BinnieBeesWorldContent;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;

public final class AlvearyHatcheryJeiCategory implements IRecipeCategory<AlvearyHatcheryJeiRecipe> {
    private final RecipeType<AlvearyHatcheryJeiRecipe> type;
    private final IDrawable icon;

    public AlvearyHatcheryJeiCategory(RecipeType<AlvearyHatcheryJeiRecipe> type, IGuiHelper gui) {
        this.type = type;
        this.icon = gui.createDrawableItemLike(BinnieBeesWorldContent.alveary(AlvearyPartType.HATCHERY).get());
    }

    @Override public RecipeType<AlvearyHatcheryJeiRecipe> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.translatable("jei.binniebees.alveary_hatchery"); }
    @Override public int getWidth() { return 150; }
    @Override public int getHeight() { return 36; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlvearyHatcheryJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(8, 10).setStandardSlotBackground().addItemStack(recipe.queen());
        builder.addOutputSlot(124, 10).setStandardSlotBackground().addItemStack(recipe.larvae());
    }
    @Override
    public void draw(AlvearyHatcheryJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        Component text = Component.translatable("jei.binniebees.alveary_hatchery.chance", recipe.attemptDenominator());
        graphics.drawString(font, text, 75 - font.width(text) / 2, 13, 0x555555, false);
    }
    @Override public net.minecraft.resources.ResourceLocation getRegistryName(AlvearyHatcheryJeiRecipe recipe) { return recipe.id(); }
}
