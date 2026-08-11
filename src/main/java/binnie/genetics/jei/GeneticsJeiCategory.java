package binnie.genetics.jei;

import binnie.genetics.registry.GeneticsMachines;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;

public final class GeneticsJeiCategory implements IRecipeCategory<GeneticsJeiRecipe> {
    private final String machine;
    private final RecipeType<GeneticsJeiRecipe> type;
    private final IDrawable icon;
    private final IDrawableAnimated processArrow;

    public GeneticsJeiCategory(String machine, RecipeType<GeneticsJeiRecipe> type, IGuiHelper gui) {
        this.machine = machine;
        this.type = type;
        this.icon = gui.createDrawableItemLike(GeneticsMachines.REGISTRY.block(machine).get());
        ResourceLocation squeezer = ResourceLocation.fromNamespaceAndPath("forestry", "textures/gui/squeezersocket.png");
        IDrawableStatic arrow = gui.createDrawable(squeezer, 176, 60, 43, 18);
        this.processArrow = gui.createAnimatedDrawable(arrow, 200, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<GeneticsJeiRecipe> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.binniegenetics." + machine);
    }

    @Override
    public int getWidth() {
        return 230;
    }

    @Override
    public int getHeight() {
        return 70;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GeneticsJeiRecipe recipe, IFocusGroup focuses) {
        addItems(builder, recipe.inputs(), recipe.inputAlternatives(), 4, 5, true);
        addItems(builder, recipe.catalysts(), List.of(), 28, 5, true);
        addItems(builder, recipe.reagents(), List.of(), 52, 5, true);
        addFluids(builder, recipe.fluidInputs(), 4, 45, true);
        addItems(builder, recipe.outputs(), recipe.outputAlternatives(), 190, 5, false);
        addFluids(builder, recipe.fluidOutputs(), 190, 45, false);
    }
    @Override
    public void draw(GeneticsJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        if (recipe.duration() == 1 && recipe.energyPerTick() == 2) {
            centered(graphics, Component.translatable("gui.binniecore.jei.indefinite", recipe.energyPerTick()), 70, 184, 13);
        } else {
            centered(graphics, Component.translatable("gui.binniecore.jei.ticks", recipe.duration()), 70, 184, 8);
            centered(graphics, Component.translatable("gui.binniecore.jei.energy_per_tick", recipe.energyPerTick()), 70, 184, 21);
            centered(graphics, Component.translatable("gui.binniecore.jei.total_energy",
                    recipe.duration() * recipe.energyPerTick()), 70, 184, 34);
        }
        // The Incubator routes fluid-to-fluid conversions across the lower
        // row; other laboratory categories leave the arrow slightly higher
        // beneath their operation statistics.
        this.processArrow.draw(graphics, machine.equals("incubator") ? 100 : 105,
                machine.equals("incubator") ? 46 : 45);
    }
    @Override
    public void getTooltip(ITooltipBuilder tooltip, GeneticsJeiRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseX >= 68 && mouseX < 186 && mouseY >= 4 && mouseY < 66) {
            tooltip.add(recipe.operationTooltip());
        }
    }
    @Override public net.minecraft.resources.ResourceLocation getRegistryName(GeneticsJeiRecipe recipe) { return recipe.id(); }

    private static void addItems(IRecipeLayoutBuilder builder, java.util.List<ItemStack> stacks,
                                 java.util.List<ItemStack> firstSlotAlternatives, int x, int y, boolean input) {
        for (int index = 0; index < stacks.size(); index++) {
            int slotX = x + (index % 2) * 20;
            int slotY = y + (index / 2) * 20;
            var slot = input ? builder.addInputSlot(slotX, slotY) : builder.addOutputSlot(slotX, slotY);
            slot.setStandardSlotBackground();
            if (index == 0 && !firstSlotAlternatives.isEmpty()) slot.addItemStacks(firstSlotAlternatives);
            else slot.addItemStack(stacks.get(index));
        }
    }

    private static void addFluids(IRecipeLayoutBuilder builder, java.util.List<FluidStack> stacks, int x, int y, boolean input) {
        for (int index = 0; index < stacks.size(); index++) {
            FluidStack stack = stacks.get(index);
            int slotX = x + index * 20;
            var slot = input ? builder.addInputSlot(slotX, y) : builder.addOutputSlot(slotX, y);
            slot.setStandardSlotBackground()
                    .setFluidRenderer(Math.max(1, stack.getAmount()), false, 16, 16)
                    .addFluidStack(stack.getFluid(), stack.getAmount(), stack.getComponentsPatch());
        }
    }
    private static void centered(GuiGraphics graphics, Component text, int left, int right, int y) {
        var font = Minecraft.getInstance().font;
        int width = right - left;
        Component fitted = font.width(text) <= width ? text : Component.literal(font.plainSubstrByWidth(text.getString(), width));
        graphics.drawString(font, fitted, (left + right - font.width(fitted)) / 2, y, 0x555555, false);
    }
}
