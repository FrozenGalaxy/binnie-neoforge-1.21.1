package binnie.core.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MachineModePayloadTest {
    @Test
    void streamCodecRoundTripsContainerAndBoundedModeRequest() {
        MachineModePayload expected = new MachineModePayload(41, 2);
        RegistryFriendlyByteBuf buffer = RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY, ConnectionType.NEOFORGE).apply(Unpooled.buffer());
        MachineModePayload.STREAM_CODEC.encode(buffer, expected);
        assertEquals(expected, MachineModePayload.STREAM_CODEC.decode(buffer));
    }

    @Test
    void manualDesignerPayloadsRoundTripWithoutClientSuppliedStacks() {
        RegistryFriendlyByteBuf selectionBuffer = RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY, ConnectionType.NEOFORGE).apply(Unpooled.buffer());
        ManualMachineSelectionPayload selection = new ManualMachineSelectionPayload(41, 100);
        ManualMachineSelectionPayload.STREAM_CODEC.encode(selectionBuffer, selection);
        assertEquals(selection, ManualMachineSelectionPayload.STREAM_CODEC.decode(selectionBuffer));

        RegistryFriendlyByteBuf craftBuffer = RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY, ConnectionType.NEOFORGE).apply(Unpooled.buffer());
        ManualMachineCraftPayload craft = new ManualMachineCraftPayload(41);
        ManualMachineCraftPayload.STREAM_CODEC.encode(craftBuffer, craft);
        assertEquals(craft, ManualMachineCraftPayload.STREAM_CODEC.decode(craftBuffer));
    }
}
