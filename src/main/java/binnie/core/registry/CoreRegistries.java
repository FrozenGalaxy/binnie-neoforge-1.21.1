package binnie.core.registry;

import binnie.core.BinnieCore;
import binnie.core.menu.FieldKitMenu;
import binnie.core.menu.GenesisMenu;
import binnie.core.storage.CompartmentBlock;
import binnie.core.storage.CompartmentBlockEntity;
import binnie.core.storage.CompartmentBlockItem;
import binnie.core.storage.CompartmentMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registrations for Core-owned blocks, portable menus and capabilities. */
public final class CoreRegistries {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BinnieCore.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BinnieCore.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BinnieCore.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BinnieCore.MOD_ID);

    public static final DeferredBlock<CompartmentBlock> COMPARTMENT_BLOCK = BLOCKS.register("compartment", () ->
            new CompartmentBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD)));
    public static final DeferredItem<CompartmentBlockItem> COMPARTMENT_ITEM = ITEMS.registerItem("compartment", properties ->
            new CompartmentBlockItem(COMPARTMENT_BLOCK.get(), properties));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompartmentBlockEntity>> COMPARTMENT_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("compartment", () -> BlockEntityType.Builder.of(
                    CompartmentBlockEntity::new, COMPARTMENT_BLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<CompartmentMenu>> COMPARTMENT_MENU = MENUS.register(
            "compartment", () -> new MenuType<>((IContainerFactory<CompartmentMenu>) CompartmentMenu::fromNetwork,
                    FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<FieldKitMenu>> FIELD_KIT_MENU = MENUS.register(
            "field_kit", () -> new MenuType<>((IContainerFactory<FieldKitMenu>) FieldKitMenu::fromNetwork,
                    FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<GenesisMenu>> GENESIS_MENU = MENUS.register(
            "genesis", () -> new MenuType<>((IContainerFactory<GenesisMenu>) GenesisMenu::fromNetwork,
                    FeatureFlags.DEFAULT_FLAGS));

    private CoreRegistries() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(CoreRegistries::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, COMPARTMENT_BLOCK_ENTITY.get(),
                (compartment, side) -> compartment.externalInventory());
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> ((binnie.core.liquid.BinnieFluidContainerItem) stack.getItem()).createFluidHandler(stack),
                CoreContent.GLASS.get(), CoreContent.CYLINDER.get());
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> ((binnie.trees.item.GlasswareItem) stack.getItem()).createFluidHandler(stack),
                binnie.trees.registry.BinnieTreesContent.GLASSWARE.get());
    }
}
