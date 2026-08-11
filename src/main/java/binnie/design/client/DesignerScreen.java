package binnie.design.client;

import binnie.botany.registry.BotanyWorldContent;
import binnie.core.client.MachineScreen;
import binnie.core.data.DesignData;
import binnie.core.machine.MachineMenu;
import binnie.core.network.ManualMachineCraftPayload;
import binnie.core.network.ManualMachineSelectionPayload;
import binnie.design.api.DesignedBlockKind;
import binnie.design.data.DesignCatalog;
import binnie.design.data.DesignMaterialCatalog;
import binnie.trees.registry.BinnieTreesDesignContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
  * A 320x216 manual Designer screen based on WindowDesigner
  * with its own design browser, filtered scrolling selection and preview slot.
  */
public abstract class DesignerScreen extends MachineScreen {
    private static final int DESIGN_X = 190;
    private static final int DESIGN_Y = 50;
    private static final int DESIGN_WIDTH = 114;
    private static final int DESIGN_HEIGHT = 122;
    // The panel bevel occupies one pixel inside these logical bounds. Content
    // must be clipped to the black inset, not to the surrounding light bevel.
    private static final int DESIGN_CLIP_X = DESIGN_X + 1;
    private static final int DESIGN_CLIP_Y = DESIGN_Y + 1;
    private static final int DESIGN_CLIP_WIDTH = DESIGN_WIDTH - 2;
    private static final int DESIGN_CLIP_HEIGHT = DESIGN_HEIGHT - 2;
    /** ControlTileSelect wraps after the fourth 18px tile at x>90. */
    private static final int COLUMNS = 4;
    private static final int ROW_HEIGHT = 20;

    private final DesignedBlockKind outputKind;
    private EditBox filter;
    private int scrollRows;

    protected DesignerScreen(MachineMenu menu, Inventory inventory, Component title, DesignedBlockKind outputKind) {
        super(menu, inventory, title);
        this.outputKind = outputKind;
        imageWidth = 320;
        imageHeight = 216;
        inventoryLabelX = 14;
        inventoryLabelY = 86;
    }

    @Override
    protected void init() {
        super.init();
        filter = addRenderableWidget(new EditBox(font, leftPos + 188, topPos + 178, 118, 12,
                Component.translatable("screen.binniedesign.designer.search")));
        filter.setMaxLength(64);
        filter.setResponder(value -> scrollRows = 0);
    }

