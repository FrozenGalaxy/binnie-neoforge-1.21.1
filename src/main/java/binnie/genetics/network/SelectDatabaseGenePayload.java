package binnie.genetics.network;

import binnie.genetics.BinnieGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serverbound request to apply one visible gene from the open database to the carried serum. */
public record SelectDatabaseGenePayload(int containerId, int geneIndex) implements CustomPacketPayload {
    public static final Type<SelectDatabaseGenePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BinnieGenetics.MOD_ID, "select_database_gene"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectDatabaseGenePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectDatabaseGenePayload::containerId,
            ByteBufCodecs.VAR_INT, SelectDatabaseGenePayload::geneIndex,
            SelectDatabaseGenePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
