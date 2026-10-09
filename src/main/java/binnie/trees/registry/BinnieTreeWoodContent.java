package binnie.trees.registry;

import binnie.core.registry.ModuleRegistry;
import binnie.trees.wood.BinnieTreeWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.api.plugin.IArboricultureRegistration;
import forestry.arboriculture.wood.WoodAccess;
import forestry.arboriculture.wood.ForestryFenceBlock;
import forestry.arboriculture.wood.ForestryFenceGateBlock;
import forestry.arboriculture.wood.ForestryPlanksBlock;
import forestry.arboriculture.wood.ForestrySlabBlock;
import forestry.arboriculture.wood.ForestryStairsBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit current block identities and Forestry wood-access bindings for all 36 Binnie's Trees woods. */
public class BinnieTreeWoodContent {
    private static final String MOD_ID = "binnietrees";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final Map<BinnieTreeWoodType, WoodSet> WOODS = new EnumMap<>(BinnieTreeWoodType.class);
    private static final AtomicBoolean FORESTRY_REGISTERED = new AtomicBoolean();

    static {
        for (BinnieTreeWoodType type : BinnieTreeWoodType.values()) {
            WOODS.put(type, register(type));
        }
    }

    private BinnieTreeWoodContent() {
    }

    private static WoodSet register(BinnieTreeWoodType type) {
        String id = type.getSerializedName();
        DeferredBlock<RotatedPillarBlock> strippedLog = pillar(type, id + "_stripped_log", false,
                WoodBlockKind.STRIPPED_LOG, null);
        DeferredBlock<RotatedPillarBlock> fireproofStrippedLog = pillar(type, id + "_fireproof_stripped_log", true,
                WoodBlockKind.STRIPPED_LOG, null);
        DeferredBlock<RotatedPillarBlock> strippedWood = pillar(type, id + "_stripped_wood", false,
                WoodBlockKind.STRIPPED_WOOD, null);
        DeferredBlock<RotatedPillarBlock> fireproofStrippedWood = pillar(type, id + "_fireproof_stripped_wood", true,
                WoodBlockKind.STRIPPED_WOOD, null);
        DeferredBlock<RotatedPillarBlock> log = pillar(type, id + "_log", false, WoodBlockKind.LOG, strippedLog::get);
        DeferredBlock<RotatedPillarBlock> fireproofLog = pillar(type, id + "_fireproof_log", true,
                WoodBlockKind.LOG, fireproofStrippedLog::get);
        DeferredBlock<RotatedPillarBlock> wood = pillar(type, id + "_wood", false, WoodBlockKind.WOOD, strippedWood::get);
        DeferredBlock<RotatedPillarBlock> fireproofWood = pillar(type, id + "_fireproof_wood", true,
                WoodBlockKind.WOOD, fireproofStrippedWood::get);

            DeferredBlock<ForestryPlanksBlock> planks = BLOCKS.register(id + "_planks",
                    () -> new ForestryPlanksBlock(false, type));
        DeferredBlock<ForestryPlanksBlock> fireproofPlanks = BLOCKS.register(id + "_fireproof_planks",
                () -> new ForestryPlanksBlock(true, type));
        DeferredBlock<ForestrySlabBlock> slab = BLOCKS.register(id + "_slab",
                () -> new ForestrySlabBlock(planks.get()));
        DeferredBlock<ForestrySlabBlock> fireproofSlab = BLOCKS.register(id + "_fireproof_slab",
                () -> new ForestrySlabBlock(fireproofPlanks.get()));
        DeferredBlock<ForestryStairsBlock> stairs = BLOCKS.register(id + "_stairs",
                () -> new ForestryStairsBlock(planks.get()));
        DeferredBlock<ForestryStairsBlock> fireproofStairs = BLOCKS.register(id + "_fireproof_stairs",
                () -> new ForestryStairsBlock(fireproofPlanks.get()));
        DeferredBlock<ForestryFenceBlock> fence = BLOCKS.register(id + "_fence",
                () -> new ForestryFenceBlock(false, type));
        DeferredBlock<ForestryFenceBlock> fireproofFence = BLOCKS.register(id + "_fireproof_fence",
                () -> new ForestryFenceBlock(true, type));
        DeferredBlock<ForestryFenceGateBlock> fenceGate = BLOCKS.register(id + "_fence_gate",
                () -> new ForestryFenceGateBlock(false, type));
        DeferredBlock<ForestryFenceGateBlock> fireproofFenceGate = BLOCKS.register(id + "_fireproof_fence_gate",
                () -> new ForestryFenceGateBlock(true, type));

        DeferredBlock<BinnieTreeWoodControlBlocks.Door> door = BLOCKS.register(id + "_door",
                () -> new BinnieTreeWoodControlBlocks.Door(type, woodProperties(type, false).noOcclusion()));
        DeferredBlock<BinnieTreeWoodControlBlocks.Trapdoor> trapdoor = BLOCKS.register(id + "_trapdoor",
                () -> new BinnieTreeWoodControlBlocks.Trapdoor(type, trapdoorProperties(type)));
        DeferredBlock<BinnieTreeWoodControlBlocks.PressurePlate> pressurePlate = BLOCKS.register(id + "_pressure_plate",
                () -> new BinnieTreeWoodControlBlocks.PressurePlate(type, pressurePlateProperties()));
        DeferredBlock<BinnieTreeWoodControlBlocks.Button> button = BLOCKS.register(id + "_button",
                () -> new BinnieTreeWoodControlBlocks.Button(type, buttonProperties()));

        return new WoodSet(
                log, fireproofLog, wood, fireproofWood, strippedLog, fireproofStrippedLog, strippedWood,
                fireproofStrippedWood, planks, fireproofPlanks, slab, fireproofSlab, stairs, fireproofStairs,
                fence, fireproofFence, fenceGate, fireproofFenceGate, door, trapdoor, pressurePlate, button,
                item(log), item(fireproofLog), item(wood), item(fireproofWood), item(strippedLog),
                item(fireproofStrippedLog), item(strippedWood), item(fireproofStrippedWood), item(planks),
                item(fireproofPlanks), item(slab), item(fireproofSlab), item(stairs), item(fireproofStairs),
                item(fence), item(fireproofFence), item(fenceGate), item(fireproofFenceGate), item(door),
                item(trapdoor), item(pressurePlate), item(button));
    }

