package binnie.bees.menu;

import binnie.bees.BinnieBees;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BinnieBeesMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BinnieBees.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<AlvearyMenu>> ALVEARY = MENUS.register(
            "alveary_component", () -> new MenuType<>((IContainerFactory<AlvearyMenu>) AlvearyMenu::fromNetwork,
                    FeatureFlags.DEFAULT_FLAGS));

    private BinnieBeesMenus() { }
    public static void register(IEventBus bus) { MENUS.register(bus); }
}
