package binnie.bees.block;

import binnie.bees.BinnieBees;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.apiculture.genetics.IBee;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 1.12 wild-hive bee drop tables, adapted to ForestryCE individuals and item stacks. */
public final class BinnieBeeHiveDrops {
    private static final ResourceLocation VALIANT = ResourceLocation.fromNamespaceAndPath("forestry", "valiant");
    private static final Map<BinnieBeeHiveBlock.HiveType, List<HiveDrop>> DROPS = Map.of(
            BinnieBeeHiveBlock.HiveType.WATER, table("water"),
            BinnieBeeHiveBlock.HiveType.ROCK, table("rock"),
            BinnieBeeHiveBlock.HiveType.NETHER, table("basalt"),
            BinnieBeeHiveBlock.HiveType.MARBLE, table("marble")
    );

    private BinnieBeeHiveDrops() {}

    private static List<HiveDrop> table(String primarySpecies) {
        return List.of(
                new HiveDrop(ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, primarySpecies), 0.80, 0.5),
                new HiveDrop(VALIANT, 0.03, 0.5)
        );
    }

    public static List<HiveDrop> dropTable(BinnieBeeHiveBlock.HiveType type) {
        return DROPS.get(type);
    }

    /** Mirrors the legacy bounded princess retries, single drone roll, and ignoble share. */
    public static List<ItemStack> createDrops(BinnieBeeHiveBlock.HiveType type, RandomSource random, int fortune) {
        if (!hasBeeSpecies(dropTable(type).getFirst().species())) return List.of();
        List<HiveDrop> drops = new ArrayList<>(dropTable(type).stream()
                .filter(drop -> hasBeeSpecies(drop.species()))
                .toList());
        if (drops.isEmpty()) return List.of();
        shuffle(drops, random);
        List<ItemStack> result = new ArrayList<>(2);

        for (int attempt = 0; attempt <= 10; attempt++) {
            HiveDrop selected = roll(drops, random);
            if (selected == null) continue;
            IBee princess = createBee(selected.species());
            if (random.nextDouble() < selected.ignobleChance()) princess.setPristine(false);
            result.add(SpeciesUtil.BEE_TYPE.get().createStack(princess, BeeLifeStage.PRINCESS));
            break;
        }

        HiveDrop drone = roll(drops, random);
        if (drone != null) {
            result.add(SpeciesUtil.BEE_TYPE.get().createStack(createBee(drone.species()), BeeLifeStage.DRONE));
        }
        return result;
    }

    private static HiveDrop roll(List<HiveDrop> drops, RandomSource random) {
        for (HiveDrop drop : drops) {
            if (random.nextDouble() < drop.chance()) return drop;
        }
        return null;
    }

    private static IBee createBee(ResourceLocation speciesId) {
        var species = SpeciesUtil.getBeeSpecies(speciesId);
        if (species == null) throw new IllegalStateException("Missing bee species for wild hive drop: " + speciesId);
        return species.createIndividual();
    }

    private static boolean hasBeeSpecies(ResourceLocation speciesId) {
        return SpeciesUtil.BEE_TYPE.get().getAllSpecies().stream()
                .anyMatch(species -> species.id().equals(speciesId));
    }

    private static void shuffle(List<HiveDrop> drops, RandomSource random) {
        for (int i = drops.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            HiveDrop current = drops.get(i);
            drops.set(i, drops.get(j));
            drops.set(j, current);
        }
    }

    public record HiveDrop(ResourceLocation species, double chance, double ignobleChance) {
    }
}
