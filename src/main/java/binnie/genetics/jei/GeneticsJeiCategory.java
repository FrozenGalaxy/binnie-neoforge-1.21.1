package binnie.genetics.jei;

import binnie.genetics.registry.GeneticsMachines;
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

import java.util.List;
import java.text.NumberFormat;

/** Per-machine layouts reconstructed from the active Binnie's Genetics 1.12 JEI categories. */
public final class GeneticsJeiCategory implements IRecipeCategory<GeneticsJeiRecipe> {
    private static final ResourceLocation CORE_PANELS =
            ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-panels.png");
    private static final ResourceLocation CORE_SLOTS =
            ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-slots.png");

    private final String machine;
    private final RecipeType<GeneticsJeiRecipe> type;
    private final IDrawable icon;
    private final IDrawable tank;
    private final IDrawable tankOverlay;
    private final IDrawableAnimated arrowAnimated;

    public GeneticsJeiCategory(String machine, RecipeType<GeneticsJeiRecipe> type, IGuiHelper gui) {
        this.machine = machine;
        this.type = type;
        this.icon = gui.createDrawableItemLike(GeneticsMachines.REGISTRY.block(machine).get());
        this.tank = gui.createDrawable(CORE_SLOTS, 8, 28, 18, 60);
        this.tankOverlay = gui.createDrawable(CORE_SLOTS, 33, 29, 16, 58);
        IDrawableStatic arrowFill = gui.createDrawable(CORE_PANELS, 207, 79, 14, 10);
        this.arrowAnimated = gui.createAnimatedDrawable(arrowFill, 56,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<GeneticsJeiRecipe> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.translatable("jei.binniegenetics." + machine); }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public int getWidth() {
        return switch (machine) {
            case "sequencer" -> 72;
            case "incubator" -> 112;
            case "genepool" -> 114;
            case "analyser", "acclimatiser" -> 92;
            default -> 130;
        };
    }

    @Override
    public int getHeight() {
        return switch (machine) {
            case "sequencer", "analyser", "acclimatiser" -> 41;
            default -> 60;
        };
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GeneticsJeiRecipe recipe, IFocusGroup focuses) {
        switch (machine) {
            case "isolator" -> isolator(builder, recipe);
            case "sequencer" -> sequencer(builder, recipe);
            case "polymeriser" -> polymeriser(builder, recipe);
            case "inoculator" -> inoculator(builder, recipe, true);
            case "splicer" -> inoculator(builder, recipe, false);
            case "incubator" -> incubator(builder, recipe);
            case "genepool" -> genepool(builder, recipe);
            case "analyser" -> analyser(builder, recipe);
            case "acclimatiser" -> acclimatiser(builder, recipe);
            default -> throw new IllegalStateException("Unknown Genetics JEI machine " + machine);
        }
    }

    private void isolator(IRecipeLayoutBuilder b, GeneticsJeiRecipe r) {
        fluid(b, r.fluidInputs(), 0, 1, 1, true, 100);
        item(b, r.inputs(), r.inputAlternatives(), 0, 22, 0, true);
        item(b, r.catalysts(), List.of(), 0, 42, 21, true);
        item(b, r.reagents(), List.of(), 0, 22, 42, true);
        item(b, r.outputs(), r.outputAlternatives(), 0, 92, 21, false);
    }

    private void sequencer(IRecipeLayoutBuilder b, GeneticsJeiRecipe r) {
        item(b, r.inputs(), r.inputAlternatives(), 0, 0, 0, true);
        item(b, r.reagents(), List.of(), 0, 0, 21, true);
        item(b, r.catalysts(), List.of(), 0, 52, 0, true);
        item(b, r.outputs(), r.outputAlternatives(), 0, 52, 21, false);
    }

    private void polymeriser(IRecipeLayoutBuilder b, GeneticsJeiRecipe r) {
        fluid(b, r.fluidInputs(), 0, 1, 1, true, 10_000);
        fluid(b, r.fluidInputs(), 1, 21, 1, true, 10_000);
        item(b, r.inputs(), r.inputAlternatives(), 0, 42, 21, true);
        var accelerator = item(b, r.catalysts(), List.of(), 0, 42, 42, true);
        if (accelerator != null) accelerator.addRichTooltipCallback((view, tooltip) ->
                tooltip.add(Component.translatable("jei.binniegenetics.polymeriser.accelerator")));
        item(b, r.outputs(), r.outputAlternatives(), 0, 92, 21, false);
    }

    private void inoculator(IRecipeLayoutBuilder b, GeneticsJeiRecipe r, boolean vectorTank) {
        if (vectorTank) fluid(b, r.fluidInputs(), 0, 1, 1, true, 100);
        item(b, r.catalysts(), List.of(), 0, 22, vectorTank ? 2 : 0, true);
        item(b, r.inputs(), r.inputAlternatives(), 0, 42, 21, true);
        item(b, r.outputs(), r.outputAlternatives(), 0, 92, 21, false);
    }

    private void incubator(IRecipeLayoutBuilder b, GeneticsJeiRecipe r) {
        fluid(b, r.fluidInputs(), 0, 1, 1, true, 50);
        fluid(b, r.fluidOutputs(), 0, 95, 1, false, 50);
        item(b, r.reagents(), List.of(), 0, 22, 22, true);
        item(b, r.outputs(), r.outputAlternatives(), 0, 72, 22, false);
    }

    private void genepool(IRecipeLayoutBuilder b, GeneticsJeiRecipe r) {
        fluid(b, r.fluidInputs(), 0, 1, 1, true, 100);
        fluid(b, r.fluidOutputs(), 0, 96, 1, false, 100);
        item(b, r.reagents(), List.of(), 0, 22, 2, true);
        item(b, r.inputs(), r.inputAlternatives(), 0, 42, 41, true);
    }

    private void analyser(IRecipeLayoutBuilder b, GeneticsJeiRecipe r) {
        item(b, r.inputs(), r.inputAlternatives(), 0, 0, 10, true);
        item(b, r.reagents(), List.of(), 0, 26, 10, true);
        item(b, r.outputs(), r.outputAlternatives(), 0, 72, 10, false);
    }

    private void acclimatiser(IRecipeLayoutBuilder b, GeneticsJeiRecipe r) {
        item(b, r.inputs(), r.inputAlternatives(), 0, 0, 10, true);
        item(b, r.reagents(), List.of(), 0, 26, 10, true);
        item(b, r.outputs(), r.outputAlternatives(), 0, 72, 10, false);
    }

    @Override
    public void draw(GeneticsJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        switch (machine) {
            case "sequencer" -> arrow(graphics, 28, 4);
            case "incubator" -> arrow(graphics, 49, 26);
            case "genepool" -> {
                arrow(graphics, 69, 45);
                graphics.drawString(Minecraft.getInstance().font, Component.literal("0.25"), 22, 22, 0x808080, false);
            }
            case "analyser", "acclimatiser" -> arrow(graphics, 50, 14);
            default -> arrow(graphics, 69, 25);
        }
        if (machine.equals("isolator")) {
            centered(graphics, Component.translatable("jei.binniegenetics.isolator.random"), 82, 122, 45);
            graphics.drawString(Minecraft.getInstance().font,
                    Component.translatable("jei.binniegenetics.loss", 5), 42, 0, 0x808080, false);
        } else if (machine.equals("incubator")) {
            int loss = incubatorLoss(recipe);
            if (loss > 0) {
                graphics.drawString(Minecraft.getInstance().font,
                        Component.translatable("jei.binniegenetics.incubator.item"), 24, 0, 0x808080, false);
                graphics.drawString(Minecraft.getInstance().font,
                        Component.translatable("jei.binniegenetics.loss", loss), 24, 10, 0x808080, false);
            }
        } else if (machine.equals("inoculator") || machine.equals("splicer")) {
            Component power = Component.literal(NumberFormat.getIntegerInstance().format(
                    (long) recipe.duration() * recipe.energyPerTick()) + " FE");
            centered(graphics, power, 42, getWidth(), 5);
            centered(graphics, Component.literal(formatDuration(recipe.duration())), 42, getWidth(), 45);
        }
    }

    private void arrow(GuiGraphics graphics, int x, int y) {
        graphics.blit(CORE_PANELS, x, y, 191, 79, 14, 10, 256, 256);
        arrowAnimated.draw(graphics, x, y);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, GeneticsJeiRecipe recipe, IRecipeSlotsView slots,
                           double mouseX, double mouseY) {
        int[] arrow = switch (machine) {
            case "sequencer" -> new int[]{28, 4};
            case "incubator" -> new int[]{49, 26};
            case "genepool" -> new int[]{69, 45};
            case "analyser", "acclimatiser" -> new int[]{50, 14};
            default -> new int[]{69, 25};
        };
        if (mouseX >= arrow[0] && mouseX < arrow[0] + 14 && mouseY >= arrow[1] && mouseY < arrow[1] + 10) {
            tooltip.add(recipe.operationTooltip());
        }
        if (machine.equals("genepool") && mouseX >= 22 && mouseX < 46 && mouseY >= 21 && mouseY < 33) {
            tooltip.add(Component.translatable("jei.binniegenetics.genepool.enzyme_use"));
            tooltip.add(Component.translatable("jei.binniegenetics.genepool.enzyme_use_detail"));
        }
    }

    @Override public ResourceLocation getRegistryName(GeneticsJeiRecipe recipe) { return recipe.id(); }

    private static int incubatorLoss(GeneticsJeiRecipe recipe) {
        return switch (recipe.id().getPath().substring(recipe.id().getPath().lastIndexOf('/') + 1)) {
            case "growth_medium", "bacteria" -> 20;
            case "bacteria_growth", "polymerising_growth", "vector_growth", "vector" -> 5;
            case "enzyme" -> 50;
            case "polymerising" -> 10;
            default -> 0;
        };
    }

    private static String formatDuration(int ticks) {
        int seconds = Math.max(0, ticks / 20);
        int minutes = seconds / 60;
        seconds %= 60;
        if (minutes == 0) return seconds + (seconds == 1 ? " second" : " seconds");
        if (seconds == 0) return minutes + (minutes == 1 ? " minute" : " minutes");
        return minutes + " min " + seconds + " sec";
    }

    private mezz.jei.api.gui.builder.IRecipeSlotBuilder item(IRecipeLayoutBuilder builder, List<ItemStack> stacks,
                                                              List<ItemStack> alternatives, int index,
                                                              int x, int y, boolean input) {
        if (index >= stacks.size() || stacks.get(index).isEmpty()) return null;
        var slot = (input ? builder.addInputSlot(x, y) : builder.addOutputSlot(x, y)).setStandardSlotBackground();
        if (index == 0 && !alternatives.isEmpty()) slot.addItemStacks(alternatives);
        else slot.addItemStack(stacks.get(index));
        return slot;
    }

    private void fluid(IRecipeLayoutBuilder builder, List<FluidStack> fluids, int index,
                       int x, int y, boolean input, int capacity) {
        if (index >= fluids.size() || fluids.get(index).isEmpty()) return;
        FluidStack stack = fluids.get(index);
        (input ? builder.addInputSlot(x + 1, y + 1) : builder.addOutputSlot(x + 1, y + 1))
                .setBackground(tank, -1, -1)
                .setFluidRenderer(Math.max(capacity, stack.getAmount()), false, 16, 58)
                .setOverlay(tankOverlay, 0, 0)
                .addFluidStack(stack.getFluid(), stack.getAmount(), stack.getComponentsPatch());
    }

    private static void centered(GuiGraphics graphics, Component text, int left, int right, int y) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, text, (left + right - font.width(text)) / 2, y, 0x808080, false);
    }
}
