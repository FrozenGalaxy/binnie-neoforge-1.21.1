package binnie.botany.jei;

import binnie.botany.registry.BotanyContent;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class GardeningJeiCategory implements IRecipeCategory<GardeningJeiRecipe> {
    private final RecipeType<GardeningJeiRecipe> type;
    private final IDrawable icon;

    public GardeningJeiCategory(RecipeType<GardeningJeiRecipe> type, IGuiHelper gui) {
        this.type = type;
        this.icon = gui.createDrawableItemLike(BotanyContent.TROWEL_IRON.get());
    }

    @Override public RecipeType<GardeningJeiRecipe> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.translatable("jei.binniebotany.gardening"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 150; }
    @Override public int getHeight() { return 54; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GardeningJeiRecipe recipe, IFocusGroup focuses) {
        int start = Math.max(2, 48 - recipe.inputs().size() * 10);
        for (int i = 0; i < recipe.inputs().size(); i++) {
            builder.addInputSlot(start + i * 20, 5).setStandardSlotBackground()
                    .addItemStacks(recipe.inputs().get(i));
        }
        builder.addOutputSlot(112, 5).setOutputSlotBackground().addItemStacks(recipe.outputs());
    }

    @Override
    public void draw(GardeningJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        graphics.drawString(Minecraft.getInstance().font, "→", 93, 9, 0x505050, false);
        var font = Minecraft.getInstance().font;
        int y = 29;
        for (var line : font.split(recipe.description(), 146)) {
            graphics.drawString(font, line, 2, y, 0x505050, false);
            y += font.lineHeight;
        }
    }
}
