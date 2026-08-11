package binnie.core.client;

import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.genetics.FlowerSpeciesTypes;
import binnie.core.menu.FieldKitMenu;
import binnie.genetics.api.GeneticOrganismAdapters;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.core.genetics.alleles.ButterflyChromosomes;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Current rendering of the active 1.12 parchment Field Kit window. */
public final class FieldKitScreen extends AbstractContainerScreen<FieldKitMenu> {
    public static final int WIDTH = 280;
    public static final int HEIGHT = 230;
    public static final int GLASS_X = 16;
    public static final int GLASS_Y = 32;
    public static final int CHROMOSOME_X = 150;
    public static final int CHROMOSOME_Y = 24;
    public static final int HELP_TOOLTIP_WIDTH = 140;
    public static final int INFO_TOOLTIP_WIDTH = 200;

    private static final ResourceLocation PUNNETT = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/punnett.png");
    private static final ResourceLocation BREEDING = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/breeding.png");
    private static final int TEXT_COLOR = 0x222222;
    private static final RandomSource RANDOM = RandomSource.create();

    private float glassOffsetX;
    private float glassOffsetY;
    private float glassVelocityX;
    private float glassVelocityY;
    private int previousProgress;

    public FieldKitScreen(FieldKitMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = -10_000;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int progress = menu.analysisProgress();
        if (progress > 0) {
            if (previousProgress == 0) {
                glassOffsetX = 0;
                glassOffsetY = 0;
                glassVelocityX = 0;
                glassVelocityY = 0;
            }
            float fraction = progress / (float) FieldKitMenu.ANALYSIS_TICKS;
            // Keep WindowFieldKit's original hand-lens movement, including its
            // nervous pull toward the upper-left during analysis.
            glassVelocityX += RANDOM.nextFloat() - 2.5F - glassOffsetX;
            glassVelocityY += RANDOM.nextFloat() - 2.5F - glassOffsetY;
            glassOffsetX = (glassOffsetX + glassVelocityX) * (1.0F - fraction);
            glassOffsetY = (glassOffsetY + glassVelocityY) * (1.0F - fraction);
        } else {
            glassOffsetX = 0;
            glassOffsetY = 0;
            glassVelocityX = 0;
            glassVelocityY = 0;
        }
        previousProgress = progress;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawParchmentWindow(graphics, leftPos, topPos, imageWidth, imageHeight);

        int glassX = leftPos + GLASS_X + Math.round(glassOffsetX);
        int glassY = topPos + GLASS_Y + Math.round(glassOffsetY);
        graphics.blit(PUNNETT, glassX, glassY, 0, 160, 96, 96, 256, 256);

        IIndividual individual = individual();
        FieldKitLayout layout = individual != null && individual.isAnalyzed() ? layout(individual.getType().id()) : null;
        if (layout != null) {
            graphics.blit(BREEDING, leftPos + CHROMOSOME_X, topPos + CHROMOSOME_Y,
                    layout.textureU(), layout.textureV(), 96, 96, 256, 256);
            for (Picker picker : layout.pickers()) {
                boolean hovered = inside(mouseX, mouseY, CHROMOSOME_X + picker.x(), CHROMOSOME_Y + picker.y(), 16, 16);
                graphics.blit(PUNNETT, leftPos + CHROMOSOME_X + picker.x(), topPos + CHROMOSOME_Y + picker.y(),
                        160, hovered ? 18 : 34, 16, 16, 256, 256);
            }
        }

        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                graphics.blit(PUNNETT, leftPos + slot.x - 1, topPos + slot.y - 1,
                        160, 0, 18, 18, 256, 256);
            }
        }

        graphics.blit(PUNNETT, leftPos + 8, topPos + 8, 178, 0, 16, 16, 256, 256);
        graphics.blit(PUNNETT, leftPos + 30, topPos + 8, 178, 16, 16, 16, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 12, 0x404040, false);
        graphics.drawString(font, Component.translatable("screen.binniecore.field_kit.paper_label"), 232, 13, TEXT_COLOR, false);

        ItemStack input = menu.kitInventory().getItem(FieldKitMenu.INDIVIDUAL_SLOT);
        if (!input.isEmpty() && !isAnalyzed(input) && menu.paperRemaining() == 0) {
            Component noPaper = Component.translatable("screen.binniecore.field_kit.no_paper_legacy");
            graphics.drawString(font, noPaper, (imageWidth - font.width(noPaper)) / 2, 127, TEXT_COLOR, false);
            return;
        }

        IIndividual individual = individual();
        if (individual == null || !individual.isAnalyzed()) return;
    // AbstractContainerScreen keeps the foreground pose translated, but passes
    // global mouse coordinates to renderLabels. Do not offset them a second time.
        Picker picker = hoveredPicker(mouseX, mouseY, individual.getType().id());
        if (picker == null) return;
        AllelePair<?> pair = individual.getGenome().getAllelePair(picker.chromosome());
        Component value = alleleName(picker.chromosome(), pair.active().value());
        graphics.drawString(font, value, (imageWidth - font.width(value)) / 2, 127, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        IIndividual individual = individual();
        Picker picker = individual != null && individual.isAnalyzed()
                ? hoveredPicker(mouseX, mouseY, individual.getType().id()) : null;
        if (picker != null) {
            graphics.renderTooltip(font, Component.translatable(picker.chromosome().chromosomeTranslationKey()), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 8, 8, 16, 16)) {
            graphics.renderTooltip(font, wrappedTooltip(HELP_TOOLTIP_WIDTH,
                    Component.translatable("gui.binniecore.help.title"),
                    Component.translatable("gui.binniecore.help.desc", "Tab")
            ), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 30, 8, 16, 16)) {
            graphics.renderTooltip(font, wrappedTooltip(INFO_TOOLTIP_WIDTH,
                    Component.translatable("gui.binniecore.info.title"),
                    Component.translatable("tooltip.binniecore.field_kit.description")
            ), mouseX, mouseY);
        } else if (isTabDown() && hoveredSlot != null && !hoveredSlot.hasItem()) {
            Component accepted = hoveredSlot.index == FieldKitMenu.INDIVIDUAL_SLOT
                    ? Component.translatable("screen.binniecore.field_kit.individual")
                    : hoveredSlot.index == FieldKitMenu.PAPER_SLOT
                    ? Component.translatable("screen.binniecore.field_kit.paper_label")
                    : Component.translatable("screen.binniecore.field_kit.player_slot");
            graphics.renderTooltip(font, Component.translatable("screen.binniecore.field_kit.accepts", accepted), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    /** Keeps the 1.12 controls' 140px help and 200px information widths. */
    private List<FormattedCharSequence> wrappedTooltip(int width, Component title, Component description) {
        ArrayList<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(font.split(title, width));
        lines.addAll(font.split(description, width));
        return List.copyOf(lines);
    }

    private IIndividual individual() {
        ItemStack input = menu.kitInventory().getItem(FieldKitMenu.INDIVIDUAL_SLOT);
        if (!GeneticOrganismAdapters.supports(input)) return null;
        try {
            return GeneticOrganismAdapters.forStack(input).read(input);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return null;
        }
    }

    private static boolean isAnalyzed(ItemStack stack) {
        try {
            return GeneticOrganismAdapters.forStack(stack).read(stack).isAnalyzed();
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return false;
        }
    }

    private Picker hoveredPicker(int mouseX, int mouseY, ResourceLocation type) {
        FieldKitLayout layout = layout(type);
        if (layout == null) return null;
        for (Picker picker : layout.pickers()) {
            if (inside(mouseX, mouseY, CHROMOSOME_X + picker.x(), CHROMOSOME_Y + picker.y(), 16, 16)) return picker;
        }
        return null;
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= x && localX < x + width && localY >= y && localY < y + height;
    }

    private static boolean isTabDown() {
        long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_TAB) == GLFW.GLFW_PRESS;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Component alleleName(IChromosome chromosome, Object value) {
        return Component.translatableWithFallback(chromosome.translationKey(value), String.valueOf(value));
    }

    private static void drawParchmentWindow(GuiGraphics graphics, int x, int y, int width, int height) {
        final int edge = 32;
        graphics.blit(PUNNETT, x, y, 0, 0, edge, edge, 256, 256);
        graphics.blit(PUNNETT, x + width - edge, y, 128, 0, edge, edge, 256, 256);
        graphics.blit(PUNNETT, x, y + height - edge, 0, 128, edge, edge, 256, 256);
        graphics.blit(PUNNETT, x + width - edge, y + height - edge, 128, 128, edge, edge, 256, 256);
        for (int xx = edge; xx < width - edge; xx += 96) {
            int tileWidth = Math.min(96, width - edge - xx);
            graphics.blit(PUNNETT, x + xx, y, 32, 0, tileWidth, edge, 256, 256);
            graphics.blit(PUNNETT, x + xx, y + height - edge, 32, 128, tileWidth, edge, 256, 256);
            for (int yy = edge; yy < height - edge; yy += 96) {
                int tileHeight = Math.min(96, height - edge - yy);
                graphics.blit(PUNNETT, x + xx, y + yy, 32, 32, tileWidth, tileHeight, 256, 256);
            }
        }
        for (int yy = edge; yy < height - edge; yy += 96) {
            int tileHeight = Math.min(96, height - edge - yy);
            graphics.blit(PUNNETT, x, y + yy, 0, 32, edge, tileHeight, 256, 256);
            graphics.blit(PUNNETT, x + width - edge, y + yy, 128, 32, edge, tileHeight, 256, 256);
        }
    }

    private static FieldKitLayout layout(ResourceLocation type) {
        if (ForestrySpeciesTypes.BEE.equals(type)) return BEE;
        if (ForestrySpeciesTypes.TREE.equals(type)) return TREE;
        if (ForestrySpeciesTypes.BUTTERFLY.equals(type)) return BUTTERFLY;
        if (FlowerSpeciesTypes.FLOWER.equals(type)) return FLOWER;
        return null;
    }

    private record Picker(IChromosome<?> chromosome, int x, int y) {}
    private record FieldKitLayout(int textureU, int textureV, List<Picker> pickers) {}

    private static Picker at(IChromosome<?> chromosome, int x, int y) { return new Picker(chromosome, x, y); }

    private static final FieldKitLayout BEE = new FieldKitLayout(0, 0, List.of(
            at(BeeChromosomes.SPECIES, 28, 47), at(BeeChromosomes.SPEED, 28, 72),
            at(BeeChromosomes.LIFESPAN, 19, 20), at(BeeChromosomes.FERTILITY, 55, 65),
            at(BeeChromosomes.TEMPERATURE_TOLERANCE, 28, 1), at(BeeChromosomes.ACTIVITY, 61, 37),
            at(BeeChromosomes.HUMIDITY_TOLERANCE, 81, 76), at(BeeChromosomes.TOLERATES_RAIN, 44, 21),
            at(BeeChromosomes.CAVE_DWELLING, 3, 37), at(BeeChromosomes.FLOWER_TYPE, 4, 65),
            at(BeeChromosomes.POLLINATION, 83, 27), at(BeeChromosomes.TERRITORY, 71, 10),
            at(BeeChromosomes.EFFECT, 84, 54)
    ));
    private static final FieldKitLayout TREE = new FieldKitLayout(96, 0, List.of(
            at(TreeChromosomes.SPECIES, 48, 48), at(TreeChromosomes.HEIGHT, 43, 84),
            at(TreeChromosomes.SAPLINGS, 25, 63), at(TreeChromosomes.FRUIT, 72, 57),
            at(TreeChromosomes.YIELD, 21, 43), at(TreeChromosomes.SAPPINESS, 15, 17),
            at(TreeChromosomes.EFFECT, 67, 15), at(TreeChromosomes.MATURATION, 70, 34),
            at(TreeChromosomes.GIRTH, 45, 67), at(TreeChromosomes.FIREPROOF, 5, 70)
    ));
    private static final FieldKitLayout BUTTERFLY = new FieldKitLayout(96, 96, List.of(
            at(ButterflyChromosomes.SPECIES, 40, 40), at(ButterflyChromosomes.SIZE, 63, 32),
            at(ButterflyChromosomes.SPEED, 32, 63), at(ButterflyChromosomes.LIFESPAN, 11, 27),
            at(ButterflyChromosomes.METABOLISM, 16, 12), at(ButterflyChromosomes.FERTILITY, 17, 63),
            at(ButterflyChromosomes.TEMPERATURE_TOLERANCE, 34, 12), at(ButterflyChromosomes.HUMIDITY_TOLERANCE, 22, 46),
            at(ButterflyChromosomes.NEVER_SLEEPS, 53, 26), at(ButterflyChromosomes.TOLERATES_RAIN, 71, 53),
            at(ButterflyChromosomes.FIREPROOF, 78, 12), at(ButterflyChromosomes.FLOWER_TYPE, 55, 55),
            at(ButterflyChromosomes.EFFECT, 27, 31), at(ButterflyChromosomes.COCOON, 87, 45)
    ));
    private static final FieldKitLayout FLOWER = new FieldKitLayout(0, 96, List.of(
            at(FlowerChromosomes.SPECIES, 35, 81), at(FlowerChromosomes.PRIMARY_COLOR, 36, 28),
            at(FlowerChromosomes.SECONDARY_COLOR, 27, 13), at(FlowerChromosomes.FERTILITY, 47, 15),
            at(FlowerChromosomes.TERRITORY, 54, 31), at(FlowerChromosomes.EFFECT, 15, 55),
            at(FlowerChromosomes.LIFESPAN, 23, 38), at(FlowerChromosomes.TEMPERATURE_TOLERANCE, 17, 77),
            at(FlowerChromosomes.HUMIDITY_TOLERANCE, 52, 51), at(FlowerChromosomes.PH_TOLERANCE, 54, 80),
            at(FlowerChromosomes.SAPPINESS, 41, 42), at(FlowerChromosomes.STEM, 37, 63)
    ));
}
