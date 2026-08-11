package binnie.core.registry;

import binnie.core.item.VariantItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.Consumer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.ArrayList;
import java.util.List;

public final class ModuleRegistry {
    private final String modId;
    private final DeferredRegister.Items items;
    private final DeferredRegister<CreativeModeTab> tabs;
    private final List<DeferredItem<? extends Item>> simpleItems = new ArrayList<>();
    private final List<DeferredItem<VariantItem>> variantItems = new ArrayList<>();
    private final List<Supplier<? extends ItemLike>> includedItems = new ArrayList<>();
    private final List<Consumer<CreativeModeTab.Output>> displayItemProviders = new ArrayList<>();
    private DeferredHolder<CreativeModeTab, CreativeModeTab> tab;
    private Supplier<ItemStack> iconSupplier;
    private Consumer<CreativeModeTab.Output> displayOverride;
    public ModuleRegistry(String modId) {
        this.modId = modId;
        this.items = DeferredRegister.createItems(modId);
        this.tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, modId);
    }
    public DeferredItem<Item> simple(String id) {
        DeferredItem<Item> item = items.registerSimpleItem(id);
        simpleItems.add(item);
        return item;
    }
    public DeferredItem<VariantItem> variants(String id, String category, List<String> values) {
        return variants(id, category, values, true);
    }
    public DeferredItem<VariantItem> variantsHidden(String id, String category, List<String> values) {
        return variants(id, category, values, false);
    }
    private DeferredItem<VariantItem> variants(String id, String category, List<String> values, boolean display) {
        DeferredItem<VariantItem> item = items.registerItem(id, properties -> new VariantItem(properties, category, values));
        if (display) variantItems.add(item);
        return item;
    }
    public <T extends Item> DeferredItem<T> custom(String id, Function<Item.Properties, T> factory, boolean includeDefaultStack) {
        DeferredItem<T> item = items.registerItem(id, factory);
        if (includeDefaultStack) simpleItems.add(item);
        return item;
    }
    public void include(Supplier<? extends ItemLike> item) {
        includedItems.add(item);
    }
    public void addDisplayItems(Consumer<CreativeModeTab.Output> provider) {
        displayItemProviders.add(provider);
    }
    public void setDisplayItems(Consumer<CreativeModeTab.Output> provider) {
        this.displayOverride = provider;
    }
    public void acceptSimpleItems(CreativeModeTab.Output output) {
        for (DeferredItem<? extends Item> item : simpleItems) output.accept(item.get());
    }
    public void acceptIncludedItems(CreativeModeTab.Output output) {
        for (Supplier<? extends ItemLike> item : includedItems) output.accept(item.get());
    }
    public void acceptVariantItems(CreativeModeTab.Output output) {
        for (DeferredItem<VariantItem> item : variantItems) {
            VariantItem value = item.get();
            for (String variant : value.variants()) output.accept(value.createStack(variant));
        }
    }
    public void acceptAdditionalItems(CreativeModeTab.Output output) {
        for (Consumer<CreativeModeTab.Output> provider : displayItemProviders) provider.accept(output);
    }
    public void acceptAutomaticItems(CreativeModeTab.Output output) {
        acceptSimpleItems(output);
        acceptIncludedItems(output);
        acceptVariantItems(output);
        acceptAdditionalItems(output);
    }
    public void setIcon(Supplier<ItemStack> iconSupplier) {
        this.iconSupplier = iconSupplier;
    }
    public void register(IEventBus bus, String titleKey) {
        if (tab == null) {
            tab = tabs.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable(titleKey))
                    .icon(() -> iconStack())
                    .displayItems((parameters, output) -> {
                        if (displayOverride != null) displayOverride.accept(output);
                        else acceptAutomaticItems(output);
                    }).build());
        }
        items.register(bus);
        tabs.register(bus);
    }
    /** Registers module items without exposing a dedicated creative tab. */
    public void registerItems(IEventBus bus) {
        items.register(bus);
    }
    private ItemStack iconStack() {
        if (iconSupplier != null) {
            ItemStack stack = iconSupplier.get();
            if (!stack.isEmpty()) return stack;
        }
        if (!variantItems.isEmpty()) {
            VariantItem item = variantItems.getFirst().get();
            return item.createStack(item.variants().getFirst());
        }
        return simpleItems.isEmpty() ? ItemStack.EMPTY : new ItemStack(simpleItems.getFirst().get());
    }
}
