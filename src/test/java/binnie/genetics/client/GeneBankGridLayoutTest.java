package binnie.genetics.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GeneBankGridLayoutTest {
    private static final ResourceLocation SPECIES = ResourceLocation.withDefaultNamespace("species");
    private static final ResourceLocation SPEED = ResourceLocation.withDefaultNamespace("speed");

    @Test
    void layoutMatchesOriginalControlGeneScrollGeometry() {
        ArrayList<ResourceLocation> chromosomes = new ArrayList<>();
        for (int i = 0; i < 7; i++) chromosomes.add(SPECIES);
        chromosomes.addAll(List.of(SPEED, SPEED));

        List<GeneBankGridLayout.Cell> cells = GeneBankGridLayout.layout(chromosomes);
        assertEquals(new GeneBankGridLayout.Cell(0, 167, 47, 35, true), cells.get(0));
        assertEquals(new GeneBankGridLayout.Cell(5, 257, 47, 35, false), cells.get(5));
        assertEquals(new GeneBankGridLayout.Cell(6, 167, 67, 35, false), cells.get(6));
        assertEquals(new GeneBankGridLayout.Cell(7, 167, 103, 91, true), cells.get(7));
        assertEquals(92, GeneBankGridLayout.contentHeight(cells));
    }

    @Test
    void renderedHoverAndClickUseTheSameScrolledLocalCell() {
        List<GeneBankGridLayout.Cell> cells = GeneBankGridLayout.layout(List.of(SPECIES, SPECIES));
        assertEquals(0, GeneBankGridLayout.hitTest(cells, 168, 48, 0));
        assertEquals(1, GeneBankGridLayout.hitTest(cells, 186, 47, 0));
        assertEquals(0, GeneBankGridLayout.hitTest(cells, 168, 36, 12));
        assertEquals(-1, GeneBankGridLayout.hitTest(cells, 150, 48, 0));
    }
}
