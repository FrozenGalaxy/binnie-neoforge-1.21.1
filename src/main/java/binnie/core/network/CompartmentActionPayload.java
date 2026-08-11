package binnie.core.network;

import binnie.core.BinnieCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded server-authoritative tab operation for an open compartment. */
public record CompartmentActionPayload(int containerId, Action action, int tab, int value, String text)
        implements CustomPacketPayload {
    public enum Action { SELECT, RENAME, CYCLE_COLOR, SET_ICON }
    public static final Type<CompartmentActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BinnieCore.MOD_ID, "compartment_action"));
    private static final StreamCodec<ByteBuf, Action> ACTION_CODEC = ByteBufCodecs.idMapper(
            id -> id >= 0 && id < Action.values().length ? Action.values()[id] : Action.SELECT, Action::ordinal);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompartmentActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CompartmentActionPayload::containerId,
            ACTION_CODEC, CompartmentActionPayload::action,
            ByteBufCodecs.VAR_INT, CompartmentActionPayload::tab,
            ByteBufCodecs.VAR_INT, CompartmentActionPayload::value,
            ByteBufCodecs.stringUtf8(32), CompartmentActionPayload::text,
            CompartmentActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
