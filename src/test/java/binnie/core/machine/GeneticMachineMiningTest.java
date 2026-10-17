package binnie.core.machine;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeneticMachineMiningTest {
    private static final Set<String> MACHINES = Set.of(
            "isolator", "sequencer", "polymeriser", "inoculator", "splicer",
            "analyser", "genepool", "incubator", "acclimatiser", "laboratory_machine");

    @Test
    void everyGeneticMachineIsMineableAndHasItsOwnDropTable() throws Exception {
        Path tagPath = Path.of("src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json");
        var tag = JsonParser.parseString(Files.readString(tagPath)).getAsJsonObject();
        Set<String> values = tag.getAsJsonArray("values").asList().stream()
                .map(element -> element.getAsString()).collect(java.util.stream.Collectors.toSet());

        for (String machine : MACHINES) {
            assertTrue(values.contains("binniegenetics:" + machine), machine + " is not pickaxe-mineable");
            Path lootPath = Path.of("src/main/resources/data/binniegenetics/loot_table/blocks/" + machine + ".json");
            var loot = JsonParser.parseString(Files.readString(lootPath)).getAsJsonObject();
            assertEquals("binniegenetics:" + machine,
                    loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")
                            .get(0).getAsJsonObject().get("name").getAsString(), machine);
        }
    }
}
