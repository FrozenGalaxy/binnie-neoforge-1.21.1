package binnie.bees;

import binnie.bees.registry.BinnieBeesContent;
import binnie.bees.registry.BinnieBeesWorldContent;
import binnie.bees.forestry.MarbleBeeAvailabilityReloadListener;
import binnie.bees.menu.BinnieBeesMenus;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(BinnieBees.MOD_ID)
public final class BinnieBees {
    public static final String MOD_ID = "binniebees";
    public static final Logger LOGGER = LogUtils.getLogger();
    public BinnieBees(IEventBus modBus, ModContainer container) {
        BinnieBeesWorldContent.register(modBus);
        BinnieBeesMenus.register(modBus);
        BinnieBeesContent.register(modBus);
        NeoForge.EVENT_BUS.addListener(MarbleBeeAvailabilityReloadListener::addReloadListener);
    }
}
