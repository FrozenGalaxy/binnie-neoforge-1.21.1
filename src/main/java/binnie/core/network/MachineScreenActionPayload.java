package binnie.core.network;

import binnie.core.BinnieCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A bounded serverbound request for a 1.12 source-screen control. */
public record MachineScreenActionPayload(int containerId, Action action) implements CustomPacketPayload {
    public enum Action { FRUIT_PRESS, CLEAR_FRUIT }

    public static final Type<MachineScreenActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BinnieCore.MOD_ID, "machine_screen_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineScreenActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MachineScreenActionPayload::containerId,
            ByteBufCodecs.idMapper(id -> Action.values()[id], Action::ordinal), MachineScreenActionPayload::action,
            MachineScreenActionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
