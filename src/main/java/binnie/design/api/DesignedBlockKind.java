package binnie.design.api;

/** Current dynamic block families backed by the 1.12 Design subsystem. */
public enum DesignedBlockKind {
    WOOD(false),
    PANEL(true),
    GLASS(false),
    CERAMIC(false);

    private final boolean panel;

    DesignedBlockKind(boolean panel) {
        this.panel = panel;
    }

    public boolean isPanel() {
        return panel;
    }
}
