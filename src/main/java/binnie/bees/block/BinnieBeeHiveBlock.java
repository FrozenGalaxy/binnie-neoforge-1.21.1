package binnie.bees.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

public final class BinnieBeeHiveBlock extends Block {
    private final HiveType hiveType;

    public BinnieBeeHiveBlock(Properties properties, HiveType hiveType) {
        super(properties);
        this.hiveType = hiveType;
    }

    public HiveType hiveType() {
        return hiveType;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return BinnieBeeHiveDrops.createDrops(hiveType, builder.getLevel().getRandom(), 0);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    // Wild hives are stable until harvested. Swarm creation is handled by Forestry.
    }

    public enum HiveType {
        WATER("water"), ROCK("rock"), NETHER("nether"), MARBLE("marble");
        private final String id;
        HiveType(String id) { this.id = id; }
        public String id() { return id; }
    }
}
