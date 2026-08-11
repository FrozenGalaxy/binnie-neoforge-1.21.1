package binnie.core.network;

import binnie.core.BinnieCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serverbound click on a manual machine's preview result. */
public record ManualMachineCraftPayload(int containerId) implements CustomPacketPayload {
    public static final Type<ManualMachineCraftPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BinnieCore.MOD_ID, "manual_machine_craft"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManualMachineCraftPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ManualMachineCraftPayload::containerId,
            ManualMachineCraftPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
