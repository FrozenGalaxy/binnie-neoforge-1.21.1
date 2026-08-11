package binnie.bees.forestry;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.core.genetics.IEffectData;
import forestry.api.core.genetics.IGenome;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.phys.AABB;

/**
  * Safe server-side implementations for all 1.12 Binnie's Bees effect allele identities.
  *
  * <p>Effects that historically depended on removed integrations use a deterministic vanilla
  * analogue instead of silently doing nothing. Expensive effects are rate-limited.</p>
  */
public final class LegacyBeeEffects {
    private LegacyBeeEffects() {}

    public static IBeeEffect create(String id) {
        return new Effect(id);
    }

    private record Effect(String id) implements IBeeEffect {
        @Override public boolean isDominant() { return true; }

        @Override
        public IEffectData doEffect(IGenome genome, IEffectData storedData, IBeeHousing housing) {
            Level level = housing.getLevel();
            if (!(level instanceof ServerLevel server) || server.random.nextInt(20) != 0) return storedData;
            BlockPos center = housing.getBlockPos();
            switch (id) {
                case "lightning", "meteor" -> lightning(server, center, id.equals("meteor") ? 180 : 120);
                case "spawn_zombie" -> spawn(server, center, EntityType.ZOMBIE);
                case "spawn_skeleton" -> spawn(server, center, EntityType.SKELETON);
                case "spawn_creeper" -> spawn(server, center, EntityType.CREEPER);
                case "hunger" -> affectPlayers(server, center, new MobEffectInstance(MobEffects.HUNGER, 100, 0));
                case "blindness" -> affectPlayers(server, center, new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                case "confusion" -> affectPlayers(server, center, new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                case "wither", "radioactive" -> affectPlayers(server, center, new MobEffectInstance(MobEffects.WITHER, 60, 0));
                case "slow" -> affectPlayers(server, center, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
                case "gravity" -> affectPlayers(server, center, new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
                case "food" -> affectPlayers(server, center, new MobEffectInstance(MobEffects.SATURATION, 20, 0));
                case "water" -> players(server, center).forEach(ServerPlayer::clearFire);
                case "acid", "corrosive", "caustic" -> players(server, center)
                        .forEach(player -> player.hurt(server.damageSources().magic(), 1.0F));
                case "ectoplasm", "shadow", "darkened" ->
                        affectPlayers(server, center, new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0));
                case "fireworks", "festival", "birthday", "celebratory" ->
                        affectPlayers(server, center, new MobEffectInstance(MobEffects.GLOWING, 120, 0));
                case "teleport", "spatial", "quantum" -> teleportPlayers(server, center);
                case "thief" -> players(server, center).forEach(player -> {
                    if (server.random.nextInt(40) == 0) {
                        int slot = server.random.nextInt(player.getInventory().getContainerSize());
                        var stolen = player.getInventory().removeItem(slot, 1);
                        if (!stolen.isEmpty()) player.drop(stolen, false);
                    }
                });
                case "bonemeal_sapling", "bonemeal_fruit", "bonemeal_mushroom", "growing", "blooming" ->
                        bonemealNearby(server, center);
                default -> {
                    // Identity-only effects whose original visual/external-mod dependency no longer exists.
                }
            }
            return storedData;
        }

        private static java.util.List<ServerPlayer> players(ServerLevel level, BlockPos center) {
            return level.getEntitiesOfClass(ServerPlayer.class, new AABB(center).inflate(6));
        }

        private static void affectPlayers(ServerLevel level, BlockPos center, MobEffectInstance effect) {
            players(level, center).forEach(player -> player.addEffect(new MobEffectInstance(effect)));
        }

        private static void lightning(ServerLevel level, BlockPos center, int rarity) {
            if (level.random.nextInt(rarity) != 0) return;
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5);
                level.addFreshEntity(bolt);
            }
        }

        private static void teleportPlayers(ServerLevel level, BlockPos center) {
            for (ServerPlayer player : players(level, center)) {
                if (level.random.nextInt(40) != 0) continue;
                double x = player.getX() + level.random.nextInt(17) - 8;
                double z = player.getZ() + level.random.nextInt(17) - 8;
                player.randomTeleport(x, player.getY(), z, true);
            }
        }

        private static void bonemealNearby(ServerLevel level, BlockPos center) {
            for (int attempt = 0; attempt < 8; attempt++) {
                BlockPos pos = center.offset(level.random.nextInt(11) - 5,
                        level.random.nextInt(5) - 2, level.random.nextInt(11) - 5);
                var state = level.getBlockState(pos);
                if (state.getBlock() instanceof BonemealableBlock growable && growable.isValidBonemealTarget(level, pos, state)) {
                    if (growable.isBonemealSuccess(level, level.random, pos, state)) {
                        growable.performBonemeal(level, level.random, pos, state);
                    }
                    return;
                }
            }
        }

        private static <T extends Mob> void spawn(ServerLevel level, BlockPos center, EntityType<T> type) {
            if (level.random.nextInt(80) != 0) return;
            T mob = type.create(level);
            if (mob != null) {
                mob.moveTo(center.getX() + 1.5, center.getY(), center.getZ() + 1.5,
                        level.random.nextFloat() * 360F, 0);
                if (level.noCollision(mob)) level.addFreshEntity(mob);
            }
        }
    }
}
