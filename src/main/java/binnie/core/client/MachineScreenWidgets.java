package binnie.core.client;

import binnie.core.machine.MachineProcessError;
import binnie.core.machine.MachineRedstoneMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Shared status controls and line-broken machine tooltips. */
public final class MachineScreenWidgets {
    private static final ResourceLocation CONTROLS = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/craftgui-controls.png");
    public static final int REDSTONE_X = 8;
    public static final int REDSTONE_Y = 8;
    public static final int REDSTONE_SIZE = 16;

    private MachineScreenWidgets() {}

    /**
      * Header Help, Info and Owner controls are disabled for the
      * port. Keep the hook so 1.12 screen implementations remain compatible.
      */
    public static void renderHeaderControls(GuiGraphics graphics, int left, int top, int width) {
    // graphics.blit(CONTROLS, left + 8, top + 8, 88, 4, 16, 16, 256, 256); // Help
    // graphics.blit(CONTROLS, left + 30, top + 8, 110, 4, 16, 16, 256, 256); // Info
    // graphics.blit(CONTROLS, left + width - 24, top + 8, 132, 4, 16, 16, 256, 256); // Owner
    }

    /** 1.12 ControlEnergyBar layering: back, tinted glow, then glass. */
    public static void energyBar(GuiGraphics graphics, int x, int y, int width, int height, int stored, int capacity) {
        nineSlice(graphics, x, y, width, height, 40, 50, 12, 14, 1);
        float percent = capacity <= 0 ? 0.0F : Math.clamp((float) stored / capacity, 0.0F, 1.0F);
        int red = percent > 0.5F ? Math.round(255.0F * (2.0F - 2.0F * percent)) : 255;
        int green = percent > 0.5F ? 255 : Math.round(510.0F * percent);
        graphics.setColor(red / 255.0F, green / 255.0F, 0.0F, 1.0F);
    // ControlEnergyBar resizes the full glow area. The final glass
    // layer supplies the one-pixel inset/border; shrinking here leaves an
    // incorrect extra black gutter at full charge.
        int fill = width > height ? Math.round(width * percent) : Math.round(height * percent);
        if (fill > 0) {
            if (width > height) nineSlice(graphics, x, y, fill, height, 4, 50, 12, 14, 1);
            else nineSlice(graphics, x, y + height - fill, width, fill, 4, 50, 12, 14, 1);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        nineSlice(graphics, x, y, width, height, 22, 50, 12, 14, 1);
    }

    private static void nineSlice(GuiGraphics graphics, int x, int y, int width, int height,
                                  int sourceU, int sourceV, int sourceWidth, int sourceHeight, int padding) {
        if (width < padding * 2 || height < padding * 2) {
            graphics.blit(CONTROLS, x, y, sourceU, sourceV, width, height, 256, 256);
            return;
        }
        int centerWidth = sourceWidth - padding * 2;
        int centerHeight = sourceHeight - padding * 2;
        graphics.blit(CONTROLS, x, y, sourceU, sourceV, padding, padding, 256, 256);
        graphics.blit(CONTROLS, x + width - padding, y, sourceU + sourceWidth - padding, sourceV, padding, padding, 256, 256);
        graphics.blit(CONTROLS, x, y + height - padding, sourceU, sourceV + sourceHeight - padding, padding, padding, 256, 256);
        graphics.blit(CONTROLS, x + width - padding, y + height - padding, sourceU + sourceWidth - padding, sourceV + sourceHeight - padding, padding, padding, 256, 256);
        for (int currentX = padding; currentX < width - padding; ) {
            int tileWidth = Math.min(centerWidth, width - padding - currentX);
            graphics.blit(CONTROLS, x + currentX, y, sourceU + padding, sourceV, tileWidth, padding, 256, 256);
            graphics.blit(CONTROLS, x + currentX, y + height - padding, sourceU + padding, sourceV + sourceHeight - padding, tileWidth, padding, 256, 256);
            for (int currentY = padding; currentY < height - padding; ) {
                int tileHeight = Math.min(centerHeight, height - padding - currentY);
                graphics.blit(CONTROLS, x + currentX, y + currentY, sourceU + padding, sourceV + padding, tileWidth, tileHeight, 256, 256);
                currentY += tileHeight;
            }
            currentX += tileWidth;
        }
    }

    public static void redstoneControl(GuiGraphics graphics, Font font, int x, int y,
                                       MachineRedstoneMode mode, boolean powered) {
        boolean allowed = mode.allows(powered);
        graphics.fill(x, y, x + 16, y + 16, 0xff4a4a4a);
        graphics.fill(x + 1, y + 1, x + 15, y + 15, 0xffd9d9d9);
        graphics.fill(x + 2, y + 2, x + 14, y + 14, 0xff686868);
        graphics.renderItem(new ItemStack(Items.REDSTONE_TORCH), x, y);
        if (!allowed) graphics.fill(x + 2, y + 2, x + 14, y + 14, 0x990f0f0f);
    }

    public static List<FormattedCharSequence> helpTooltip(Font font) {
        return lines(font, 140,
                Component.translatable("gui.binniecore.help.title").withStyle(ChatFormatting.AQUA),
                Component.translatable("gui.binniecore.help.desc", "Tab").withStyle(ChatFormatting.GRAY));
    }

    public static List<FormattedCharSequence> errorTooltip(Font font, MachineProcessError error) {
        Component detail = error == MachineProcessError.NONE
                ? Component.translatable("screen.binniecore.machine_error.none")
                : Component.translatable(error.translationKey());
        return lines(font, 176,
                Component.translatable("gui.binniecore.error.title")
                        .withStyle(error == MachineProcessError.NONE ? ChatFormatting.GREEN : ChatFormatting.RED),
                detail.copy().withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.binniecore.error.highlight_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static List<FormattedCharSequence> redstoneTooltip(Font font, MachineRedstoneMode mode, boolean powered) {
        return lines(font, 176,
                Component.translatable("gui.binniecore.redstone.title").withStyle(ChatFormatting.RED),
                mode.displayName().copy().withStyle(ChatFormatting.GRAY),
                Component.translatable(powered
                        ? "gui.binniecore.redstone.signal_present"
                        : "gui.binniecore.redstone.signal_absent").withStyle(ChatFormatting.DARK_GRAY),
                Component.translatable(mode.allows(powered)
                        ? "gui.binniecore.redstone.allowed"
                        : "gui.binniecore.redstone.blocked")
                        .withStyle(mode.allows(powered) ? ChatFormatting.GREEN : ChatFormatting.RED),
                Component.translatable("gui.binniecore.redstone.click").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static List<FormattedCharSequence> lines(Font font, int width, Component... components) {
        ArrayList<FormattedCharSequence> result = new ArrayList<>();
        for (Component component : components) result.addAll(font.split(component, width));
        return List.copyOf(result);
    }
}
