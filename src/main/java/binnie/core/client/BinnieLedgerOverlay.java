package binnie.core.client;

import forestry.api.client.ForestrySprites;
import forestry.api.client.IForestryClientApi;
import forestry.api.core.ForestryError;
import binnie.core.machine.MachineProcessError;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
  * Forestry-style side ledgers for Binnie screens.
  *
  * <p>Forestry's ledger classes are implementation classes tied to
  * {@code GuiForestry}. This adapter reproduces their presentation
  * contract while allowing Binnie's existing container screens to opt in by
  * composition. It uses Forestry's ledger chrome and GUI sprite atlas directly.
  */
public final class BinnieLedgerOverlay {
    public enum Side { LEFT, RIGHT }

    private static final ResourceLocation RIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "forestry", "textures/gui/ledger.png");
    private static final ResourceLocation LEFT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "forestry", "textures/gui/ledger_left.png");
    private static final int CLOSED_SIZE = 24;
    private static final int MAX_WIDTH = 124;
    private static final float PIXELS_PER_MILLISECOND = 8.0F / 16.667F;

    /** A ledger's visible presentation and its expanded text. */
    public record Entry(String id, Side side, ResourceLocation sprite, int tint,
                        Component tooltip, Component title, List<Component> detail) {
        public Entry {
            detail = List.copyOf(detail);
        }
    }

    private final Map<String, State> states = new HashMap<>();
    private List<PlacedEntry> placedEntries = List.of();
    private long lastUpdateMillis;

    /** Draws all visible ledgers and advances their Forestry-matched expansion animation. */
    public void render(GuiGraphics graphics, Font font, int left, int top, int guiWidth, int screenWidth,
                       List<Entry> entries) {
        int ledgerWidth = Math.clamp((screenWidth - guiWidth) / 2, CLOSED_SIZE, MAX_WIDTH);
        long now = System.currentTimeMillis();
        float movement = lastUpdateMillis == 0 ? PIXELS_PER_MILLISECOND * 16.667F
                : PIXELS_PER_MILLISECOND * Math.max(0, now - lastUpdateMillis);
        lastUpdateMillis = now;

        List<PlacedEntry> placed = new ArrayList<>();
        for (Side side : Side.values()) {
            int y = top + 8;
            // The caller supplies Forestry's intended ledger order. Sorting by
            // an implementation id made the visible stack arbitrary.
            List<Entry> sideEntries = entries.stream().filter(entry -> entry.side() == side).toList();
            for (Entry entry : sideEntries) {
                State state = states.computeIfAbsent(entry.id(), ignored -> new State());
                int targetHeight = expandedHeight(font, entry, ledgerWidth);
                state.update(movement, ledgerWidth, targetHeight);
                int width = state.width();
                int height = state.height();
                int x = side == Side.RIGHT ? left + guiWidth : left - width;
                PlacedEntry placedEntry = new PlacedEntry(entry, state, x, y, width, height, ledgerWidth, targetHeight);
                placed.add(placedEntry);
                drawLedger(graphics, font, placedEntry);
                y += height;
            }
        }
        placedEntries = List.copyOf(placed);
    }

    /** Handles expansion clicks before the screen processes inventory clicks. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        for (PlacedEntry placed : placedEntries) {
            if (!placed.contains(mouseX, mouseY)) continue;
            boolean opening = !placed.state.open;
            if (opening) {
                for (PlacedEntry other : placedEntries) {
                    if (other.entry.side() == placed.entry.side() && other.state != placed.state) other.state.open = false;
                }
            }
            placed.state.open = opening;
            return true;
        }
        return false;
    }

    /** Draws the compact Forestry-style hover label and reports whether it consumed the hover. */
    public boolean renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        for (PlacedEntry placed : placedEntries) {
            if (placed.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, placed.entry.tooltip(), mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    /** Lets JEI integrations reserve the animated ledger rectangles when needed. */
    public List<Rect2i> extraAreas() {
        return placedEntries.stream().map(placed -> new Rect2i(placed.x, placed.y, placed.width, placed.height)).toList();
    }

    public static Entry owner(String ownerName) {
        return new Entry("owner", Side.RIGHT, ForestrySprites.MISC_ACCESS_SHARED, 0xFFFFFFFF,
                Component.translatable("for.gui.owner").append(": ").append(ownerName),
                Component.translatable("for.gui.owner"), List.of(Component.literal(ownerName)));
    }

    public static Entry hint(String id, Component tooltip, Component title, Component detail) {
        return new Entry("hint." + id, Side.RIGHT, ForestrySprites.MISC_HINT, 0xFFB52AD0,
                tooltip, title, List.of(detail));
    }

    /** Forestry's PowerLedger, adapted for NeoForge FE terminology. */
    public static Entry energy(int stored, int capacity, int maximumInput) {
        Component energy = Component.translatable("gui.binniecore.ledger.energy");
        return new Entry("energy", Side.RIGHT, ForestrySprites.MISC_ENERGY, 0xFFB35C17,
                energy,
                energy,
                List.of(
                        Component.translatable("gui.binniecore.ledger.energy.stored").append(": ").append(formatFe(stored)),
                        Component.translatable("gui.binniecore.ledger.energy.maximum").append(": ").append(formatFe(capacity)),
                        Component.translatable("gui.binniecore.ledger.energy.input").append(": ").append(formatFe(maximumInput))));
    }

    public static Entry error(String id, ResourceLocation sprite, Component tooltip, Component help) {
        return new Entry("error." + id, Side.LEFT, sprite, 0xFFD62727,
                tooltip, tooltip, List.of(help));
    }

    /** Standard Binnie machine ledgers shared by processors, designers and Genetics screens. */
    public static List<Entry> machineEntries(ResourceLocation machineId, Component information, Component tip, String ownerName,
                                             MachineProcessError error, int storedEnergy, int maxEnergy, int maxEnergyInput) {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(hint(machineId.getNamespace() + "." + machineId.getPath(), tip,
                Component.translatable("for.gui.didyouknow").append("?"), information));
        if (ownerName != null && !ownerName.isBlank()) entries.add(owner(ownerName));
        if (maxEnergy > 0) entries.add(energy(storedEnergy, maxEnergy, maxEnergyInput));
        if (error != MachineProcessError.NONE) {
            Component message = Component.translatable(error.translationKey());
            entries.add(error("machine." + machineId.getNamespace() + "." + machineId.getPath() + "."
                    + error.name().toLowerCase(java.util.Locale.ROOT), errorSprite(error), message, message));
        }
        return List.copyOf(entries);
    }

    private static Component formatFe(int value) {
        return Component.literal(String.format(java.util.Locale.ROOT, "%,d FE", Math.max(0, value)));
    }

    private static ResourceLocation errorSprite(MachineProcessError error) {
        return switch (error) {
            case NO_POWER -> ForestryError.NO_POWER.getSprite();
            case NO_SPACE, OUTPUT_BLOCKED -> ForestryError.NO_SPACE_INVENTORY.getSprite();
            case NO_SPACE_TANK, OUTPUT_TANK_FULL, TANK_FULL -> ForestryError.NO_SPACE_TANK.getSprite();
            case NO_RECIPE, INVALID_RECIPE -> ForestryError.NO_RECIPE.getSprite();
            case REDSTONE_PAUSED -> ForestryError.DISABLED_BY_REDSTONE.getSprite();
            case NONE -> ForestrySprites.ERROR_UNKNOWN;
            default -> ForestryError.NO_RESOURCE.getSprite();
        };
    }

    private static int expandedHeight(Font font, Entry entry, int maxWidth) {
        int textWidth = Math.max(1, maxWidth - 18);
        int lines = font.split(entry.title(), textWidth).size();
        for (Component line : entry.detail()) lines += font.split(line, textWidth).size();
        return Math.max(CLOSED_SIZE, 16 + lines * font.lineHeight + 4);
    }

    private static void drawLedger(GuiGraphics graphics, Font font, PlacedEntry placed) {
        graphics.setColor(((placed.entry.tint() >> 16) & 0xFF) / 255.0F,
                ((placed.entry.tint() >> 8) & 0xFF) / 255.0F,
                (placed.entry.tint() & 0xFF) / 255.0F, 1.0F);
        drawBackground(graphics, placed.entry.side() == Side.RIGHT ? RIGHT_TEXTURE : LEFT_TEXTURE,
                placed.x, placed.y, placed.width, placed.height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        TextureAtlasSprite sprite = IForestryClientApi.INSTANCE.getTextureManager().getSprite(placed.entry.sprite());
        int iconX = placed.entry.side() == Side.RIGHT ? placed.x + 3 : placed.x + 5;
        graphics.blit(iconX, placed.y + 4, 0, 16, 16, sprite);
        if (placed.width < placed.maxWidth || placed.height < placed.targetHeight) return;

        int textX = placed.entry.side() == Side.RIGHT ? placed.x + 22 : placed.x + 24;
        int y = placed.y + 8;
        y += drawWrapped(graphics, font, placed.entry.title(), textX, y, placed.maxWidth - 18, 0xFFF0D51B, true);
        for (Component line : placed.entry.detail()) {
            y += 2;
            y += drawWrapped(graphics, font, line, placed.x + 12, y, placed.maxWidth - 18, 0xFF101010, false);
        }
    }

    /** Implementation of Forestry Ledger.drawBackground's four texture regions. */
    private static void drawBackground(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        graphics.blit(texture, x, y + 4, 0, 256 - height + 4, 4, height - 4, 256, 256);
        graphics.blit(texture, x + 4, y, 256 - width + 4, 0, width - 4, 4, 256, 256);
        graphics.blit(texture, x, y, 0, 0, 4, 4, 256, 256);
        graphics.blit(texture, x + 4, y + 4, 256 - width + 4, 256 - height + 4,
                width - 4, height - 4, 256, 256);
    }

    private static int drawWrapped(GuiGraphics graphics, Font font, Component text, int x, int y, int width,
                                   int color, boolean shadow) {
        int start = y;
        for (var line : font.split(text, Math.max(1, width))) {
            graphics.drawString(font, line, x, y, color, shadow);
            y += font.lineHeight;
        }
        return y - start;
    }

    private static final class State {
        private boolean open;
        private float currentWidth = CLOSED_SIZE;
        private float currentHeight = CLOSED_SIZE;

        private void update(float movement, int targetWidth, int targetHeight) {
            currentWidth = move(currentWidth, open ? targetWidth : CLOSED_SIZE, movement);
            currentHeight = move(currentHeight, open ? targetHeight : CLOSED_SIZE, movement);
        }

        private static float move(float current, float target, float amount) {
            if (current < target) return Math.min(target, current + amount);
            return Math.max(target, current - amount);
        }

        private int width() { return Math.round(currentWidth); }
        private int height() { return Math.round(currentHeight); }
    }

    private record PlacedEntry(Entry entry, State state, int x, int y, int width, int height,
                               int maxWidth, int targetHeight) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
