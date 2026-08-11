package binnie.genetics.client;

/**
 * Client state for the original WindowAnalyst bottom ControlSlide.
 *
 * <p>The drawer starts fully open so the player inventory is immediately available for moving
 * a specimen and DNA Dye into the two Analyst slots. The first analyzed specimen collapses it,
 * after which the centered handle remains available to reopen it.</p>
 */
final class AnalystInventoryDrawer {
    private static final float STEP = 0.25F;

    private boolean requestedOpen = true;
    private float progress = 1.0F;
    private boolean analyzedLastTick;

    void tick() {
        float target = requestedOpen ? 1.0F : 0.0F;
        if (progress < target) progress = Math.min(target, progress + STEP);
        else if (progress > target) progress = Math.max(target, progress - STEP);
    }

    void toggle() {
        requestedOpen = !requestedOpen;
    }

    void updateAnalyzedSpecimen(boolean analyzed) {
        if (analyzed && !analyzedLastTick) requestedOpen = false;
        analyzedLastTick = analyzed;
    }

    float progress() {
        return progress;
    }

    boolean isOpenRequested() {
        return requestedOpen;
    }

    boolean slotsVisible() {
        return requestedOpen && progress >= 1.0F;
    }

    int top() {
        int travel = binnie.genetics.menu.KnowledgeUiContract.ANALYST_DRAWER_HEIGHT
                - binnie.genetics.menu.KnowledgeUiContract.ANALYST_DRAWER_COLLAPSED_HEIGHT;
        return binnie.genetics.menu.KnowledgeUiContract.ANALYST_DRAWER_Y
                + Math.round((1.0F - progress) * travel);
    }
}
