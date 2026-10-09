package binnie.trees.forestry;

import binnie.core.data.VariantData;
import binnie.core.registry.BinnieDataComponents;
import binnie.trees.BinnieTrees;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.arboriculture.trees.TileTreeContainer;
import forestry.arboriculture.trees.genetics.Tree;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Converts old Binnie's Trees duplicate species and products as their saved data is loaded. */
@EventBusSubscriber(modid = BinnieTrees.MOD_ID)
public class LegacyTreeWorldMigration {
    private static final Map<String, String> SPECIES = Map.of(
            "beech", "beech",
            "orange", "orange",
            "olive", "olive",
            "pear", "pear",
            "sour_cherry", "sour_cherry",
            "coconut", "coconut"
    );
    private static final Set<String> DUPLICATE_WOODS = Set.of("beech", "olive", "pear");
    private static final Map<String, String> DUPLICATE_FRUITS = Map.of(
            "orange", "orange", "olive", "olive", "pear", "pear", "coconut", "coconut",
            "sour_cherry", "cherry"
    );

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        migrateChunk(level, chunk);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        migrateContainer(event.getEntity().getInventory());
        migrateContainer(event.getEntity().getEnderChestInventory());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        Entity entity = event.getEntity();
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack old = itemEntity.getItem();
            ItemStack migrated = migrateStack(old);
            if (migrated != old) itemEntity.setItem(migrated);
        } else if (entity instanceof ItemFrame itemFrame) {
            ItemStack old = itemFrame.getItem();
            ItemStack migrated = migrateStack(old);
            if (migrated != old) itemFrame.setItem(migrated);
        } else if (entity instanceof Container container) {
            migrateContainer(container);
        }
    }

    private static void migrateChunk(ServerLevel level, LevelChunk chunk) {
        boolean changed = migrateBlocks(level, chunk);
        for (var blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof TileTreeContainer treeContainer) {
                ITree tree = treeContainer.getTree();
                ITree migrated = migrateTree(tree);
                if (migrated != tree && migrated != null) {
                    treeContainer.setTree(migrated);
                    changed = true;
                }
            }
            if (blockEntity instanceof Container container) changed |= migrateContainer(container);
        }

        if (changed) chunk.setUnsaved(true);
    }

    private static boolean migrateBlocks(ServerLevel level, LevelChunk chunk) {
        boolean changed = false;
        LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (!section.maybeHas(state -> forestryBlockFor(state.getBlock()).isPresent())) continue;
            int minY = (chunk.getMinSection() + sectionIndex) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState oldState = section.getBlockState(x, y, z);
                        Optional<Block> target = forestryBlockFor(oldState.getBlock());
                        if (target.isEmpty()) continue;
                        BlockState newState = copyProperties(oldState, target.get().defaultBlockState());
                        level.setBlock(new BlockPos(chunk.getPos().getMinBlockX() + x, minY + y,
                                chunk.getPos().getMinBlockZ() + z), newState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private static Optional<Block> forestryBlockFor(Block block) {
        ResourceLocation oldId = BuiltInRegistries.BLOCK.getKey(block);
        if (!oldId.getNamespace().equals("binnietrees")) return Optional.empty();
        String path = oldId.getPath();
        String candidate = null;
        for (String wood : DUPLICATE_WOODS) {
            if (path.startsWith(wood + "_")) {
                candidate = path;
                break;
            }
        }
        for (String species : SPECIES.keySet()) {
            if (path.equals("decorative_leaves_" + species)) {
                candidate = species + "_decorative_leaves";
                break;
            }
        }
        if (candidate == null) return Optional.empty();
        Block target = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("forestry", candidate));
        return target == net.minecraft.world.level.block.Blocks.AIR ? Optional.empty() : Optional.of(target);
    }

    private static BlockState copyProperties(BlockState source, BlockState target) {
        BlockState result = target;
        for (Property<?> property : source.getProperties()) result = copyProperty(source, result, property);
        return result;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState source, BlockState target,
                                                                      Property<T> property) {
        if (!target.hasProperty(property)) return target;
        return target.setValue(property, source.getValue(property));
    }

    private static ITree migrateTree(ITree tree) {
        if (tree == null) return null;
        IGenome migratedGenome = migrateGenome(tree.getGenome());
        Optional<IGenome> mate = tree.getMateOptional().map(LegacyTreeWorldMigration::migrateGenome);
        if (migratedGenome == tree.getGenome()
                && mate.equals(tree.getMateOptional())) return tree;
        IIndividual individual = tree.copyWithGenome(migratedGenome);
        mate.ifPresent(individual::setMate);
        return individual.cast();
    }

    private static IGenome migrateGenome(IGenome genome) {
        AllelePair<ResourceLocation> pair = genome.getAllelePair(TreeChromosomes.SPECIES);
        AllelePair<ResourceLocation> migratedSpecies = migratePair(pair, LegacyTreeWorldMigration::forestrySpecies);
        AllelePair<ResourceLocation> fruitPair = genome.getAllelePair(TreeChromosomes.FRUIT);
        AllelePair<ResourceLocation> migratedFruit = migratePair(fruitPair, LegacyTreeWorldMigration::forestryFruit);
        if (migratedSpecies == pair && migratedFruit == fruitPair) return genome;
        Map<forestry.api.core.genetics.alleles.IChromosome<?>, AllelePair<?>> updates = new HashMap<>();
        if (migratedSpecies != pair) updates.put(TreeChromosomes.SPECIES, migratedSpecies);
        if (migratedFruit != fruitPair) updates.put(TreeChromosomes.FRUIT, migratedFruit);
        return genome.copyWithPairs(updates);
    }

    private static AllelePair<ResourceLocation> migratePair(AllelePair<ResourceLocation> pair,
                                                               java.util.function.Function<ResourceLocation, ResourceLocation> mapper) {
        ResourceLocation active = mapper.apply(pair.active().value());
        ResourceLocation inactive = mapper.apply(pair.inactive().value());
        if (active.equals(pair.active().value()) && inactive.equals(pair.inactive().value())) return pair;
        return new AllelePair<>(new Allele<>(active, pair.active().dominant()),
                new Allele<>(inactive, pair.inactive().dominant()));
    }

    private static ResourceLocation forestrySpecies(ResourceLocation oldId) {
        if (!oldId.getNamespace().equals("binnietrees")) return oldId;
        String target = SPECIES.get(oldId.getPath());
        return target == null ? oldId : ResourceLocation.fromNamespaceAndPath("forestry", target);
    }

    private static ResourceLocation forestryFruit(ResourceLocation oldId) {
        if (!oldId.getNamespace().equals("binnietrees") || !oldId.getPath().startsWith("fruit_")) return oldId;
        String species = oldId.getPath().substring("fruit_".length());
        if (species.equals("sour_cherry")) return ResourceLocation.fromNamespaceAndPath("forestry", "fruit_cherry");
        return SPECIES.containsKey(species)
                ? ResourceLocation.fromNamespaceAndPath("forestry", oldId.getPath())
                : oldId;
    }

    private static boolean migrateContainer(Container container) {
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack old = container.getItem(slot);
            ItemStack migrated = migrateStack(old);
            if (migrated != old) {
                container.setItem(slot, migrated);
                changed = true;
            }
        }
        if (changed) container.setChanged();
        return changed;
    }

    private static ItemStack migrateStack(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (stack.getItem() instanceof BlockItem blockItem) {
            Optional<Block> targetBlock = forestryBlockFor(blockItem.getBlock());
            if (targetBlock.isPresent()) return replacementStack(stack, targetBlock.get().asItem());
        }
        String fruit = legacyFruitVariant(stack, id);
        if (fruit != null && DUPLICATE_FRUITS.containsKey(fruit)) {
            var targetItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("forestry", DUPLICATE_FRUITS.get(fruit)));
            if (targetItem != net.minecraft.world.item.Items.AIR) {
                return replacementStack(stack, targetItem);
            }
        }
        IIndividual individual = forestry.api.core.genetics.capability.IIndividualHandlerItem.getIndividual(stack);
        if (individual == null || !individual.getGenome().getChromosomes().containsKey(TreeChromosomes.SPECIES)) return stack;
        IGenome genome = migrateGenome(individual.getGenome());
        Optional<IGenome> mate = individual.getMateOptional().map(LegacyTreeWorldMigration::migrateGenome);
        if (genome == individual.getGenome() && mate.equals(individual.getMateOptional())) return stack;
        IIndividual migrated = individual.copyWithGenome(genome);
        mate.ifPresent(migrated::setMate);
        migrated.saveToStack(stack);
        return stack;
    }

    private static ItemStack replacementStack(ItemStack oldStack, net.minecraft.world.item.Item targetItem) {
        ItemStack result = new ItemStack(targetItem, oldStack.getCount());
        result.applyComponents(oldStack.getComponentsPatch());
        return result;
    }

    private static String legacyFruitVariant(ItemStack stack, ResourceLocation itemId) {
        if (!itemId.getNamespace().equals("binnietrees")) return null;
        if (itemId.getPath().startsWith("food_")) return itemId.getPath().substring("food_".length());
        if (!itemId.getPath().equals("food")) return null;
        VariantData variant = stack.get(BinnieDataComponents.VARIANT);
        return variant != null && variant.category().equals("binnietrees.food") ? variant.value() : null;
    }
}
