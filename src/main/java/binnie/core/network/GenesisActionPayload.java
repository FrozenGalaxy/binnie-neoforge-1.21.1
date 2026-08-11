package binnie.core.network;

import binnie.core.BinnieCore;
import binnie.core.menu.GenesisMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded action request for the currently open creative Genesis builder. */
public record GenesisActionPayload(int containerId, GenesisMenu.Action action, int delta, boolean absolute) implements CustomPacketPayload {
    public static final Type<GenesisActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BinnieCore.MOD_ID, "genesis_action"));
    private static final StreamCodec<ByteBuf, GenesisMenu.Action> ACTION_CODEC = ByteBufCodecs.idMapper(
            id -> id >= 0 && id < GenesisMenu.Action.values().length ? GenesisMenu.Action.values()[id] : GenesisMenu.Action.RESET,
            GenesisMenu.Action::ordinal);
    public static final StreamCodec<RegistryFriendlyByteBuf, GenesisActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GenesisActionPayload::containerId,
            ACTION_CODEC, GenesisActionPayload::action,
            ByteBufCodecs.VAR_INT, GenesisActionPayload::delta,
            ByteBufCodecs.BOOL, GenesisActionPayload::absolute,
            GenesisActionPayload::new);
    public GenesisActionPayload(int containerId, GenesisMenu.Action action, int delta) {
        this(containerId, action, delta, false);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
