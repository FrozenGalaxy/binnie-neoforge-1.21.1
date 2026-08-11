package binnie.core;

import binnie.core.liquid.BinnieFluidContainerType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BinnieFluidContainerTest {
    @Test
    void sourceContainerRulesKeepBeveragesInGlassAndGeneticsInCylinders() {
        ResourceLocation beverage = ResourceLocation.fromNamespaceAndPath("binnietrees", "beverage");
        ResourceLocation bacteria = ResourceLocation.fromNamespaceAndPath("binniegenetics", "bacteria");
        ResourceLocation growthMedium = ResourceLocation.fromNamespaceAndPath("binniegenetics", "growth_medium");

        assertTrue(BinnieFluidContainerType.GLASS.accepts(beverage, true));
        assertTrue(BinnieFluidContainerType.GLASS.showInCreative(beverage, true));
        assertTrue(BinnieFluidContainerType.CYLINDER.accepts(bacteria, false));
        assertFalse(BinnieFluidContainerType.GLASS.accepts(bacteria, false));
        assertTrue(BinnieFluidContainerType.GLASS.accepts(growthMedium, false));
        assertFalse(BinnieFluidContainerType.GLASS.showInCreative(growthMedium, false));
    }
}
