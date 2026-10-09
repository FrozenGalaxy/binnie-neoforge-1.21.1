package binnie.trees.registry;

import binnie.core.data.DesignData;
import binnie.botany.registry.BotanyWorldContent;
import binnie.core.registry.ModuleRegistry;
import binnie.design.api.DesignedBlockKind;
import binnie.design.block.DesignedBlock;
import binnie.design.block.DesignedBlockEntity;
import binnie.design.block.DesignedBlockItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BinnieTreesDesignContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("binnietrees");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("binnietrees");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "binnietrees");

    public static final DeferredBlock<DesignedBlock> PATTERNED_WOOD_BLOCK = BLOCKS.register("patterned_wood", () ->
            new DesignedBlock(BlockBehaviour.Properties.of().strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava(), DesignedBlockKind.WOOD));
    public static final DeferredBlock<DesignedBlock> PATTERNED_PANEL_BLOCK = BLOCKS.register("patterned_panel", () ->
            new DesignedBlock(BlockBehaviour.Properties.of().strength(1.2F, 2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava(), DesignedBlockKind.PANEL));
    public static final DeferredBlock<DesignedBlock> PATTERNED_GLASS_BLOCK = BLOCKS.register("patterned_glass", () ->
            new DesignedBlock(BlockBehaviour.Properties.of().strength(0.3F).sound(SoundType.GLASS).noOcclusion(), DesignedBlockKind.GLASS));

    public static final DeferredItem<DesignedBlockItem> PATTERNED_WOOD = item(PATTERNED_WOOD_BLOCK, DesignedBlockKind.WOOD);
    public static final DeferredItem<DesignedBlockItem> PATTERNED_PANEL = item(PATTERNED_PANEL_BLOCK, DesignedBlockKind.PANEL);
    public static final DeferredItem<DesignedBlockItem> PATTERNED_GLASS = item(PATTERNED_GLASS_BLOCK, DesignedBlockKind.GLASS);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DesignedBlockEntity>> DESIGNED_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("designed_block", () -> BlockEntityType.Builder.of(DesignedBlockEntity::new,
                    PATTERNED_WOOD_BLOCK.get(), PATTERNED_PANEL_BLOCK.get(), PATTERNED_GLASS_BLOCK.get(),
                    BotanyWorldContent.CERAMIC_TILE.get()).build(null));

    private BinnieTreesDesignContent() {}

    private static DeferredItem<DesignedBlockItem> item(DeferredBlock<DesignedBlock> block, DesignedBlockKind kind) {
        return ITEMS.registerItem(block.getId().getPath(), properties -> new DesignedBlockItem(block.get(), properties, kind));
    }

    public static DeferredItem<DesignedBlockItem> itemFor(DesignedBlockKind kind) {
        return switch (kind) {
            case WOOD -> PATTERNED_WOOD;
            case PANEL -> PATTERNED_PANEL;
            case GLASS -> PATTERNED_GLASS;
            case CERAMIC -> throw new IllegalArgumentException("Ceramic designed items are owned by Botany");
        };
    }

    public static DesignData example(DesignedBlockKind kind, String design, Item primary, Item secondary) {
        return new DesignData(design, design, BuiltInRegistries.ITEM.getKey(primary), BuiltInRegistries.ITEM.getKey(secondary), 0, false);
    }

    public static void includeCreativeItems(ModuleRegistry registry) {
        registry.addDisplayItems(output -> {
            // CreativeModeTab.Output only accepts canonical one-item stacks.
            output.accept(PATTERNED_WOOD.get().create(example(DesignedBlockKind.WOOD, "diamond", Items.OAK_PLANKS, Items.BIRCH_PLANKS), 1));
            output.accept(PATTERNED_PANEL.get().create(example(DesignedBlockKind.PANEL, "barred", Items.SPRUCE_PLANKS, Items.CHERRY_PLANKS), 1));
            output.accept(PATTERNED_GLASS.get().create(example(DesignedBlockKind.GLASS, "ringed", Items.GLASS, Items.WHITE_STAINED_GLASS), 1));
        });
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
