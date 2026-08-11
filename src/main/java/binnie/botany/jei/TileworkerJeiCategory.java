package binnie.botany.jei;

import binnie.botany.registry.BotanyMachines;
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

public final class TileworkerJeiCategory implements IRecipeCategory<TileworkerJeiRecipe> {
    private final RecipeType<TileworkerJeiRecipe> type;
    private final IDrawable icon;
    public TileworkerJeiCategory(RecipeType<TileworkerJeiRecipe> type, IGuiHelper gui) {
        this.type = type; this.icon = gui.createDrawableItemLike(BotanyMachines.REGISTRY.block("tileworker").get());
    }
    @Override public RecipeType<TileworkerJeiRecipe> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.translatable("jei.binniebotany.tileworker"); }
    @Override public int getWidth() { return 190; }
    @Override public int getHeight() { return 54; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder b, TileworkerJeiRecipe r, IFocusGroup f) {
        b.addInputSlot(5,5).setStandardSlotBackground().addIngredients(r.primary())
                .addItemStacks(r.materialAlternatives());
        b.addInputSlot(27,5).setStandardSlotBackground().addIngredients(r.secondary())
                .addItemStacks(r.materialAlternatives());
        b.addInputSlot(16,31).setStandardSlotBackground().addItemStack(r.mortar());
        b.addOutputSlot(164,16).setStandardSlotBackground().addItemStacks(r.outputAlternatives());
    }
    @Override
    public void draw(TileworkerJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        Component text = Component.translatable("gui.binniecore.jei.manual");
        graphics.drawString(font, text, 111 - font.width(text) / 2, 8, 0x555555, false);
    }
    @Override public net.minecraft.resources.ResourceLocation getRegistryName(TileworkerJeiRecipe recipe) { return recipe.id(); }
}
