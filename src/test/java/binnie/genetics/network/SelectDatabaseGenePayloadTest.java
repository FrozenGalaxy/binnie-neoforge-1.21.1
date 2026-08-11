package binnie.genetics.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SelectDatabaseGenePayloadTest {
    @Test
    void streamCodecRoundTripsContainerAndGeneIndex() {
        SelectDatabaseGenePayload expected = new SelectDatabaseGenePayload(91, 247);
        RegistryFriendlyByteBuf buffer = RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY).apply(Unpooled.buffer());
        SelectDatabaseGenePayload.STREAM_CODEC.encode(buffer, expected);
        assertEquals(expected, SelectDatabaseGenePayload.STREAM_CODEC.decode(buffer));
    }
}
