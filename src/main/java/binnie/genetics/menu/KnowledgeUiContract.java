package binnie.genetics.menu;

import binnie.botany.genetics.FlowerSpeciesTypes;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
  * Matching geometry and page-order contract of the original Binnie 1.12 knowledge interfaces.
  * Keeping this independent from client classes makes parity testable on a headless server.
  */
public final class KnowledgeUiContract {
    public static final int ANALYST_WIDTH = 312;
    public static final int ANALYST_HEIGHT = 230;
    public static final int ANALYST_PANEL_X = 16;
    public static final int ANALYST_PANEL_Y = 54;
    public static final int ANALYST_PANEL_WIDTH = 280;
    public static final int ANALYST_PANEL_HEIGHT = 164;
    public static final int ANALYST_PAGE_WIDTH = 134;
    public static final int ANALYST_PAGE_HEIGHT = 156;
    public static final int ANALYST_LEFT_PAGE_X = 19;
    public static final int ANALYST_RIGHT_PAGE_X = 157;
    public static final int ANALYST_PAGE_Y = 57;

    /** Original WindowAnalyst bottom ControlSlide and its player inventory. */
    public static final int ANALYST_DRAWER_X = 34;
    public static final int ANALYST_DRAWER_Y = 151;
    public static final int ANALYST_DRAWER_WIDTH = 244;
    public static final int ANALYST_DRAWER_HEIGHT = 80;
    public static final int ANALYST_DRAWER_COLLAPSED_HEIGHT = 6;
    public static final int ANALYST_INVENTORY_X = 75;
    public static final int ANALYST_INVENTORY_Y = 153;
    public static final int ANALYST_DRAWER_HANDLE_WIDTH = 28;
    public static final int ANALYST_DRAWER_HANDLE_HEIGHT = 16;

    public static final int GENE_DATABASE_WIDTH = 320;
    public static final int GENE_DATABASE_HEIGHT = 224;
    /** Original ControlPlayerInventory(x=16,y=32): six columns by six rows. */
    public static final int GENE_DATABASE_INVENTORY_X = 16;
    public static final int GENE_DATABASE_INVENTORY_Y = 32;
    public static final int GENE_DATABASE_INVENTORY_COLUMNS = 6;
    public static final int GENE_DATABASE_INVENTORY_ROWS = 6;
    public static final int GENE_DATABASE_SYSTEM_X = 140;
    public static final int GENE_DATABASE_SYSTEM_Y = 32;
    public static final int GENE_DATABASE_SYSTEM_WIDTH = 24;
    public static final int GENE_DATABASE_SYSTEM_HEIGHT = 120;
    public static final int GENE_DATABASE_BOX_X = 164;
    public static final int GENE_DATABASE_BOX_Y = 32;
    public static final int GENE_DATABASE_BOX_WIDTH = 120;
    public static final int GENE_DATABASE_BOX_HEIGHT = 120;
    public static final int GENE_DATABASE_SCROLL_X = 284;
    public static final int GENE_DATABASE_SCROLL_WIDTH = 14;
    public static final int GENE_DATABASE_SEARCH_X = 217;
    public static final int GENE_DATABASE_SEARCH_Y = 18;
    public static final int GENE_DATABASE_SEARCH_WIDTH = 80;
    public static final int GENE_DATABASE_SEARCH_HEIGHT = 12;
    public static final int GENE_DATABASE_INFO_TABS_X = 140;
    public static final int GENE_DATABASE_INFO_TABS_Y = 160;
    public static final int GENE_DATABASE_INFO_TABS_WIDTH = 16;
    public static final int GENE_DATABASE_INFO_TABS_HEIGHT = 50;
    public static final int GENE_DATABASE_INFO_X = 156;
    public static final int GENE_DATABASE_INFO_Y = 160;
    public static final int GENE_DATABASE_INFO_WIDTH = 140;
    public static final int GENE_DATABASE_INFO_HEIGHT = 50;

    public static final int DATABASE_HEIGHT = 208;
    /** Explicit ControlHelp added by WindowAbstractDatabase at (4,4). */
    public static final int DATABASE_INFO_X = 24;
    public static final int DATABASE_INFO_Y = 24;
    public static final int DATABASE_INFO_WIDTH = 144;
    public static final int DATABASE_INFO_HEIGHT = 176;
    public static final int DATABASE_LIST_X = 176;
    public static final int DATABASE_LIST_Y = 24;
    public static final int DATABASE_LIST_HEIGHT = 160;
    public static final int DATABASE_SEARCH_X = 176;
    public static final int DATABASE_SEARCH_Y = 184;
    public static final int DATABASE_SEARCH_HEIGHT = 16;
    public static final int DATABASE_LIST_ROW_HEIGHT = 12;
    public static final int DATABASE_INFO_TABS_X = 8;
    public static final int DATABASE_INFO_TABS_Y = 24;
    public static final int DATABASE_INFO_TABS_WIDTH = 16;
    public static final int DATABASE_INFO_TABS_HEIGHT = 176;

