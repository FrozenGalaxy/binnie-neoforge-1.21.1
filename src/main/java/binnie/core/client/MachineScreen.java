package binnie.core.client;

import binnie.core.machine.MachineMenu;
import binnie.core.machine.MachineProcessError;
import binnie.core.machine.MachineUiLayout;
import binnie.core.registry.CoreItemTags;
import binnie.core.network.MachineRedstonePayload;
import binnie.core.network.MachinePipettePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import binnie.core.tooltip.LegacyTooltipDescriptions;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

/**
  * Shared implementation for Binnie machine screens. Concrete machine classes own all
  * machine-specific geometry, rendering, clicks, tooltips and error highlighting.
  */
public abstract class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    protected static final ResourceLocation PANELS = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-panels.png");
    protected static final ResourceLocation SLOTS = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-slots.png");
    protected static final ResourceLocation CONTROLS = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-controls.png");
    protected static final ResourceLocation VALIDATOR_BLOCK = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/items/validator/block.0.png");
    protected static final ResourceLocation EXTRA_TREES_GUI = ResourceLocation.fromNamespaceAndPath("binnietrees", "textures/gui/legacy/gui.png");

    protected final MachineUiLayout sourceLayout;
    private final BinnieLedgerOverlay ledgers = new BinnieLedgerOverlay();
    private final java.util.List<TankArea> tankAreas = new java.util.ArrayList<>();

    protected MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        sourceLayout = MachineUiLayout.forMachine(menu.machineId()).orElse(null);
        if (sourceLayout != null) {
            imageWidth = sourceLayout.width();
            imageHeight = sourceLayout.height();
            inventoryLabelX = sourceLayout.playerInventory().x();
            inventoryLabelY = sourceLayout.playerInventory().y() - 11;
        } else {
            imageWidth = 176;
            imageHeight = 166;
        }
    }

    @Override
    protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        tankAreas.clear();
        if (sourceLayout != null) {
            renderSourceWindowAndSlots(graphics);
            renderMachineFace(graphics, partialTick, mouseX, mouseY);
        } else {
            renderGenericMachine(graphics);
            renderMachineFace(graphics, partialTick, mouseX, mouseY);
        }
    }

    /** Machine-owned visual content. Shared chrome and slots are already rendered. */
    protected void renderMachineFace(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    protected final void renderGenericMachine(GuiGraphics graphics) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF20262C);
        graphics.fill(left + 7, top + 7, left + imageWidth - 7, top + 75, 0xFF313A43);
        graphics.fill(left + 25, top + 34, left + 43, top + 52, 0xFF11161B);
        graphics.fill(left + 61, top + 34, left + 79, top + 52, 0xFF11161B);
        graphics.fill(left + 79, top + 57, left + 97, top + 75, 0xFF11161B);
        graphics.fill(left + 115, top + 34, left + 133, top + 52, 0xFF11161B);
        graphics.fill(left + 139, top + 34, left + 157, top + 52, 0xFF11161B);
        graphics.fill(left + 139, top + 57, left + 157, top + 75, 0xFF11161B);
        int progressWidth = menu.maxProgress() <= 0 ? 0 : 42 * menu.progress() / menu.maxProgress();
        graphics.fill(left + 62, top + 57, left + 62 + progressWidth, top + 62, 0xFFFFFFFF);
        int energyHeight = menu.maxEnergy() <= 0 ? 0 : 50 * menu.energy() / menu.maxEnergy();
        graphics.fill(left + 151, top + 68 - energyHeight, left + 158, top + 68, 0xFFFF3030);
        int fluidHeight = menu.fluidCapacity() <= 0 ? 0 : 50 * menu.fluid() / menu.fluidCapacity();
        graphics.fill(left + 162, top + 68 - fluidHeight, left + 169, top + 68, 0xFF3090FF);
        registerTankArea(162, 18, 8, 50, 0);
        renderSharedStatusOverlay(graphics);
    }

    protected final void renderSourceWindowAndSlots(GuiGraphics graphics) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFF8B8B8B);
        graphics.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFFC6C6C6);
    // Header Help, Info and Owner controls disabled.
    // MachineScreenWidgets.renderHeaderControls(graphics, x, y, imageWidth);
        MachineScreenWidgets.redstoneControl(graphics, font,
                x + MachineScreenWidgets.REDSTONE_X, y + MachineScreenWidgets.REDSTONE_Y,
                menu.redstoneMode(), menu.redstonePowered());
        for (Slot slot : menu.slots) {
            if (slot.x < 0 || slot.y < 0 || slot.x >= imageWidth || slot.y >= imageHeight) continue;
            graphics.blit(SLOTS, x + slot.x - 1, y + slot.y - 1, 4, 4, 18, 18, 256, 256);
        }
    }

    protected final void renderSourcePanel(GuiGraphics graphics, int x, int y, int width, int height, boolean black) {
        int fill = black ? 0xFF171717 : 0xFF919191;
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, 0xFF4A4A4A);
        graphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + width - 1, topPos + y + height - 1, 0xFFD9D9D9);
        graphics.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + width - 2, topPos + y + height - 2, fill);
    }

    protected final void validator(GuiGraphics graphics, int x, int y) {
        graphics.blit(VALIDATOR_BLOCK, leftPos + x + 1, topPos + y + 1, 0, 0, 16, 16, 16, 16);
    }

    protected final void sourceError(GuiGraphics graphics, int x, int y) {
        boolean error = menu.processError() != MachineProcessError.NONE;
        if (error) graphics.setColor(1.0F, 0.25F, 0.25F, 1.0F);
        graphics.blit(CONTROLS, leftPos + x, topPos + y, 44, 30, 14, 14, 256, 256);
        if (error) graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected final void drawTank(GuiGraphics graphics, int x, int y, FluidStack fluid, int amount, int capacity) {
        drawTank(graphics, x, y, fluid, amount, capacity, 0);
    }

    protected final void drawTank(GuiGraphics graphics, int x, int y, FluidStack fluid, int amount, int capacity, int tankIndex) {
        int absoluteX = leftPos + x;
        int absoluteY = topPos + y;
        registerTankArea(x, y, 18, 60, tankIndex);
        graphics.blit(SLOTS, absoluteX, absoluteY, 8, 28, 18, 60, 256, 256);
        int height = fluid.isEmpty() || capacity <= 0 ? 0 : 58 * Math.clamp(amount, 0, capacity) / capacity;
        if (height > 0) {
            IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(extension.getStillTexture(fluid));
            int tint = extension.getTintColor(fluid);
            graphics.enableScissor(absoluteX + 1, absoluteY + 59 - height, absoluteX + 17, absoluteY + 59);
            graphics.setColor(((tint >>> 16) & 0xFF) / 255.0F, ((tint >>> 8) & 0xFF) / 255.0F,
                    (tint & 0xFF) / 255.0F, 1.0F);
            for (int tileY = 0; tileY < 58; tileY += 16) graphics.blit(absoluteX + 1, absoluteY + 1 + tileY, 0, 16, 16, sprite);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.disableScissor();
        }
        graphics.blit(SLOTS, absoluteX, absoluteY, 32, 28, 18, 60, 256, 256);
    }

    protected final void registerTankArea(int x, int y, int width, int height, int tankIndex) {
        tankAreas.add(new TankArea(x, y, width, height, tankIndex));
    }

    protected final void drawEnergyBar(GuiGraphics graphics, int x, int y, int width, int height, boolean horizontal) {
        int absoluteX = leftPos + x;
        int absoluteY = topPos + y;
        nineSlice(graphics, CONTROLS, absoluteX, absoluteY, width, height, 40, 50, 12, 14, 1);
        float percent = menu.maxEnergy() <= 0 ? 0.0F : Math.clamp((float) menu.energy() / menu.maxEnergy(), 0.0F, 1.0F);
        int red = percent > 0.5F ? Math.round(255.0F * (2.0F - 2.0F * percent)) : 255;
        int green = percent > 0.5F ? 255 : Math.round(510.0F * percent);
        graphics.setColor(red / 255.0F, green / 255.0F, 0.0F, 1.0F);
        if (horizontal) {
            int fill = Math.round((width - 2) * percent);
            if (fill > 0) nineSlice(graphics, CONTROLS, absoluteX + 1, absoluteY + 1, fill, Math.max(1, height - 2), 4, 50, 12, 14, 1);
        } else {
            int fill = Math.round((height - 2) * percent);
            if (fill > 0) nineSlice(graphics, CONTROLS, absoluteX + 1, absoluteY + height - 1 - fill, Math.max(1, width - 2), fill, 4, 50, 12, 14, 1);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        nineSlice(graphics, CONTROLS, absoluteX, absoluteY, width, height, 22, 50, 12, 14, 1);
    }

    private static void nineSlice(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
                                  int u, int v, int sourceWidth, int sourceHeight, int edge) {
        g.blit(texture, x, y, u, v, edge, edge, 256, 256);
        g.blit(texture, x + width - edge, y, u + sourceWidth - edge, v, edge, edge, 256, 256);
        g.blit(texture, x, y + height - edge, u, v + sourceHeight - edge, edge, edge, 256, 256);
        g.blit(texture, x + width - edge, y + height - edge, u + sourceWidth - edge, v + sourceHeight - edge, edge, edge, 256, 256);
        for (int xx = edge; xx < width - edge; xx += sourceWidth - edge * 2) {
            int w = Math.min(sourceWidth - edge * 2, width - edge - xx);
            for (int yy = edge; yy < height - edge; yy += sourceHeight - edge * 2) {
                int h = Math.min(sourceHeight - edge * 2, height - edge - yy);
                g.blit(texture, x + xx, y + yy, u + edge, v + edge, w, h, 256, 256);
            }
        }
    }

    protected static int fluidColor(FluidStack stack, int fallback) {
        if (stack == null || stack.isEmpty()) return fallback;
        return IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack) | 0xFF000000;
    }

    protected final void renderSharedStatusOverlay(GuiGraphics graphics) {
    // Header Help, Info and Owner controls disabled.
    // MachineScreenWidgets.renderHeaderControls(graphics, leftPos, topPos, imageWidth);
        MachineScreenWidgets.redstoneControl(graphics, font,
                leftPos + MachineScreenWidgets.REDSTONE_X, topPos + MachineScreenWidgets.REDSTONE_Y,
                menu.redstoneMode(), menu.redstonePowered());
        boolean error = menu.processError() != MachineProcessError.NONE;
        if (error) graphics.setColor(1.0F, 0.25F, 0.25F, 1.0F);
        graphics.blit(CONTROLS, leftPos + imageWidth - 42, topPos + 8, 44, 30, 14, 14, 256, 256);
        if (error) graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (sourceLayout != null) graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 12, 0x555555, false);
        else graphics.drawString(font, title, titleLabelX, titleLabelY, 0x555555, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ledgers.mouseClicked(mouseX, mouseY, button)) return true;
        if (handlePipetteClick(mouseX, mouseY)) return true;
        if (button == 0 && inside((int) mouseX - leftPos, (int) mouseY - topPos,
                MachineScreenWidgets.REDSTONE_X, MachineScreenWidgets.REDSTONE_Y,
                MachineScreenWidgets.REDSTONE_SIZE, MachineScreenWidgets.REDSTONE_SIZE)) {
            PacketDistributor.sendToServer(new MachineRedstonePayload(menu.containerId, menu.redstoneMode().next().ordinal()));
            return true;
        }
        if (handleMachineClick(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handlePipetteClick(double mouseX, double mouseY) {
        if (!CoreItemTags.isMachinePipette(menu.getCarried())) return false;
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        for (TankArea area : tankAreas) {
            if (!area.contains(x, y)) continue;
            PacketDistributor.sendToServer(new MachinePipettePayload(menu.containerId, area.tankIndex()));
            return true;
        }
        return false;
    }

    protected boolean handleMachineClick(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (sourceLayout != null) renderSourceErrorFeedback(graphics, mouseX, mouseY);
        renderAdditionalErrorFeedback(graphics, mouseX, mouseY);
        renderLedgers(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
        if (ledgers.renderTooltip(graphics, font, mouseX, mouseY)) return;
        boolean occupiedMachineSlot = hoveredSlot != null
                && hoveredSlot.index < menu.machineSlotCount() && !hoveredSlot.getItem().isEmpty();
        if (sourceLayout != null && !occupiedMachineSlot && sourceMachineTooltip(graphics, mouseX, mouseY)) return;
        int titleWidth = font.width(title);
        int titleX = leftPos + (imageWidth - titleWidth) / 2;
        if (hasJeiRecipes() && net.neoforged.fml.ModList.get().isLoaded("jei")
                && mouseX >= titleX && mouseX < titleX + titleWidth && mouseY >= topPos + 8 && mouseY < topPos + 20) {
            graphics.renderTooltip(font, Component.translatable("gui.binniecore.jei.show_recipes"), mouseX, mouseY);
    // Help, Info and Owner header tooltips disabled.
        } else if (inside(mouseX - leftPos, mouseY - topPos, MachineScreenWidgets.REDSTONE_X,
                MachineScreenWidgets.REDSTONE_Y, MachineScreenWidgets.REDSTONE_SIZE, MachineScreenWidgets.REDSTONE_SIZE)) {
            graphics.renderTooltip(font, MachineScreenWidgets.redstoneTooltip(font, menu.redstoneMode(), menu.redstonePowered()), mouseX, mouseY);
        }
        if (sourceLayout == null && inside(mouseX - leftPos, mouseY - topPos, imageWidth - 42, 8, 14, 14)) {
            graphics.renderTooltip(font, MachineScreenWidgets.errorTooltip(font, menu.processError()), mouseX, mouseY);
        }
    }

    /** Renders the shared Forestry-style ledgers; Designer overrides call this from their custom render path. */
    protected final void renderLedgers(GuiGraphics graphics, int mouseX, int mouseY) {
        ledgers.render(graphics, font, leftPos, topPos, imageWidth, width,
                BinnieLedgerOverlay.machineEntries(menu.machineId(), machineInfo(), machineTip(), menu.ownerName(), menu.processError(),
                        menu.energy(), menu.maxEnergy(), menu.maxEnergyInput()));
    }

    /** Absolute areas consumed by JEI's getGuiExtraAreas handler. */
    public final List<net.minecraft.client.renderer.Rect2i> ledgerAreas() {
        return ledgers.extraAreas();
    }

    protected void renderAdditionalErrorFeedback(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    protected void renderSourceErrorFeedback(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    protected boolean sourceMachineTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        return false;
    }

    protected boolean hasJeiRecipes() {
        return false;
    }

    protected Component machineInfo() {
        return LegacyTooltipDescriptions.machineInfo(menu.machineId())
                .map(Component::translatable)
                .orElseGet(() -> Component.translatable("gui.binniecore.ledger.info.default", title));
    }

    protected Component machineTip() {
        return LegacyTooltipDescriptions.machineTip(menu.machineId())
                .map(Component::translatable)
                .map(tip -> Component.translatable("gui.binniecore.ledger.tip", tip))
                .orElseGet(() -> Component.translatable("gui.binniecore.ledger.tip.default"));
    }

    protected final void highlight(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + width - 1, topPos + y + height - 1, 0x80FF3030);
    }

    protected static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    protected final boolean tooltip(GuiGraphics graphics, Component text, int mouseX, int mouseY) {
        graphics.renderTooltip(font, text, mouseX, mouseY);
        return true;
    }

    protected final boolean energyTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.renderTooltip(font, tooltipLines(Component.translatable("gui.binniecore.energy.bar"),
                Component.literal(menu.energy() + "/" + menu.maxEnergy() + " FE").withStyle(net.minecraft.ChatFormatting.GRAY)), mouseX, mouseY);
        return true;
    }

    protected final boolean progressTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int total = legacyProcessLength();
        int progress = menu.maxProgress() > 0 ? menu.progress() : 0;
        int activeTotal = menu.maxProgress() > 0 ? menu.maxProgress() : total;
        String state = progress == 0 ? "Not in Progress" : "In Progress";
        graphics.renderTooltip(font, tooltipLines(
                Component.translatable("gui.binniecore.progress.title"), Component.literal(state),
                Component.translatable("gui.binniecore.progress.time_left", legacyTime(Math.max(0, activeTotal - progress))),
                Component.translatable("gui.binniecore.progress.time_total", legacyTime(activeTotal)),
                Component.translatable("gui.binniecore.progress.energy_total", legacyProcessEnergy() * 10)), mouseX, mouseY);
        return true;
    }

    protected final boolean errorTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.renderTooltip(font, MachineScreenWidgets.errorTooltip(font, menu.processError()), mouseX, mouseY);
        return true;
    }

    protected final boolean fluidTooltip(GuiGraphics graphics, String emptyValidatorKey, FluidStack stack,
                                         int amount, int capacity, int mouseX, int mouseY) {
        if (stack.isEmpty()) {
            boolean output = emptyValidatorKey.endsWith("fermented") || emptyValidatorKey.endsWith("distilled")
                    || emptyValidatorKey.endsWith("output");
            graphics.renderTooltip(font, tooltipLines(
                    Component.translatable("gui.binniecore.tank.validated", Component.translatable(emptyValidatorKey)),
                    Component.translatable("gui.binniecore.tank.capacity", capacity),
                    Component.translatable(output ? "gui.binniecore.side.extract" : "gui.binniecore.side.insert", "Any")), mouseX, mouseY);
            return true;
        }
        int percent = capacity <= 0 ? 0 : amount * 100 / capacity;
        graphics.renderTooltip(font, tooltipLines(stack.getHoverName(),
                Component.translatable("gui.binniecore.tank.percent", percent),
                Component.translatable("gui.binniecore.tank.amount", amount)), mouseX, mouseY);
        return true;
    }

    protected static List<FormattedCharSequence> tooltipLines(Component... lines) {
        return Arrays.stream(lines).map(Component::getVisualOrderText).toList();
    }

    protected int legacyProcessLength() {
        return Math.max(1, menu.maxProgress());
    }

    protected int legacyProcessEnergy() {
        return 0;
    }

    protected static String legacyTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;
        if (minutes == 0) return seconds + " " + (seconds == 1 ? "second" : "seconds");
        if (seconds == 0) return minutes + " " + (minutes == 1 ? "minute" : "minutes");
        return minutes + " " + (minutes == 1 ? "minute" : "minutes") + " " + seconds + " " + (seconds == 1 ? "second" : "seconds");
    }

    private record TankArea(int x, int y, int width, int height, int tankIndex) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
