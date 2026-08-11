package binnie.bees.client.screen;

import binnie.bees.menu.AlvearyMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class AlvearyHatcheryScreen extends AlvearyComponentScreen {
    private static final ResourceLocation SLOTS = ResourceLocation.fromNamespaceAndPath(
            "binniecore", "textures/gui/craftgui-slots.png");
    /** First frame of the original animated larvae-outline sprite. */
    private static final ResourceLocation LARVAE_OUTLINE = ResourceLocation.fromNamespaceAndPath(
            "binniecore", "textures/items/validator/larvae.0.png");
    public AlvearyHatcheryScreen(AlvearyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "guihatchery");
    }

    /**
      * Keep the normal slot in both states, but show its larva marker only
      * while empty. The 1.12 GUI drew the validator separately from the slot.
      */
    @Override
    protected void renderLegacyForegroundBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        for (int slotIndex = 0; slotIndex < menu.partType().inventorySlots(); slotIndex++) {
            Slot slot = menu.getSlot(slotIndex);
            int x = leftPos + slot.x;
            int y = topPos + slot.y;

            // Redraw the 1.12 slot over the port's baked bee placeholder.
            // This leaves a normal slot behind a larvae stack as well.
            graphics.blit(SLOTS, x - 1, y - 1, 4, 4, 18, 18, 256, 256);
            if (slot.getItem().isEmpty()) {
                graphics.blit(LARVAE_OUTLINE, x, y, 0, 0, 16, 16, 16, 32);
            }
        }
    }
}
