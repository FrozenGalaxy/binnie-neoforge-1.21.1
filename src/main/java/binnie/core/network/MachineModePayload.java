package binnie.core.network;

import binnie.core.BinnieCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serverbound request to change a bounded mode on the currently open machine. */
public record MachineModePayload(int containerId, int mode) implements CustomPacketPayload {
    public static final Type<MachineModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BinnieCore.MOD_ID, "machine_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MachineModePayload::containerId,
            ByteBufCodecs.VAR_INT, MachineModePayload::mode,
            MachineModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
