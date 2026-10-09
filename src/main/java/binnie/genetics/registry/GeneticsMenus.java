package binnie.genetics.registry;

import binnie.genetics.BinnieGenetics;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.genetics.menu.PunnettSquareMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class GeneticsMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BinnieGenetics.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<GeneticKnowledgeMenu>> KNOWLEDGE = MENUS.register(
            "knowledge", () -> new MenuType<>((IContainerFactory<GeneticKnowledgeMenu>) GeneticKnowledgeMenu::fromNetwork,
                    FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<PunnettSquareMenu>> PUNNETT = MENUS.register(
            "punnett_square", () -> new MenuType<>((IContainerFactory<PunnettSquareMenu>) PunnettSquareMenu::fromNetwork,
                    FeatureFlags.DEFAULT_FLAGS));

    private GeneticsMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
