package binnie.design.registry;

import binnie.core.item.VariantItem;
import binnie.core.registry.ModuleRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

public final class DesignContent {
    public static final ModuleRegistry CONTENT = new ModuleRegistry("binniedesign");
    public static final DeferredItem<VariantItem> DESIGN = CONTENT.variantsHidden("design", "binniedesign.design", binnie.core.catalog.LegacyCatalogs.values("binniedesign.design"));
    public static final DeferredItem<VariantItem> PATTERN = CONTENT.variantsHidden("pattern", "binniedesign.pattern", binnie.core.catalog.LegacyCatalogs.values("binniedesign.pattern"));
    private DesignContent() {}
    public static void register(IEventBus bus) { CONTENT.registerItems(bus); }
}
