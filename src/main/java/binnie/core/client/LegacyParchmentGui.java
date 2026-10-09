package binnie.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Small renderer for the original Binnie parchment and CraftGUI panel atlases. */
public class LegacyParchmentGui {
    private static final ResourceLocation PUNNETT = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/punnett.png");
    private static final ResourceLocation PANELS = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-panels.png");
    private static final ResourceLocation SLOTS = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-slots.png");

    private LegacyParchmentGui() {}

    public static void parchment(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PUNNETT, x, y, width, height, 0, 0, 160, 160, 32);
    }

    public static void window(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANELS, x, y, width, height, 6, 6, 64, 64, 8);
    }

    public static void blackPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANELS, x, y, width, height, 116, 4, 32, 32, 2);
    }

    public static void outline(GuiGraphics graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANELS, x, y, width, height, 76, 40, 16, 16, 2);
    }

    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.blit(SLOTS, x, y, 4, 4, 18, 18, 256, 256);
    }

    public static void chromosome(GuiGraphics graphics, int x, int y, boolean selected) {
        graphics.blit(PUNNETT, x, y, 160, 36, 16, 16, 256, 256);
        if (selected) {
            graphics.pose().pushPose();
            // The source cross's visual centre sits two pixels above its 16px texture centre.
            graphics.pose().translate(x + 4, y + 2, 0);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.blit(PUNNETT, 0, 0, 160, 52, 16, 16, 256, 256);
            graphics.pose().popPose();
        }
    }

    private static void nineSlice(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h,
                                  int u, int v, int sourceW, int sourceH, int border) {
        int middleW = Math.max(0, w - border * 2);
        int middleH = Math.max(0, h - border * 2);
        int sourceMiddleW = sourceW - border * 2;
        int sourceMiddleH = sourceH - border * 2;
        g.blit(texture, x, y, u, v, border, border, 256, 256);
        g.blit(texture, x + w - border, y, u + sourceW - border, v, border, border, 256, 256);
        g.blit(texture, x, y + h - border, u, v + sourceH - border, border, border, 256, 256);
        g.blit(texture, x + w - border, y + h - border, u + sourceW - border, v + sourceH - border, border, border, 256, 256);
        if (middleW > 0) {
            g.blit(texture, x + border, y, middleW, border, u + border, v, sourceMiddleW, border, 256, 256);
            g.blit(texture, x + border, y + h - border, middleW, border, u + border, v + sourceH - border, sourceMiddleW, border, 256, 256);
        }
        if (middleH > 0) {
            g.blit(texture, x, y + border, border, middleH, u, v + border, border, sourceMiddleH, 256, 256);
            g.blit(texture, x + w - border, y + border, border, middleH, u + sourceW - border, v + border, border, sourceMiddleH, 256, 256);
        }
        if (middleW > 0 && middleH > 0) {
            g.blit(texture, x + border, y + border, middleW, middleH, u + border, v + border,
                    sourceMiddleW, sourceMiddleH, 256, 256);
        }
    }
}
