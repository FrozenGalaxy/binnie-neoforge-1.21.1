package binnie.botany.block;

import binnie.botany.gardening.BotanyGardening;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Non-item garden plant used for the 1.12 weed and dead-flower lifecycle. */
public final class GardenPlantBlock extends Block {
    public static final EnumProperty<GardenPlantType> TYPE = EnumProperty.create("type", GardenPlantType.class);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public GardenPlantBlock(Properties properties) {
        super(properties.noCollission().instabreak().randomTicks().replaceable());
        registerDefaultState(stateDefinition.any().setValue(TYPE, GardenPlantType.WEEDS));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(GardenPlantBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof BotanySoilBlock;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, false);
            return;
        }

        GardenPlantType type = state.getValue(TYPE);
        if (random.nextInt(4) == 0) {
            GardenPlantType next = switch (type) {
                case WEEDS -> GardenPlantType.LONG_WEEDS;
                case LONG_WEEDS -> GardenPlantType.VERY_LONG_WEEDS;
                case VERY_LONG_WEEDS -> GardenPlantType.VERY_LONG_WEEDS;
                case DEAD_FLOWER -> GardenPlantType.DECAYING_FLOWER;
                case DECAYING_FLOWER -> null;
            };
            if (next == null) {
                level.destroyBlock(pos, false);
                return;
            }
            if (next != type) {
                type = next;
                state = state.setValue(TYPE, next);
                level.setBlock(pos, state, UPDATE_CLIENTS);
            }
        }

    // 1.12 weeds also regress naturally, preventing every untreated bed from remaining overgrown forever.
        if (type.isWeed() && random.nextInt(6) == 0) {
            GardenPlantType previous = switch (type) {
                case WEEDS -> null;
                case LONG_WEEDS -> GardenPlantType.WEEDS;
                case VERY_LONG_WEEDS -> GardenPlantType.LONG_WEEDS;
                default -> type;
            };
            if (previous == null) {
                level.destroyBlock(pos, false);
                return;
            }
            if (previous != type) {
                type = previous;
                state = state.setValue(TYPE, previous);
                level.setBlock(pos, state, UPDATE_CLIENTS);
            }
        }

        if (random.nextInt(3) != 0) return;
        BlockPos soilPos = pos.below();
        BlockState soilState = level.getBlockState(soilPos);
        if (!(soilState.getBlock() instanceof BotanySoilBlock soil)) return;

        if (type.isWeed()) {
            SoilQuality target = soil.quality().ordinal() > SoilQuality.LOAM.ordinal()
                    ? SoilQuality.LOAM : SoilQuality.SOIL;
            if (target != soil.quality()) {
                level.setBlock(soilPos, BotanyGardening.degradeQuality(soilState, target), UPDATE_CLIENTS);
            }
        } else if (type == GardenPlantType.DECAYING_FLOWER) {
            SoilQuality target = soil.quality() == SoilQuality.SOIL ? SoilQuality.LOAM : SoilQuality.FLOWERBED;
            if (target != soil.quality()) {
                level.setBlock(soilPos, BotanyGardening.changeQuality(soilState, target), UPDATE_CLIENTS);
            }
        }
    }
}