    public static int databaseSelectionWidth(ResourceLocation family) {
        if (ForestrySpeciesTypes.BEE.equals(family)) return 110;
        if (ForestrySpeciesTypes.BUTTERFLY.equals(family)) return 160;
        if (FlowerSpeciesTypes.FLOWER.equals(family)) return 130;
        return 120;
    }

    public static int databaseWidth(ResourceLocation family) {
        return 176 + databaseSelectionWidth(family) + 22 + 8;
    }

    public static int databaseModeTabsX(ResourceLocation family) {
        return 176 + databaseSelectionWidth(family);
    }

    public static final int ARBORIST_WIDTH = 326;
    public static final int ARBORIST_HEIGHT = DATABASE_HEIGHT;
    public static final int ARBORIST_INFO_X = DATABASE_INFO_X;
    public static final int ARBORIST_INFO_Y = DATABASE_INFO_Y;
    public static final int ARBORIST_INFO_WIDTH = DATABASE_INFO_WIDTH;
    public static final int ARBORIST_INFO_HEIGHT = DATABASE_INFO_HEIGHT;
    public static final int ARBORIST_LIST_X = DATABASE_LIST_X;
    public static final int ARBORIST_LIST_Y = DATABASE_LIST_Y;
    public static final int ARBORIST_LIST_WIDTH = 120;
    public static final int ARBORIST_LIST_HEIGHT = DATABASE_LIST_HEIGHT;
    public static final int ARBORIST_SEARCH_X = DATABASE_SEARCH_X;
    public static final int ARBORIST_SEARCH_Y = DATABASE_SEARCH_Y;
    public static final int ARBORIST_SEARCH_WIDTH = 120;
    public static final int ARBORIST_SEARCH_HEIGHT = DATABASE_SEARCH_HEIGHT;
    public static final int ARBORIST_MODE_TABS_X = 296;
    public static final int ARBORIST_MODE_TABS_Y = 24;
    public static final int ARBORIST_MODE_TABS_WIDTH = 22;
    public static final int ARBORIST_MODE_TABS_HEIGHT = 176;
    public static final int ARBORIST_INFO_TABS_X = DATABASE_INFO_TABS_X;
    public static final int ARBORIST_INFO_TABS_Y = DATABASE_INFO_TABS_Y;
    public static final int ARBORIST_INFO_TABS_WIDTH = DATABASE_INFO_TABS_WIDTH;
    public static final int ARBORIST_INFO_TABS_HEIGHT = DATABASE_INFO_TABS_HEIGHT;

    public enum AnalystPage {
        DATABASE,
        DESCRIPTION,
        ACTIVE_GENOME,
        INACTIVE_GENOME,
        KARYOGRAM,
        CLIMATE,
        PRODUCTS,
        BIOLOGY,
        BEHAVIOUR,
        FRUIT,
        WOOD,
        GROWTH,
        SPECIMEN,
        SOIL,
        APPEARANCE,
        MUTATIONS
    }

    public enum ArboristMode {
        SPECIES,
        BRANCHES,
        BREEDER,
        FRUIT,
        WOOD,
        PLANKS,
        COLOR
    }

    public enum ArboristSpeciesPage {
        OVERVIEW,
        GENOME,
        CLASSIFICATION,
        RESULTANT,
        FURTHER
    }

    public static List<ResourceLocation> familyOrder() {
        return List.of(ForestrySpeciesTypes.BEE, ForestrySpeciesTypes.TREE,
                ForestrySpeciesTypes.BUTTERFLY, FlowerSpeciesTypes.FLOWER);
    }

    /** 1.12 Gene Bank tab order shown by Binnie's active breeding-system bar. */
    public static List<ResourceLocation> geneBankFamilyOrder() {
        return List.of(ForestrySpeciesTypes.TREE, FlowerSpeciesTypes.FLOWER,
                ForestrySpeciesTypes.BEE, ForestrySpeciesTypes.BUTTERFLY);
    }

    /** Matching lower ControlTabBar labels created by WindowGeneBank. */
    public static List<String> geneDatabaseInfoTabs() {
        return List.of("info", "stats", "ranking");
    }

