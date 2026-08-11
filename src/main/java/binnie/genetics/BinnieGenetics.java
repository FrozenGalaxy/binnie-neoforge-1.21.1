package binnie.genetics;

import binnie.genetics.registry.GeneticsContent;
import binnie.genetics.registry.GeneticsMachines;
import binnie.genetics.registry.GeneticsMenus;
import binnie.genetics.registry.GeneticsFluids;
import binnie.genetics.machine.GeneticsMachineProcesses;
import binnie.genetics.network.GeneticsNetwork;
import binnie.genetics.recipe.GeneticsRecipeRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(BinnieGenetics.MOD_ID)
public final class BinnieGenetics {
    public static final String MOD_ID = "binniegenetics";
    public BinnieGenetics(IEventBus modBus, ModContainer container) {
        GeneticsRecipeRegistries.register(modBus);
        GeneticsMachineProcesses.register();
        GeneticsFluids.register(modBus);
        GeneticsMachines.register(modBus);
        GeneticsMenus.register(modBus);
        GeneticsContent.register(modBus);
        modBus.addListener(GeneticsNetwork::register);
    }
}
