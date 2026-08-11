package binnie.botany.block;

import binnie.botany.registry.BotanyWorldContent;
import binnie.core.catalog.LegacyCatalogs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/** Component-backed replacement for the 80 metadata variants of 1.12 BlockCeramic. */
public final class CeramicBlock extends Block {
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 79);

    public CeramicBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(stackFor(state));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return stackFor(state);
    }

    private static ItemStack stackFor(BlockState state) {
        String color = LegacyCatalogs.values("binniebotany.flower_color").get(state.getValue(COLOR));
        return BotanyWorldContent.CERAMIC_ITEM.get().createStack(color);
    }
}
