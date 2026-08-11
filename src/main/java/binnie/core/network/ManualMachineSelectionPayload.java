package binnie.core.network;

import binnie.core.BinnieCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serverbound selection for a 1.12-style manual machine such as the Designer. */
public record ManualMachineSelectionPayload(int containerId, int selection) implements CustomPacketPayload {
    public static final Type<ManualMachineSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BinnieCore.MOD_ID, "manual_machine_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManualMachineSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ManualMachineSelectionPayload::containerId,
            ByteBufCodecs.VAR_INT, ManualMachineSelectionPayload::selection,
            ManualMachineSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
