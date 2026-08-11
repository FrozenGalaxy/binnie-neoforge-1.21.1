package binnie.core;

import binnie.core.machine.MachineProcessError;
import binnie.core.machine.MachineRedstoneMode;
import binnie.core.storage.CompartmentTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CorePlatformTest {
    @Test
    void compartmentTiersRetainExactLegacyCapacities() {
        assertEquals(100, CompartmentTier.PLAIN.capacity());
        assertEquals(150, CompartmentTier.COPPER.capacity());
        assertEquals(200, CompartmentTier.BRONZE.capacity());
        assertEquals(200, CompartmentTier.IRON.capacity());
        assertEquals(300, CompartmentTier.GOLD.capacity());
        assertEquals(400, CompartmentTier.DIAMOND.capacity());
        for (CompartmentTier tier : CompartmentTier.values()) {
            assertEquals(tier, CompartmentTier.byName(tier.getSerializedName()));
            assertEquals(tier.tabs() * tier.slotsPerTab(), tier.capacity());
        }
    }

    @Test
    void redstoneModesHaveStableBoundedTruthTable() {
        assertTrue(MachineRedstoneMode.IGNORED.allows(false));
        assertTrue(MachineRedstoneMode.IGNORED.allows(true));
        assertFalse(MachineRedstoneMode.REQUIRE_SIGNAL.allows(false));
        assertTrue(MachineRedstoneMode.REQUIRE_SIGNAL.allows(true));
        assertTrue(MachineRedstoneMode.REQUIRE_NO_SIGNAL.allows(false));
        assertFalse(MachineRedstoneMode.REQUIRE_NO_SIGNAL.allows(true));
        assertEquals(MachineRedstoneMode.IGNORED, MachineRedstoneMode.byId(-1));
        assertEquals(MachineRedstoneMode.IGNORED, MachineRedstoneMode.byId(999));
    }

    @Test
    void sharedErrorsAndPersistedModesRejectForeignOrdinals() {
        assertEquals(MachineProcessError.NONE, MachineProcessError.byId(-1));
        assertEquals(MachineProcessError.NONE, MachineProcessError.byId(Integer.MAX_VALUE));
        assertEquals(MachineRedstoneMode.REQUIRE_SIGNAL, MachineRedstoneMode.IGNORED.next());
        assertEquals(MachineRedstoneMode.REQUIRE_NO_SIGNAL, MachineRedstoneMode.REQUIRE_SIGNAL.next());
        assertEquals(MachineRedstoneMode.IGNORED, MachineRedstoneMode.REQUIRE_NO_SIGNAL.next());
    }
}
