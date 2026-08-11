package binnie.bees.jei;

import binnie.bees.block.AlvearyPartType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class BinnieBeesJeiCatalogTest {
    @Test
    void onlyRecipeLikeAlvearyOperationGetsCategory() {
        assertEquals(2_400, BinnieBeesJeiPlugin.HATCHERY_ATTEMPT_DENOMINATOR);
        assertEquals(AlvearyPartType.HATCHERY, AlvearyPartType.valueOf("HATCHERY"));
        assertEquals(7, AlvearyPartType.values().length);
    }
}
