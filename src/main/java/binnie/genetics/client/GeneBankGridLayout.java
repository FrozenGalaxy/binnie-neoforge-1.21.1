package binnie.genetics.client;

import binnie.genetics.menu.KnowledgeUiContract;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
  * Matching layout contract of 1.12's {@code ControlGeneScroll}.
  *
  * <p>Keeping cell placement and hit testing in one pure helper prevents the rendered hover,
  * tooltip, and click targets from using different coordinate systems.</p>
  */
class GeneBankGridLayout {
    static final int CONTENT_X = KnowledgeUiContract.GENE_DATABASE_BOX_X + 3;
    static final int CONTENT_Y = KnowledgeUiContract.GENE_DATABASE_BOX_Y + 3;
    static final int CELL_SIZE = 16;
    static final int COLUMN_STEP = 18;
    static final int ROW_STEP = 20;
    static final int HEADING_HEIGHT = 12;
    static final int GROUP_BOTTOM_GAP = 24;
    static final int COLUMNS = 6;
    static final int VIEWPORT_HEIGHT = 116;

    private GeneBankGridLayout() {}

    static List<Cell> layout(List<ResourceLocation> chromosomes) {
        ArrayList<Cell> result = new ArrayList<>(chromosomes.size());
        int ordinal = 0;
        int headingY = CONTENT_Y;
        while (ordinal < chromosomes.size()) {
            ResourceLocation chromosome = chromosomes.get(ordinal);
            int groupStart = ordinal;
            while (ordinal < chromosomes.size() && chromosome.equals(chromosomes.get(ordinal))) ordinal++;
            int groupSize = ordinal - groupStart;
            int firstGeneY = headingY + HEADING_HEIGHT;
            for (int index = 0; index < groupSize; index++) {
                int column = index % COLUMNS;
                int row = index / COLUMNS;
                result.add(new Cell(groupStart + index,
                        CONTENT_X + column * COLUMN_STEP,
                        firstGeneY + row * ROW_STEP,
                        headingY,
                        index == 0));
            }
            int rows = Math.max(1, (groupSize + COLUMNS - 1) / COLUMNS);
            int lastRowY = firstGeneY + (rows - 1) * ROW_STEP;
            headingY = lastRowY + GROUP_BOTTOM_GAP;
        }
        return List.copyOf(result);
    }

    static int contentHeight(List<Cell> cells) {
        if (cells.isEmpty()) return 0;
        Cell last = cells.getLast();
        return last.y() + GROUP_BOTTOM_GAP - CONTENT_Y;
    }

    static int hitTest(List<Cell> cells, double localMouseX, double localMouseY, int scroll) {
        for (Cell cell : cells) {
            int renderedY = cell.y() - scroll;
            if (localMouseX >= cell.x() && localMouseX < cell.x() + CELL_SIZE
                    && localMouseY >= renderedY && localMouseY < renderedY + CELL_SIZE) {
                return cell.ordinal();
            }
        }
        return -1;
    }

    record Cell(int ordinal, int x, int y, int headingY, boolean firstInChromosome) {}
}
