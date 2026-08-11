package binnie.design;

import binnie.design.registry.DesignContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(BinnieDesign.MOD_ID)
public final class BinnieDesign {
    public static final String MOD_ID = "binniedesign";
    public BinnieDesign(IEventBus modBus, ModContainer container) {
        DesignContent.register(modBus);
    }
}
