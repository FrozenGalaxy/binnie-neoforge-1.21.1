package binnie.botany.block;

import net.minecraft.util.StringRepresentable;

/** 1.12 ceramic-brick pattern order and ids. */
public enum CeramicBrickType implements StringRepresentable {
    TILE("tile"), BRICK("brick"), STRIPE_BRICK("brickstripe"), LARGE_BRICK("bricklarge"),
    SPLIT("split"), CHEQUERED("cheque"), MIXED("mixed"), VERTICAL_BRICK("verticalbrick"),
    VERTICAL_STRIPE_BRICK("verticalbrickstripe"), VERTICAL_LARGE_BRICK("verticalbricklarge");

    private final String id;
    CeramicBrickType(String id) { this.id = id; }
    @Override public String getSerializedName() { return id; }
    public boolean canDouble() { return this != TILE && this != SPLIT && this != CHEQUERED && this != MIXED; }
}
