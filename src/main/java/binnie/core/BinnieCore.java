package binnie.core;

import binnie.core.registry.CoreContent;
import binnie.core.registry.BinnieDataComponents;
import binnie.core.registry.CoreRegistries;
import binnie.core.machine.MachineRegistries;
import binnie.core.network.CoreNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(BinnieCore.MOD_ID)
public final class BinnieCore {
    public static final String MOD_ID = "binniecore";
    public BinnieCore(IEventBus modBus, ModContainer container) {
        BinnieDataComponents.register(modBus);
        MachineRegistries.register(modBus);
        CoreRegistries.register(modBus);
        CoreContent.register(modBus);
        modBus.addListener(CoreNetwork::register);
        modBus.addListener(BinnieCore::registerGameTests);
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        try {
            event.register(Class.forName("binnie.core.gametest.CoreGameTests"));
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Binnie Core GameTests are enabled but the development class is absent", exception);
        }
    }
}
