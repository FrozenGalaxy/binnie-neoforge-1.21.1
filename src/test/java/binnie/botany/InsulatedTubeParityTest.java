package binnie.botany;

import binnie.botany.farming.BotanyFarmVariants;
import binnie.botany.farming.BotanyTubeVariants;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks in 1.12's one physical tube -> managed and manual circuit recipe mapping. */
final class InsulatedTubeParityTest {
    @Test
    void everyPhysicalTubeExposesItsTwoSourceFarmLayouts() {
        Set<BotanyFarmVariants.Variant> profiles = new HashSet<>();
        for (String tube : BotanyTubeVariants.physicalIds()) {
            var circuits = BotanyTubeVariants.circuitsFor(tube);
            assertEquals(2, circuits.size(), tube);
            assertEquals(1, circuits.stream().filter(BotanyFarmVariants.Variant::manual).count(), tube);
            assertEquals(1, circuits.stream().filter(circuit -> !circuit.manual()).count(), tube);
            for (var circuit : circuits) {
                assertEquals(tube, BotanyTubeVariants.physicalId(circuit), circuit.id());
                profiles.add(circuit);
            }
        }
        assertEquals(Set.copyOf(BotanyFarmVariants.all()), profiles);
    }

    @Test
    void profileOnlyCompatibilityVariantsAreNotPhysicalTubeEffects() {
        for (BotanyFarmVariants.Variant profile : BotanyFarmVariants.all()) {
            assertFalse(BotanyTubeVariants.circuitsFor(profile.id()).size() > 0, profile.id());
        }
        assertTrue(BotanyTubeVariants.circuitsFor("copper_sand").size() == 2);
    }
}