    private static DeferredBlock<RotatedPillarBlock> pillar(BinnieTreeWoodType type, String id, boolean fireproof,
                                                             WoodBlockKind kind,
                                                             java.util.function.Supplier<? extends Block> stripped) {
        return BLOCKS.register(id, () -> new BinnieTreeLogBlock(kind, fireproof, type, stripped));
    }

    private static BlockBehaviour.Properties woodProperties(BinnieTreeWoodType type, boolean fireproof) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .strength(type.getHardness(), type.getHardness() * 1.5F)
                .sound(SoundType.WOOD);
        return fireproof ? properties : properties.ignitedByLava();
    }

    private static BlockBehaviour.Properties trapdoorProperties(BinnieTreeWoodType type) {
        return woodProperties(type, false)
                .mapColor(MapColor.WOOD)
                .instrument(NoteBlockInstrument.BASS)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties pressurePlateProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .forceSolidOn()
                .instrument(NoteBlockInstrument.BASS)
                .noCollission()
                .strength(0.5F)
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties buttonProperties() {
        return BlockBehaviour.Properties.of()
                .noCollission()
                .strength(0.5F)
                .pushReaction(PushReaction.DESTROY);
    }

    private static <T extends Block> DeferredItem<BlockItem> item(DeferredBlock<T> block) {
        return ITEMS.registerItem(block.getId().getPath(), properties -> new BlockItem(block.get(), properties));
    }

    public static WoodSet get(BinnieTreeWoodType type) {
        WoodSet set = WOODS.get(type);
        if (set == null) {
            throw new IllegalArgumentException("Unknown Binnie's Trees wood type: " + type);
        }
        return set;
    }

    /** Called by Forestry immediately after item registration. Safe against repeated plugin initialization. */
    public static void registerForestryBindings(IArboricultureRegistration arboriculture) {
        if (!FORESTRY_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        WoodAccess access = WoodAccess.INSTANCE;
        for (BinnieTreeWoodType type : BinnieTreeWoodType.values()) {
            WoodSet set = get(type);
            bind(access, type, WoodBlockKind.LOG, set.log(), set.logItem(), false);
            bind(access, type, WoodBlockKind.LOG, set.fireproofLog(), set.fireproofLogItem(), true);
            bind(access, type, WoodBlockKind.WOOD, set.wood(), set.woodItem(), false);
            bind(access, type, WoodBlockKind.WOOD, set.fireproofWood(), set.fireproofWoodItem(), true);
            bind(access, type, WoodBlockKind.STRIPPED_LOG, set.strippedLog(), set.strippedLogItem(), false);
            bind(access, type, WoodBlockKind.STRIPPED_LOG, set.fireproofStrippedLog(), set.fireproofStrippedLogItem(), true);
            bind(access, type, WoodBlockKind.STRIPPED_WOOD, set.strippedWood(), set.strippedWoodItem(), false);
            bind(access, type, WoodBlockKind.STRIPPED_WOOD, set.fireproofStrippedWood(), set.fireproofStrippedWoodItem(), true);
            bind(access, type, WoodBlockKind.PLANKS, set.planks(), set.planksItem(), false);
            bind(access, type, WoodBlockKind.PLANKS, set.fireproofPlanks(), set.fireproofPlanksItem(), true);
            bind(access, type, WoodBlockKind.SLAB, set.slab(), set.slabItem(), false);
            bind(access, type, WoodBlockKind.SLAB, set.fireproofSlab(), set.fireproofSlabItem(), true);
            bind(access, type, WoodBlockKind.STAIRS, set.stairs(), set.stairsItem(), false);
            bind(access, type, WoodBlockKind.STAIRS, set.fireproofStairs(), set.fireproofStairsItem(), true);
            bind(access, type, WoodBlockKind.FENCE, set.fence(), set.fenceItem(), false);
            bind(access, type, WoodBlockKind.FENCE, set.fireproofFence(), set.fireproofFenceItem(), true);
            bind(access, type, WoodBlockKind.FENCE_GATE, set.fenceGate(), set.fenceGateItem(), false);
            bind(access, type, WoodBlockKind.FENCE_GATE, set.fireproofFenceGate(), set.fireproofFenceGateItem(), true);
            bind(access, type, WoodBlockKind.DOOR, set.door(), set.doorItem(), false);
            bind(access, type, WoodBlockKind.TRAPDOOR, set.trapdoor(), set.trapdoorItem(), false);
            bind(access, type, WoodBlockKind.PRESSURE_PLATE, set.pressurePlate(), set.pressurePlateItem(), false);
            bind(access, type, WoodBlockKind.BUTTON, set.button(), set.buttonItem(), false);

            access.registerLogTag(type, false, blockLogTag(type, false), itemLogTag(type, false));
            access.registerLogTag(type, true, blockLogTag(type, true), itemLogTag(type, true));

            wax(arboriculture, set.log(), set.fireproofLog());
            wax(arboriculture, set.wood(), set.fireproofWood());
            wax(arboriculture, set.strippedLog(), set.fireproofStrippedLog());
            wax(arboriculture, set.strippedWood(), set.fireproofStrippedWood());
            wax(arboriculture, set.planks(), set.fireproofPlanks());
            wax(arboriculture, set.slab(), set.fireproofSlab());
            wax(arboriculture, set.stairs(), set.fireproofStairs());
            wax(arboriculture, set.fence(), set.fireproofFence());
            wax(arboriculture, set.fenceGate(), set.fireproofFenceGate());
        }
    }

    private static <T extends Block> void bind(WoodAccess access, BinnieTreeWoodType type, WoodBlockKind kind,
                                                DeferredBlock<T> block, DeferredItem<BlockItem> item,
                                                boolean fireproof) {
        access.register(type, kind, fireproof, block.get().defaultBlockState(), item::get);
    }

    private static void wax(IArboricultureRegistration arboriculture, DeferredBlock<? extends Block> normal,
                            DeferredBlock<? extends Block> fireproof) {
        arboriculture.registerRefractoryWaxable(normal.get(), fireproof.get());
    }

    public static TagKey<Block> blockLogTag(BinnieTreeWoodType type, boolean fireproof) {
        return TagKey.create(Registries.BLOCK, woodId(type, fireproof));
    }

    public static TagKey<Item> itemLogTag(BinnieTreeWoodType type, boolean fireproof) {
        return TagKey.create(Registries.ITEM, woodId(type, fireproof));
    }

    private static ResourceLocation woodId(BinnieTreeWoodType type, boolean fireproof) {
        String suffix = fireproof ? "_fireproof_logs" : "_logs";
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, type.getSerializedName() + suffix);
    }

    public static void includeCreativeItems(ModuleRegistry registry) {
        for (WoodSet set : WOODS.values()) {
            set.includeCreativeItems(registry);
        }
    }

    public static void acceptCreativeItems(net.minecraft.world.item.CreativeModeTab.Output output) {
        for (WoodSet set : WOODS.values()) {
            output.accept(set.logItem().get());
            output.accept(set.fireproofLogItem().get());
            output.accept(set.woodItem().get());
            output.accept(set.fireproofWoodItem().get());
            output.accept(set.strippedLogItem().get());
            output.accept(set.fireproofStrippedLogItem().get());
            output.accept(set.strippedWoodItem().get());
            output.accept(set.fireproofStrippedWoodItem().get());
            output.accept(set.planksItem().get());
            output.accept(set.fireproofPlanksItem().get());
            output.accept(set.slabItem().get());
            output.accept(set.fireproofSlabItem().get());
            output.accept(set.stairsItem().get());
            output.accept(set.fireproofStairsItem().get());
            output.accept(set.fenceItem().get());
            output.accept(set.fireproofFenceItem().get());
            output.accept(set.fenceGateItem().get());
            output.accept(set.fireproofFenceGateItem().get());
            output.accept(set.doorItem().get());
            output.accept(set.trapdoorItem().get());
            output.accept(set.pressurePlateItem().get());
            output.accept(set.buttonItem().get());
        }
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public record WoodSet(
            DeferredBlock<RotatedPillarBlock> log,
            DeferredBlock<RotatedPillarBlock> fireproofLog,
            DeferredBlock<RotatedPillarBlock> wood,
            DeferredBlock<RotatedPillarBlock> fireproofWood,
            DeferredBlock<RotatedPillarBlock> strippedLog,
            DeferredBlock<RotatedPillarBlock> fireproofStrippedLog,
            DeferredBlock<RotatedPillarBlock> strippedWood,
            DeferredBlock<RotatedPillarBlock> fireproofStrippedWood,
            DeferredBlock<ForestryPlanksBlock> planks,
            DeferredBlock<ForestryPlanksBlock> fireproofPlanks,
            DeferredBlock<ForestrySlabBlock> slab,
            DeferredBlock<ForestrySlabBlock> fireproofSlab,
            DeferredBlock<ForestryStairsBlock> stairs,
            DeferredBlock<ForestryStairsBlock> fireproofStairs,
            DeferredBlock<ForestryFenceBlock> fence,
            DeferredBlock<ForestryFenceBlock> fireproofFence,
            DeferredBlock<ForestryFenceGateBlock> fenceGate,
            DeferredBlock<ForestryFenceGateBlock> fireproofFenceGate,
            DeferredBlock<BinnieTreeWoodControlBlocks.Door> door,
            DeferredBlock<BinnieTreeWoodControlBlocks.Trapdoor> trapdoor,
            DeferredBlock<BinnieTreeWoodControlBlocks.PressurePlate> pressurePlate,
            DeferredBlock<BinnieTreeWoodControlBlocks.Button> button,
            DeferredItem<BlockItem> logItem,
            DeferredItem<BlockItem> fireproofLogItem,
            DeferredItem<BlockItem> woodItem,
            DeferredItem<BlockItem> fireproofWoodItem,
            DeferredItem<BlockItem> strippedLogItem,
            DeferredItem<BlockItem> fireproofStrippedLogItem,
            DeferredItem<BlockItem> strippedWoodItem,
            DeferredItem<BlockItem> fireproofStrippedWoodItem,
            DeferredItem<BlockItem> planksItem,
            DeferredItem<BlockItem> fireproofPlanksItem,
            DeferredItem<BlockItem> slabItem,
            DeferredItem<BlockItem> fireproofSlabItem,
            DeferredItem<BlockItem> stairsItem,
            DeferredItem<BlockItem> fireproofStairsItem,
            DeferredItem<BlockItem> fenceItem,
            DeferredItem<BlockItem> fireproofFenceItem,
            DeferredItem<BlockItem> fenceGateItem,
            DeferredItem<BlockItem> fireproofFenceGateItem,
            DeferredItem<BlockItem> doorItem,
            DeferredItem<BlockItem> trapdoorItem,
            DeferredItem<BlockItem> pressurePlateItem,
            DeferredItem<BlockItem> buttonItem) {

        private void includeCreativeItems(ModuleRegistry registry) {
            registry.include(this.logItem);
            registry.include(this.fireproofLogItem);
            registry.include(this.woodItem);
            registry.include(this.fireproofWoodItem);
            registry.include(this.strippedLogItem);
            registry.include(this.fireproofStrippedLogItem);
            registry.include(this.strippedWoodItem);
            registry.include(this.fireproofStrippedWoodItem);
            registry.include(this.planksItem);
            registry.include(this.fireproofPlanksItem);
            registry.include(this.slabItem);
            registry.include(this.fireproofSlabItem);
            registry.include(this.stairsItem);
            registry.include(this.fireproofStairsItem);
            registry.include(this.fenceItem);
            registry.include(this.fireproofFenceItem);
            registry.include(this.fenceGateItem);
            registry.include(this.fireproofFenceGateItem);
            registry.include(this.doorItem);
            registry.include(this.trapdoorItem);
            registry.include(this.pressurePlateItem);
            registry.include(this.buttonItem);
        }
    }
}
