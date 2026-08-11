package binnie.genetics.menu;

import binnie.botany.genetics.FlowerSpeciesTypes;
import binnie.trees.wood.BinnieTreeWoodType;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static binnie.genetics.menu.KnowledgeUiContract.AnalystPage.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KnowledgeUiContractTest {
    @Test
    void originalWindowGeometryIsPreserved() {
        assertEquals(List.of(312, 230, 16, 54, 280, 164, 134, 156, 19, 157, 57), List.of(
                KnowledgeUiContract.ANALYST_WIDTH, KnowledgeUiContract.ANALYST_HEIGHT,
                KnowledgeUiContract.ANALYST_PANEL_X, KnowledgeUiContract.ANALYST_PANEL_Y,
                KnowledgeUiContract.ANALYST_PANEL_WIDTH, KnowledgeUiContract.ANALYST_PANEL_HEIGHT,
                KnowledgeUiContract.ANALYST_PAGE_WIDTH, KnowledgeUiContract.ANALYST_PAGE_HEIGHT,
                KnowledgeUiContract.ANALYST_LEFT_PAGE_X, KnowledgeUiContract.ANALYST_RIGHT_PAGE_X,
                KnowledgeUiContract.ANALYST_PAGE_Y));
        assertEquals(List.of(320, 224, 16, 32, 6, 6, 140, 32, 164, 32, 120, 120, 284, 217, 18, 80, 12, 140, 160, 16, 50, 156, 160, 140, 50), List.of(
                KnowledgeUiContract.GENE_DATABASE_WIDTH, KnowledgeUiContract.GENE_DATABASE_HEIGHT,
                KnowledgeUiContract.GENE_DATABASE_INVENTORY_X, KnowledgeUiContract.GENE_DATABASE_INVENTORY_Y,
                KnowledgeUiContract.GENE_DATABASE_INVENTORY_COLUMNS, KnowledgeUiContract.GENE_DATABASE_INVENTORY_ROWS,
                KnowledgeUiContract.GENE_DATABASE_SYSTEM_X, KnowledgeUiContract.GENE_DATABASE_SYSTEM_Y,
                KnowledgeUiContract.GENE_DATABASE_BOX_X, KnowledgeUiContract.GENE_DATABASE_BOX_Y,
                KnowledgeUiContract.GENE_DATABASE_BOX_WIDTH, KnowledgeUiContract.GENE_DATABASE_BOX_HEIGHT,
                KnowledgeUiContract.GENE_DATABASE_SCROLL_X, KnowledgeUiContract.GENE_DATABASE_SEARCH_X,
                KnowledgeUiContract.GENE_DATABASE_SEARCH_Y, KnowledgeUiContract.GENE_DATABASE_SEARCH_WIDTH,
                KnowledgeUiContract.GENE_DATABASE_SEARCH_HEIGHT,
                KnowledgeUiContract.GENE_DATABASE_INFO_TABS_X, KnowledgeUiContract.GENE_DATABASE_INFO_TABS_Y,
                KnowledgeUiContract.GENE_DATABASE_INFO_TABS_WIDTH, KnowledgeUiContract.GENE_DATABASE_INFO_TABS_HEIGHT,
                KnowledgeUiContract.GENE_DATABASE_INFO_X,
                KnowledgeUiContract.GENE_DATABASE_INFO_Y, KnowledgeUiContract.GENE_DATABASE_INFO_WIDTH,
                KnowledgeUiContract.GENE_DATABASE_INFO_HEIGHT));
        assertEquals(List.of(326, 208, 24, 24, 144, 176, 176, 24, 120, 160, 176, 184, 120, 16, 296, 24, 22, 176, 8, 24, 16, 176), List.of(
                KnowledgeUiContract.ARBORIST_WIDTH, KnowledgeUiContract.ARBORIST_HEIGHT,
                KnowledgeUiContract.ARBORIST_INFO_X, KnowledgeUiContract.ARBORIST_INFO_Y,
                KnowledgeUiContract.ARBORIST_INFO_WIDTH, KnowledgeUiContract.ARBORIST_INFO_HEIGHT,
                KnowledgeUiContract.ARBORIST_LIST_X, KnowledgeUiContract.ARBORIST_LIST_Y,
                KnowledgeUiContract.ARBORIST_LIST_WIDTH, KnowledgeUiContract.ARBORIST_LIST_HEIGHT,
                KnowledgeUiContract.ARBORIST_SEARCH_X, KnowledgeUiContract.ARBORIST_SEARCH_Y,
                KnowledgeUiContract.ARBORIST_SEARCH_WIDTH, KnowledgeUiContract.ARBORIST_SEARCH_HEIGHT,
                KnowledgeUiContract.ARBORIST_MODE_TABS_X, KnowledgeUiContract.ARBORIST_MODE_TABS_Y,
                KnowledgeUiContract.ARBORIST_MODE_TABS_WIDTH, KnowledgeUiContract.ARBORIST_MODE_TABS_HEIGHT,
                KnowledgeUiContract.ARBORIST_INFO_TABS_X, KnowledgeUiContract.ARBORIST_INFO_TABS_Y,
                KnowledgeUiContract.ARBORIST_INFO_TABS_WIDTH, KnowledgeUiContract.ARBORIST_INFO_TABS_HEIGHT));
    }

    @Test
    void geneBankUsesItsOriginalBreedingSystemTabOrder() {
        assertEquals(List.of(ForestrySpeciesTypes.TREE, FlowerSpeciesTypes.FLOWER,
                        ForestrySpeciesTypes.BEE, ForestrySpeciesTypes.BUTTERFLY),
                KnowledgeUiContract.geneBankFamilyOrder());
        assertEquals(List.of("info", "stats", "ranking"), KnowledgeUiContract.geneDatabaseInfoTabs());
    }

    @Test
    void specialistDatabaseHelpIsAvailableDirectlyFromTabsWithShift() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        assertTrue(source.contains("Screen.hasShiftDown()"));
        assertTrue(source.contains("screen.binniegenetics.database.hold_shift"));
        assertFalse(source.contains("databaseHelp"),
                "database help must not retain toggle state");
        assertFalse(source.contains("DATABASE_HELP_"),
                "removed top-left help controls must not retain hitboxes");
    }

    @Test
    void everyKnowledgePaginatorRendersARepresentativeMutedIcon() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        assertTrue(source.contains("renderMutedTabIcon(graphics, analystPageIcon(pages.get(i))"),
                "Registry and Analyst page tabs must not remain blank");
        assertTrue(source.contains("renderMutedTabIcon(graphics, geneDatabaseTabIcon(i)"),
                "Gene Bank information tabs must not remain blank");
        assertTrue(source.contains("renderMutedTabIcon(graphics, databaseModeIcon(mode)"),
                "specialist database mode tabs must not remain blank");
        assertTrue(source.contains("renderMutedTabIcon(graphics, databasePageIcon(infoPages.get(i))"),
                "specialist database information tabs must not remain blank");

        for (String page : List.of("DATABASE", "DESCRIPTION", "ACTIVE_GENOME", "INACTIVE_GENOME",
                "KARYOGRAM", "CLIMATE", "PRODUCTS", "BIOLOGY", "BEHAVIOUR", "FRUIT", "WOOD",
                "GROWTH", "SPECIMEN", "SOIL", "APPEARANCE", "MUTATIONS")) {
            assertTrue(source.contains("case " + page + " ->"), "missing Registry icon mapping for " + page);
        }
        for (String page : List.of("overview", "classification", "genome", "products", "climate",
                "resultant", "further", "specimen", "species", "breeder", "natural", "potential")) {
            assertTrue(source.contains("\"" + page + "\""), "missing database icon mapping for " + page);
        }
    }

    @Test
    void analystGenomeKaryogramAndMutationPagesRetainTheirSourceControls() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        assertTrue(source.contains("float scale = 0.78F"),
                "genome rows must use the legacy scaled layout instead of shortening trait labels");
        assertTrue(source.contains("renderKaryogramTooltip"));
        assertTrue(source.contains("screen.binniegenetics.analyst.karyogram.active"));
        assertTrue(source.contains("screen.binniegenetics.analyst.karyogram.inactive"));
        assertTrue(source.contains("int groupWidth = 44 * 2 + gap"),
                "mutations must use the source two-column 44-pixel controls");
        assertTrue(source.contains("mutation.getSpecialConditions()"),
                "mutation hover details must retain source conditions");
        assertTrue(source.contains("safeDisplay(condition)"),
                "missing upstream condition translations must not leak raw keys");
        assertFalse(source.contains("x + 82, rowY + 5"),
                "the overflowing inline mutation percentage row must stay removed");
    }

    @Test
    void flowerAllelesNeverExposeRawTranslationKeysInKnowledgeScreens() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        assertTrue(source.contains("isRawTranslationKey(translated) ? alleleFallback(chromosome, value)"),
                "Analyzer allele rows need a typed fallback");
        assertTrue(source.contains("isRawTranslationKey(translated) ? alleleFallback(chromosome, value) : translated"),
                "Gene Bank allele hover text needs the same typed fallback");
        assertTrue(source.contains("vector.getX() + \" × \" + vector.getY() + \" × \" + vector.getZ()"),
                "flower territory genes need a readable typed representation");
        assertTrue(source.contains("Math.round(number * 100.0F) + \"%\""),
                "flower sappiness genes need the source percentage representation");
    }

    @Test
    void geneBankFamilyTabsNeverExposeRawSpeciesTypeKeys() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        assertTrue(source.contains("renderTooltip(font, safeDisplay(familyType(i).getDisplayName())"),
                "Registry family tabs need a readable fallback for custom species types");
        assertTrue(source.contains("safeDisplay(familyType(i).getDisplayName()),\n"
                        + "                    Component.translatable(\"screen.binniegenetics.database.gene_count\""),
                "Gene Bank family tabs need a readable fallback before the gene-count line");
        assertFalse(source.contains("renderTooltip(font, familyType(i).getDisplayName()"),
                "raw species-type display components must not reach family-tab tooltips");
    }

    @Test
    void analystInformationPagesWrapAndSpaceTheirCompleteValues() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        assertTrue(source.contains("font.split(chromosomeName"),
                "long chromosome labels must wrap instead of ending in an ellipsis");
        assertTrue(source.contains("font.split(alleleName"),
                "long allele values must wrap instead of ending in an ellipsis");
        assertTrue(source.contains("analystPageContentHeight(page)"),
                "wrapped genome rows need a matching scrollbar extent");
        assertTrue(source.contains("safeDisplay(resolved.type().getDisplayName())"),
                "the Biology family value must not expose a species-type translation key");
        assertTrue(source.contains("x, y + 48, width, Component.translatable(\"screen.binniegenetics.analyst.family\")"),
                "Biology rows need the restored vertical separation");
        assertTrue(source.contains("y + 11, 0x333333"),
                "information values need padding below their underlined labels");
    }

    @Test
    void analystPageOrderMatchesEveryOriginalPlugin() {
        assertEquals(List.of(DESCRIPTION, ACTIVE_GENOME, INACTIVE_GENOME, KARYOGRAM, CLIMATE, PRODUCTS, BIOLOGY, BEHAVIOUR, MUTATIONS),
                KnowledgeUiContract.analystPages(ForestrySpeciesTypes.BEE, false));
        assertEquals(List.of(DESCRIPTION, ACTIVE_GENOME, INACTIVE_GENOME, KARYOGRAM, FRUIT, WOOD, BIOLOGY, GROWTH, MUTATIONS),
                KnowledgeUiContract.analystPages(ForestrySpeciesTypes.TREE, false));
        assertEquals(List.of(DESCRIPTION, ACTIVE_GENOME, INACTIVE_GENOME, KARYOGRAM, CLIMATE, SPECIMEN, BIOLOGY, BEHAVIOUR, MUTATIONS),
                KnowledgeUiContract.analystPages(ForestrySpeciesTypes.BUTTERFLY, false));
        assertEquals(List.of(DESCRIPTION, ACTIVE_GENOME, INACTIVE_GENOME, KARYOGRAM, CLIMATE, SOIL, BIOLOGY, APPEARANCE, MUTATIONS),
                KnowledgeUiContract.analystPages(FlowerSpeciesTypes.FLOWER, false));
    }

    @Test
    void registryLocksDatabaseLeftAndOmitsDiploidOnlyPages() {
        assertEquals(List.of(DATABASE, DESCRIPTION, ACTIVE_GENOME, CLIMATE, PRODUCTS, BIOLOGY, BEHAVIOUR, MUTATIONS),
                KnowledgeUiContract.analystPages(ForestrySpeciesTypes.BEE, true));
        assertEquals(List.of(DATABASE, DESCRIPTION, ACTIVE_GENOME, FRUIT, WOOD, BIOLOGY, GROWTH, MUTATIONS),
                KnowledgeUiContract.analystPages(ForestrySpeciesTypes.TREE, true));
    }

    @Test
    void masterToolsBypassOnlyTheirOriginalDiscoveryTrackers() {
        assertTrue(GeneticKnowledgeMenu.Mode.MASTER_REGISTRY.exposesCompleteSpeciesCatalog(
                GeneticKnowledgeMenu.InterfaceType.REGISTRY));
        assertTrue(GeneticKnowledgeMenu.Mode.MASTER_REGISTRY.exposesCompleteMutationCatalog(
                GeneticKnowledgeMenu.InterfaceType.REGISTRY));

        assertFalse(GeneticKnowledgeMenu.Mode.REGISTRY.exposesCompleteSpeciesCatalog(
                GeneticKnowledgeMenu.InterfaceType.REGISTRY));
        assertFalse(GeneticKnowledgeMenu.Mode.REGISTRY.exposesCompleteMutationCatalog(
                GeneticKnowledgeMenu.InterfaceType.REGISTRY));
        assertFalse(GeneticKnowledgeMenu.Mode.MASTER_DATABASE.exposesCompleteSpeciesCatalog(
                GeneticKnowledgeMenu.InterfaceType.GENE_DATABASE));
        assertFalse(GeneticKnowledgeMenu.Mode.MASTER_REGISTRY.exposesCompleteSpeciesCatalog(
                GeneticKnowledgeMenu.InterfaceType.ANALYST));
        assertTrue(GeneticKnowledgeMenu.Mode.MASTER_DATABASE.exposesCompleteGeneCatalog(
                GeneticKnowledgeMenu.InterfaceType.GENE_DATABASE));
        assertFalse(GeneticKnowledgeMenu.Mode.DATABASE.exposesCompleteGeneCatalog(
                GeneticKnowledgeMenu.InterfaceType.GENE_DATABASE));
        assertFalse(GeneticKnowledgeMenu.Mode.MASTER_DATABASE.exposesCompleteGeneCatalog(
                GeneticKnowledgeMenu.InterfaceType.REGISTRY));
        assertFalse(GeneticKnowledgeMenu.Mode.MASTER_REGISTRY.exposesCompleteGeneCatalog(
                GeneticKnowledgeMenu.InterfaceType.REGISTRY));
    }


    @Test
    void specialistDatabaseWidthsAndModesMatchOriginalWindows() {
        assertEquals(110, KnowledgeUiContract.databaseSelectionWidth(ForestrySpeciesTypes.BEE));
        assertEquals(120, KnowledgeUiContract.databaseSelectionWidth(ForestrySpeciesTypes.TREE));
        assertEquals(160, KnowledgeUiContract.databaseSelectionWidth(ForestrySpeciesTypes.BUTTERFLY));
        assertEquals(130, KnowledgeUiContract.databaseSelectionWidth(FlowerSpeciesTypes.FLOWER));

        assertEquals(316, KnowledgeUiContract.databaseWidth(ForestrySpeciesTypes.BEE));
        assertEquals(326, KnowledgeUiContract.databaseWidth(ForestrySpeciesTypes.TREE));
        assertEquals(366, KnowledgeUiContract.databaseWidth(ForestrySpeciesTypes.BUTTERFLY));
        assertEquals(336, KnowledgeUiContract.databaseWidth(FlowerSpeciesTypes.FLOWER));

        assertEquals(List.of(KnowledgeUiContract.ArboristMode.SPECIES,
                        KnowledgeUiContract.ArboristMode.BRANCHES,
                        KnowledgeUiContract.ArboristMode.BREEDER),
                KnowledgeUiContract.databaseModes(ForestrySpeciesTypes.BUTTERFLY, false));
        assertEquals(List.of(KnowledgeUiContract.ArboristMode.SPECIES,
                        KnowledgeUiContract.ArboristMode.BRANCHES,
                        KnowledgeUiContract.ArboristMode.COLOR,
                        KnowledgeUiContract.ArboristMode.BREEDER),
                KnowledgeUiContract.databaseModes(FlowerSpeciesTypes.FLOWER, false));
        assertEquals(List.of(KnowledgeUiContract.ArboristMode.SPECIES,
                        KnowledgeUiContract.ArboristMode.BRANCHES,
                        KnowledgeUiContract.ArboristMode.BREEDER,
                        KnowledgeUiContract.ArboristMode.FRUIT,
                        KnowledgeUiContract.ArboristMode.WOOD,
                        KnowledgeUiContract.ArboristMode.PLANKS),
                KnowledgeUiContract.databaseModes(ForestrySpeciesTypes.TREE, true));
    }

    @Test
    void specialistPageSetsMatchOriginalDatabasePlugins() {
        assertEquals(List.of("overview", "classification", "genome", "products", "climate", "resultant", "further"),
                KnowledgeUiContract.databaseInfoPages(ForestrySpeciesTypes.BEE,
                        KnowledgeUiContract.ArboristMode.SPECIES, false));
        assertEquals(List.of("overview", "classification", "specimen", "resultant", "further"),
                KnowledgeUiContract.databaseInfoPages(ForestrySpeciesTypes.BUTTERFLY,
                        KnowledgeUiContract.ArboristMode.SPECIES, false));
        assertEquals(List.of("overview", "genome", "classification", "resultant", "further"),
                KnowledgeUiContract.databaseInfoPages(FlowerSpeciesTypes.FLOWER,
                        KnowledgeUiContract.ArboristMode.SPECIES, false));
        assertEquals(List.of("resultant", "further"),
                KnowledgeUiContract.databaseInfoPages(FlowerSpeciesTypes.FLOWER,
                        KnowledgeUiContract.ArboristMode.COLOR, false));
    }

    @Test
    void knowledgeScreensDoNotUseTextDropShadows() throws IOException {
        String source = Files.readString(Path.of("src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        assertFalse(source.contains("drawCenteredString("),
                "GuiGraphics.drawCenteredString always enables a drop shadow");
        assertTrue(source.contains("search.setTextShadow(false)"),
                "knowledge database search fields must render without a drop shadow");
        assertTrue(source.contains("drawCenteredNoShadow"),
                "centered knowledge UI text must use the no-shadow helper");
        assertTrue(source.contains("!I18n.exists(key)"),
                "species without source descriptions must not expose raw translation keys");
        assertTrue(source.contains("y += 11;"),
                "ellipsized genome values must retain the source one-row layout");
    }

    @Test
    void arboristModesExposeOriginalInformationTabs() {
        assertEquals(List.of("overview", "genome", "classification", "resultant", "further"),
                KnowledgeUiContract.arboristInfoPages(KnowledgeUiContract.ArboristMode.SPECIES));
        assertEquals(List.of("overview", "species"),
                KnowledgeUiContract.arboristInfoPages(KnowledgeUiContract.ArboristMode.BRANCHES));
        assertEquals(List.of("breeder"),
                KnowledgeUiContract.arboristInfoPages(KnowledgeUiContract.ArboristMode.BREEDER));
        assertEquals(List.of("natural", "potential"),
                KnowledgeUiContract.arboristInfoPages(KnowledgeUiContract.ArboristMode.FRUIT));
        assertEquals(List.of("natural"),
                KnowledgeUiContract.arboristInfoPages(KnowledgeUiContract.ArboristMode.WOOD));
        assertEquals(List.of("overview", "natural"),
                KnowledgeUiContract.arboristInfoPages(KnowledgeUiContract.ArboristMode.PLANKS));
    }

    @Test
    void breederDiscoveryProgressIsWidthBounded() throws IOException {
        String screen = Files.readString(Path.of(
                "src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        assertTrue(screen.contains("drawCenteredWrapped(graphics, discovery, x + 4, y + 6, width - 8"),
                "the translated breeder discovery sentence must wrap inside the database page");
        assertTrue(screen.contains("int barY = y + 10 + discoveryHeight"),
                "the progress bar must follow the actual wrapped discovery text height");
    }

    @Test
    void specialistMaterialAndBranchPagesUseLegacySpeciesControls() throws IOException {
        String screen = Files.readString(Path.of(
                "src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        assertTrue(screen.contains("renderSpeciesRelationBox"));
        assertTrue(screen.contains("graphics.renderItem(value.icon(), x + 3, rowY + 2)"),
                "ControlSpeciesBox rows must retain the source specimen icon");
        assertTrue(screen.contains("value.species().getDisplayName()"),
                "ControlSpeciesBox rows must show the real runtime species name");
        assertTrue(screen.contains("Component.literal(entry.key()).withStyle(ChatFormatting.ITALIC)"));
        assertTrue(screen.contains("members.getFirst().species().getAuthority()"));
        assertTrue(screen.contains("arboristMode = ArboristMode.SPECIES"),
                "clicking a related species must navigate to its Species page like the source list control");
        assertTrue(screen.contains("set.fenceItem().get()"));
        assertTrue(screen.contains("set.fenceGateItem().get()"));
        assertTrue(screen.contains("set.doorItem().get()"));
        assertTrue(screen.contains("BinnieTreesMachineProcesses.lumbermillOutput"),
                "plank relations must use the real Lumbermill output rather than name-only matching");
    }

    @Test
    void arboristPlankOverviewExposesEveryCraftableDerivativeAndJeiRecipe() throws IOException {
        String screen = Files.readString(Path.of(
                "src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        String jei = Files.readString(Path.of(
                "src/main/java/binnie/trees/jei/BinnieTreesJeiPlugin.java"));
        for (String accessor : List.of("stairsItem", "slabItem", "fenceItem", "fenceGateItem",
                "doorItem", "trapdoorItem", "pressurePlateItem", "buttonItem")) {
            assertTrue(screen.contains("set." + accessor + "().get()"), accessor + " is missing from plank uses");
        }
        assertTrue(screen.contains("getAllRecipesFor(RecipeType.CRAFTING)"),
                "Forestry, vanilla, and compatible external plank derivatives must come from real recipes");
        assertTrue(screen.contains("private int databaseEntryRowHeight()"));
        assertTrue(screen.contains("arboristMode == ArboristMode.WOOD || arboristMode == ArboristMode.PLANKS ? 20 : 14"),
                "wood and plank selector rows need enough vertical space for their item models");
        assertTrue(screen.contains("safeDisplay(stack.getHoverName()), stack, null"),
                "wood rows must not expose missing translation keys");
        assertTrue(screen.contains("BinnieTreesJeiPlugin.showCraftingRecipe(uses.get(i))"),
                "clicking a derivative must open its recipe in JEI");
        assertTrue(jei.contains("RecipeIngredientRole.OUTPUT"));
        assertTrue(jei.contains("current.getRecipesGui().show(focus)"));
    }

    @Test
    void arboristDoorRelationsUseVanillaStyleFlatItemModels() throws IOException {
        for (BinnieTreeWoodType wood : BinnieTreeWoodType.values()) {
            String id = wood.getSerializedName();
            String model = Files.readString(Path.of(
                    "src/main/resources/assets/binnietrees/models/item/" + id + "_door.json"));
            assertTrue(model.contains("minecraft:item/generated"));
            assertTrue(model.contains("binnietrees:item/" + id + "_door"));
            assertFalse(model.contains("binnietrees:block/" + id + "_door_bottom_left"),
                    id + " door still uses the broken lower-door block geometry as its item model");
        }
    }

    @Test
    void analystUsesOriginalBottomDrawerGeometryAndAnalyzedOnlyPages() throws IOException {
        assertEquals(List.of(34, 151, 244, 80, 6, 75, 153), List.of(
                KnowledgeUiContract.ANALYST_DRAWER_X,
                KnowledgeUiContract.ANALYST_DRAWER_Y,
                KnowledgeUiContract.ANALYST_DRAWER_WIDTH,
                KnowledgeUiContract.ANALYST_DRAWER_HEIGHT,
                KnowledgeUiContract.ANALYST_DRAWER_COLLAPSED_HEIGHT,
                KnowledgeUiContract.ANALYST_INVENTORY_X,
                KnowledgeUiContract.ANALYST_INVENTORY_Y));
        assertEquals(List.of(28, 16), List.of(
                KnowledgeUiContract.ANALYST_DRAWER_HANDLE_WIDTH,
                KnowledgeUiContract.ANALYST_DRAWER_HANDLE_HEIGHT));
        String screen = Files.readString(Path.of(
                "src/main/java/binnie/genetics/client/AbstractGeneticKnowledgeScreen.java"));
        assertTrue(screen.contains("if (!individual.isAnalyzed()) return null"),
                "Analyst pages must not reveal an unanalyzed genome");
        String menu = Files.readString(Path.of(
                "src/main/java/binnie/genetics/menu/GeneticKnowledgeMenu.java"));
        assertTrue(menu.contains("dye.is(GeneticsContent.DNA_DYE.get())"),
                "server analysis must require DNA Dye");
    }

    @Test
    void analystRetainsAFullDnaDyeReservoirAndCollapsesAfterAnalysis() throws IOException {
        String menu = Files.readString(Path.of("src/main/java/binnie/genetics/menu/GeneticKnowledgeMenu.java"));
        String screen = Files.readString(Path.of("src/main/java/binnie/genetics/client/AnalystScreen.java"));
        assertTrue(menu.contains("ItemContainerContents.fromItems(stored)"));
        assertTrue(menu.contains("getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(stored)"));
        assertTrue(menu.contains("class AnalystDyeSlot"));
        assertTrue(menu.substring(menu.indexOf("class AnalystDyeSlot")).contains("return 64;"));
        assertTrue(screen.contains("inventoryDrawer.updateAnalyzedSpecimen(hasAnalystSpecimen())"));
    }

    @Test
    void everyKnowledgeInterfaceHasItsOwnConcreteScreenSource() {
        for (String source : List.of(
                "AnalystScreen.java",
                "RegistryScreen.java",
                "GeneDatabaseScreen.java",
                "ArboristDatabaseScreen.java",
                "ApiaristDatabaseScreen.java",
                "MothDatabaseScreen.java",
                "BotanistDatabaseScreen.java")) {
            assertTrue(Files.isRegularFile(Path.of("src/main/java/binnie/genetics/client", source)), source);
        }
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/binnie/genetics/client/GeneticKnowledgeScreenFactory.java")));
    }

}
