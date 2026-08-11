package binnie.core.client;

import binnie.core.menu.GenesisMenu;
import binnie.core.network.GenesisActionPayload;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.IChromosome;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** 1.12 two-list Genesis genome editor with current server-authoritative actions. */
public final class GenesisScreen extends AbstractContainerScreen<GenesisMenu> {
    private static final int GENE_X = 32, GENE_Y = 28, GENE_W = 230, GENE_H = 100;
    private static final int OPTION_X = 274, OPTION_Y = 28, OPTION_W = 130, OPTION_H = 100;
    private static final int PICKUP_X = 16, PICKUP_Y = 140, PICKUP_W = 60, PICKUP_H = 42;
    private static final int ANALYZED_X = 16, ANALYZED_Y = 186;
    private static final int PICKUP_SLOT_OFFSET = 3, PICKUP_SLOT_SIZE = 18;
    private static final int ROW_HEIGHT = 20;
    private int geneScroll;
    private int alleleScroll;
    private Checkbox analyzedCheckbox;

    public GenesisScreen(GenesisMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 420;
        imageHeight = 228;
        inventoryLabelX = 129;
        inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        analyzedCheckbox = Checkbox.builder(Component.empty(), font)
                .pos(leftPos + ANALYZED_X, topPos + ANALYZED_Y)
                .selected(menu.analyzed())
                .tooltip(Tooltip.create(Component.translatable("screen.binniecore.genesis.analyzed.tooltip")))
                .onValueChange((checkbox, selected) -> send(GenesisMenu.Action.TOGGLE_ANALYZED, 1))
                .build();
        addRenderableWidget(analyzedCheckbox);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        LegacyParchmentGui.window(graphics, leftPos, topPos, imageWidth, imageHeight);
        LegacyParchmentGui.blackPanel(graphics, leftPos + GENE_X - 1, topPos + GENE_Y - 1, GENE_W + 2, GENE_H + 2);
        LegacyParchmentGui.blackPanel(graphics, leftPos + OPTION_X - 1, topPos + OPTION_Y - 1, OPTION_W + 2, OPTION_H + 2);
        LegacyParchmentGui.blackPanel(graphics, leftPos + PICKUP_X, topPos + PICKUP_Y, PICKUP_W, PICKUP_H);
        renderSystemTabs(graphics, mouseX, mouseY);
        renderGeneRows(graphics, mouseX, mouseY);
        renderAlleleRows(graphics, mouseX, mouseY);
        renderPickup(graphics);
        drawPlayerInventoryChrome(graphics);
    }

