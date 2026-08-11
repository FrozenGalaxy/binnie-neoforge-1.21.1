package binnie.trees.forestry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.genetics.IMutationCondition;
import forestry.api.core.genetics.MutationConditionType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

/** Keeps the three 1.12 Binnie's Trees mutations that require a minimum Y level. */
public record LegacyHeightMutationCondition(int min) implements IMutationCondition {
    private static final MapCodec<LegacyHeightMutationCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("min").forGetter(LegacyHeightMutationCondition::min)
    ).apply(instance, LegacyHeightMutationCondition::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, LegacyHeightMutationCondition> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, LegacyHeightMutationCondition::min, LegacyHeightMutationCondition::new);
    public static final MutationConditionType<LegacyHeightMutationCondition> TYPE =
            new MutationConditionType<>(CODEC, STREAM_CODEC);

    public LegacyHeightMutationCondition {
        if (min < -64 || min > 320) {
            throw new IllegalArgumentException("Invalid minimum tree mutation height: " + min);
        }
    }

    @Override
    public float modifyChance(Level level, BlockPos pos, IMutation<?> mutation, IGenome firstGenome,
                              IGenome secondGenome, IClimateProvider climate, float currentChance) {
        return pos.getY() >= this.min ? currentChance : 0.0F;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("mutation.condition.binnietrees.height", this.min);
    }

    @Override
    public MutationConditionType<?> type() {
        return TYPE;
    }
}
