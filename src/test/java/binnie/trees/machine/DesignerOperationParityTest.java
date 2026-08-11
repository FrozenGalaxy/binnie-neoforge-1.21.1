package binnie.trees.machine;

import binnie.design.data.DesignCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DesignerOperationParityTest {
    @Test
    void allSourceRegisteredDesignsAreAvailableToEachDistinctWorker() {
        assertEquals(101, DesignCatalog.DESIGNS.size());
        assertEquals(101, DesignCatalog.DESIGNS.stream().distinct().count());
        assertEquals(100, DesignCatalog.SELECTABLE_DESIGNS.size());
        assertTrue(DesignCatalog.SELECTABLE_DESIGNS.contains("blank"));
        assertTrue(DesignCatalog.SELECTABLE_DESIGNS.contains("diamond"));
        assertFalse(DesignCatalog.SELECTABLE_DESIGNS.contains("test"));
    }
}
