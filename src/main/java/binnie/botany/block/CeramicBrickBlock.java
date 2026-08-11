package binnie.botany.block;

import binnie.botany.item.CeramicBrickItem;
import binnie.botany.registry.BotanyWorldContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class CeramicBrickBlock extends BaseEntityBlock {
    private static final MapCodec<CeramicBrickBlock> CODEC = simpleCodec(CeramicBrickBlock::new);
    public static final EnumProperty<CeramicBrickType> TYPE = EnumProperty.create("type", CeramicBrickType.class);
    public CeramicBrickBlock(BlockBehaviour.Properties properties) {
        super(properties); registerDefaultState(stateDefinition.any().setValue(TYPE, CeramicBrickType.TILE));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(TYPE); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CeramicBrickBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof CeramicBrickBlockEntity be) be.setColors(CeramicBrickItem.first(stack), CeramicBrickItem.second(stack));
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        return List.of(be instanceof CeramicBrickBlockEntity brick ? BotanyWorldContent.CERAMIC_BRICK_ITEM.get().createStack(state.getValue(TYPE), brick.first(), brick.second()) : BotanyWorldContent.CERAMIC_BRICK_ITEM.get().createStack(state.getValue(TYPE), "white", "white"));
    }
    @Override public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return level.getBlockEntity(pos) instanceof CeramicBrickBlockEntity brick ? BotanyWorldContent.CERAMIC_BRICK_ITEM.get().createStack(state.getValue(TYPE), brick.first(), brick.second()) : ItemStack.EMPTY;
    }
}
