package binnie.genetics.client;

import binnie.genetics.api.GeneticOperations;
import binnie.core.client.LegacyParchmentGui;
import binnie.genetics.menu.PunnettSquareMenu;
import binnie.genetics.network.SelectPunnettChromosomePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import forestry.api.core.genetics.alleles.AllelePair;

/** Functional chromosome selector and probability display for two genetic parents. */
public final class PunnettSquareScreen extends AbstractContainerScreen<PunnettSquareMenu> {
    private static final int TABLE_X = 76;
    private static final int TABLE_Y = 48;
    private static final int ROW_HEADER_WIDTH = 95;
    private static final int OUTCOME_WIDTH = 103;
    private static final int ROW_HEIGHT = 34;
    /** Chromosome selector is centered above the middle of the table's three columns. */
    private static final int HEADER_CENTER = TABLE_X + ROW_HEADER_WIDTH + OUTCOME_WIDTH / 2;
    public PunnettSquareScreen(PunnettSquareMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 380;
        imageHeight = 225;
        inventoryLabelX = 109;
        inventoryLabelY = 132;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        LegacyParchmentGui.parchment(graphics, leftPos, topPos, imageWidth, imageHeight);
        LegacyParchmentGui.slot(graphics, leftPos + 30, topPos + 34);
        LegacyParchmentGui.slot(graphics, leftPos + 30, topPos + 82);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            LegacyParchmentGui.slot(graphics, leftPos + 109 + column * 18, topPos + 137 + row * 18);
        for (int column = 0; column < 9; column++) LegacyParchmentGui.slot(graphics, leftPos + 109 + column * 18, topPos + 195);
        graphics.fill(leftPos + TABLE_X, topPos + TABLE_Y, leftPos + 377, topPos + TABLE_Y + 1, 0xffaaa07e);
        graphics.fill(leftPos + TABLE_X + ROW_HEADER_WIDTH, topPos + TABLE_Y,
                leftPos + TABLE_X + ROW_HEADER_WIDTH + 1, topPos + TABLE_Y + ROW_HEIGHT * 2, 0xffaaa07e);
        graphics.fill(leftPos + TABLE_X + ROW_HEADER_WIDTH + OUTCOME_WIDTH, topPos + TABLE_Y,
                leftPos + TABLE_X + ROW_HEADER_WIDTH + OUTCOME_WIDTH + 1, topPos + TABLE_Y + ROW_HEIGHT * 2, 0xffaaa07e);
        graphics.fill(leftPos + TABLE_X, topPos + TABLE_Y + ROW_HEIGHT,
                leftPos + 377, topPos + TABLE_Y + ROW_HEIGHT + 1, 0xffaaa07e);
        for (int column = 0; column < 2; column++) for (int row = 0; row < 2; row++)
            graphics.fill(leftPos + TABLE_X + ROW_HEADER_WIDTH + 1 + column * OUTCOME_WIDTH,
                    topPos + TABLE_Y + 1 + row * ROW_HEIGHT,
                    leftPos + TABLE_X + ROW_HEADER_WIDTH + (column + 1) * OUTCOME_WIDTH,
                    topPos + TABLE_Y + (row + 1) * ROW_HEIGHT, 0x22a88d58);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        centered(graphics, title, imageWidth / 2, 10, 0x4c4638);
        ResourceLocation chromosome = menu.selectedChromosomeId();
        Component chromosomeName = chromosome == null
                ? Component.translatable("screen.binniegenetics.punnett.insert_parents")
                : Component.literal(pretty(chromosome.getPath()));
        Component fittedChromosome = fit(chromosomeName, 170);
        centered(graphics, fittedChromosome, HEADER_CENTER, 30, 0x514a3c);
        int selected = menu.selectedChromosomeIndex();
        int count = menu.chromosomeIds().size();
        int leftArrow = selectorLeft(fittedChromosome);
        int rightArrow = selectorRight(fittedChromosome);
        if (selected > 0) centered(graphics, Component.literal("‹"), leftArrow, 30, 0x514a3c);
        if (selected + 1 < count) centered(graphics, Component.literal("›"), rightArrow, 30, 0x514a3c);
        AllelePair<?> first = menu.parentPair(0);
        AllelePair<?> second = menu.parentPair(1);
        if (first != null && second != null) {
            centered(graphics, fit(valueName(first.active().value()), OUTCOME_WIDTH - 6),
                    TABLE_X + ROW_HEADER_WIDTH + OUTCOME_WIDTH / 2, 40, 0x514a3c);
            centered(graphics, fit(valueName(first.inactive().value()), OUTCOME_WIDTH - 6),
                    TABLE_X + ROW_HEADER_WIDTH + OUTCOME_WIDTH + OUTCOME_WIDTH / 2, 40, 0x514a3c);
            centered(graphics, fit(valueName(second.active().value()), ROW_HEADER_WIDTH - 6),
                    TABLE_X + ROW_HEADER_WIDTH / 2, TABLE_Y + 12, 0x514a3c);
            centered(graphics, fit(valueName(second.inactive().value()), ROW_HEADER_WIDTH - 6),
                    TABLE_X + ROW_HEADER_WIDTH / 2, TABLE_Y + ROW_HEIGHT + 12, 0x514a3c);
        }
        List<GeneticOperations.PunnettOutcome> outcomes = menu.outcomes();
        for (int index = 0; index < Math.min(4, outcomes.size()); index++) {
            GeneticOperations.PunnettOutcome outcome = outcomes.get(index);
            int x = TABLE_X + ROW_HEADER_WIDTH + (index % 2) * OUTCOME_WIDTH;
            int y = TABLE_Y + (index / 2) * ROW_HEIGHT;
            centered(graphics, fit(valueName(outcome.pair().active().value()), OUTCOME_WIDTH - 6), x + OUTCOME_WIDTH / 2, y + 2, 0x514a3c);
            centered(graphics, fit(valueName(outcome.pair().inactive().value()), OUTCOME_WIDTH - 6), x + OUTCOME_WIDTH / 2, y + 12, 0x746b57);
            centered(graphics, Component.literal(Math.round(outcome.probability() * 100) + "%"),
                    x + OUTCOME_WIDTH / 2, y + 23, 0x746b57);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= topPos + 24 && mouseY < topPos + 44) {
            Component label = menu.selectedChromosomeId() == null ? Component.empty()
                    : fit(Component.literal(pretty(menu.selectedChromosomeId().getPath())), 170);
            int leftArrow = selectorLeft(label);
            int rightArrow = selectorRight(label);
            if (menu.selectedChromosomeIndex() > 0
                    && mouseX >= leftPos + leftArrow - 12 && mouseX < leftPos + leftArrow + 12) {
                request(menu.selectedChromosomeIndex() - 1);
                return true;
            }
            if (menu.selectedChromosomeIndex() + 1 < menu.chromosomeIds().size()
                    && mouseX >= leftPos + rightArrow - 12 && mouseX < leftPos + rightArrow + 12) {
                request(menu.selectedChromosomeIndex() + 1);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static String pretty(String value) {
        int separator = Math.max(value.lastIndexOf(':'), value.lastIndexOf('.'));
        if (separator >= 0) value = value.substring(separator + 1);
        String[] words = value.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) if (!word.isEmpty()) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private Component fit(Component value, int width) {
        if (font.width(value) <= width) return value;
        int available = Math.max(0, width - font.width("…"));
        return Component.literal(font.plainSubstrByWidth(value.getString(), available) + "…");
    }

    private void centered(GuiGraphics graphics, Component value, int centerX, int y, int color) {
        graphics.drawString(font, value, centerX - font.width(value) / 2, y, color, false);
    }

    private int selectorLeft(Component label) { return HEADER_CENTER - font.width(label) / 2 - 14; }
    private int selectorRight(Component label) { return HEADER_CENTER + font.width(label) / 2 + 14; }

    private Component valueName(Object value) {
        ResourceLocation chromosomeId = menu.selectedChromosomeId();
        if (value instanceof Vec3i vector)
            return Component.literal(vector.getX() + " × " + vector.getY() + " × " + vector.getZ());
        if (value instanceof Float number && chromosomeId != null && "sappiness".equals(chromosomeId.getPath()))
            return Component.literal(Math.round(number * 100.0F) + "%");
        if (value instanceof Enum<?> enumeration) return Component.literal(pretty(enumeration.name()));
        if (chromosomeId != null) {
            var chromosome = binnie.genetics.api.GeneticOrganismAdapters.forStack(menu.parents().getItem(0)).chromosomes()
                    .stream().filter(candidate -> candidate.id().equals(chromosomeId)).findFirst().orElse(null);
            if (chromosome != null) return translated(chromosome, value);
        }
        return Component.literal(pretty(String.valueOf(value)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Component translated(forestry.api.core.genetics.alleles.IChromosome chromosome, Object value) {
        return Component.translatableWithFallback(chromosome.translationKey(value), pretty(String.valueOf(value)));
    }

    private void request(int index) {
        PacketDistributor.sendToServer(new SelectPunnettChromosomePayload(menu.containerId, index));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
