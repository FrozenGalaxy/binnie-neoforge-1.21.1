package binnie.trees.jei;

import binnie.trees.registry.BinnieTreesMachines;
import net.minecraft.core.registries.BuiltInRegistries;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Machine-specific JEI layouts reconstructed from Extra Trees 1.12. */
public final class BinnieTreesJeiCategory implements IRecipeCategory<BinnieTreesJeiRecipe> {
    private static final ResourceLocation CORE_PANELS =
            ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-panels.png");
    private static final ResourceLocation CORE_SLOTS =
            ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-slots.png");
    private static final ResourceLocation EXTRA_TREES_GUI =
            ResourceLocation.fromNamespaceAndPath("binnietrees", "textures/gui/legacy/gui.png");

    private final String machine;
    private final RecipeType<BinnieTreesJeiRecipe> type;
    private final IDrawable icon;
    private final IDrawable tank;
    private final IDrawableStatic arrowFill;
    private final IDrawableAnimated arrowAnimated;

    public BinnieTreesJeiCategory(String machine, RecipeType<BinnieTreesJeiRecipe> type, IGuiHelper gui) {
        this.machine = machine;
        this.type = type;
        this.icon = gui.createDrawableItemLike(machine.equals("bottler")
                ? BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("forestry", "bottler"))
                : BinnieTreesMachines.REGISTRY.block(machine).get());
        this.tank = gui.createDrawable(CORE_SLOTS, 8, 28, 18, 60);
        this.arrowFill = gui.createDrawable(CORE_PANELS, 207, 79, 14, 10);
        this.arrowAnimated = gui.createAnimatedDrawable(arrowFill, 56,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<BinnieTreesJeiRecipe> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.translatable("jei.binnietrees." + machine); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() {
        return switch (machine) {
            case "press" -> 74;
            case "distillery" -> 84;
            case "bottler" -> 84;
            case "brewery", "lumbermill" -> 130;
            default -> 230;
        };
    }
    @Override public int getHeight() { return switch (machine) {
        case "press", "brewery", "distillery", "lumbermill", "bottler" -> 60;
        default -> 70;
    }; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BinnieTreesJeiRecipe recipe, IFocusGroup focuses) {
        switch (machine) {
            case "press" -> press(builder, recipe);
            case "brewery" -> brewery(builder, recipe);
            case "distillery" -> distillery(builder, recipe);
            case "lumbermill" -> lumbermill(builder, recipe);
            case "bottler" -> bottler(builder, recipe);
            default -> designer(builder, recipe);
        }
    }

    private void press(IRecipeLayoutBuilder builder, BinnieTreesJeiRecipe recipe) {
        inputIngredient(builder, recipe, 0, 0, 22);
        fluid(builder, recipe.fluidOutputs(), 55, 1, false, 1000);
    }

    private void brewery(IRecipeLayoutBuilder builder, BinnieTreesJeiRecipe recipe) {
        fluid(builder, recipe.fluidInputs(), 1, 1, true, 1000);
        fluid(builder, recipe.fluidOutputs(), 113, 1, false, 1000);
        if (recipe.inputs().size() > 1) item(builder, recipe.inputs(), 1, 21, 42, true);
        // The source category deliberately repeats the selected grain in all
        // three crafting positions to communicate the Brewery's 3-slot grid.
        ItemStack grain = recipe.inputs().isEmpty() ? ItemStack.EMPTY : recipe.inputs().getFirst();
        if (!grain.isEmpty()) for (int y : new int[]{0, 21, 42})
            builder.addInputSlot(42, y).setStandardSlotBackground().addItemStack(grain);
        item(builder, recipe.reagents(), 0, 63, 42, true);
    }

    private void distillery(IRecipeLayoutBuilder builder, BinnieTreesJeiRecipe recipe) {
        fluid(builder, recipe.fluidInputs(), 1, 1, true, 1000);
        fluid(builder, recipe.fluidOutputs(), 65, 1, false, 1000);
    }

    private void lumbermill(IRecipeLayoutBuilder builder, BinnieTreesJeiRecipe recipe) {
        fluid(builder, recipe.fluidInputs(), 1, 1, true, 5000);
        item(builder, recipe.inputs(), 0, 42, 21, true);
        item(builder, recipe.outputs(), 0, 112, 0, false);
        item(builder, recipe.outputs(), 1, 92, 21, false);
        item(builder, recipe.outputs(), 2, 112, 42, false);
    }

    private void bottler(IRecipeLayoutBuilder builder, BinnieTreesJeiRecipe recipe) {
        item(builder, recipe.inputs(), 0, 0, 21, true);
        fluid(builder, recipe.fluidInputs(), 23, 1, true,
                recipe.fluidInputs().isEmpty() ? 1_000 : recipe.fluidInputs().getFirst().getAmount());
        item(builder, recipe.outputs(), 0, 66, 21, false);
    }

    private void designer(IRecipeLayoutBuilder builder, BinnieTreesJeiRecipe recipe) {
        for (int i = 0; i < recipe.inputIngredients().size(); i++) {
            var slot = builder.addInputSlot(4 + i * 21, 5).setStandardSlotBackground()
                    .addIngredients(recipe.inputIngredients().get(i));
            if (i < recipe.inputAlternatives().size()) slot.addItemStacks(recipe.inputAlternatives().get(i));
        }
        item(builder, recipe.reagents(), 0, 46, 5, true);
        if (!recipe.outputAlternatives().isEmpty())
            builder.addOutputSlot(190, 5).setStandardSlotBackground().addItemStacks(recipe.outputAlternatives().getFirst());
    }

    @Override
    public void draw(BinnieTreesJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        switch (machine) {
            case "press" -> arrow(graphics, 27, 26);
            case "brewery" -> arrow(graphics, 90, 25);
            case "lumbermill" -> arrow(graphics, 69, 25);
            case "bottler" -> arrow(graphics, 48, 25);
            case "distillery" -> {
                graphics.blit(EXTRA_TREES_GUI, 0, 0, 43, 0, 58, 60, 256, 256);
                int reveal = (int) ((System.currentTimeMillis() / 40L) % 101L * 38L / 100L);
                if (reveal > 0) graphics.blit(EXTRA_TREES_GUI, 18, 0, 101, 0, reveal, 60, 256, 256);
                graphics.blit(EXTRA_TREES_GUI, 47, 14 + Math.clamp(recipe.mode(), 0, 2) * 15,
                        68, 66, 17, 7, 256, 256);
            }
            default -> {
                if (recipe.duration() > 0) centered(graphics,
                        Component.translatable("gui.binniecore.jei.ticks", recipe.duration()), 68, 186, 9);
            }
        }
    }

    private void arrow(GuiGraphics graphics, int x, int y) {
        graphics.blit(CORE_PANELS, x, y, 191, 79, 14, 10, 256, 256);
        arrowAnimated.draw(graphics, x, y);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, BinnieTreesJeiRecipe recipe, IRecipeSlotsView slots,
                           double mouseX, double mouseY) {
        if (machine.equals("distillery") && mouseX >= 18 && mouseX < 76 && mouseY >= 0 && mouseY < 60) {
            tooltip.add(Component.translatable("gui.binniecore.jei.ticks", recipe.duration()));
            tooltip.add(Component.translatable("gui.binniecore.jei.energy_per_tick", recipe.energyPerTick()));
            tooltip.add(Component.translatable("gui.binniecore.jei.total_energy", recipe.duration() * recipe.energyPerTick()));
        }
    }

    @Override public ResourceLocation getRegistryName(BinnieTreesJeiRecipe recipe) { return recipe.id(); }

    private void inputIngredient(IRecipeLayoutBuilder builder, BinnieTreesJeiRecipe recipe, int index, int x, int y) {
        if (index < recipe.inputIngredients().size()) {
            var slot = builder.addInputSlot(x, y).setStandardSlotBackground()
                    .addIngredients(recipe.inputIngredients().get(index));
            if (index < recipe.inputAlternatives().size()) slot.addItemStacks(recipe.inputAlternatives().get(index));
        } else item(builder, recipe.inputs(), index, x, y, true);
    }

    private static void item(IRecipeLayoutBuilder builder, java.util.List<ItemStack> stacks, int index,
                             int x, int y, boolean input) {
        if (index >= stacks.size() || stacks.get(index).isEmpty()) return;
        (input ? builder.addInputSlot(x, y) : builder.addOutputSlot(x, y))
                .setStandardSlotBackground().addItemStack(stacks.get(index));
    }

    private void fluid(IRecipeLayoutBuilder builder, java.util.List<FluidStack> fluids, int x, int y,
                       boolean input, int capacity) {
        if (fluids.isEmpty() || fluids.getFirst().isEmpty()) return;
        FluidStack stack = fluids.getFirst();
        (input ? builder.addInputSlot(x + 1, y + 1) : builder.addOutputSlot(x + 1, y + 1))
                .setBackground(tank, -1, -1).setFluidRenderer(capacity, false, 16, 58)
                .addFluidStack(stack.getFluid(), stack.getAmount(), stack.getComponentsPatch());
    }

    private static void centered(GuiGraphics graphics, Component text, int left, int right, int y) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, text, (left + right - font.width(text)) / 2, y, 0x555555, false);
    }
}
