package binnie.core.network;

import binnie.core.menu.GenesisMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CorePayloadCodecTest {
    @Test
    void redstoneAndGenesisPayloadsRoundTrip() {
        RegistryFriendlyByteBuf redstoneBuffer = RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY).apply(Unpooled.buffer());
        MachineRedstonePayload redstone = new MachineRedstonePayload(17, 2);
        MachineRedstonePayload.STREAM_CODEC.encode(redstoneBuffer, redstone);
        assertEquals(redstone, MachineRedstonePayload.STREAM_CODEC.decode(redstoneBuffer));

        RegistryFriendlyByteBuf genesisBuffer = RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY).apply(Unpooled.buffer());
        GenesisActionPayload genesis = new GenesisActionPayload(19, GenesisMenu.Action.INACTIVE, -3);
        GenesisActionPayload.STREAM_CODEC.encode(genesisBuffer, genesis);
        assertEquals(genesis, GenesisActionPayload.STREAM_CODEC.decode(genesisBuffer));

        RegistryFriendlyByteBuf absoluteBuffer = RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY).apply(Unpooled.buffer());
        GenesisActionPayload absolute = new GenesisActionPayload(19, GenesisMenu.Action.TYPE, 3, true);
        GenesisActionPayload.STREAM_CODEC.encode(absoluteBuffer, absolute);
        assertEquals(absolute, GenesisActionPayload.STREAM_CODEC.decode(absoluteBuffer));
    }

    @Test
    void compartmentPayloadRoundTripsBoundedTextAndAction() {
        RegistryFriendlyByteBuf buffer = RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY).apply(Unpooled.buffer());
        CompartmentActionPayload expected = new CompartmentActionPayload(21,
                CompartmentActionPayload.Action.RENAME, 7, 0, "Forestry Products");
        CompartmentActionPayload.STREAM_CODEC.encode(buffer, expected);
        assertEquals(expected, CompartmentActionPayload.STREAM_CODEC.decode(buffer));
    }
}
