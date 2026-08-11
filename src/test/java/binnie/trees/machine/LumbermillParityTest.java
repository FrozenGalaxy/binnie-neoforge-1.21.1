package binnie.trees.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LumbermillParityTest {
    @Test
    void sourceProcessContractIsRepresentedByTheMachinePlan() {
        assertEquals(30, BinnieTreesMachineProcesses.lumbermillDurationForAudit());
        assertEquals(900, BinnieTreesMachineProcesses.lumbermillEnergyForAudit());
        assertEquals(10, BinnieTreesMachineProcesses.lumbermillWaterDrainForAudit());
        assertEquals(5, BinnieTreesMachineProcesses.lumbermillWaterThresholdForAudit());
    }
}
