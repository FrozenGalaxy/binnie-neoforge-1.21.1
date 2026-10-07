package binnie.bees.forestry;

import binnie.bees.BinnieBees;
import binnie.bees.block.BinnieBeeHiveBlock;
import binnie.bees.registry.BinnieBeesWorldContent;
import forestry.api.apiculture.hives.IHiveDefinition;
import forestry.api.apiculture.hives.IHivePlacement;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.core.genetics.ClimateHelper;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.plugin.IApicultureRegistration;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/** Registers Binnie's hive locations with Forestry's shared chunk hive decorator. */
public final class LegacyHiveCatalog {
    private static final TagKey<Block> MARBLE_REPLACEABLES = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, "marble_hive_replaceables"));

    private LegacyHiveCatalog() {}

    public static int register(IApicultureRegistration apiculture) {
        register(apiculture, "water", "water", BinnieBeeHiveBlock.HiveType.WATER, 2.0F);
        register(apiculture, "rock", "rock", BinnieBeeHiveBlock.HiveType.ROCK, 6.0F);
        register(apiculture, "marble", "marble", BinnieBeeHiveBlock.HiveType.MARBLE, 20.0F);
        register(apiculture, "basalt", "nether", BinnieBeeHiveBlock.HiveType.NETHER, 6.0F);
        return 4;
    }

    private static void register(IApicultureRegistration apiculture, String beeSpecies, String hiveName,
                                 BinnieBeeHiveBlock.HiveType type, float chance) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, "hive_" + hiveName);
        apiculture.registerHive(id, new HiveDefinition(beeSpecies, type)).setGenerationChance(chance);
    }

    private record HiveDefinition(String beeSpecies, BinnieBeeHiveBlock.HiveType type) implements IHiveDefinition {
        @Override public IHivePlacement getHiveGen() { return new Placement(type); }

        @Override public BlockState getBlockState() {
            return BinnieBeesWorldContent.hive(type).get().defaultBlockState();
        }

        @Override public boolean isGoodBiome(Holder<Biome> biome) {
            // The old condition allowed every biome for non-Nether hives and only Nether biomes for basalt.
            return type != BinnieBeeHiveBlock.HiveType.NETHER || biome.is(BiomeTags.IS_NETHER);
        }

        @Override public boolean isGoodHumidity(HumidityType humidity) {
            var species = SpeciesUtil.getBeeSpecies(id(beeSpecies));
            return species != null && ClimateHelper.isWithinLimits(humidity, species.getHumidity(),
                    species.getDefaultGenome().getActiveValue(BeeChromosomes.HUMIDITY_TOLERANCE));
        }

        @Override public boolean isGoodTemperature(TemperatureType temperature) {
            var species = SpeciesUtil.getBeeSpecies(id(beeSpecies));
            return species != null && ClimateHelper.isWithinLimits(temperature, species.getTemperature(),
                    species.getDefaultGenome().getActiveValue(BeeChromosomes.TEMPERATURE_TOLERANCE));
        }

        @Override public void postGen(WorldGenLevel level, RandomSource random, BlockPos pos) { }
    }

    private record Placement(BinnieBeeHiveBlock.HiveType type) implements IHivePlacement {
        @Override public BlockPos getPosForHive(WorldGenLevel level, RandomSource random, int x, int z) {
            if (type == BinnieBeeHiveBlock.HiveType.NETHER
                    && !level.getBiome(new BlockPos(x, 0, z)).is(BiomeTags.IS_NETHER)) return null;
            int maxHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (maxHeight <= 0) return null;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, maxHeight, z);
            for (int attempt = 0; attempt < 10; attempt++) {
                pos.setY(random.nextInt(maxHeight));
                if (isValidLocation(level, pos)) return pos.immutable();
            }
            return null;
        }

        @Override public boolean isValidLocation(WorldGenLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return switch (type) {
                case WATER -> state.is(Blocks.WATER) && isWaterHiveSupport(level.getBlockState(pos.below()));
                case ROCK -> state.is(Blocks.STONE) && hasHorizontalAir(level, pos);
                case MARBLE -> state.is(MARBLE_REPLACEABLES) && hasDifferentNeighbor(level, pos, state.getBlock());
                case NETHER -> level.getBiome(pos).is(BiomeTags.IS_NETHER) && state.is(Blocks.NETHERRACK)
                        && level.getBlockState(pos.above()).is(Blocks.NETHERRACK) && hasAirNeighbor(level, pos);
            };
        }

        @Override public boolean canReplace(BlockState state, WorldGenLevel level, BlockPos pos) {
            return switch (type) {
                case WATER -> state.is(Blocks.WATER);
                case ROCK -> state.is(Blocks.STONE);
                case MARBLE -> state.is(MARBLE_REPLACEABLES);
                case NETHER -> state.is(Blocks.NETHERRACK);
            };
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, path);
    }

    private static boolean isWaterHiveSupport(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.CLAY)
                || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.FARMLAND)
                || state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "stone")));
    }

    private static boolean hasHorizontalAir(WorldGenLevel level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = pos.relative(direction);
            if (level.hasChunkAt(adjacent) && level.getBlockState(adjacent).isAir()) return true;
        }
        return false;
    }

    private static boolean hasDifferentNeighbor(BlockGetter level, BlockPos pos, Block block) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).getBlock() != block) return true;
        }
        return false;
    }

    private static boolean hasAirNeighbor(BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isAir()) return true;
        }
        return false;
    }
}
