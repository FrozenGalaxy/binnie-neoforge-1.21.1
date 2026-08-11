package binnie.core;

import binnie.bees.alveary.StimulatorCircuit;
import binnie.bees.alveary.StimulatorProgram;
import binnie.bees.block.BinnieBeesAlvearyBlockEntity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents literal project translation keys from silently falling back to their raw key in-game. */
final class TranslationCoverageTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets");
    private static final Path SOURCES = Path.of("src/main/java");
    /** Matches only a full literal first argument, excluding generated key prefixes. */
    private static final Pattern TRANSLATABLE = Pattern.compile(
            "Component\\.translatable\\(\\s*\"([^\"]+)\"\\s*(?:,|\\))");

    @Test
    void everyLiteralProjectTranslationKeyHasEnglishText() throws IOException {
        Set<String> english = readEnglishTranslations();
        List<String> missing = new ArrayList<>();

        try (var files = Files.walk(SOURCES)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = TRANSLATABLE.matcher(Files.readString(source));
                while (matcher.find()) {
                    String key = matcher.group(1);
                    String namespace = namespaceFor(key);
                    if (namespace != null && !english.contains(key)) {
                        missing.add(key + " (" + SOURCES.relativize(source) + ")");
                    }
                }
            }
        }

        missing.sort(Comparator.naturalOrder());
        assertTrue(missing.isEmpty(), "Missing en_us translations:\n" + String.join("\n", missing));
    }

    @Test
    void englishTranslationsAreNotBlank() throws IOException {
        List<String> blank = new ArrayList<>();
        try (var files = Files.walk(ASSETS)) {
            for (Path language : files.filter(path -> path.endsWith(Path.of("lang", "en_us.json"))).toList()) {
                JsonObject entries = JsonParser.parseString(Files.readString(language)).getAsJsonObject();
                for (String key : entries.keySet()) {
                    if (!entries.get(key).isJsonPrimitive() || entries.get(key).getAsString().isBlank()) {
                        blank.add(key + " (" + language + ")");
                    }
                }
            }
        }
        blank.sort(Comparator.naturalOrder());
        assertTrue(blank.isEmpty(), "Blank en_us translations:\n" + String.join("\n", blank));
    }

    @Test
    void generatedStimulatorCircuitAndLayoutKeysHaveEnglishText() throws IOException {
        Set<String> english = readEnglishTranslations();
        List<String> missing = new ArrayList<>();

    // Derive these from the registered program data so a new stimulator
    // program cannot bypass the translation check.
        for (StimulatorProgram program : StimulatorProgram.values()) {
            String key = new StimulatorCircuit(program).getTranslationKey();
            if (!english.contains(key)) missing.add(key);
        }

        String layoutKey = "circuit.layout." + BinnieBeesAlvearyBlockEntity.STIMULATOR_LAYOUT;
        if (!english.contains(layoutKey)) missing.add(layoutKey);
        if (!english.contains(layoutKey + ".usage")) missing.add(layoutKey + ".usage");

        missing.sort(Comparator.naturalOrder());
        assertTrue(missing.isEmpty(), "Missing generated stimulator translations:\n" + String.join("\n", missing));
    }

    private static Set<String> readEnglishTranslations() throws IOException {
        Set<String> result = new HashSet<>();
        try (var files = Files.walk(ASSETS)) {
            for (Path language : files.filter(path -> path.endsWith(Path.of("lang", "en_us.json"))).toList()) {
                JsonObject entries = JsonParser.parseString(Files.readString(language)).getAsJsonObject();
                result.addAll(entries.keySet());
            }
        }
        return result;
    }

    /** Binnie's own keys always contain the owning mod id as their second segment. */
    private static String namespaceFor(String key) {
        String[] parts = key.split("\\.");
        if (parts.length < 2) return null;
        return switch (parts[1]) {
            case "binniecore", "binniedesign", "binniebotany", "binniebees", "binnietrees", "binniegenetics" -> parts[1];
            default -> null;
        };
    }
}
