package binnie.trees.gametest;

import binnie.trees.block.HopsBlock;
import binnie.trees.registry.BinnieTreesContent;
import binnie.trees.registry.BinnieTreesWorldContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** World-level parity checks for the non-machine Binnie's Trees gameplay layer. */
@GameTestHolder("binniecore")
@PrefixGameTestTemplate(false)
public final class BinnieTreesWorldGameTests {
    private BinnieTreesWorldGameTests() {}

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void hopsPlaceAsSynchronizedTwoBlockCrop(GameTestHelper helper) {
        BlockPos lower = new BlockPos(2, 2, 2);
        helper.setBlock(lower.below(), Blocks.FARMLAND);
        BinnieTreesWorldContent.HOPS.get().placeAt(helper.getLevel(), helper.absolutePos(lower), 3, 3);

        var lowerState = helper.getBlockState(lower);
        var upperState = helper.getBlockState(lower.above());
        helper.assertTrue(lowerState.is(BinnieTreesWorldContent.HOPS.get())
                        && lowerState.getValue(HopsBlock.HALF) == DoubleBlockHalf.LOWER
                        && lowerState.getValue(HopsBlock.AGE) == 3,
                "Lower hops block did not retain its age and half");
        helper.assertTrue(upperState.is(BinnieTreesWorldContent.HOPS.get())
                        && upperState.getValue(HopsBlock.HALF) == DoubleBlockHalf.UPPER
                        && upperState.getValue(HopsBlock.AGE) == 3,
                "Upper hops block did not mirror the lower age");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void matureHopsHarvestDropsCropAndResetsBothHalves(GameTestHelper helper) {
        BlockPos lower = new BlockPos(2, 2, 2);
        helper.setBlock(lower.below(), Blocks.FARMLAND);
        BinnieTreesWorldContent.HOPS.get().placeAt(helper.getLevel(), helper.absolutePos(lower), 7, 3);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absolute = helper.absolutePos(lower.above());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        helper.getBlockState(lower.above()).useWithoutItem(helper.getLevel(), player, hit);

        helper.assertTrue(helper.getBlockState(lower).getValue(HopsBlock.AGE) == 0
                        && helper.getBlockState(lower.above()).getValue(HopsBlock.AGE) == 0,
                "Harvesting mature hops did not reset the two-block crop");
        helper.assertItemEntityPresent(BinnieTreesContent.HOPS.get(), lower, 2.0D);
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void breakingUpperHopsRemovesAndDropsLowerCrop(GameTestHelper helper) {
        BlockPos lower = new BlockPos(2, 2, 2);
        helper.setBlock(lower.below(), Blocks.FARMLAND);
        BinnieTreesWorldContent.HOPS.get().placeAt(helper.getLevel(), helper.absolutePos(lower), 5, 3);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos upperAbsolute = helper.absolutePos(lower.above());
        var upperState = helper.getBlockState(lower.above());
        upperState.getBlock().playerWillDestroy(helper.getLevel(), upperAbsolute, upperState, player);
        helper.getLevel().setBlock(upperAbsolute, Blocks.AIR.defaultBlockState(), 3);
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(BinnieTreesWorldContent.HOPS.get(), lower);
            helper.assertBlockNotPresent(BinnieTreesWorldContent.HOPS.get(), lower.above());
            helper.assertItemEntityPresent(BinnieTreesContent.HOPS.get(), lower, 2.0D);
        });
    }
}
