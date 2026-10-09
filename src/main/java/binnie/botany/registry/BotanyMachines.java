package binnie.botany.registry;

import binnie.core.machine.MachineModuleRegistry;
import net.neoforged.bus.api.IEventBus;

public class BotanyMachines {
    public static final MachineModuleRegistry REGISTRY = new MachineModuleRegistry("binniebotany", BotanyContent.CONTENT, "tileworker");
    private BotanyMachines() {}
    public static void register(IEventBus bus) { REGISTRY.register(bus); }
}
