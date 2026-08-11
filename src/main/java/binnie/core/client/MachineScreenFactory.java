package binnie.core.client;

import binnie.core.machine.MachineMenu;
import binnie.design.client.screen.GlassworkerScreen;
import binnie.design.client.screen.PanelworkerScreen;
import binnie.design.client.screen.TileworkerScreen;
import binnie.design.client.screen.WoodworkerScreen;
import binnie.trees.client.screen.BreweryScreen;
import binnie.trees.client.screen.DistilleryScreen;
import binnie.trees.client.screen.LumbermillScreen;
import binnie.trees.client.screen.PressScreen;
import binnie.genetics.client.AcclimatiserScreen;
import binnie.genetics.client.AnalyserScreen;
import binnie.genetics.client.GenePoolScreen;
import binnie.genetics.client.IncubatorScreen;
import binnie.genetics.client.InoculatorScreen;
import binnie.genetics.client.IsolatorScreen;
import binnie.genetics.client.LaboratoryMachineScreen;
import binnie.genetics.client.PolymeriserScreen;
import binnie.genetics.client.SequencerScreen;
import binnie.genetics.client.SplicerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Dispatches the shared machine menu to one concrete screen class per registered machine. */
public final class MachineScreenFactory {
    private MachineScreenFactory() {
    }

    public static AbstractContainerScreen<MachineMenu> create(MachineMenu menu, Inventory inventory, Component title) {
        return switch (menu.machineId().getNamespace()) {
            case "binnietrees" -> binnieTrees(menu, inventory, title);
            case "binniegenetics" -> binniegenetics(menu, inventory, title);
            case "binniebotany" -> binniebotany(menu, inventory, title);
            default -> new GenericMachineScreen(menu, inventory, title);
        };
    }

    private static AbstractContainerScreen<MachineMenu> binnieTrees(MachineMenu menu, Inventory inventory, Component title) {
        return switch (menu.machineId().getPath()) {
            case "press" -> new PressScreen(menu, inventory, title);
            case "brewery" -> new BreweryScreen(menu, inventory, title);
            case "distillery" -> new DistilleryScreen(menu, inventory, title);
            case "lumbermill" -> new LumbermillScreen(menu, inventory, title);
            case "woodworker" -> new WoodworkerScreen(menu, inventory, title);
            case "panelworker" -> new PanelworkerScreen(menu, inventory, title);
            case "glassworker" -> new GlassworkerScreen(menu, inventory, title);
            default -> new GenericMachineScreen(menu, inventory, title);
        };
    }

    private static AbstractContainerScreen<MachineMenu> binniegenetics(MachineMenu menu, Inventory inventory, Component title) {
        return switch (menu.machineId().getPath()) {
            case "isolator" -> new IsolatorScreen(menu, inventory, title);
            case "sequencer" -> new SequencerScreen(menu, inventory, title);
            case "polymeriser" -> new PolymeriserScreen(menu, inventory, title);
            case "inoculator" -> new InoculatorScreen(menu, inventory, title);
            case "splicer" -> new SplicerScreen(menu, inventory, title);
            case "analyser" -> new AnalyserScreen(menu, inventory, title);
            case "genepool" -> new GenePoolScreen(menu, inventory, title);
            case "incubator" -> new IncubatorScreen(menu, inventory, title);
            case "acclimatiser" -> new AcclimatiserScreen(menu, inventory, title);
            case "laboratory_machine" -> new LaboratoryMachineScreen(menu, inventory, title);
            default -> new GenericMachineScreen(menu, inventory, title);
        };
    }

    private static AbstractContainerScreen<MachineMenu> binniebotany(MachineMenu menu, Inventory inventory, Component title) {
        return "tileworker".equals(menu.machineId().getPath())
                ? new TileworkerScreen(menu, inventory, title)
                : new GenericMachineScreen(menu, inventory, title);
    }


    /** Pure mapping used by regression tests and tooling without constructing a Minecraft screen. */
    public static Class<? extends AbstractContainerScreen<MachineMenu>> screenClass(ResourceLocation id) {
        String key = id.getNamespace() + ":" + id.getPath();
        return switch (key) {
            case "binnietrees:press" -> PressScreen.class;
            case "binnietrees:brewery" -> BreweryScreen.class;
            case "binnietrees:distillery" -> DistilleryScreen.class;
            case "binnietrees:lumbermill" -> LumbermillScreen.class;
            case "binnietrees:woodworker" -> WoodworkerScreen.class;
            case "binnietrees:panelworker" -> PanelworkerScreen.class;
            case "binnietrees:glassworker" -> GlassworkerScreen.class;
            case "binniebotany:tileworker" -> TileworkerScreen.class;
            case "binniegenetics:isolator" -> IsolatorScreen.class;
            case "binniegenetics:sequencer" -> SequencerScreen.class;
            case "binniegenetics:polymeriser" -> PolymeriserScreen.class;
            case "binniegenetics:inoculator" -> InoculatorScreen.class;
            case "binniegenetics:splicer" -> SplicerScreen.class;
            case "binniegenetics:analyser" -> AnalyserScreen.class;
            case "binniegenetics:genepool" -> GenePoolScreen.class;
            case "binniegenetics:incubator" -> IncubatorScreen.class;
            case "binniegenetics:acclimatiser" -> AcclimatiserScreen.class;
            case "binniegenetics:laboratory_machine" -> LaboratoryMachineScreen.class;
            default -> GenericMachineScreen.class;
        };
    }
}
