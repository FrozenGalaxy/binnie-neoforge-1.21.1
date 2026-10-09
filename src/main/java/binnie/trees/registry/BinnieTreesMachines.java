package binnie.trees.registry;

import binnie.core.machine.MachineModuleRegistry;
import net.neoforged.bus.api.IEventBus;

public class BinnieTreesMachines {
    public static final MachineModuleRegistry REGISTRY = new MachineModuleRegistry(
            "binnietrees", BinnieTreesContent.CONTENT,
            "lumbermill", "woodworker", "panelworker", "press", "brewery", "distillery", "glassworker");
    private BinnieTreesMachines() {}
    public static void register(IEventBus bus) { REGISTRY.register(bus); }
}
