package binnie.bees.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import forestry.apiculture.hives.HiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import binnie.bees.registry.BinnieBeesWorldContent;
import forestry.api.ForestryTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public final class BinnieBeeHiveBlock extends Block implements EntityBlock {
    private final HiveType hiveType;

    public BinnieBeeHiveBlock(Properties properties, HiveType hiveType) {
        super(properties);
        this.hiveType = hiveType;
    }

    public HiveType hiveType() {
        return hiveType;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BinnieBeeHiveBlockEntity(BinnieBeesWorldContent.HIVE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level,
                                                                    BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != BinnieBeesWorldContent.HIVE_BLOCK_ENTITY.get()) return null;
        return (tickLevel, tickPos, tickState, blockEntity) -> ((HiveBlockEntity) blockEntity).tick(tickLevel);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (!tool.is(ForestryTags.Items.SCOOPS)) return List.of();
        var enchantments = builder.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.SILK_TOUCH), tool) > 0) {
            return List.of(new ItemStack(this));
        }
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.FORTUNE), tool);
        return BinnieBeeHiveDrops.createDrops(hiveType, builder.getLevel().getRandom(), fortune);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    // Wild hives are stable until harvested. Swarm creation is handled by Forestry.
    }

    public enum HiveType {
        WATER("water", "water"), ROCK("rock", "rock"), MARBLE("marble", "marble");
        private final String id;
        private final String beeSpeciesId;
        HiveType(String id, String beeSpeciesId) { this.id = id; this.beeSpeciesId = beeSpeciesId; }
        public String id() { return id; }
        public String beeSpeciesId() { return beeSpeciesId; }
    }
}
