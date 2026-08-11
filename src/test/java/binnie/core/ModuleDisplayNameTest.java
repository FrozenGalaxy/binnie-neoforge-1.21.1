package binnie.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModuleDisplayNameTest {
    @Test
    void allSixLogicalModsUseTheSplitBinniesNamingScheme() throws Exception {
        var stream = ModuleDisplayNameTest.class.getClassLoader().getResourceAsStream("META-INF/neoforge.mods.toml");
        assertNotNull(stream, "Generated NeoForge mod metadata is missing");
        String metadata;
        try (stream) {
            metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String name : List.of("Core", "Design", "Botany", "Bees", "Trees", "Genetics")) {
            assertTrue(metadata.contains("displayName = \"Binnies: " + name + "\""),
                    "Missing split module display name: Binnies: " + name);
        }
        for (var module : Map.of(
                "binniecore", "Core",
                "binniedesign", "Design",
                "binniebotany", "Botany",
                "binniebees", "Bees",
                "binnietrees", "Trees",
                "binniegenetics", "Genetics").entrySet()) {
            assertTrue(metadata.contains("modId = \"" + module.getKey() + "\""),
                    "Missing branded module id for " + module.getValue());
        }
        for (String retired : List.of("botany", "extrabees", "extratrees", "genetics")) {
            assertTrue(!metadata.contains("modId = \"" + retired + "\""),
                    "Unbranded module id remains active: " + retired);
        }
    }
}
