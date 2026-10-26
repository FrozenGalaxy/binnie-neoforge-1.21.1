package binnie.genetics.client;

import binnie.core.client.MachineScreenWidgets;
import binnie.core.client.BinnieLedgerOverlay;
import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineMenu;
import binnie.core.machine.MachineProcessError;
import binnie.core.network.MachineRedstonePayload;
import binnie.core.network.MachinePipettePayload;
import binnie.core.registry.BinnieDataComponents;
import binnie.core.registry.CoreItemTags;
import binnie.core.tooltip.LegacyTooltipDescriptions;
import binnie.genetics.data.LaboratoryChargeData;
import binnie.genetics.machine.GeneticsMachineInventoryLayout;
import binnie.genetics.machine.GeneticsMachineLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** Shared source-atlas implementation for concrete Genetics laboratory screens. */
public abstract class GeneticsMachineScreen extends AbstractContainerScreen<MachineMenu> {
    protected final GeneticsMachineLayout.Layout layout;
    private int errorX = -1;
    private int errorY = -1;
    private ChargeArea chargeArea;
    private final List<TankArea> tankAreas = new ArrayList<>();
    private final BinnieLedgerOverlay ledgers = new BinnieLedgerOverlay();

    protected GeneticsMachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        layout = GeneticsMachineLayout.forMachine(menu.machineId())
                .orElseThrow(() -> new IllegalArgumentException("Not a Genetics machine: " + menu.machineId()));
        imageWidth = layout.width();
        imageHeight = layout.height();
        inventoryLabelX = 10_000;
        inventoryLabelY = 10_000;
    }

    @Override
    protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        LegacyGeneticsGui.window(graphics, leftPos, topPos, imageWidth, imageHeight);
    // Header Help, Info and Owner controls disabled.
    // LegacyGeneticsGui.windowControls(graphics, leftPos, topPos, imageWidth);
        chargeArea = null;
        tankAreas.clear();
        errorX = -1;
        errorY = -1;
        renderMachineFace(graphics, partialTick, mouseX, mouseY);
        MachineScreenWidgets.redstoneControl(graphics, font,
                leftPos + MachineScreenWidgets.REDSTONE_X, topPos + MachineScreenWidgets.REDSTONE_Y,
                menu.redstoneMode(), menu.redstonePowered());
        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (slot.x <= -512 || slot.y <= -512) continue;
            LegacyGeneticsGui.slot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
            if (slotIndex < menu.machineSlotCount() && slot.getItem().isEmpty()) {
                String validator = validatorSprite(slotIndex);
                if (validator != null) LegacyGeneticsGui.validator(graphics, validator, leftPos + slot.x, topPos + slot.y);
            }
        }
    }

    /** Concrete machine-owned controls, tanks, progress art and error indicator. */
    protected abstract void renderMachineFace(GuiGraphics graphics, float partialTick, int mouseX, int mouseY);

    /** Concrete 1.12 slot-validator sprite name, or {@code null}. */
    protected abstract String validatorSprite(int slot);

    protected final void renderError(GuiGraphics graphics, int x, int y) {
        errorX = x;
        errorY = y;
        boolean error = menu.processError() != MachineProcessError.NONE;
        if (error) graphics.setColor(1.0F, 0.25F, 0.25F, 1.0F);
        LegacyGeneticsGui.errorState(graphics, x, y);
        if (error) graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected final void renderCharge(GuiGraphics graphics, int machineSlot, int x, int y, int color) {
        if (machineSlot >= menu.machineSlotCount()) return;
        var stack = menu.getSlot(machineSlot).getItem();
        LaboratoryChargeData data = stack.get(BinnieDataComponents.LABORATORY_CHARGE.get());
        int charge = stack.isEmpty() ? 0 : data == null ? LaboratoryChargeData.FULL : data.milliCharge();
        LegacyGeneticsGui.charge(graphics, leftPos + x, topPos + y, charge, color);
        chargeArea = new ChargeArea(x, y, machineSlot, charge);
    }

    /** Draws a source-accurate Genetics tank and records its matching hover rectangle. */
    protected final void renderTank(GuiGraphics graphics, int x, int y, FluidStack stack, int capacity) {
        renderTank(graphics, x, y, stack, capacity, false, 0);
    }

    /** Draws a source-accurate horizontal/vertical Genetics tank and records its hover rectangle. */
    protected final void renderTank(GuiGraphics graphics, int x, int y, FluidStack stack, int capacity, boolean horizontal) {
        renderTank(graphics, x, y, stack, capacity, horizontal, 0);
    }

    protected final void renderTank(GuiGraphics graphics, int x, int y, FluidStack stack, int capacity, int tankIndex) {
        renderTank(graphics, x, y, stack, capacity, false, tankIndex);
    }

    protected final void renderTank(GuiGraphics graphics, int x, int y, FluidStack stack, int capacity, boolean horizontal, int tankIndex) {
        LegacyGeneticsGui.tank(graphics, leftPos + x, topPos + y, stack, capacity, horizontal);
        tankAreas.add(new TankArea(x, y, horizontal ? 60 : 18, horizontal ? 18 : 60, stack.copy(), capacity, tankIndex));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 10, 0x555555, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ledgers.mouseClicked(mouseX, mouseY, button)) return true;
        if (handlePipetteClick(mouseX, mouseY)) return true;
        int x = leftPos + MachineScreenWidgets.REDSTONE_X;
        int y = topPos + MachineScreenWidgets.REDSTONE_Y;
        if (button == 0 && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            PacketDistributor.sendToServer(new MachineRedstonePayload(menu.containerId, menu.redstoneMode().next().ordinal()));
            return true;
        }
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderErrorFeedback(graphics, mouseX, mouseY);
        ledgers.render(graphics, font, leftPos, topPos, imageWidth, width,
                BinnieLedgerOverlay.machineEntries(menu.machineId(), machineInfo(), machineTip(), menu.ownerName(), menu.processError(),
                        menu.energy(), menu.maxEnergy(), menu.maxEnergyInput()));
        renderTooltip(graphics, mouseX, mouseY);
        if (ledgers.renderTooltip(graphics, font, mouseX, mouseY)) return;
        if (hoveredSlot != null && hoveredSlot.index < menu.machineSlotCount() && hoveredSlot.getItem().isEmpty()) {
            graphics.renderTooltip(font, slotRole(hoveredSlot.index), mouseX, mouseY);
        }
        int redstoneX = leftPos + MachineScreenWidgets.REDSTONE_X;
        int redstoneY = topPos + MachineScreenWidgets.REDSTONE_Y;
    // Header Help tooltip disabled.
        if (mouseX >= redstoneX && mouseX < redstoneX + 16 && mouseY >= redstoneY && mouseY < redstoneY + 16) {
            graphics.renderTooltip(font, MachineScreenWidgets.redstoneTooltip(font, menu.redstoneMode(), menu.redstonePowered()), mouseX, mouseY);
        }
        if (errorX >= 0 && errorY >= 0 && mouseX >= errorX && mouseX < errorX + 16 && mouseY >= errorY && mouseY < errorY + 16) {
            graphics.renderTooltip(font, MachineScreenWidgets.errorTooltip(font, menu.processError()), mouseX, mouseY);
        }
        if (chargeArea != null && chargeArea.contains(mouseX - leftPos, mouseY - topPos)) {
            graphics.renderTooltip(font, Component.translatable("tooltip.binniegenetics.reagent_charge", chargeArea.milliCharge() / 100), mouseX, mouseY);
        }
        for (TankArea tankArea : tankAreas) {
            if (!tankArea.contains(mouseX - leftPos, mouseY - topPos)) continue;
            graphics.renderTooltip(font, tankArea.tooltip().stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
            break;
        }
        GeneticsJeiAreas.Area descriptor = GeneticsJeiAreas.AREAS.get(menu.machineId().getPath());
        GeneticsJeiAreas.Resolved jeiArea = descriptor == null ? null : descriptor.resolve(font.width(title));
        boolean overRedstone = mouseX >= redstoneX && mouseX < redstoneX + 16 && mouseY >= redstoneY && mouseY < redstoneY + 16;
        boolean overError = errorX >= 0 && errorY >= 0 && mouseX >= errorX && mouseX < errorX + 16 && mouseY >= errorY && mouseY < errorY + 16;
        if (net.neoforged.fml.ModList.get().isLoaded("jei") && jeiArea != null && hoveredSlot == null
                && !overRedstone && !overError && jeiArea.contains(mouseX - leftPos, mouseY - topPos)) {
            graphics.renderTooltip(font, Component.translatable("gui.binniecore.jei.show_recipes"), mouseX, mouseY);
        }
    }

    /** Absolute areas consumed by JEI's getGuiExtraAreas handler. */
    public final List<net.minecraft.client.renderer.Rect2i> ledgerAreas() {
        return ledgers.extraAreas();
    }

    /** The original item-help text, reused as the expanded Forestry info ledger. */
    private Component machineInfo() {
        return LegacyTooltipDescriptions.machineInfo(menu.machineId())
                .map(Component::translatable)
                .orElseGet(() -> Component.translatable("gui.binniecore.ledger.info.default", title));
    }

    private Component machineTip() {
        return LegacyTooltipDescriptions.machineTip(menu.machineId())
                .map(Component::translatable)
                .map(tip -> Component.translatable("gui.binniecore.ledger.tip", tip))
                .orElseGet(() -> Component.translatable("gui.binniecore.ledger.tip.default"));
    }

    private void renderErrorFeedback(GuiGraphics graphics, int mouseX, int mouseY) {
        MachineProcessError error = menu.processError();
        if (error == MachineProcessError.NONE || errorX < 0 || errorY < 0
                || mouseX < errorX || mouseX >= errorX + 14 || mouseY < errorY || mouseY >= errorY + 14) return;
        switch (error) {
            case REDSTONE_PAUSED -> highlight(graphics, MachineScreenWidgets.REDSTONE_X, MachineScreenWidgets.REDSTONE_Y, 16, 16);
            // Energy is represented by the Forestry Power ledger instead of an inline meter.
            case NO_POWER -> { }
            case NO_OWNER -> { /* Owner header control intentionally disabled. */ }
            case OUTPUT_BLOCKED, NO_SPACE -> highlightOutputSlots(graphics);
            case OUTPUT_TANK_FULL, NO_SPACE_TANK, TANK_FULL -> highlightTank(graphics, true, false);
            case INSUFFICIENT_BACTERIA -> highlightTank(graphics, false, false);
            case INSUFFICIENT_DNA -> highlightTank(graphics, false, true);
            case INSUFFICIENT_ETHANOL, INSUFFICIENT_VECTOR, INSUFFICIENT_LIQUID,
                    TANK_EMPTY, TANK_DIFFERENT_FLUID -> highlightTank(graphics, false, false);
            case NO_ENZYME, NO_DYE -> {
                highlightSlot(graphics, MachineBlockEntity.REAGENT_SLOT);
                highlightCharge(graphics);
            }
            case NO_EMPTY_SEQUENCER -> highlightSlot(graphics, MachineBlockEntity.CATALYST_SLOT);
            case NO_SERUM, INVALID_SERUM, INVALID_SERUM_MISMATCH, DEFUNCT_SERUM,
                    EMPTY_SERUM, NO_GENE, ITEM_FILLED -> highlightSerumSlots(graphics);
            case NO_INDIVIDUAL, NO_SEQUENCE, ALREADY_ANALYZED -> highlightInputSlots(graphics);
            case CANNOT_ACCLIMATISE -> {
                highlightInputSlots(graphics);
                highlightAcclimatiserReagents(graphics);
            }
            case INVALID_INPUT -> highlightInvalidInput(graphics);
            case NO_ITEM, INVALID_ITEM, NO_RECIPE, INVALID_RECIPE -> highlightAllInputs(graphics);
            default -> highlightAllInputs(graphics);
        }
    }

    protected void highlightEnergy(GuiGraphics graphics) {
    }

    protected void highlightTank(GuiGraphics graphics, boolean output, boolean secondary) {
    }

    protected boolean includePrimaryTankInAllInputErrors() {
        return false;
    }

    protected void highlightInvalidInput(GuiGraphics graphics) {
        highlightInputSlots(graphics);
    }

    protected void highlightSerumSlots(GuiGraphics graphics) {
        highlightSlot(graphics, MachineBlockEntity.CATALYST_SLOT);
        for (int slot : GeneticsMachineInventoryLayout.slots(menu.machineId(), GeneticsMachineInventoryLayout.Group.SERUM_RESERVE)) {
            highlightSlot(graphics, slot);
        }
    }

    private void highlightAllInputs(GuiGraphics graphics) {
        for (int slot = 0; slot < menu.machineSlotCount(); slot++) {
            if (slot >= MachineBlockEntity.OUTPUT_SLOT && slot <= MachineBlockEntity.TERTIARY_OUTPUT_SLOT) continue;
            if (GeneticsMachineInventoryLayout.isOutput(menu.machineId(), slot)) continue;
            highlightSlot(graphics, slot);
        }
        if (includePrimaryTankInAllInputErrors()) highlightTank(graphics, false, false);
    }

    protected final void highlightInputSlots(GuiGraphics graphics) {
        highlightSlot(graphics, MachineBlockEntity.INPUT_SLOT);
        for (int slot : GeneticsMachineInventoryLayout.slots(menu.machineId(), GeneticsMachineInventoryLayout.Group.INPUT_RESERVE)) highlightSlot(graphics, slot);
        for (int slot : GeneticsMachineInventoryLayout.slots(menu.machineId(), GeneticsMachineInventoryLayout.Group.QUEUE)) highlightSlot(graphics, slot);
    }

    protected final void highlightAcclimatiserReagents(GuiGraphics graphics) {
        highlightSlot(graphics, MachineBlockEntity.CATALYST_SLOT);
        highlightSlot(graphics, MachineBlockEntity.REAGENT_SLOT);
        for (int slot : GeneticsMachineInventoryLayout.slots(menu.machineId(), GeneticsMachineInventoryLayout.Group.ACCLIMATISER)) highlightSlot(graphics, slot);
    }

    private void highlightOutputSlots(GuiGraphics graphics) {
        for (int slot = 0; slot < menu.machineSlotCount(); slot++) {
            if (slot >= MachineBlockEntity.OUTPUT_SLOT && slot <= MachineBlockEntity.TERTIARY_OUTPUT_SLOT
                    || GeneticsMachineInventoryLayout.isOutput(menu.machineId(), slot)) highlightSlot(graphics, slot);
        }
    }

    protected final void highlightSlot(GuiGraphics graphics, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.machineSlotCount()) return;
        Slot slot = menu.getSlot(slotIndex);
        if (slot.x <= -512 || slot.y <= -512) return;
        highlight(graphics, slot.x - 1, slot.y - 1, 18, 18);
    }

    private void highlightCharge(GuiGraphics graphics) {
        if (chargeArea != null) highlight(graphics, chargeArea.x(), chargeArea.y(), 4, 18);
    }

    protected final void highlight(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + width - 1, topPos + y + height - 1, 0x80ff3030);
    }

    private Component slotRole(int slot) {
        String validator = validatorSprite(slot);
        if (validator != null) return Component.translatable("tooltip.binniegenetics.slot." + validator);
        var supplementary = GeneticsMachineInventoryLayout.slot(menu.machineId(), slot);
        if (supplementary.isPresent()) {
            return Component.translatable("tooltip.binniegenetics.slot." + supplementary.get().group().name().toLowerCase(java.util.Locale.ROOT));
        }
        if (slot == MachineBlockEntity.INPUT_SLOT) return Component.translatable("tooltip.binniegenetics.slot.input");
        if (slot == MachineBlockEntity.CATALYST_SLOT) return Component.translatable("tooltip.binniegenetics.slot.catalyst");
        if (slot == MachineBlockEntity.REAGENT_SLOT) return Component.translatable("tooltip.binniegenetics.slot.reagent");
        if (slot >= MachineBlockEntity.OUTPUT_SLOT && slot <= MachineBlockEntity.SECONDARY_OUTPUT_SLOT) {
            return Component.translatable("tooltip.binniegenetics.slot.output");
        }
        return Component.translatable("tooltip.binniegenetics.slot.machine");
    }

    private record ChargeArea(int x, int y, int slot, int milliCharge) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + 4 && mouseY >= y && mouseY < y + 18;
        }
    }

    private record TankArea(int x, int y, int width, int height, FluidStack fluid, int capacity, int tankIndex) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        List<Component> tooltip() {
            Component name = fluid.isEmpty() ? Component.translatable("tooltip.binniecore.tank.empty") : fluid.getHoverName();
            return List.of(name, Component.translatable("tooltip.binniecore.tank.amount", fluid.getAmount(), capacity));
        }
    }
}
