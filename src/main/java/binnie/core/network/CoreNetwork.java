package binnie.core.network;

import binnie.core.machine.MachineMenu;
import binnie.core.storage.CompartmentMenu;
import binnie.core.menu.GenesisMenu;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Shared server-authoritative machine payloads. */
public class CoreNetwork {
    private CoreNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(MachineModePayload.TYPE, MachineModePayload.STREAM_CODEC,
                (payload, context) -> handleMode(payload, (ServerPlayer) context.player()));
        registrar.playToServer(MachineRedstonePayload.TYPE, MachineRedstonePayload.STREAM_CODEC,
                (payload, context) -> handleRedstone(payload, (ServerPlayer) context.player()));
        registrar.playToServer(CompartmentActionPayload.TYPE, CompartmentActionPayload.STREAM_CODEC,
                (payload, context) -> handleCompartment(payload, (ServerPlayer) context.player()));
        registrar.playToServer(GenesisActionPayload.TYPE, GenesisActionPayload.STREAM_CODEC,
                (payload, context) -> handleGenesis(payload, (ServerPlayer) context.player()));
        registrar.playToServer(ManualMachineSelectionPayload.TYPE, ManualMachineSelectionPayload.STREAM_CODEC,
                (payload, context) -> handleManualSelection(payload, (ServerPlayer) context.player()));
        registrar.playToServer(ManualMachineCraftPayload.TYPE, ManualMachineCraftPayload.STREAM_CODEC,
                (payload, context) -> handleManualCraft(payload, (ServerPlayer) context.player()));
        registrar.playToServer(MachineScreenActionPayload.TYPE, MachineScreenActionPayload.STREAM_CODEC,
                (payload, context) -> handleScreenAction(payload, (ServerPlayer) context.player()));
    }

    public static boolean handleMode(MachineModePayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof MachineMenu menu)) return false;
        if (menu.containerId != payload.containerId()) return false;
        return menu.setMode(payload.mode());
    }

    public static boolean handleRedstone(MachineRedstonePayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof MachineMenu menu)) return false;
        if (menu.containerId != payload.containerId()) return false;
        return menu.setRedstoneMode(payload.mode());
    }

    public static boolean handleCompartment(CompartmentActionPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof CompartmentMenu menu)) return false;
        if (menu.containerId != payload.containerId()) return false;
        return switch (payload.action()) {
            case SELECT -> menu.selectTab(payload.tab());
            case RENAME -> menu.renameTab(payload.tab(), payload.text());
            case CYCLE_COLOR -> menu.cycleColor(payload.tab(), payload.value());
            case SET_ICON -> menu.setIconFromCarried(payload.tab());
        };
    }

    public static boolean handleGenesis(GenesisActionPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof GenesisMenu menu)) return false;
        if (menu.containerId != payload.containerId()) return false;
        return payload.absolute()
                ? menu.select(payload.action(), payload.delta())
                : menu.apply(payload.action(), payload.delta());
    }

    public static boolean handleManualSelection(ManualMachineSelectionPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof MachineMenu menu)) return false;
        if (menu.containerId != payload.containerId() || !menu.isManualCrafting()) return false;
        return menu.setManualSelection(payload.selection());
    }

    public static boolean handleManualCraft(ManualMachineCraftPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof MachineMenu menu)) return false;
        if (menu.containerId != payload.containerId() || !menu.isManualCrafting()) return false;
        return menu.takeManualPreview();
    }

    public static boolean handleScreenAction(MachineScreenActionPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof MachineMenu menu)) return false;
        if (menu.containerId != payload.containerId()) return false;
        return menu.handleScreenAction(payload.action());
    }
}
