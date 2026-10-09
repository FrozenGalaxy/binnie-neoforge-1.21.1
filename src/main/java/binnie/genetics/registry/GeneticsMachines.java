package binnie.genetics.registry;

import binnie.core.machine.MachineModuleRegistry;
import net.neoforged.bus.api.IEventBus;

public class GeneticsMachines {
    public static final MachineModuleRegistry REGISTRY = new MachineModuleRegistry(
            "binniegenetics", GeneticsContent.CONTENT,
            "isolator", "sequencer", "polymeriser", "inoculator", "splicer", "analyser", "genepool", "incubator", "acclimatiser", "laboratory_machine");
    private GeneticsMachines() {}

    /** Maps the original metadata package name to the dedicated current block registry id. */
    public static String runtimeId(String legacyId) {
        return "lab_machine".equals(legacyId) ? "laboratory_machine" : legacyId;
    }

    public static void register(IEventBus bus) { REGISTRY.register(bus); }
}
