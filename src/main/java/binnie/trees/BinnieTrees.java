package binnie.trees;

import binnie.trees.registry.BinnieTreesContent;
import binnie.trees.registry.BinnieTreeWoodContent;
import binnie.trees.registry.BinnieTreesMachines;
import binnie.trees.registry.BinnieTreesWorldContent;
import binnie.trees.registry.BinnieTreesFluids;
import binnie.trees.registry.BinnieTreesDesignContent;
import binnie.trees.machine.BinnieTreesMachineProcesses;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(BinnieTrees.MOD_ID)
public final class BinnieTrees {
    public static final String MOD_ID = "binnietrees";
    public BinnieTrees(IEventBus modBus, ModContainer container) {
        BinnieTreeWoodContent.includeCreativeItems(BinnieTreesContent.CONTENT);
        BinnieTreesWorldContent.register(modBus);
        BinnieTreeWoodContent.register(modBus);
        BinnieTreesDesignContent.register(modBus);
        BinnieTreesFluids.register(modBus);
        BinnieTreesMachines.register(modBus);
        BinnieTreesMachineProcesses.register();
        BinnieTreesContent.register(modBus);
        modBus.addListener(BinnieTrees::commonSetup);
        modBus.addListener(BinnieTrees::registerGameTests);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // ForestryCE 1.21.1 replaced FuelManager/EngineBronzeFuel with the DataMaps API.
            // Engine fuels are now registered via datapack JSON or a DataMapProvider.
            // The resin biogas fuel entry is provided through the data generator instead.
        });
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        try {
            event.register(Class.forName("binnie.trees.gametest.BinnieTreesMachineGameTests"));
            event.register(Class.forName("binnie.trees.gametest.BinnieTreesWorldGameTests"));
            event.register(Class.forName("binnie.design.gametest.DesignWorldGameTests"));
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Binnie's Trees/Design GameTests are enabled but a development class is absent", exception);
        }
    }
}
