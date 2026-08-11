package binnie.trees.forestry;

import binnie.trees.wood.BinnieTreeWoodType;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 1.12 invariants for the full Binnie's Trees arboriculture catalog. */
final class LegacyTreeCatalogTest {
    @Test
    void catalogAndGeneratorCoverageAreComplete() {
        LegacyTreeCatalog catalog = LegacyTreeCatalog.load();
        assertEquals(97, catalog.species().size());
        assertEquals(59, catalog.fruits().size());

        Set<String> catalogGenerators = catalog.species().stream()
                .map(LegacyTreeCatalog.TreeBinding::generator)
                .collect(Collectors.toSet());
        assertEquals(59, catalogGenerators.size());
        assertEquals(catalogGenerators, LegacyTreeFeature.supportedGenerators());
        catalogGenerators.forEach(generator -> {
            LegacyTreeFeature.GeneratorProfile profile = LegacyTreeFeature.profile(generator);
            assertTrue(profile.baseHeight() >= 1, generator);
            assertTrue(profile.heightVariation() >= 1, generator);
            assertTrue(profile.minimumHeight() >= 1, generator);
            assertNotNull(profile.shape(), generator);
        });
        assertThrows(IllegalArgumentException.class, () -> LegacyTreeFeature.profile("WorldGenMissing.Tree"));
    }

    @Test
    void fruitAndWoodBindingsResolveWithoutFallbacks() {
        LegacyTreeCatalog catalog = LegacyTreeCatalog.load();
        assertEquals(4, catalog.fruits().stream().filter(LegacyTreeCatalog.FruitBinding::pod).count());
        assertEquals(Set.of("banana", "red_banana", "plantain", "coconut"), catalog.fruits().stream()
                .filter(LegacyTreeCatalog.FruitBinding::pod)
                .map(LegacyTreeCatalog.FruitBinding::podType)
                .collect(Collectors.toSet()));

        Set<BinnieTreeWoodType> customWoods = catalog.species().stream()
                .map(LegacyTreeCatalog.TreeBinding::wood)
                .filter(wood -> wood.startsWith("EnumETLog."))
                .filter(wood -> !wood.equals("EnumETLog.Cherry") && !wood.equals("EnumETLog.Cinnamon"))
                .map(BinnieTreeWoodType::fromLegacyExpression)
                .collect(Collectors.toSet());
        assertTrue(customWoods.stream().noneMatch(java.util.Objects::isNull));
        assertEquals(Set.of(BinnieTreeWoodType.values()), customWoods);
    }
}
