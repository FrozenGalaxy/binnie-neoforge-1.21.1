package binnie.trees.registry;

import binnie.trees.block.HopsBlock;
import binnie.trees.block.HedgeBlock;
import binnie.trees.block.DecorativeLeavesBlock;
import binnie.trees.forestry.LegacyTreeCatalog;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.BlockItem;
import java.util.LinkedHashMap;
import java.util.Map;

/** Non-machine Binnie's Trees world blocks. */
public class BinnieTreesWorldContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("binnietrees");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("binnietrees");
    private static final Map<String, DeferredBlock<DecorativeLeavesBlock>> DECORATIVE_LEAVES = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<BlockItem>> DECORATIVE_LEAF_ITEMS = new LinkedHashMap<>();
    private static final Map<String, DeferredBlock<HedgeBlock>> HEDGES = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<BlockItem>> HEDGE_ITEMS = new LinkedHashMap<>();

    public static final DeferredBlock<HopsBlock> HOPS = BLOCKS.register("hops", () -> new HopsBlock(
            BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().sound(SoundType.CROP)));

    static {
        for (LegacyTreeCatalog.TreeBinding tree : LegacyTreeCatalog.load().species()) {
            String path = "decorative_leaves_" + tree.id();
            DeferredBlock<DecorativeLeavesBlock> block = BLOCKS.register(path, () -> new DecorativeLeavesBlock(
                    BlockBehaviour.Properties.of().strength(0.2F).sound(SoundType.GRASS).noOcclusion().ignitedByLava(),
                    tree.id(), tree.leafColor()));
            DECORATIVE_LEAVES.put(tree.id(), block);
            DECORATIVE_LEAF_ITEMS.put(tree.id(), ITEMS.registerItem(path, properties -> new BlockItem(block.get(), properties)));
            String hedgePath = "hedge_" + tree.id();
            DeferredBlock<HedgeBlock> hedge = BLOCKS.register(hedgePath, () -> new HedgeBlock(
                    BlockBehaviour.Properties.of().strength(0.2F).sound(SoundType.GRASS).noOcclusion().ignitedByLava()));
            HEDGES.put(tree.id(), hedge);
            HEDGE_ITEMS.put(tree.id(), ITEMS.registerItem(hedgePath, properties -> new BlockItem(hedge.get(), properties)));
        }
    }

    private BinnieTreesWorldContent() {}

    public static Map<String, DeferredBlock<DecorativeLeavesBlock>> decorativeLeaves() { return Map.copyOf(DECORATIVE_LEAVES); }
    public static Map<String, DeferredItem<BlockItem>> decorativeLeafItems() { return Map.copyOf(DECORATIVE_LEAF_ITEMS); }
    public static Map<String, DeferredBlock<HedgeBlock>> hedges() { return Map.copyOf(HEDGES); }
    public static Map<String, DeferredItem<BlockItem>> hedgeItems() { return Map.copyOf(HEDGE_ITEMS); }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
