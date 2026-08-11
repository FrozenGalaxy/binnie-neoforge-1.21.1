package binnie.bees.block;

import binnie.bees.BinnieBees;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
  * 1.12 wild-hive species pools. Keeping the selection in one server-side class makes
  * world generation, block harvesting, commands, and tests agree on the same hive ecology.
  */
public final class BinnieBeeHiveDrops {
    private static final Map<BinnieBeeHiveBlock.HiveType, List<String>> POOLS = Map.of(
            BinnieBeeHiveBlock.HiveType.WATER, List.of("water", "river", "ocean", "ink"),
            BinnieBeeHiveBlock.HiveType.ROCK, List.of("rock", "stone", "granite", "mineral"),
            BinnieBeeHiveBlock.HiveType.NETHER, List.of("glowstone", "basalt", "volcanic", "malicious"),
            BinnieBeeHiveBlock.HiveType.MARBLE, List.of("marble", "roman", "greek", "classical")
    );

    private BinnieBeeHiveDrops() {}

    public static List<String> pool(BinnieBeeHiveBlock.HiveType type) {
        return POOLS.get(type);
    }

    public static ResourceLocation selectSpecies(BinnieBeeHiveBlock.HiveType type, RandomSource random) {
        List<String> pool = pool(type);
        return ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, pool.get(random.nextInt(pool.size())));
    }

    public static List<ItemStack> createDrops(BinnieBeeHiveBlock.HiveType type, RandomSource random, int fortune) {
        ResourceLocation species = selectSpecies(type, random);
        ItemStack princess = SpeciesUtil.BEE_TYPE.get().createStack(species, BeeLifeStage.PRINCESS);
        ItemStack drones = SpeciesUtil.BEE_TYPE.get().createStack(species, BeeLifeStage.DRONE);
        drones.setCount(Math.min(4, 1 + random.nextInt(2 + Math.max(0, fortune))));
        return List.of(princess, drones);
    }
}
