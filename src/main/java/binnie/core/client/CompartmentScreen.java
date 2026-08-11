package binnie.core.client;

import binnie.core.network.CompartmentActionPayload;
import binnie.core.storage.CompartmentMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Functional 1.12-style tabbed compartment with tab naming, colors and carried-stack icons. */
public final class CompartmentScreen extends AbstractContainerScreen<CompartmentMenu> {
    private EditBox nameBox;
    private EditBox searchBox;

    public CompartmentScreen(CompartmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.slotsPerTab() == 50 ? 196 : 176;
        imageHeight = menu.slotsPerTab() == 50 ? 210 : 228;
        inventoryLabelY = menu.slotsPerTab() == 50 ? 101 : 119;
    }

    @Override
    protected void init() {
        super.init();
        nameBox = new EditBox(font, leftPos + 42, topPos + 6, imageWidth - 86, 14, Component.translatable("screen.binniecore.compartment.rename"));
        nameBox.setMaxLength(32);
        nameBox.setValue(menu.tabName(menu.selectedTab()));
        addRenderableWidget(nameBox);
        searchBox = new EditBox(font, leftPos + 42, topPos + imageHeight - 17, imageWidth - 55, 13,
                Component.translatable("screen.binniecore.compartment.search"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(this::search);
        addRenderableWidget(searchBox);
    }

    private void search(String value) {
        int tab = menu.findMatchingTab(value, menu.selectedTab() - 1);
        if (tab >= 0 && tab != menu.selectedTab()) {
            commitName();
            menu.selectTab(tab);
            nameBox.setValue(menu.tabName(tab));
            PacketDistributor.sendToServer(new CompartmentActionPayload(menu.containerId,
                    CompartmentActionPayload.Action.SELECT, tab, 0, ""));
        }
    }

    private void commitName() {
        if (nameBox == null) return;
        PacketDistributor.sendToServer(new CompartmentActionPayload(menu.containerId,
                CompartmentActionPayload.Action.RENAME, menu.selectedTab(), 0, nameBox.getValue()));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc8b996);
        graphics.fill(leftPos + 7, topPos + 22, leftPos + imageWidth - 7, topPos + inventoryLabelY - 4, 0xff5d5141);
        for (int tab = 0; tab < menu.tabCount(); tab++) {
            int x = leftPos - 22;
            int y = topPos + 22 + tab * 20;
            int color = 0xff000000 | menu.tabColor(tab);
            graphics.fill(x, y, leftPos, y + 18, color);
            if (tab == menu.selectedTab()) graphics.fill(x + 2, y + 2, leftPos, y + 16, 0x55ffffff);
            if (!menu.tabIcon(tab).isEmpty()) graphics.renderItem(menu.tabIcon(tab), x + 3, y + 1);
            else graphics.drawCenteredString(font, Integer.toString(tab + 1), x + 11, y + 5, 0xffffff);
        }
        graphics.fill(leftPos + 8, topPos + inventoryLabelY + 8, leftPos + imageWidth - 8, topPos + imageHeight - 22, 0xff8f8065);
        graphics.fill(leftPos + imageWidth - 39, topPos + 5, leftPos + imageWidth - 27, topPos + 19, 0xff6b6254);
        graphics.fill(leftPos + imageWidth - 25, topPos + 5, leftPos + imageWidth - 13, topPos + 19, 0xff6b6254);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 8, 0x29251f, false);
        graphics.drawString(font, "−", imageWidth - 36, 8, 0xffffff, false);
        graphics.drawString(font, "+", imageWidth - 22, 8, 0xffffff, false);
        graphics.drawString(font, Component.translatable("screen.binniecore.compartment.search"), 8, imageHeight - 15, 0x29251f, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int tab = 0; tab < menu.tabCount(); tab++) {
            if (mouseX >= leftPos - 22 && mouseX < leftPos && mouseY >= topPos + 22 + tab * 20 && mouseY < topPos + 40 + tab * 20) {
                commitName();
                menu.selectTab(tab);
                nameBox.setValue(menu.tabName(tab));
                PacketDistributor.sendToServer(new CompartmentActionPayload(menu.containerId,
                        CompartmentActionPayload.Action.SELECT, tab, 0, ""));
                if (button == 1) PacketDistributor.sendToServer(new CompartmentActionPayload(menu.containerId,
                        CompartmentActionPayload.Action.SET_ICON, tab, 0, ""));
                return true;
            }
        }
        if (mouseY >= topPos + 5 && mouseY < topPos + 19) {
            int direction = mouseX >= leftPos + imageWidth - 25 && mouseX < leftPos + imageWidth - 13 ? 1
                    : mouseX >= leftPos + imageWidth - 39 && mouseX < leftPos + imageWidth - 27 ? -1 : 0;
            if (direction != 0) {
                PacketDistributor.sendToServer(new CompartmentActionPayload(menu.containerId,
                        CompartmentActionPayload.Action.CYCLE_COLOR, menu.selectedTab(), direction, ""));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.isFocused() && (keyCode == 257 || keyCode == 335)) {
            commitName();
            nameBox.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        commitName();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
