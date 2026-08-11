package binnie.bees.alveary;

import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Values from AlvearySimulatorCircuitType. */
final class StimulatorCircuitParityTest {
    @Test
    void programsUseTheirOriginalForestryElectronTubesAndPowerCosts() {
        assertEquals(Map.of(
                StimulatorProgram.LOW_VOLTAGE, "iron_electron_tube",
                StimulatorProgram.HIGH_VOLTAGE, "diamantine_electron_tube",
                StimulatorProgram.PLANT, "lapis_electron_tube",
                StimulatorProgram.DEATH, "obsidian_electron_tube",
                StimulatorProgram.LIFE, "apatine_electron_tube",
                StimulatorProgram.NETHER, "blazing_electron_tube",
                StimulatorProgram.MUTATION, "golden_electron_tube",
                StimulatorProgram.INHIBITOR, "tin_electron_tube",
                StimulatorProgram.TERRITORY, "bronze_electron_tube"
        ), java.util.Arrays.stream(StimulatorProgram.values()).collect(java.util.stream.Collectors.toMap(
                program -> program, StimulatorProgram::electronTubeId)));
        assertEquals(10, StimulatorProgram.LOW_VOLTAGE.powerUsage());
        assertEquals(20, StimulatorProgram.HIGH_VOLTAGE.powerUsage());
        assertEquals(15, StimulatorProgram.NETHER.powerUsage());
        assertEquals(15, StimulatorProgram.MUTATION.powerUsage());
    }

    @Test
    void programsRetainTheirSourceModifiers() {
        assertEquals(1.5F, StimulatorProgram.LOW_VOLTAGE.modifyProductionSpeed(null, 1F));
        assertEquals(2.5F, StimulatorProgram.HIGH_VOLTAGE.modifyProductionSpeed(null, 1F));
        assertEquals(1.5F, StimulatorProgram.PLANT.modifyPollination(null, 1F));
    // Current Forestry exposes aging rate instead of the old lifespan multiplier.
        assertEquals(1.25F, StimulatorProgram.DEATH.modifyAging(null, null, 1F));
        assertEquals(1F / 1.5F, StimulatorProgram.LIFE.modifyAging(null, null, 1F));
        assertEquals(1.5F, StimulatorProgram.MUTATION.modifyMutationChance(null, null, null, 1F));
        assertEquals(new Vec3i(4, 4, 4), StimulatorProgram.INHIBITOR.modifyTerritory(null, new Vec3i(10, 10, 10)));
        assertEquals(0.9F, StimulatorProgram.INHIBITOR.modifyProductionSpeed(null, 1F));
        assertEquals(new Vec3i(15, 15, 15), StimulatorProgram.TERRITORY.modifyTerritory(null, new Vec3i(10, 10, 10)));
        assertEquals(1.5F, StimulatorProgram.NETHER.modifyGeneticDecay(null, 1F));
        assertEquals(true, StimulatorProgram.NETHER.isHellish());
    }
}