    public static List<AnalystPage> analystPages(ResourceLocation type, boolean registry) {
        List<AnalystPage> details;
        if (ForestrySpeciesTypes.BEE.equals(type)) {
            details = List.of(AnalystPage.DESCRIPTION, AnalystPage.ACTIVE_GENOME,
                    AnalystPage.INACTIVE_GENOME, AnalystPage.KARYOGRAM,
                    AnalystPage.CLIMATE, AnalystPage.PRODUCTS, AnalystPage.BIOLOGY,
                    AnalystPage.BEHAVIOUR, AnalystPage.MUTATIONS);
        } else if (ForestrySpeciesTypes.TREE.equals(type)) {
            details = List.of(AnalystPage.DESCRIPTION, AnalystPage.ACTIVE_GENOME,
                    AnalystPage.INACTIVE_GENOME, AnalystPage.KARYOGRAM,
                    AnalystPage.FRUIT, AnalystPage.WOOD, AnalystPage.BIOLOGY,
                    AnalystPage.GROWTH, AnalystPage.MUTATIONS);
        } else if (ForestrySpeciesTypes.BUTTERFLY.equals(type)) {
            details = List.of(AnalystPage.DESCRIPTION, AnalystPage.ACTIVE_GENOME,
                    AnalystPage.INACTIVE_GENOME, AnalystPage.KARYOGRAM,
                    AnalystPage.CLIMATE, AnalystPage.SPECIMEN, AnalystPage.BIOLOGY,
                    AnalystPage.BEHAVIOUR, AnalystPage.MUTATIONS);
        } else if (FlowerSpeciesTypes.FLOWER.equals(type)) {
            details = List.of(AnalystPage.DESCRIPTION, AnalystPage.ACTIVE_GENOME,
                    AnalystPage.INACTIVE_GENOME, AnalystPage.KARYOGRAM,
                    AnalystPage.CLIMATE, AnalystPage.SOIL, AnalystPage.BIOLOGY,
                    AnalystPage.APPEARANCE, AnalystPage.MUTATIONS);
        } else {
            details = List.of(AnalystPage.DESCRIPTION, AnalystPage.ACTIVE_GENOME,
                    AnalystPage.INACTIVE_GENOME, AnalystPage.KARYOGRAM, AnalystPage.MUTATIONS);
        }
        if (!registry) return details;
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(AnalystPage.DATABASE),
                details.stream().filter(page -> page != AnalystPage.INACTIVE_GENOME && page != AnalystPage.KARYOGRAM)).toList();
    }

    public static List<String> arboristInfoPages(ArboristMode mode) {
        return switch (mode) {
            case SPECIES -> List.of("overview", "genome", "classification", "resultant", "further");
            case BRANCHES -> List.of("overview", "species");
            case BREEDER -> List.of("breeder");
            case FRUIT -> List.of("natural", "potential");
            case WOOD -> List.of("natural");
            case PLANKS -> List.of("overview", "natural");
            case COLOR -> List.of("resultant", "further");
        };
    }


    public static List<ArboristMode> databaseModes(ResourceLocation family, boolean arborist) {
        if (arborist) {
            return List.of(ArboristMode.SPECIES, ArboristMode.BRANCHES, ArboristMode.BREEDER,
                    ArboristMode.FRUIT, ArboristMode.WOOD, ArboristMode.PLANKS);
        }
        if (FlowerSpeciesTypes.FLOWER.equals(family)) {
            return List.of(ArboristMode.SPECIES, ArboristMode.BRANCHES, ArboristMode.COLOR, ArboristMode.BREEDER);
        }
        return List.of(ArboristMode.SPECIES, ArboristMode.BRANCHES, ArboristMode.BREEDER);
    }

    public static List<String> databaseInfoPages(ResourceLocation family, ArboristMode mode, boolean arborist) {
        if (mode != ArboristMode.SPECIES || arborist) return arboristInfoPages(mode);
        if (ForestrySpeciesTypes.BEE.equals(family)) {
            return List.of("overview", "classification", "genome", "products", "climate", "resultant", "further");
        }
        if (ForestrySpeciesTypes.BUTTERFLY.equals(family)) {
            return List.of("overview", "classification", "specimen", "resultant", "further");
        }
        if (FlowerSpeciesTypes.FLOWER.equals(family)) {
            return List.of("overview", "genome", "classification", "resultant", "further");
        }
        return List.of("overview", "genome", "classification", "resultant", "further");
    }

    private KnowledgeUiContract() {}
}
