package binnie.bees.registry;

import binnie.bees.block.AlvearyPartType;
import binnie.bees.block.BinnieBeeHiveBlock;
import binnie.bees.block.BinnieBeeHiveBlockEntity;
import binnie.bees.block.BinnieBeesAlvearyBlock;
import binnie.bees.block.BinnieBeesAlvearyBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class BinnieBeesWorldContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("binniebees");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("binniebees");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "binniebees");

    private static final Map<AlvearyPartType, DeferredBlock<BinnieBeesAlvearyBlock>> ALVEARY_BLOCKS = new EnumMap<>(AlvearyPartType.class);
    private static final Map<BinnieBeeHiveBlock.HiveType, DeferredBlock<BinnieBeeHiveBlock>> HIVE_BLOCKS = new EnumMap<>(BinnieBeeHiveBlock.HiveType.class);

    static {
        for (AlvearyPartType type : AlvearyPartType.values()) {
            DeferredBlock<BinnieBeesAlvearyBlock> block = BLOCKS.register("alveary_" + type.id(), () -> new BinnieBeesAlvearyBlock(
                    BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.WOOD), type));
            ALVEARY_BLOCKS.put(type, block);
            DeferredItem<BlockItem> item = ITEMS.registerItem("alveary_" + type.id(), p -> new BlockItem(block.get(), p));
            BinnieBeesContent.CONTENT.include(item);
        }
        for (BinnieBeeHiveBlock.HiveType type : BinnieBeeHiveBlock.HiveType.values()) {
            DeferredBlock<BinnieBeeHiveBlock> block = BLOCKS.register(type.id() + "_hive", () -> new BinnieBeeHiveBlock(
                    BlockBehaviour.Properties.of().strength(2.0F, 3.0F).sound(SoundType.STONE).randomTicks(), type));
            HIVE_BLOCKS.put(type, block);
            DeferredItem<BlockItem> item = ITEMS.registerItem(type.id() + "_hive", p -> new BlockItem(block.get(), p));
            BinnieBeesContent.CONTENT.include(item);
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BinnieBeesAlvearyBlockEntity>> ALVEARY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("alveary_component", () -> BlockEntityType.Builder.of(
                    BinnieBeesWorldContent::createAlvearyBlockEntity,
                    ALVEARY_BLOCKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BinnieBeeHiveBlockEntity>> HIVE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("wild_hive", () -> BlockEntityType.Builder.of(
                    BinnieBeesWorldContent::createHiveBlockEntity,
                    HIVE_BLOCKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    private BinnieBeesWorldContent() {}

    private static BinnieBeesAlvearyBlockEntity createAlvearyBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new BinnieBeesAlvearyBlockEntity(ALVEARY_BLOCK_ENTITY.get(), pos, state);
    }

    private static BinnieBeeHiveBlockEntity createHiveBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new BinnieBeeHiveBlockEntity(HIVE_BLOCK_ENTITY.get(), pos, state);
    }

    public static DeferredBlock<BinnieBeeHiveBlock> hive(BinnieBeeHiveBlock.HiveType type) { return HIVE_BLOCKS.get(type); }
    public static DeferredBlock<BinnieBeesAlvearyBlock> alveary(AlvearyPartType type) { return ALVEARY_BLOCKS.get(type); }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(BinnieBeesWorldContent::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ALVEARY_BLOCK_ENTITY.get(),
                (alveary, side) -> alveary.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ALVEARY_BLOCK_ENTITY.get(),
                (alveary, side) -> alveary.getItemHandler(side));
    }
}
