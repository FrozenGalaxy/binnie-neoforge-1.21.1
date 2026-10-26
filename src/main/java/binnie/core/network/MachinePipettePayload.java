package binnie.core.network;

import binnie.core.BinnieCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A request to use a Forestry pipette on one tank in the open machine screen. */
public record MachinePipettePayload(int containerId, int tank) implements CustomPacketPayload {
    public static final Type<MachinePipettePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BinnieCore.MOD_ID, "machine_pipette"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachinePipettePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MachinePipettePayload::containerId,
            ByteBufCodecs.VAR_INT, MachinePipettePayload::tank,
            MachinePipettePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
