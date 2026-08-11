package binnie.botany;

import binnie.botany.registry.BotanyContent;
import binnie.botany.registry.BotanyMachines;
import binnie.botany.registry.BotanyWorldContent;
import binnie.botany.machine.BotanyMachineProcesses;
import binnie.botany.recipe.BotanyRecipeRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

@Mod(BinnieBotany.MOD_ID)
public final class BinnieBotany {
    public static final String MOD_ID = "binniebotany";
    public BinnieBotany(IEventBus modBus, ModContainer container) {
        BotanyWorldContent.register(modBus);
        BotanyRecipeRegistries.register(modBus);
        BotanyMachines.register(modBus);
        BotanyMachineProcesses.register();
        BotanyContent.register(modBus);
        modBus.addListener(BinnieBotany::registerGameTests);
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        try {
            event.register(Class.forName("binnie.botany.gametest.BotanyWorldGameTests"));
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Botany GameTests are enabled but their development class is absent", exception);
        }
    }
}
