package binnie.botany.genetics;

import net.minecraft.resources.ResourceLocation;

/** Immutable color allele behavior object. */
public record FlowerColor(ResourceLocation id, int rgb, int wiltedRgb, boolean dominant) {
}
