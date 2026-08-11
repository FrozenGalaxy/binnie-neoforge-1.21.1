package binnie.botany.registry;

import binnie.botany.block.BotanySoilBlock;
import binnie.botany.block.CeramicBlock;
import binnie.botany.block.CeramicBrickBlock;
import binnie.botany.block.CeramicBrickBlockEntity;
import binnie.botany.block.FlowerBlock;
import binnie.botany.block.FlowerBlockEntity;
import binnie.botany.block.GardenPlantBlock;
import binnie.botany.block.SoilQuality;
import binnie.botany.block.StainedGlassBlock;
import binnie.botany.item.StainedGlassItem;
import binnie.botany.item.CeramicItem;
import binnie.botany.item.CeramicBrickItem;
import binnie.botany.item.GardenPlantItem;
import binnie.botany.item.FixedGardenStateItem;
import binnie.design.api.DesignedBlockKind;
import binnie.design.block.DesignedBlock;
import binnie.design.block.DesignedBlockItem;
import binnie.core.data.DesignData;
import binnie.core.catalog.LegacyCatalogs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** Blocks and block entities for Botany's planted genetics and soil gameplay. */
public final class BotanyWorldContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("binniebotany");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("binniebotany");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "binniebotany");

    public static final DeferredBlock<BotanySoilBlock> SOIL = soil("soil", SoilQuality.SOIL);
    public static final DeferredBlock<BotanySoilBlock> LOAM = soil("loam", SoilQuality.LOAM);
    public static final DeferredBlock<BotanySoilBlock> FLOWERBED = soil("flowerbed", SoilQuality.FLOWERBED);
    public static final DeferredItem<BlockItem> SOIL_ITEM = blockItem(SOIL);
    public static final DeferredItem<BlockItem> LOAM_ITEM = blockItem(LOAM);
    public static final DeferredItem<BlockItem> FLOWERBED_ITEM = blockItem(FLOWERBED);
    public static final DeferredBlock<StainedGlassBlock> STAINED_GLASS = BLOCKS.register("stained", () -> new StainedGlassBlock(
            BlockBehaviour.Properties.of().strength(0.3F).sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredItem<StainedGlassItem> STAINED_GLASS_ITEM = ITEMS.registerItem("stained",
            properties -> new StainedGlassItem(STAINED_GLASS.get(), properties));
    public static final DeferredBlock<CeramicBlock> CERAMIC = BLOCKS.register("ceramic", () -> new CeramicBlock(
            BlockBehaviour.Properties.of().strength(1.5F, 6.0F).sound(SoundType.STONE)));
    public static final DeferredItem<CeramicItem> CERAMIC_ITEM = ITEMS.registerItem("ceramic",
            properties -> new CeramicItem(CERAMIC.get(), properties));
    public static final DeferredBlock<CeramicBrickBlock> CERAMIC_BRICK = BLOCKS.register("ceramic_brick", () -> new CeramicBrickBlock(
            BlockBehaviour.Properties.of().strength(1.0F, 5.0F).sound(SoundType.STONE)));
    public static final DeferredItem<CeramicBrickItem> CERAMIC_BRICK_ITEM = ITEMS.registerItem("ceramic_brick",
            properties -> new CeramicBrickItem(CERAMIC_BRICK.get(), properties));
    public static final DeferredBlock<DesignedBlock> CERAMIC_TILE = BLOCKS.register("ceramic_tile", () -> new DesignedBlock(
            BlockBehaviour.Properties.of().strength(1.5F, 6.0F).sound(SoundType.STONE), DesignedBlockKind.CERAMIC));
    public static final DeferredItem<DesignedBlockItem> CERAMIC_TILE_ITEM = ITEMS.registerItem("ceramic_tile",
            properties -> new DesignedBlockItem(CERAMIC_TILE.get(), properties, DesignedBlockKind.CERAMIC));

    public static final DeferredBlock<FlowerBlock> FLOWER_BLOCK = BLOCKS.register("genetic_flower", () -> new FlowerBlock(
            BlockBehaviour.Properties.of().strength(0.0F).sound(SoundType.GRASS).noOcclusion().replaceable()));
    public static final DeferredBlock<GardenPlantBlock> GARDEN_PLANT = BLOCKS.register("garden_plant", () -> new GardenPlantBlock(
            BlockBehaviour.Properties.of().strength(0.0F).sound(SoundType.GRASS).noOcclusion().replaceable()));
    public static final DeferredItem<GardenPlantItem> GARDEN_PLANT_ITEM = ITEMS.registerItem("garden_plant",
            properties -> new GardenPlantItem(GARDEN_PLANT.get(), properties));
    public static final List<DeferredItem<FixedGardenStateItem>> LEGACY_PLANT_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<FixedGardenStateItem>> LEGACY_SOIL_ITEMS = new ArrayList<>();

    static {
        for (binnie.botany.block.GardenPlantType type : binnie.botany.block.GardenPlantType.values()) {
            String id = switch (type) {
                case WEEDS -> "weeds";
                case LONG_WEEDS -> "weeds_long";
                case VERY_LONG_WEEDS -> "weeds_very_long";
                case DEAD_FLOWER -> "dead_flower";
                case DECAYING_FLOWER -> "decaying_flower";
            };
            LEGACY_PLANT_ITEMS.add(ITEMS.registerItem(id, properties -> new FixedGardenStateItem(properties,
                    () -> GARDEN_PLANT.get().defaultBlockState().setValue(GardenPlantBlock.TYPE, type))));
        }
        registerLegacySoils("soil", SOIL, SOIL_ITEM, SoilQuality.SOIL);
        registerLegacySoils("loam", LOAM, LOAM_ITEM, SoilQuality.LOAM);
        registerLegacySoils("flowerbed", FLOWERBED, FLOWERBED_ITEM, SoilQuality.FLOWERBED);
        registerLegacySoils("soil_no_weed", SOIL, SOIL_ITEM, SoilQuality.SOIL, true);
        registerLegacySoils("loam_no_weed", LOAM, LOAM_ITEM, SoilQuality.LOAM, true);
        registerLegacySoils("flowerbed_no_weed", FLOWERBED, FLOWERBED_ITEM, SoilQuality.FLOWERBED, true);
    }
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlowerBlockEntity>> FLOWER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("genetic_flower", () -> BlockEntityType.Builder.of(FlowerBlockEntity::new, FLOWER_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CeramicBrickBlockEntity>> CERAMIC_BRICK_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("ceramic_brick", () -> BlockEntityType.Builder.of(CeramicBrickBlockEntity::new, CERAMIC_BRICK.get()).build(null));

    private BotanyWorldContent() {}


    private static void registerLegacySoils(String family, DeferredBlock<BotanySoilBlock> block,
                                            DeferredItem<BlockItem> ignoredBaseItem, SoilQuality quality) {
        registerLegacySoils(family, block, ignoredBaseItem, quality, false);
    }

    private static void registerLegacySoils(String family, DeferredBlock<BotanySoilBlock> block,
                                            DeferredItem<BlockItem> ignoredBaseItem, SoilQuality quality,
                                            boolean weedKilled) {
        for (binnie.botany.api.genetics.SoilAcidity acidity : binnie.botany.api.genetics.SoilAcidity.values()) {
            for (binnie.botany.api.genetics.SoilMoisture moisture : binnie.botany.api.genetics.SoilMoisture.values()) {
                String suffix = acidity.getSerializedName() + "_" + moisture.getSerializedName();
                String id = "creative_" + family + "_" + suffix;
                LEGACY_SOIL_ITEMS.add(ITEMS.registerItem(id, properties -> new FixedGardenStateItem(properties,
                        () -> block.get().defaultBlockState()
                                .setValue(BotanySoilBlock.ACIDITY, acidity)
                                .setValue(BotanySoilBlock.MOISTURE, moisture)
                                .setValue(BotanySoilBlock.FERTILITY, quality.initialFertility())
                                .setValue(BotanySoilBlock.WEEDKILLED, weedKilled))));
            }
        }
    }
    private static DeferredBlock<BotanySoilBlock> soil(String id, SoilQuality quality) {
        return BLOCKS.register(id, () -> new BotanySoilBlock(
                BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.GRAVEL), quality));
    }

    private static DeferredItem<BlockItem> blockItem(DeferredBlock<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.registerItem(block.getId().getPath(), properties -> new BlockItem(block.get(), properties));
    }

    public static void includeCreativeItems() {
        // Creative ordering is installed by BotanyContent after all registries are declared.
    }

    public static net.minecraft.world.item.ItemStack soilStack(net.minecraft.world.item.Item item,
                                                                binnie.botany.api.genetics.SoilMoisture moisture,
                                                                binnie.botany.api.genetics.SoilAcidity acidity,
                                                                int fertility, boolean weedKilled) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
        java.util.Map<String, String> properties = new java.util.LinkedHashMap<>();
        properties.put(BotanySoilBlock.MOISTURE.getName(), moisture.getSerializedName());
        properties.put(BotanySoilBlock.ACIDITY.getName(), acidity.getSerializedName());
        properties.put(BotanySoilBlock.FERTILITY.getName(), Integer.toString(fertility));
        properties.put(BotanySoilBlock.WEEDKILLED.getName(), Boolean.toString(weedKilled));
        stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                new net.minecraft.world.item.component.BlockItemStateProperties(properties));
        return stack;
    }

    public static void register(IEventBus bus) {
        includeCreativeItems();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
