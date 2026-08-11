package binnie.genetics.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
  * Persistent fractional charge for a single 1.12 laboratory reagent cell.
  *
  * <p>1.12 stored this as a float in the machine's charged-slot component.
  * Keeping ten-thousandths on the stack keeps partial enzyme/dye/catalyst
  * use through inventories, menus, networking and save/load.</p>
  */
public record LaboratoryChargeData(int milliCharge) {
    public static final int FULL = 10_000;
    public static final Codec<LaboratoryChargeData> CODEC = Codec.intRange(0, FULL)
            .xmap(LaboratoryChargeData::new, LaboratoryChargeData::milliCharge);
    public static final StreamCodec<ByteBuf, LaboratoryChargeData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public LaboratoryChargeData {
        if (milliCharge < 0 || milliCharge > FULL) {
            throw new IllegalArgumentException("Laboratory charge must be between zero and one");
        }
    }

    public static LaboratoryChargeData full() {
        return new LaboratoryChargeData(FULL);
    }

    public LaboratoryChargeData consume(int amount) {
        if (amount < 0 || amount > milliCharge) {
            throw new IllegalArgumentException("Cannot consume unavailable laboratory charge");
        }
        return new LaboratoryChargeData(milliCharge - amount);
    }
}
