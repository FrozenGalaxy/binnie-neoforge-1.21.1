package binnie.genetics.network;

import binnie.genetics.BinnieGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serverbound request to select one chromosome in the currently open Punnett menu. */
public record SelectPunnettChromosomePayload(int containerId, int chromosomeIndex) implements CustomPacketPayload {
    public static final Type<SelectPunnettChromosomePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BinnieGenetics.MOD_ID, "select_punnett_chromosome"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectPunnettChromosomePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectPunnettChromosomePayload::containerId,
            ByteBufCodecs.VAR_INT, SelectPunnettChromosomePayload::chromosomeIndex,
            SelectPunnettChromosomePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
