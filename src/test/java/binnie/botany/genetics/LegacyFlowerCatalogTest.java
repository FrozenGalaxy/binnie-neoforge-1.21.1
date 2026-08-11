package binnie.botany.genetics;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 1.12 Botany catalog invariants, including the lexical/executable count distinction. */
final class LegacyFlowerCatalogTest {
    private static final int LEGACY_MUTATION_TOKENS = 38;
    private static final int LEGACY_COLOR_MIX_TOKENS = 2_816;

    @Test
    void catalogCountsAndReferencesAreComplete() {
        LegacyFlowerCatalog catalog = LegacyFlowerCatalog.INSTANCE;
        assertEquals(47, catalog.species().size());
        assertEquals(80, catalog.colors().size());
        assertEquals(37, catalog.mutations().size());
        assertEquals(2_815, catalog.colorMixes().size());
        assertEquals(12, chromosomeCount());

        Set<ResourceLocation> speciesIds = new HashSet<>();
        catalog.species().forEach(species -> {
            assertTrue(speciesIds.add(species.id()), () -> "Duplicate species " + species.id());
            assertEquals(11, species.alleles().size(), () -> species.id() + " explicit non-species chromosomes");
            for (String chromosome : requiredExplicitChromosomes()) {
                assertNotNull(species.allele(chromosome), () -> species.id() + " missing " + chromosome);
            }
        });

        Set<ResourceLocation> colorIds = new HashSet<>();
        catalog.colors().forEach(color -> assertTrue(colorIds.add(color.id()), () -> "Duplicate color " + color.id()));

        catalog.mutations().forEach(mutation -> {
            assertTrue(speciesIds.contains(mutation.first()), () -> "Missing first parent " + mutation);
            assertTrue(speciesIds.contains(mutation.second()), () -> "Missing second parent " + mutation);
            assertTrue(speciesIds.contains(mutation.result()), () -> "Missing result " + mutation);
            assertTrue(mutation.chance() > 0 && mutation.chance() <= 100, () -> "Invalid chance " + mutation);
        });

        Set<String> pairs = new HashSet<>();
        catalog.colorMixes().forEach(mix -> {
            assertTrue(colorIds.contains(mix.first()), () -> "Missing first color " + mix);
            assertTrue(colorIds.contains(mix.second()), () -> "Missing second color " + mix);
            assertTrue(colorIds.contains(mix.result()), () -> "Missing result color " + mix);
            String pair = mix.first().toString().compareTo(mix.second().toString()) <= 0
                    ? mix.first() + "|" + mix.second() : mix.second() + "|" + mix.first();
            assertTrue(pairs.add(pair), () -> "Duplicate unordered color pair " + pair);
            assertTrue(mix.chance() > 0, () -> "Invalid color chance " + mix);
            assertSame(mix, catalog.colorMix(mix.second(), mix.first()), () -> "Color lookup must be unordered for " + pair);
        });
    }

    @Test
    void helperDeclarationsAreNotRegistrations() {
        assertEquals(1, LEGACY_MUTATION_TOKENS - LegacyFlowerCatalog.INSTANCE.mutations().size(),
                "The sole extra registerMutation token is the helper declaration");
        assertEquals(1, LEGACY_COLOR_MIX_TOKENS - LegacyFlowerCatalog.INSTANCE.colorMixes().size(),
                "The sole extra addMix token is the helper declaration");
    }

    @Test
    void legacyFlowerNamesAndLifeStageGrammarArePresent() throws IOException {
        String language = readBotanyLanguage();
        for (LegacyFlowerCatalog.SpeciesDef species : LegacyFlowerCatalog.INSTANCE.species()) {
            assertTrue(language.contains("\"item.binniebotany.flower." + species.id().getPath() + "\":"),
                    () -> "Missing legacy display name for " + species.id());
        }
        assertTrue(language.contains("\"item.binniebotany.grammar.pollen\":"));
        assertTrue(language.contains("\"item.binniebotany.grammar.seed\":"));
        assertTrue(language.contains("\"binniebotany.analyzer.soil_requirements\":"));
        assertTrue(language.contains("\"binniebotany.analyzer.temperature.hellish\":"));
        assertTrue(language.contains("\"binniebotany.analyzer.moisture.damp\":"));
        assertTrue(language.contains("\"binniebotany.analyzer.acidity.alkaline\":"));
    }

    private static String readBotanyLanguage() throws IOException {
        try (InputStream stream = LegacyFlowerCatalogTest.class.getResourceAsStream("/assets/binniebotany/lang/en_us.json")) {
            assertNotNull(stream, "Botany English language resource is absent");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int chromosomeCount() {
        return 1 + requiredExplicitChromosomes().length;
    }

    private static String[] requiredExplicitChromosomes() {
        return new String[]{
                "primary", "secondary", "fertility", "territory", "effect", "lifespan",
                "temperature_tolerance", "humidity_tolerance", "ph_tolerance", "sappiness", "stem"
        };
    }
}
