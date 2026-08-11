package binnie.trees.client.screen;

import binnie.core.client.MachineScreen;
import binnie.core.client.MachineScreenWidgets;
import binnie.core.machine.MachineMenu;
import binnie.core.machine.MachineProcessError;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Shared source-CraftGUI behavior for the four Binnie's Trees processors. */
public abstract class BinnieTreesProcessorScreen extends MachineScreen {
    protected BinnieTreesProcessorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected abstract int errorX();
    protected abstract int errorY();
    protected abstract void highlightOutput(GuiGraphics graphics);
    protected abstract void highlightInput(GuiGraphics graphics);
    protected abstract void highlightTank(GuiGraphics graphics, boolean output);

    @Override
    protected final boolean hasJeiRecipes() {
        return true;
    }

    @Override
    protected final void renderSourceErrorFeedback(GuiGraphics graphics, int mouseX, int mouseY) {
        MachineProcessError error = menu.processError();
        if (error == MachineProcessError.NONE || !inside(mouseX - leftPos, mouseY - topPos, errorX(), errorY(), 14, 14)) return;
        if (error == MachineProcessError.REDSTONE_PAUSED) {
            highlight(graphics, MachineScreenWidgets.REDSTONE_X, MachineScreenWidgets.REDSTONE_Y, 16, 16);
            return;
        }
        if (error == MachineProcessError.NO_POWER) {
            // Energy state is represented by the Forestry Power ledger.
            return;
        }
        if (error == MachineProcessError.NO_SPACE || error == MachineProcessError.OUTPUT_BLOCKED) {
            highlightOutput(graphics);
            return;
        }
        boolean tankError = switch (error) {
            case NO_SPACE_TANK, TANK_FULL, TANK_EMPTY, TANK_DIFFERENT_FLUID,
                    INSUFFICIENT_ETHANOL, INSUFFICIENT_DNA, INSUFFICIENT_BACTERIA,
                    INSUFFICIENT_VECTOR, INSUFFICIENT_LIQUID, OUTPUT_TANK_FULL -> true;
            default -> false;
        };
        if (tankError) {
            boolean output = error == MachineProcessError.NO_SPACE_TANK || error == MachineProcessError.TANK_FULL
                    || error == MachineProcessError.OUTPUT_TANK_FULL || outputForDifferentFluid(error);
            highlightTank(graphics, output);
            return;
        }
        highlightInput(graphics);
    }

    protected boolean outputForDifferentFluid(MachineProcessError error) {
        return false;
    }
}
