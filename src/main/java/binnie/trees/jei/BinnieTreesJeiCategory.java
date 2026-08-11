package binnie.trees.jei;

import binnie.trees.registry.BinnieTreesMachines;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** Compact source-layout category shared by the four timed Binnie's Trees machines. */
public final class BinnieTreesJeiCategory implements IRecipeCategory<BinnieTreesJeiRecipe> {
    private static final ResourceLocation LEGACY_GUI =
            ResourceLocation.fromNamespaceAndPath("binnietrees", "textures/gui/legacy/gui.png");
    private final String machine;
    private final RecipeType<BinnieTreesJeiRecipe> type;
    private final IDrawable icon;
    public BinnieTreesJeiCategory(String machine, RecipeType<BinnieTreesJeiRecipe> type, IGuiHelper gui) {
        this.machine = machine; this.type = type;
        this.icon = gui.createDrawableItemLike(BinnieTreesMachines.REGISTRY.block(machine).get());
    }
    @Override public RecipeType<BinnieTreesJeiRecipe> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.translatable("jei.binnietrees." + machine); }
    @Override public int getWidth() { return 230; }
    @Override public int getHeight() { return 70; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder b, BinnieTreesJeiRecipe r, IFocusGroup f) {
        if (r.inputIngredients().isEmpty()) {
            items(b, r.inputs(), 4, 5, true);
        } else {
            for (int i = 0; i < r.inputIngredients().size(); i++) {
                var slot = b.addInputSlot(4 + i * 21, 5).setStandardSlotBackground()
                        .addIngredients(r.inputIngredients().get(i));
                if (i < r.inputAlternatives().size()) slot.addItemStacks(r.inputAlternatives().get(i));
            }
        }
        items(b, r.catalysts(), 25, 5, true); items(b, r.reagents(), 46, 5, true);
        fluids(b, r.fluidInputs(), 4, 45, true);
        if (r.outputAlternatives().isEmpty()) {
            items(b, r.outputs(), 190, 5, false);
        } else {
            for (int i = 0; i < r.outputAlternatives().size(); i++) {
                b.addOutputSlot(190 + i * 20, 5).setStandardSlotBackground()
                        .addItemStacks(r.outputAlternatives().get(i));
            }
        }
        fluids(b, r.fluidOutputs(), 190, 45, false);
    }
    @Override
    public void draw(BinnieTreesJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        if ("distillery".equals(machine) && recipe.mode() >= 0) {
            drawDistilleryLevel(graphics, recipe.mode());
            return;
        }
        var font = Minecraft.getInstance().font;
        Component first = recipe.duration() > 0
                ? Component.translatable("gui.binniecore.jei.ticks", recipe.duration())
                : Component.translatable("gui.binniecore.jei.manual");
        centered(graphics, first, 70, 184, 8);
        if (recipe.duration() > 0) {
            centered(graphics, Component.translatable("gui.binniecore.jei.energy_per_tick", recipe.energyPerTick()), 70, 184, 21);
            centered(graphics, Component.translatable("gui.binniecore.jei.total_energy",
                    recipe.duration() * recipe.energyPerTick()), 70, 184, 34);
        }
    }
    @Override
    public void getTooltip(ITooltipBuilder tooltip, BinnieTreesJeiRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseX >= 68 && mouseX < 186 && mouseY >= 4 && mouseY < 66 && !recipe.detail().isBlank()) {
            if ("distillery".equals(machine)) {
                tooltip.add(Component.translatable("gui.binniecore.jei.ticks", recipe.duration()));
                tooltip.add(Component.translatable("gui.binniecore.jei.energy_per_tick", recipe.energyPerTick()));
                tooltip.add(Component.translatable("gui.binniecore.jei.total_energy",
                        recipe.duration() * recipe.energyPerTick()));
            }
            for (Component line : wrappedDetail(recipe.detail(), 190)) tooltip.add(line);
        }
    }
    @Override public net.minecraft.resources.ResourceLocation getRegistryName(BinnieTreesJeiRecipe recipe) { return recipe.id(); }
    private static void items(IRecipeLayoutBuilder b, java.util.List<net.minecraft.world.item.ItemStack> v, int x, int y, boolean input) {
        for (int i = 0; i < v.size(); i++) (input ? b.addInputSlot(x + i % 3 * 20, y + i / 3 * 20) : b.addOutputSlot(x + i % 2 * 20, y + i / 2 * 20)).setStandardSlotBackground().addItemStack(v.get(i));
    }
    private static void fluids(IRecipeLayoutBuilder b, java.util.List<FluidStack> v, int x, int y, boolean input) {
        for (int i = 0; i < v.size(); i++) { FluidStack s = v.get(i); (input ? b.addInputSlot(x + i * 20, y) : b.addOutputSlot(x + i * 20, y)).setStandardSlotBackground().setFluidRenderer(Math.max(1, s.getAmount()), false, 16, 16).addFluidStack(s.getFluid(), s.getAmount(), s.getComponentsPatch()); }
    }
    private static void centered(GuiGraphics graphics, Component text, int left, int right, int y) {
        var font = Minecraft.getInstance().font;
        int width = right - left;
        Component fitted = font.width(text) <= width ? text : Component.literal(font.plainSubstrByWidth(text.getString(), width));
        graphics.drawString(font, fitted, (left + right - font.width(fitted)) / 2, y, 0x555555, false);
    }
    private static void drawDistilleryLevel(GuiGraphics graphics, int mode) {
        int x = 98;
        graphics.blit(LEGACY_GUI, x, 2, 43, 0, 58, 66, 256, 256);
        graphics.blit(LEGACY_GUI, x + 47, 16 + Math.clamp(mode, 0, 2) * 15,
                68, 66, 17, 7, 256, 256);
        graphics.blit(LEGACY_GUI, x + 40, 2, 139, 0, 18, 66, 256, 256);
    }
    private static java.util.List<Component> wrappedDetail(String detail, int width) {
        var font = Minecraft.getInstance().font;
        java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : detail.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.width(candidate) > width) {
                lines.add(Component.literal(line.toString()));
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) lines.add(Component.literal(line.toString()));
        return lines;
    }
}
