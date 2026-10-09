package binnie.core.liquid;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** 1.12 intoxication curve used by consumable beverage containers. */
public class AlcoholEffect {
    private AlcoholEffect() {}

    public static void makeDrunk(Player player, float alcoholUnits) {
        MobEffectInstance nausea = player.getEffect(MobEffects.CONFUSION);
        int existingStrength = nausea == null ? 0 : nausea.getAmplifier();
        int existingTime = nausea == null ? 0 : nausea.getDuration();
        float strength = 100.0F * Math.max(0.0F, alcoholUnits);
        int time = Math.max(5, (int) (100.0D * Math.sqrt(strength)) + existingTime);
        float intensity = 0.1F * strength + existingStrength + existingTime / 500.0F;
        float slowIntensity = Math.max(0.0F, (intensity - 10.0F) / 4.0F);
        float blindIntensity = Math.max(0.0F, (intensity - 25.0F) / 2.0F);

        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, time, (int) intensity, false, true));
        if (slowIntensity > 0.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, time, (int) slowIntensity, false, true));
        }
        if (blindIntensity > 0.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, time, (int) blindIntensity, false, true));
        }
    }
}
