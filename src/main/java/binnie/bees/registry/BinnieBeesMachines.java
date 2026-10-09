package binnie.bees.registry;

import binnie.core.machine.MachineModuleRegistry;
import net.neoforged.bus.api.IEventBus;

public class BinnieBeesMachines {
    public static final MachineModuleRegistry REGISTRY = new MachineModuleRegistry(
            "binniebees", BinnieBeesContent.CONTENT,
            "alveary_mutator", "alveary_frame", "alveary_rain_shield", "alveary_lighting", "alveary_stimulator", "alveary_hatchery", "alveary_transmission");
    private BinnieBeesMachines() {}
    public static void register(IEventBus bus) { REGISTRY.register(bus); }
}
