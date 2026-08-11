package binnie.trees.forestry;

import forestry.api.IForestryApi;
import forestry.api.arboriculture.ITreeGenData;
import forestry.api.arboriculture.ITreeGenerator;
import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.trees.genetics.Tree;
import forestry.arboriculture.leaves.TileLeaves;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Objects;

/** Tree generator that keeps Binnie's Trees' 1.12 generator family and always places genetic leaves. */
public final class LegacyTreeGenerator implements ITreeGenerator {
    private final IWoodType woodType;
    private final LegacyTreeFeature.GeneratorProfile profile;

    public LegacyTreeGenerator(IWoodType woodType, String generatorToken) {
        this.woodType = Objects.requireNonNull(woodType, "woodType");
        this.profile = LegacyTreeFeature.profile(generatorToken);
    }

    @Override
    public Feature<NoneFeatureConfiguration> getTreeFeature(ITreeGenData tree) {
        return new LegacyTreeFeature(tree, this.profile);
    }

    @Override
    public boolean setLogBlock(IGenome genome, LevelAccessor level, BlockPos pos, Direction facing) {
        boolean fireproof = genome.getActiveValue(TreeChromosomes.FIREPROOF);
        BlockState log = IForestryApi.INSTANCE.getTreeManager()
                .getBlock(this.woodType, WoodBlockKind.LOG, fireproof);
        return level.setBlock(pos, log.setValue(RotatedPillarBlock.AXIS, facing.getAxis()),
                Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_ALL);
    }

    @Override
    public boolean setLeaves(IGenome genome, LevelAccessor level, BlockPos pos, RandomSource random) {
        return placeGeneticLeaves(genome, level, pos);
    }

    public static boolean placeGeneticLeaves(IGenome genome, LevelAccessor level, BlockPos pos) {
        BlockState leaves = ArboricultureBlocks.LEAVES.defaultState();
        int distance = 7;
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            int neighborDistance;
            if (neighbor.is(BlockTags.LOGS)) {
                neighborDistance = 0;
            } else if (neighbor.hasProperty(LeavesBlock.DISTANCE)) {
                neighborDistance = neighbor.getValue(LeavesBlock.DISTANCE);
            } else {
                neighborDistance = 7;
            }
            distance = Math.min(distance, neighborDistance + 1);
        }
        BlockState placedLeaves = leaves.hasProperty(LeavesBlock.DISTANCE)
                ? leaves.setValue(LeavesBlock.DISTANCE, Math.min(7, distance))
                : leaves;
        boolean placed = level.setBlock(pos, placedLeaves, 19);
        if (!placed) {
            return false;
        }

        TileLeaves tileLeaves = TileUtil.getTile(level, pos, TileLeaves.class);
        if (tileLeaves == null) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 19);
            return false;
        }

        Tree tree = new Tree(genome);
        tileLeaves.setTree(tree);
        tileLeaves.setFruit(tree, false);
        return true;
    }

    public LegacyTreeFeature.GeneratorProfile profile() {
        return this.profile;
    }
}
