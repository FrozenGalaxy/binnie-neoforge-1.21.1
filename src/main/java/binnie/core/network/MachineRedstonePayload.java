package binnie.core.network;

import binnie.core.BinnieCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serverbound request to change the redstone gate of the currently open machine. */
public record MachineRedstonePayload(int containerId, int mode) implements CustomPacketPayload {
    public static final Type<MachineRedstonePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BinnieCore.MOD_ID, "machine_redstone"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineRedstonePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MachineRedstonePayload::containerId,
            ByteBufCodecs.VAR_INT, MachineRedstonePayload::mode,
            MachineRedstonePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
