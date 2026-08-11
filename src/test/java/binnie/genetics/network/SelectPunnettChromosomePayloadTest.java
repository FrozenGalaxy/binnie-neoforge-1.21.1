package binnie.genetics.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SelectPunnettChromosomePayloadTest {
    @Test
    void streamCodecRoundTripsContainerAndChromosomeIndex() {
        SelectPunnettChromosomePayload expected = new SelectPunnettChromosomePayload(73, 11);
        RegistryFriendlyByteBuf buffer = RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY).apply(Unpooled.buffer());
        SelectPunnettChromosomePayload.STREAM_CODEC.encode(buffer, expected);
        assertEquals(expected, SelectPunnettChromosomePayload.STREAM_CODEC.decode(buffer));
    }
}
