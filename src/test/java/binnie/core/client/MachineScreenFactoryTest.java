package binnie.core.client;

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
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class MachineScreenFactoryTest {
    @Test
    void everyRegisteredMachineHasItsOwnConcreteScreenClass() {
        Map<String, Class<?>> expected = new LinkedHashMap<>();
        expected.put("binnietrees:press", PressScreen.class);
        expected.put("binnietrees:brewery", BreweryScreen.class);
        expected.put("binnietrees:distillery", DistilleryScreen.class);
        expected.put("binnietrees:lumbermill", LumbermillScreen.class);
        expected.put("binnietrees:woodworker", WoodworkerScreen.class);
        expected.put("binnietrees:panelworker", PanelworkerScreen.class);
        expected.put("binnietrees:glassworker", GlassworkerScreen.class);
        expected.put("binniebotany:tileworker", TileworkerScreen.class);
        expected.put("binniegenetics:isolator", IsolatorScreen.class);
        expected.put("binniegenetics:sequencer", SequencerScreen.class);
        expected.put("binniegenetics:polymeriser", PolymeriserScreen.class);
        expected.put("binniegenetics:inoculator", InoculatorScreen.class);
        expected.put("binniegenetics:splicer", SplicerScreen.class);
        expected.put("binniegenetics:analyser", AnalyserScreen.class);
        expected.put("binniegenetics:genepool", GenePoolScreen.class);
        expected.put("binniegenetics:incubator", IncubatorScreen.class);
        expected.put("binniegenetics:acclimatiser", AcclimatiserScreen.class);
        expected.put("binniegenetics:laboratory_machine", LaboratoryMachineScreen.class);

        expected.forEach((id, screen) -> assertEquals(screen,
                MachineScreenFactory.screenClass(ResourceLocation.parse(id)), id));
        assertEquals(18, expected.size());
    }

    @Test
    void sharedBasesContainNoMachineIdRenderingSwitches() throws Exception {
        for (String source : new String[]{
                "src/main/java/binnie/core/client/MachineScreen.java",
                "src/main/java/binnie/genetics/client/GeneticsMachineScreen.java",
                "src/main/java/binnie/design/client/DesignerScreen.java"
        }) {
            String text = Files.readString(Path.of(source));
            assertFalse(text.contains("switch (menu.machineId().getPath())"), source);
            assertFalse(text.contains("menu.machineId().getPath().equals("), source);
        }
    }
}
