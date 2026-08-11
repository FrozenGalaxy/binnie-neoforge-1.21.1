package binnie.genetics.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Draws the 1.12 CraftGUI texture-sheet regions using the original padding. */
final class LegacyGeneticsGui {
    static final ResourceLocation PANELS = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-panels.png");
    static final ResourceLocation CONTROLS = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-controls.png");
    static final ResourceLocation SLOTS = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-slots.png");
    static final ResourceLocation PROCESS = ResourceLocation.fromNamespaceAndPath("binniegenetics", "textures/gui/process.png");
    static final ResourceLocation PROCESS_2 = ResourceLocation.fromNamespaceAndPath("binniegenetics", "textures/gui/process2.png");
    static final ResourceLocation PROCESS_3 = ResourceLocation.fromNamespaceAndPath("binniegenetics", "textures/gui/process3.png");

    private LegacyGeneticsGui() {
    }

    static void window(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANELS, x, y, width, height, 6, 6, 64, 64, 8);
    }

    /** Draws an original standalone CraftGUI arrow sprite. */
    static void arrow(GuiGraphics graphics, String id, int x, int y) {
        graphics.blit(ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/icons/arrow-" + id + ".png"),
                x, y, 0, 0, 16, 16, 16, 16);
    }

    static void blackPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANELS, x, y, width, height, 116, 4, 32, 32, 2);
    }

    static void grayPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANELS, x, y, width, height, 78, 4, 32, 32, 2);
    }

    static void tintedPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANELS, x, y, width, height, 154, 4, 32, 32, 2);
    }

    static void slot(GuiGraphics graphics, int x, int y) {
        graphics.blit(SLOTS, x, y, 4, 4, 18, 18, 256, 256);
    }

    static void outline(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANELS, x, y, width, height, 76, 40, 16, 16, 2);
    }

    static void tab(GuiGraphics graphics, int x, int y, int width, int height, boolean selected) {
        nineSlice(graphics, PANELS, x, y, width, height, selected ? 56 : 30, 70, 20, 20, 6);
    }

    static void tabOutline(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANELS, x, y, width, height, 56, 96, 20, 20, 6);
    }

    static void tabSolid(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANELS, x, y, width, height, 82, 96, 20, 20, 6);
    }

    static void scrollTrack(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, CONTROLS, x, y, width, height, 64, 30, 11, 14, 2);
    }

    static void scrollThumb(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered) {
        nineSlice(graphics, CONTROLS, x, y, width, height, hovered ? 82 : 64, 30, 11, 14, 2);
    }

    static void searchField(GuiGraphics graphics, int x, int y, int width, int height) {
        grayPanel(graphics, x, y, width, height);
        outline(graphics, x, y, width, height);
    }

    /** 1.12 validator sprites, rendered by ControlSlot behind an empty accepted slot. */
    static void validator(GuiGraphics graphics, String id, int x, int y) {
        graphics.blit(ResourceLocation.fromNamespaceAndPath("binniegenetics", "textures/items/validator/" + id + ".0.png"),
                x, y, 0, 0, 16, 16, 16, 16);
    }

    /** Implementation of ControlSequencerProgress's deterministic 65-base readout. */
    static void sequencerReadout(GuiGraphics graphics, Font font, int x, int y, ItemStack target, int progress, int maxProgress) {
        if (target.isEmpty()) return;
        java.util.Random random = new java.util.Random(target.getHoverName().getString().length());
        float complete = maxProgress <= 0 ? 0.0F : Math.min(1.0F, (float) progress / maxProgress);
        String[] bases = {"A", "T", "G", "C"};
        int[] colors = {0x55FF55, 0xFF55FF, 0x55FFFF, 0xFF5555};
        for (int index = 0; index < 65; index++) {
            int base = random.nextInt(4);
            boolean decoded = random.nextFloat() < complete;
            int column = index % 13;
            int row = index / 13;
            String value = decoded ? bases[base] : "?";
            graphics.drawString(font, value, x + 4 + column * 7, y + 4 + row * 9, decoded ? colors[base] : 0x777777, false);
        }
    }

    /**
      * The 1.12 implementation {@code ControlSlotCharge}: a 4x18 black well with the original
      * vertical charge sprite, filled from its lower edge.
      */
    static void charge(GuiGraphics graphics, int x, int y, int milliCharge, int color) {
        blackPanel(graphics, x, y, 4, 18);
        int fill = Math.clamp(16 * milliCharge / 10_000, 0, 16);
        if (fill > 0) {
            graphics.blit(SLOTS, x + 1, y + 17 - fill, 2, 26 + 16 - fill, 2, fill, 256, 256);
            if (color != 0xFFFFFFFF) {
                graphics.fill(x + 1, y + 17 - fill, x + 3, y + 17, color & 0x00FFFFFF | 0xA0000000);
            }
        }
    }

    /** Draws tank chrome with the registered still sprite, tiled and clipped to the fill level. */
    static void tank(GuiGraphics graphics, int x, int y, FluidStack stack, int capacity) {
        tank(graphics, x, y, stack, capacity, false);
    }

    /**
      * Direct 1.12 {@code ControlLiquidTank} orientation: Polymeriser alone
      * has the two 60x18 horizontal frames; all other laboratory tanks are
      * the normal 18x60 frame.
      */
    static void tank(GuiGraphics graphics, int x, int y, FluidStack stack, int capacity, boolean horizontal) {
        int width = horizontal ? 60 : 18;
        int height = horizontal ? 18 : 60;
        int frameU = horizontal ? 56 : 8;
        int overlayU = horizontal ? 56 : 32;
        int fluidHeight = horizontal ? 16 : 58;
        graphics.blit(SLOTS, x, y, frameU, 28, width, height, 256, 256);
        int fill = stack.isEmpty() || capacity <= 0 ? 0 : Math.min(fluidHeight, fluidHeight * stack.getAmount() / capacity);
        if (fill > 0) {
            IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(stack.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(extension.getStillTexture(stack));
            int tint = extension.getTintColor(stack);
            graphics.enableScissor(x + 1, y + height - 1 - fill, x + width - 1, y + height - 1);
            graphics.setColor(((tint >>> 16) & 0xFF) / 255.0F, ((tint >>> 8) & 0xFF) / 255.0F,
                    (tint & 0xFF) / 255.0F, ((tint >>> 24) & 0xFF) / 255.0F);
            for (int tileY = 0; tileY < fluidHeight; tileY += 16) {
                for (int tileX = 0; tileX < width - 2; tileX += 16) {
                    graphics.blit(x + 1 + tileX, y + 1 + tileY, 0, 16, 16, sprite);
                }
            }
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.disableScissor();
        }
        graphics.blit(SLOTS, x, y, overlayU, horizontal ? 52 : 28, width, height, 256, 256);
    }

    static void energy(GuiGraphics graphics, int x, int y, int width, int height, int stored, int capacity) {
        nineSlice(graphics, CONTROLS, x, y, width, height, 40, 50, 12, 14, 1);
        float percent = capacity <= 0 ? 0.0F : Math.clamp((float) stored / capacity, 0.0F, 1.0F);
        int red = percent > 0.5F ? Math.round(255.0F * (2.0F - 2.0F * percent)) : 255;
        int green = percent > 0.5F ? 255 : Math.round(510.0F * percent);
        graphics.setColor(red / 255.0F, green / 255.0F, 0.0F, 1.0F);
        boolean horizontal = width > height;
        int fill = horizontal ? Math.round((width - 2) * percent) : Math.round((height - 2) * percent);
        if (fill > 0) {
            if (horizontal) nineSlice(graphics, CONTROLS, x + 1, y + 1, fill, Math.max(1, height - 2), 4, 50, 12, 14, 1);
            else nineSlice(graphics, CONTROLS, x + 1, y + height - 1 - fill, Math.max(1, width - 2), fill, 4, 50, 12, 14, 1);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        nineSlice(graphics, CONTROLS, x, y, width, height, 22, 50, 12, 14, 1);
    }

    static void errorState(GuiGraphics graphics, int x, int y) {
        graphics.blit(CONTROLS, x, y, 44, 30, 14, 14, 256, 256);
    }

    /**
      * Implementation of the 1.12 {@code ControlSplicerProgress}: coloured DNA
      * rungs travel through the centre; no unreferenced panel texture is used.
      */
    static void splicerProgress(GuiGraphics graphics, int x, int y, int width, int height, int progress, int maxProgress) {
        float completion = maxProgress <= 0 ? 0.0F : Math.min(1.0F, (float) progress / maxProgress);
        float count = Math.max(1.0F, maxProgress / 12.0F);
        int spacing = 10;
        float origin = -((count - 1.0F) * spacing) - width / 2.0F;
        float distance = (count - 1.0F) * spacing + width;
        int offset = Math.round(origin + distance * completion);
        int[] colors = {0x00990000, 0x00007700, 0x000000FF, 0x00996600};
        for (int index = 0; index < count; index++) {
            int rung = offset + index * spacing;
            if (rung <= -width / 2.0F || rung >= width / 2.0F) continue;
            float visibility = (float) Math.sqrt(1.0F - Math.abs(2.0F * rung / width));
            int inset = Math.round(((height - 16.0F) / 2.0F) * visibility);
            int alpha = Math.round(255.0F * visibility) << 24;
            int c1 = alpha | colors[Math.abs((int) (13.0 * Math.sin(index) + 48.0 * Math.cos(index) + 25.0 * Math.sin(7 * index))) % 4];
            int c2 = alpha | colors[Math.abs((int) (23.0 * Math.sin(index) + 28.0 * Math.cos(index) + 15.0 * Math.sin(7 * index))) % 4];
            int c3 = alpha | colors[Math.abs((int) (43.0 * Math.sin(index) + 38.0 * Math.cos(index) + 55.0 * Math.sin(7 * index))) % 4];
            int c4 = alpha | colors[Math.abs((int) (3.0 * Math.sin(index) + 18.0 * Math.cos(index) + 35.0 * Math.sin(7 * index))) % 4];
            int rungX = x + width / 2 + rung;
            graphics.fill(rungX, y + inset, rungX + 4, y + inset + 4, c1);
            graphics.fill(rungX, y + inset + 4, rungX + 4, y + inset + 8, rung < 0 ? c2 : c3);
            graphics.fill(rungX, y + height - inset - 8, rungX + 4, y + height - inset - 4, rung < 0 ? c3 : c2);
            graphics.fill(rungX, y + height - inset - 4, rungX + 4, y + height - inset, c4);
        }
    }

    static void progress(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x, int y,
            int baseU, int baseV,
            int activeU, int activeV,
            int width, int height,
            int progress, int maxProgress
    ) {
        graphics.blit(texture, x, y, baseU, baseV, width, height, 256, 256);
        int activeWidth = maxProgress <= 0 ? 0 : Math.min(width, width * progress / maxProgress);
        if (activeWidth > 0) {
            graphics.blit(texture, x, y, activeU, activeV, activeWidth, height, 256, 256);
        }
    }

    private static void nineSlice(
            GuiGraphics graphics, ResourceLocation texture,
            int x, int y, int width, int height,
            int sourceU, int sourceV, int sourceWidth, int sourceHeight, int padding
    ) {
        if (width < padding * 2 || height < padding * 2) {
            graphics.blit(texture, x, y, sourceU, sourceV, width, height, 256, 256);
            return;
        }
        int sourceCenterWidth = sourceWidth - padding * 2;
        int sourceCenterHeight = sourceHeight - padding * 2;
        graphics.blit(texture, x, y, sourceU, sourceV, padding, padding, 256, 256);
        graphics.blit(texture, x + width - padding, y, sourceU + sourceWidth - padding, sourceV, padding, padding, 256, 256);
        graphics.blit(texture, x, y + height - padding, sourceU, sourceV + sourceHeight - padding, padding, padding, 256, 256);
        graphics.blit(texture, x + width - padding, y + height - padding, sourceU + sourceWidth - padding, sourceV + sourceHeight - padding, padding, padding, 256, 256);
        for (int currentX = padding; currentX < width - padding; ) {
            int tileWidth = Math.min(sourceCenterWidth, width - padding - currentX);
            graphics.blit(texture, x + currentX, y, sourceU + padding, sourceV, tileWidth, padding, 256, 256);
            graphics.blit(texture, x + currentX, y + height - padding, sourceU + padding, sourceV + sourceHeight - padding, tileWidth, padding, 256, 256);
            for (int currentY = padding; currentY < height - padding; ) {
                int tileHeight = Math.min(sourceCenterHeight, height - padding - currentY);
                graphics.blit(texture, x + currentX, y + currentY, sourceU + padding, sourceV + padding, tileWidth, tileHeight, 256, 256);
                currentY += tileHeight;
            }
            currentX += tileWidth;
        }
        for (int currentY = padding; currentY < height - padding; ) {
            int tileHeight = Math.min(sourceCenterHeight, height - padding - currentY);
            graphics.blit(texture, x, y + currentY, sourceU, sourceV + padding, padding, tileHeight, 256, 256);
            graphics.blit(texture, x + width - padding, y + currentY, sourceU + sourceWidth - padding, sourceV + padding, padding, tileHeight, 256, 256);
            currentY += tileHeight;
        }
    }
}