    @Override
    protected void renderMachineFace(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    // The original WindowDesigner was assembled from Binnie Core's beveled window, slots and panels.
        renderSourceWindowAndSlots(graphics);
        renderSourcePanel(graphics, 188, 48, 118, 126, false);
        renderSourcePanel(graphics, DESIGN_X - 1, DESIGN_Y - 1, DESIGN_WIDTH + 2, DESIGN_HEIGHT + 2, true);
        renderSourcePanel(graphics, 188, 177, 118, 14, true);
        validator(graphics, 22, 34);
        validator(graphics, 62, 34);
        sourceError(graphics, 76, 65);
    // Dedicated operation/JEI affordance between the material controls
    // and ControlRecipeSlot. The 1.12 recipe slot remains exclusively
    // responsible for crafting and is never covered by a JEI click area.
        renderSourcePanel(graphics, 92, 50, 18, 18, true);
        graphics.drawCenteredString(font, Component.literal(">"), leftPos + 101, topPos + 55, 0x404040);
    // This pass uses absolute screen coordinates. Keeping the preview and
    // tiles here avoids applying leftPos twice from renderLabels.
        renderDesigns(graphics, mouseX, mouseY);
    // WindowDesigner has only the 1.12 Help/Info/User header controls;
    // its current redstone marker must not cover that 1.12 layout.
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    // Window#setTitle is centered by the 1.12 CraftGUI window, leaving
    // its Help and Info controls unobscured at the upper left.
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 12, 0x555555, false);
        graphics.drawCenteredString(font, Component.translatable("screen.binniedesign.designer.designs"), 247, 36, 0x444444);
    }

    private void renderDesigns(GuiGraphics graphics, int mouseX, int mouseY) {
    // ControlRecipeSlot is a 50px rotating display slot and is outside
      // the scroll clip. Render the current preview first.
        renderSourcePanel(graphics, 111, 33, 52, 52, true);
        ItemStack output = menu.manualPreview();
        if (!output.isEmpty()) {
            // ControlRecipeSlot is a 50px rotating item renderer. Keep its
            // originally sized presentation instead of the default 16px slot
            // icon that made valid worker inputs appear to do nothing.
            graphics.pose().pushPose();
            // Draw at the normal GUI depth. Render order already places the
            // preview above its panel, while later tooltip strata must remain
            // above this enlarged 3-D item.
            graphics.pose().translate(leftPos + 113, topPos + 35, 0.0F);
            // GuiGraphics.renderItem adds its own +150 Z translation. Scaling
            // Z by three raised the item to 450, above Minecraft tooltips at
            // Z 400. Enlarge only the screen-space axes.
            graphics.pose().scale(3.0F, 3.0F, 1.0F);
            // ControlRecipeSlot#setRotating delegates to RenderUtil,
            // which advances one degree per 20 ms around (0, -0.866, 0.5).
            // Rotate around the GUI item's own (8, 8, 150) centre exactly as
            // that renderer did, keeping a smooth 7.2-second revolution.
            float phase = (net.minecraft.Util.getMillis() % 7_200L) / 20.0F;
            graphics.pose().translate(8.0F, 8.0F, 150.0F);
            graphics.pose().mulPose(com.mojang.math.Axis.of(
                    new org.joml.Vector3f(0.0F, -0.866F, 0.5F)).rotationDegrees(phase));
            graphics.pose().translate(-8.0F, -8.0F, -150.0F);
            graphics.renderItem(output, 0, 0);
            graphics.pose().popPose();
        }
        graphics.enableScissor(leftPos + DESIGN_CLIP_X, topPos + DESIGN_CLIP_Y,
                leftPos + DESIGN_CLIP_X + DESIGN_CLIP_WIDTH, topPos + DESIGN_CLIP_Y + DESIGN_CLIP_HEIGHT);
        for (BrowserEntry entry : browserEntries()) {
            int y = DESIGN_Y + entry.y() - scrollRows * ROW_HEIGHT;
            if (y + entry.height() <= DESIGN_CLIP_Y || y >= DESIGN_CLIP_Y + DESIGN_CLIP_HEIGHT) continue;
            if (entry.design() < 0) {
                graphics.drawString(font, entry.title(), leftPos + DESIGN_X + 2, topPos + y + 3, 0x404040, false);
                continue;
            }
            int x = DESIGN_X + 2 + entry.column() * 20;
            int index = entry.design();
            boolean selected = index == menu.manualSelection();
            boolean hovered = mouseX >= leftPos + x && mouseX < leftPos + x + 18
                    && mouseY >= topPos + y && mouseY < topPos + y + 18;
            graphics.fill(leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 18,
                    selected ? 0xffd0d0d0 : hovered ? 0xff888888 : 0xff555555);
            graphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + 17, topPos + y + 17, 0xff171b20);
            graphics.renderItem(preview(index), leftPos + x + 1, topPos + y + 1);
        }
        graphics.disableScissor();
    }

    /** Matching ControlTileSelect geometry: category label (16px), then 18px tiles in four 20px columns. */
    private List<BrowserEntry> browserEntries() {
        String query = filter == null ? "" : filter.getValue().trim().toLowerCase(java.util.Locale.ROOT);
        java.util.ArrayList<BrowserEntry> entries = new java.util.ArrayList<>();
        int y = 2;
        for (var category : DesignCatalog.CATEGORY_ORDER) {
            List<Integer> designs = category.getValue().stream().filter(DesignCatalog.DESIGNS::contains)
                    .filter(id -> query.isEmpty() || id.contains(query)).map(DesignCatalog.DESIGNS::indexOf).toList();
            if (designs.isEmpty()) continue;
            entries.add(new BrowserEntry(-1, y, 0, 16, category.getKey())); y += 16;
            for (int n = 0; n < designs.size(); n++) {
                entries.add(new BrowserEntry(designs.get(n), y + n / COLUMNS * ROW_HEIGHT, n % COLUMNS, 18, ""));
            }
            y += ((designs.size() + COLUMNS - 1) / COLUMNS) * ROW_HEIGHT;
        }
        return entries;
    }

    private record BrowserEntry(int design, int y, int column, int height, String title) { }

    private ItemStack preview(int index) {
        DesignedBlockKind kind = outputKind;
    // ControlTileSelect asks the actual DesignerType for its display
            // stack. Use the material slots when populated instead
    // than a permanent oak/red placeholder.
        ItemStack primary = menu.sourceStack(0);
        ItemStack secondary = menu.sourceStack(1);
        if (primary.isEmpty()) primary = switch (kind) {
            case GLASS -> BotanyWorldContent.STAINED_GLASS_ITEM.get().createStack("red");
            case CERAMIC -> BotanyWorldContent.CERAMIC_ITEM.get().createStack("red");
            default -> new ItemStack(Items.OAK_PLANKS);
        };
        if (secondary.isEmpty()) secondary = switch (kind) {
            case GLASS -> BotanyWorldContent.STAINED_GLASS_ITEM.get().createStack("royal_blue");
            case CERAMIC -> BotanyWorldContent.CERAMIC_ITEM.get().createStack("royal_blue");
            default -> new ItemStack(Items.BIRCH_PLANKS);
        };
        String design = DesignCatalog.DESIGNS.get(index);
        DesignData data = new DesignData(design, design, BuiltInRegistries.ITEM.getKey(primary.getItem()),
                BuiltInRegistries.ITEM.getKey(secondary.getItem()), 0, false,
                DesignMaterialCatalog.variant(primary), DesignMaterialCatalog.variant(secondary));
        return kind == DesignedBlockKind.CERAMIC
                ? BotanyWorldContent.CERAMIC_TILE_ITEM.get().create(data, 1)
                : BinnieTreesDesignContent.itemFor(kind).get().create(data, 1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    // ControlRecipeSlot extends ControlSlotBase with a size of 50. Its
    // whole preview is the click target, not merely a vanilla 18px slot.
        if (button == 0 && mouseX >= leftPos + 112 && mouseX < leftPos + 162
                && mouseY >= topPos + 34 && mouseY < topPos + 84) {
            PacketDistributor.sendToServer(new ManualMachineCraftPayload(menu.containerId));
            return true;
        }
        if (button == 0 && mouseX >= leftPos + DESIGN_CLIP_X && mouseX < leftPos + DESIGN_CLIP_X + DESIGN_CLIP_WIDTH
                && mouseY >= topPos + DESIGN_CLIP_Y && mouseY < topPos + DESIGN_CLIP_Y + DESIGN_CLIP_HEIGHT) {
            int relativeY = (int) (mouseY - topPos - DESIGN_Y + scrollRows * ROW_HEIGHT);
            int column = (int) ((mouseX - leftPos - DESIGN_X - 2) / 20);
            for (BrowserEntry entry : browserEntries()) if (entry.design() >= 0 && entry.column() == column
                    && relativeY >= entry.y() && relativeY < entry.y() + 18) {
                PacketDistributor.sendToServer(new ManualMachineSelectionPayload(menu.containerId, entry.design()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos + DESIGN_CLIP_X && mouseX < leftPos + DESIGN_CLIP_X + DESIGN_CLIP_WIDTH
                && mouseY >= topPos + DESIGN_CLIP_Y && mouseY < topPos + DESIGN_CLIP_Y + DESIGN_CLIP_HEIGHT) {
            int rows = Math.max(0, (browserEntries().stream().mapToInt(entry -> entry.y() + entry.height()).max().orElse(0) - DESIGN_HEIGHT) / ROW_HEIGHT + 1);
            scrollRows = Math.clamp(scrollRows - (int) Math.signum(scrollY), 0, rows);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    // MachineScreen has already rendered the vanilla item tooltip. Do
    // not draw it a second time and do not layer an empty-slot role hint
    // over an occupied slot's item tooltip.
        boolean occupiedMachineSlot = hoveredSlot != null
                && hoveredSlot.index < menu.machineSlotCount()
                && !hoveredSlot.getItem().isEmpty();
        if (occupiedMachineSlot) return;
        hoverTooltip(graphics, mouseX, mouseY);
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        int titleX = (imageWidth - font.width(title)) / 2;
        if (net.neoforged.fml.ModList.get().isLoaded("jei")
                && x >= titleX && x < titleX + font.width(title) && y >= 8 && y < 20) {
            graphics.renderTooltip(font, Component.translatable("gui.binniecore.jei.show_recipes"), mouseX, mouseY);
        } else if (x >= 22 && x < 40 && y >= 34 && y < 52) graphics.renderTooltip(font, Component.literal("Primary design material"), mouseX, mouseY);
        else if (x >= 62 && x < 80 && y >= 34 && y < 52) graphics.renderTooltip(font, Component.literal("Secondary design material"), mouseX, mouseY);
        else if (x >= 42 && x < 60 && y >= 64 && y < 82) graphics.renderTooltip(font, Component.literal("Design adhesive"), mouseX, mouseY);
        else if (x >= 112 && x < 162 && y >= 34 && y < 84)
            graphics.renderTooltip(font, Component.translatable("screen.binniedesign.designer.craft_selected"), mouseX, mouseY);
        else if (x >= 92 && x < 110 && y >= 50 && y < 68
                && net.neoforged.fml.ModList.get().isLoaded("jei"))
            graphics.renderTooltip(font, Component.translatable("gui.binniecore.jei.show_recipes"), mouseX, mouseY);
        else if (x >= 76 && x < 90 && y >= 65 && y < 79)
            graphics.renderTooltip(font, binnie.core.client.MachineScreenWidgets.errorTooltip(
                    font, menu.processError()), mouseX, mouseY);
    }

    @Override
    protected void renderAdditionalErrorFeedback(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.processError() == binnie.core.machine.MachineProcessError.NONE
                || mouseX < leftPos + 76 || mouseX >= leftPos + 90
                || mouseY < topPos + 65 || mouseY >= topPos + 79) return;
        if (menu.processError() == binnie.core.machine.MachineProcessError.REDSTONE_PAUSED) {
            errorHighlight(graphics, 52, 8, 16, 16);
            return;
        }
        if (menu.processError() == binnie.core.machine.MachineProcessError.INVALID_RECIPE) {
            errorHighlight(graphics, 111, 33, 52, 52);
            errorHighlight(graphics, 189, 49, 116, 124);
            return;
        }
        errorHighlight(graphics, 22, 34, 18, 18);
        errorHighlight(graphics, 62, 34, 18, 18);
        errorHighlight(graphics, 42, 64, 18, 18);
    }

    private void errorHighlight(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(leftPos + x + 1, topPos + y + 1,
                leftPos + x + width - 1, topPos + y + height - 1, 0x80ff3030);
    }

    private void hoverTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (mouseX < leftPos + DESIGN_CLIP_X || mouseX >= leftPos + DESIGN_CLIP_X + DESIGN_CLIP_WIDTH
                || mouseY < topPos + DESIGN_CLIP_Y || mouseY >= topPos + DESIGN_CLIP_Y + DESIGN_CLIP_HEIGHT) return;
        int column = (mouseX - leftPos - DESIGN_X - 2) / 20;
        int relativeY = mouseY - topPos - DESIGN_Y + scrollRows * ROW_HEIGHT;
        for (BrowserEntry entry : browserEntries()) {
            if (entry.design() < 0 || entry.column() != column || relativeY < entry.y() || relativeY >= entry.y() + 18) continue;
            String design = DesignCatalog.DESIGNS.get(entry.design());
            graphics.renderTooltip(font, Component.translatable("item.binniedesign.design." + design), mouseX, mouseY);
            return;
        }
    }
}
