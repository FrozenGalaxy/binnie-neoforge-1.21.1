package binnie.botany.block;

import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import binnie.botany.registry.BotanyWorldContent;
import binnie.botany.gardening.BotanyGardening;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/** Persistent 1.12 Botany soil state: moisture, pH, nutrients and weedkiller. */
public final class BotanySoilBlock extends Block {
    private static final VoxelShape LEGACY_SHAPE = Block.box(0, 0, 0, 16, 15, 16);
    public static final EnumProperty<SoilMoisture> MOISTURE = EnumProperty.create("moisture", SoilMoisture.class);
    public static final EnumProperty<SoilAcidity> ACIDITY = EnumProperty.create("acidity", SoilAcidity.class);
    public static final IntegerProperty FERTILITY = IntegerProperty.create("fertility", 0, 3);
    public static final BooleanProperty WEEDKILLED = BooleanProperty.create("weedkilled");

    private final SoilQuality quality;

    public BotanySoilBlock(Properties properties, SoilQuality quality) {
        super(properties.randomTicks());
        this.quality = quality;
        registerDefaultState(stateDefinition.any()
                .setValue(MOISTURE, SoilMoisture.NORMAL)
                .setValue(ACIDITY, SoilAcidity.NEUTRAL)
                .setValue(FERTILITY, quality.initialFertility())
                .setValue(WEEDKILLED, false));
    }

    public SoilQuality quality() {
        return quality;
    }


    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LEGACY_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LEGACY_SHAPE;
    }
    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(properties -> new BotanySoilBlock(properties, quality));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOISTURE, ACIDITY, FERTILITY, WEEDKILLED);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        SoilMoisture moisture = state.getValue(MOISTURE);
        SoilMoisture desired = BotanyGardening.naturalMoisture(level, pos);
        if (desired.ordinal() > moisture.ordinal()) {
            moisture = moisture == SoilMoisture.DRY ? SoilMoisture.NORMAL : SoilMoisture.DAMP;
        } else if (desired.ordinal() < moisture.ordinal()) {
            moisture = moisture == SoilMoisture.DAMP ? SoilMoisture.NORMAL : SoilMoisture.DRY;
        }
        if (moisture != state.getValue(MOISTURE)) {
            state = state.setValue(MOISTURE, moisture);
            level.setBlock(pos, state, UPDATE_CLIENTS);
        }

    // The 1.12 implementation 1.12 behavior: higher-grade garden soils are more biologically active and therefore
    // sprout weeds more often unless they were treated with weedkiller.
        int weedChance = 5 - quality.ordinal();
        if (!state.getValue(WEEDKILLED) && level.isEmptyBlock(pos.above()) && random.nextInt(weedChance) == 0) {
            level.setBlock(pos.above(), BotanyWorldContent.GARDEN_PLANT.get().defaultBlockState()
                    .setValue(GardenPlantBlock.TYPE, GardenPlantType.WEEDS), UPDATE_ALL);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.WATER_BUCKET)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(MOISTURE, SoilMoisture.DAMP), UPDATE_ALL);
            if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public static boolean isSuitableSoil(BlockState state) {
        return state.getBlock() instanceof BotanySoilBlock;
    }

    public static boolean withinTolerance(int actualOrdinal, int idealOrdinal, forestry.api.core.ToleranceType tolerance) {
        return actualOrdinal >= Math.max(0, idealOrdinal - tolerance.down)
                && actualOrdinal <= idealOrdinal + tolerance.up;
    }
}