    private void renderSystemTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        List<binnie.genetics.api.GeneticOrganismAdapter> adapters = binnie.genetics.api.GeneticOrganismAdapters.all();
        for (int i = 0; i < adapters.size(); i++) {
            int y = topPos + 28 + i * 24;
            LegacyParchmentGui.outline(graphics, leftPos + 7, y, 24, 23);
            ItemStack icon;
            try {
                var type = adapters.get(i).speciesType();
                icon = type.getDefaultSpecies().createStack(type.getDefaultStage());
            } catch (RuntimeException exception) {
                icon = ItemStack.EMPTY;
            }
            if (!icon.isEmpty()) graphics.renderItem(icon, leftPos + 11, y + 3);
            if (i == menu.typeIndex()) graphics.fill(leftPos + 8, y + 1, leftPos + 30, y + 22, 0x665e9b45);
            else if (mouseX >= leftPos + 7 && mouseX < leftPos + 31 && mouseY >= y && mouseY < y + 23)
                graphics.fill(leftPos + 8, y + 1, leftPos + 30, y + 22, 0x334f7f9f);
        }
    }

    private void renderGeneRows(GuiGraphics graphics, int mouseX, int mouseY) {
        List<IChromosome<?>> chromosomes = menu.chromosomes();
        geneScroll = Math.clamp(geneScroll, 0, Math.max(0, chromosomes.size() - 5));
        for (int row = 0; row < Math.min(5, chromosomes.size() - geneScroll); row++) {
            int index = geneScroll + row;
            int y = topPos + GENE_Y + row * ROW_HEIGHT;
            if (index == menu.chromosomeIndex()) graphics.fill(leftPos + GENE_X, y, leftPos + GENE_X + GENE_W, y + ROW_HEIGHT, 0x665e9b45);
            else if (mouseX >= leftPos + GENE_X && mouseX < leftPos + GENE_X + GENE_W
                    && mouseY >= y && mouseY < y + ROW_HEIGHT)
                graphics.fill(leftPos + GENE_X, y, leftPos + GENE_X + GENE_W, y + ROW_HEIGHT, 0x334f7f9f);
            LegacyParchmentGui.chromosome(graphics, leftPos + GENE_X + 3, y + 2, index == menu.chromosomeIndex());
            IChromosome<?> chromosome = chromosomes.get(index);
            AllelePair<?> pair = index == menu.chromosomeIndex() ? menu.selectedPair()
                    : menu.pairFor(chromosome);
            graphics.drawString(font, trim(Component.translatable(chromosome.chromosomeTranslationKey()), 90),
                    leftPos + GENE_X + 23, y + 3, 0xd7d7d7, false);
            graphics.drawString(font, trim(alleleName(chromosome, pair.active()), 105), leftPos + GENE_X + 120, y + 3, 0xf0f0f0, false);
            graphics.drawString(font, trim(alleleName(chromosome, pair.inactive()), 105), leftPos + GENE_X + 120, y + 11, 0x999999, false);
        }
    }

    private void renderAlleleRows(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Allele<?>> alleles = menu.availableAlleles();
        alleleScroll = Math.clamp(alleleScroll, 0, Math.max(0, alleles.size() - 5));
        AllelePair<?> selected = menu.selectedPair();
        for (int row = 0; row < Math.min(5, alleles.size() - alleleScroll); row++) {
            Allele<?> allele = alleles.get(alleleScroll + row);
            int y = topPos + OPTION_Y + row * ROW_HEIGHT;
            boolean active = allele.equals(selected.active());
            boolean inactive = allele.equals(selected.inactive());
            if (active || inactive) graphics.fill(leftPos + OPTION_X, y, leftPos + OPTION_X + OPTION_W, y + ROW_HEIGHT,
                    active ? 0x665e9b45 : 0x66585e9b);
            else if (mouseX >= leftPos + OPTION_X && mouseX < leftPos + OPTION_X + OPTION_W
                    && mouseY >= y && mouseY < y + ROW_HEIGHT)
                graphics.fill(leftPos + OPTION_X, y, leftPos + OPTION_X + OPTION_W, y + ROW_HEIGHT, 0x334f7f9f);
            graphics.drawString(font, trim(alleleName(menu.selectedChromosome(), allele), OPTION_W - 8), leftPos + OPTION_X + 4, y + 6,
                    allele.dominant() ? 0xffffff : 0xb8b8b8, false);
        }
    }

    private void renderPickup(GuiGraphics graphics) {
        List<ILifeStage> stages = menu.stages();
        for (int i = 0; i < Math.min(6, stages.size()); i++) {
            int x = leftPos + PICKUP_X + PICKUP_SLOT_OFFSET + i % 3 * PICKUP_SLOT_SIZE;
            int y = topPos + PICKUP_Y + PICKUP_SLOT_OFFSET + i / 3 * PICKUP_SLOT_SIZE;
            LegacyParchmentGui.slot(graphics, x, y);
            ItemStack preview = menu.preview(stages.get(i));
            if (!preview.isEmpty()) graphics.renderItem(preview, x + 1, y + 1);
        }
    }

    private void drawPlayerInventoryChrome(GuiGraphics graphics) {
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            LegacyParchmentGui.slot(graphics, leftPos + 128 + col * 18, topPos + 139 + row * 18);
        for (int col = 0; col < 9; col++) LegacyParchmentGui.slot(graphics, leftPos + 128 + col * 18, topPos + 197);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawCenteredNoShadow(graphics, title, imageWidth / 2, 9, 0x333333);
        graphics.drawString(font, Component.translatable("screen.binniecore.genesis.analyzed"),
                ANALYZED_X + 24, ANALYZED_Y + 5, 0x333333, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = mouseX - leftPos, y = mouseY - topPos;
        if (x >= 7 && x < 31 && y >= 28) {
            int target = (int) ((y - 28) / 24);
            if (target >= 0 && target < menu.typeCount()) return select(GenesisMenu.Action.TYPE, target);
        }
        if (x >= GENE_X && x < GENE_X + GENE_W && y >= GENE_Y && y < GENE_Y + GENE_H) {
            int target = geneScroll + (int) ((y - GENE_Y) / ROW_HEIGHT);
            if (target < menu.chromosomes().size()) return select(GenesisMenu.Action.CHROMOSOME, target);
        }
        if (x >= OPTION_X && x < OPTION_X + OPTION_W && y >= OPTION_Y && y < OPTION_Y + OPTION_H) {
            int target = alleleScroll + (int) ((y - OPTION_Y) / ROW_HEIGHT);
            List<Allele<?>> values = menu.availableAlleles();
            if (target < values.size()) {
                return select(button == 1 ? GenesisMenu.Action.INACTIVE : GenesisMenu.Action.ACTIVE, target);
            }
        }
        int pickupIndex = pickupIndexAtScreen(mouseX, mouseY);
        if (pickupIndex >= 0) {
            if (pickupIndex < menu.stages().size()) {
                select(GenesisMenu.Action.STAGE, pickupIndex);
                return send(GenesisMenu.Action.CREATE, 1);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double x = mouseX - leftPos, y = mouseY - topPos;
        if (y >= 28 && y < 128 && x >= GENE_X && x < GENE_X + GENE_W) {
            geneScroll = Math.clamp(geneScroll - (int) Math.signum(scrollY), 0, Math.max(0, menu.chromosomes().size() - 5));
            return true;
        }
        if (y >= 28 && y < 128 && x >= OPTION_X && x < OPTION_X + OPTION_W) {
            alleleScroll = Math.clamp(alleleScroll - (int) Math.signum(scrollY), 0, Math.max(0, menu.availableAlleles().size() - 5));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean send(GenesisMenu.Action action, int delta) {
        if (delta == 0 && action != GenesisMenu.Action.CREATE) return true;
        menu.apply(action, delta);
        PacketDistributor.sendToServer(new GenesisActionPayload(menu.containerId, action, delta));
        return true;
    }

    private boolean select(GenesisMenu.Action action, int index) {
        if (!menu.select(action, index)) return false;
        PacketDistributor.sendToServer(new GenesisActionPayload(menu.containerId, action, index, true));
        return true;
    }

    private void drawCenteredNoShadow(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Component alleleName(IChromosome chromosome, Allele<?> allele) {
        Object value = allele.value();
        if (value instanceof Vec3i vector)
            return Component.literal(vector.getX() + " × " + vector.getY() + " × " + vector.getZ());
        if (value instanceof Float number && "sappiness".equals(chromosome.id().getPath()))
            return Component.literal(Math.round(number * 100.0F) + "%");
        if (value instanceof Enum<?> enumeration) return Component.literal(pretty(enumeration.name()));
        String key = chromosome.translationKey(value);
        return Component.translatableWithFallback(key, pretty(String.valueOf(value)));
    }
    private Component trim(Component value, int width) { return font.width(value) <= width ? value : Component.literal(font.plainSubstrByWidth(value.getString(), Math.max(0, width - font.width("…"))) + "…"); }
    private static String pretty(String value) {
        int split = Math.max(value.lastIndexOf(':'), value.lastIndexOf('.'));
        if (split >= 0) value = value.substring(split + 1);
        String[] words = value.replace('_', ' ').split(" ");
        StringBuilder output = new StringBuilder();
        for (String word : words) if (!word.isEmpty()) { if (!output.isEmpty()) output.append(' '); output.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)); }
        return output.toString();
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        double x = mouseX - leftPos, y = mouseY - topPos;
        if (x >= 7 && x < 31 && y >= 28) {
            int index = (int) ((y - 28) / 24);
            var adapters = binnie.genetics.api.GeneticOrganismAdapters.all();
            if (index < adapters.size()) graphics.renderTooltip(font, adapters.get(index).speciesType().getDisplayName(), mouseX, mouseY);
        } else {
            int index = pickupIndexAtScreen(mouseX, mouseY);
            if (index >= 0 && index < menu.stages().size())
                graphics.renderTooltip(font, menu.preview(menu.stages().get(index)), mouseX, mouseY);
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    /** Uses the exact screen-space rectangles used by renderPickup. */
    private int pickupIndexAtScreen(double mouseX, double mouseY) {
        for (int index = 0; index < Math.min(6, menu.stages().size()); index++) {
            int slotX = leftPos + PICKUP_X + PICKUP_SLOT_OFFSET + index % 3 * PICKUP_SLOT_SIZE;
            int slotY = topPos + PICKUP_Y + PICKUP_SLOT_OFFSET + index / 3 * PICKUP_SLOT_SIZE;
            if (mouseX >= slotX && mouseX < slotX + PICKUP_SLOT_SIZE
                    && mouseY >= slotY && mouseY < slotY + PICKUP_SLOT_SIZE) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
