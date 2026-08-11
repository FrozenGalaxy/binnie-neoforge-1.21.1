package binnie.genetics.client;

import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.genetics.FlowerSpeciesTypes;
import binnie.botany.genetics.LegacyFlowerCatalog;
import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.genetics.data.GeneticAlleleData;
import binnie.genetics.data.GeneticKnowledgeEntry;
import binnie.genetics.data.GeneticSpeciesDiscovery;
import binnie.genetics.data.GeneticSerumData;
import binnie.genetics.item.GeneticMaterialItem;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.genetics.menu.KnowledgeUiContract;
import binnie.genetics.menu.KnowledgeUiContract.AnalystPage;
import binnie.genetics.menu.KnowledgeUiContract.ArboristMode;
import binnie.genetics.network.SelectDatabaseGenePayload;
import binnie.genetics.registry.GeneticsContent;
import binnie.genetics.tracking.GeneticKnowledgeCatalog;
import binnie.trees.forestry.LegacyTreeCatalog;
import binnie.trees.machine.BinnieTreesMachineProcesses;
import binnie.trees.jei.BinnieTreesJeiPlugin;
import binnie.trees.registry.BinnieTreeWoodContent;
import binnie.trees.wood.BinnieTreeWoodType;
import forestry.api.IForestryApi;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.core.IClimateSensitive;
import forestry.api.core.IProduct;
import forestry.api.core.IProductProducer;
import forestry.api.core.ISpecialtyProducer;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.core.platform.util.GeneticsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
  * Layout matching Binnie 1.12's Analyst, Registry, Gene Bank and Arborist Database.
  * The server menu remains authoritative; this class reproduces the original client controls and navigation.
  */
public abstract class AbstractGeneticKnowledgeScreen extends AbstractContainerScreen<GeneticKnowledgeMenu> {
    private static final ResourceLocation ANALYST_TEXTURE = ResourceLocation.fromNamespaceAndPath("binniecore", "textures/gui/guianalyst.png");
    /** 1.12 Gene Bank sprite used by ControlGene, not the DNA item layers. */
    private static final ResourceLocation GENE_DNA = ResourceLocation.fromNamespaceAndPath("binniegenetics", "textures/gui/gene_dna.png");
    private static final LegacyTreeCatalog LEGACY_TREE_CATALOG = LegacyTreeCatalog.load();

    private static final int[] FAMILY_COLORS = {0xFFD69B, 0x6FAE55, 0x8C70B8, 0xC85B90};

    protected enum ScreenKind {
        ANALYST,
        REGISTRY,
        GENE_DATABASE,
        SPECIES_DATABASE
    }

    private final ScreenKind screenKind;
    @Nullable private final ResourceLocation fixedSpeciesType;
    @Nullable protected EditBox search;
    private int familyIndex;
    private int selectedSpeciesIndex;
    private int leftPageIndex;
    private int rightPageIndex = 1;
    private int leftScroll;
    private int rightScroll;
    private int registrySpeciesScroll;
    private int geneScroll;
    private int arboristScroll;
    private ArboristMode arboristMode = ArboristMode.SPECIES;
    private int arboristInfoPage;
    private int arboristSelection;
    private int geneInfoTab;
    @Nullable private Component transientMessage;
    private long transientMessageUntil;

    protected AbstractGeneticKnowledgeScreen(GeneticKnowledgeMenu menu, Inventory inventory, Component title,
                                             ScreenKind screenKind, @Nullable ResourceLocation fixedSpeciesType) {
        super(menu, inventory, title);
        this.screenKind = screenKind;
        this.fixedSpeciesType = fixedSpeciesType;
        switch (screenKind) {
            case GENE_DATABASE -> {
                imageWidth = KnowledgeUiContract.GENE_DATABASE_WIDTH;
                imageHeight = KnowledgeUiContract.GENE_DATABASE_HEIGHT;
            }
            case SPECIES_DATABASE -> {
                imageWidth = KnowledgeUiContract.databaseWidth(databaseFamily());
                imageHeight = KnowledgeUiContract.DATABASE_HEIGHT;
            }
            case ANALYST, REGISTRY -> {
                imageWidth = KnowledgeUiContract.ANALYST_WIDTH;
                imageHeight = KnowledgeUiContract.ANALYST_HEIGHT;
            }
        }
        inventoryLabelY = 10_000;
    }

    protected final ScreenKind screenKind() {
        return screenKind;
    }

    protected abstract void configureSearchBox();

    protected abstract void renderScreenBackground(GuiGraphics graphics, int mouseX, int mouseY);

    protected abstract void renderScreenContent(GuiGraphics graphics, int mouseX, int mouseY);

    protected abstract void renderScreenTooltip(GuiGraphics graphics, int mouseX, int mouseY);

    protected abstract boolean handleScreenClick(double x, double y, int button);

    /**
      * Foreground controls that must remain above page content and container slots.
      * Tooltips are rendered after this hook.
      */
    protected void renderScreenOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void init() {
        super.init();
        search = null;
        configureSearchBox();
        normalizeSelection();
    }

    protected final void createSearch(int x, int y, int width, int height, Component narration) {
        search = addRenderableWidget(new EditBox(font, x, y, width, height, narration));
        search.setMaxLength(64);
        search.setBordered(false);
        search.setTextShadow(false);
        search.setTextColor(0x303030);
        search.setTextColorUneditable(0x707070);
        search.setHint(narration.copy().withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(value -> {
            registrySpeciesScroll = 0;
            geneScroll = 0;
            arboristScroll = 0;
            normalizeSelection();
        });
    }

    @Override
    protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        LegacyGeneticsGui.window(graphics, leftPos, topPos, imageWidth, imageHeight);
    // Header Help, Info and Owner controls disabled.
    // if (screenKind != ScreenKind.GENE_DATABASE) {
    //   LegacyGeneticsGui.windowControls(graphics, leftPos, topPos, imageWidth);
    // }
        renderScreenBackground(graphics, mouseX, mouseY);
    }

    protected final void renderAnalystFrame(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean registry = screenKind == ScreenKind.REGISTRY;
        int x = 16;
        if (registry) {
            for (int i = 0; i < screenFamilyOrder().size(); i++) {
                drawSystemTab(graphics, x + i * 22, 28, i, i == familyIndex);
            }
            x += screenFamilyOrder().size() * 22;
        } else {
            LegacyGeneticsGui.slot(graphics, leftPos + 15, topPos + 28);
            LegacyGeneticsGui.slot(graphics, leftPos + 37, topPos + 28);
            ItemStack specimen = menu.analystSlots().getItem(GeneticKnowledgeMenu.ANALYST_INDIVIDUAL_SLOT);
            ItemStack dye = menu.analystSlots().getItem(GeneticKnowledgeMenu.ANALYST_DYE_SLOT);
            if (specimen.isEmpty()) LegacyGeneticsGui.validator(graphics, "individual", leftPos + 16, topPos + 29);
            if (dye.isEmpty()) LegacyGeneticsGui.validator(graphics, "dye", leftPos + 38, topPos + 29);
            x += 48;
        }

        List<AnalystPage> pages = currentAnalystPages();
        int tabWidth = pages.isEmpty() ? 0 : Math.max(1, (imageWidth - 16 - x) / pages.size());
        for (int i = 0; i < pages.size(); i++) {
            int tabX = x + i * tabWidth;
            int width = i == pages.size() - 1 ? imageWidth - 16 - tabX : tabWidth;
            boolean selected = i == leftPageIndex || i == rightPageIndex;
            drawColoredTab(graphics, tabX, 28, width, 20, pageColor(pages.get(i)), selected);
            renderMutedTabIcon(graphics, analystPageIcon(pages.get(i)),
                    tabX + (width - 14) / 2, 31, 14, selected);
        }

        int panelX = leftPos + KnowledgeUiContract.ANALYST_PANEL_X;
        int panelY = topPos + KnowledgeUiContract.ANALYST_PANEL_Y;
        graphics.fill(panelX, panelY, panelX + KnowledgeUiContract.ANALYST_PANEL_WIDTH,
                panelY + KnowledgeUiContract.ANALYST_PANEL_HEIGHT, 0x45000000 | 0x444444);
        LegacyGeneticsGui.outline(graphics, panelX, panelY,
                KnowledgeUiContract.ANALYST_PANEL_WIDTH, KnowledgeUiContract.ANALYST_PANEL_HEIGHT);

        drawPageBorder(graphics, KnowledgeUiContract.ANALYST_LEFT_PAGE_X, KnowledgeUiContract.ANALYST_PAGE_Y,
                leftPage(pages));
        drawPageBorder(graphics, KnowledgeUiContract.ANALYST_RIGHT_PAGE_X, KnowledgeUiContract.ANALYST_PAGE_Y,
                rightPage(pages));

    }

    private void drawPageBorder(GuiGraphics graphics, int x, int y, @Nullable AnalystPage page) {
        if (page == null) return;
        int color = pageColor(page);
        graphics.fill(leftPos + x + 1, topPos + y + 1,
                leftPos + x + KnowledgeUiContract.ANALYST_PAGE_WIDTH - 1,
                topPos + y + KnowledgeUiContract.ANALYST_PAGE_HEIGHT - 1,
                0x18000000 | color);
        LegacyGeneticsGui.tabOutline(graphics, leftPos + x, topPos + y,
                KnowledgeUiContract.ANALYST_PAGE_WIDTH, KnowledgeUiContract.ANALYST_PAGE_HEIGHT);
        drawThinScrollbar(graphics, x + KnowledgeUiContract.ANALYST_PAGE_WIDTH - 4, y + 5,
                KnowledgeUiContract.ANALYST_PAGE_HEIGHT - 10,
                page == leftPage(currentAnalystPages()) ? leftScroll : rightScroll,
                analystPageContentHeight(page), color);
    }

    protected final void renderGeneDatabaseFrame(GuiGraphics graphics, int mouseX, int mouseY) {
        drawGeneBankInventoryChrome(graphics);
        LegacyGeneticsGui.blackPanel(graphics, leftPos + KnowledgeUiContract.GENE_DATABASE_BOX_X,
                topPos + KnowledgeUiContract.GENE_DATABASE_BOX_Y,
                KnowledgeUiContract.GENE_DATABASE_BOX_WIDTH,
                KnowledgeUiContract.GENE_DATABASE_BOX_HEIGHT);
        LegacyGeneticsGui.grayPanel(graphics, leftPos + KnowledgeUiContract.GENE_DATABASE_SCROLL_X,
                topPos + KnowledgeUiContract.GENE_DATABASE_BOX_Y,
                KnowledgeUiContract.GENE_DATABASE_SCROLL_WIDTH,
                KnowledgeUiContract.GENE_DATABASE_BOX_HEIGHT);
        LegacyGeneticsGui.searchField(graphics, leftPos + KnowledgeUiContract.GENE_DATABASE_SEARCH_X - 1,
                topPos + KnowledgeUiContract.GENE_DATABASE_SEARCH_Y - 1,
                KnowledgeUiContract.GENE_DATABASE_SEARCH_WIDTH + 2,
                KnowledgeUiContract.GENE_DATABASE_SEARCH_HEIGHT + 2);
        LegacyGeneticsGui.blackPanel(graphics, leftPos + KnowledgeUiContract.GENE_DATABASE_INFO_X,
                topPos + KnowledgeUiContract.GENE_DATABASE_INFO_Y,
                KnowledgeUiContract.GENE_DATABASE_INFO_WIDTH,
                KnowledgeUiContract.GENE_DATABASE_INFO_HEIGHT);
        for (int i = 0; i < 3; i++) {
            int tabHeight = KnowledgeUiContract.GENE_DATABASE_INFO_TABS_HEIGHT / 3;
            drawLeftTab(graphics, KnowledgeUiContract.GENE_DATABASE_INFO_TABS_X,
                    KnowledgeUiContract.GENE_DATABASE_INFO_TABS_Y + i * tabHeight,
                    KnowledgeUiContract.GENE_DATABASE_INFO_TABS_WIDTH, tabHeight,
                    i == geneInfoTab, 0x444444);
            renderMutedTabIcon(graphics, geneDatabaseTabIcon(i),
                    KnowledgeUiContract.GENE_DATABASE_INFO_TABS_X + 2,
                    KnowledgeUiContract.GENE_DATABASE_INFO_TABS_Y + i * tabHeight + (tabHeight - 12) / 2,
                    12, i == geneInfoTab);
        }

        int systemHeight = KnowledgeUiContract.GENE_DATABASE_SYSTEM_HEIGHT / screenFamilyOrder().size();
        for (int i = 0; i < screenFamilyOrder().size(); i++) {
            drawVerticalSystemTab(graphics, KnowledgeUiContract.GENE_DATABASE_SYSTEM_X,
                    KnowledgeUiContract.GENE_DATABASE_SYSTEM_Y + i * systemHeight,
                    KnowledgeUiContract.GENE_DATABASE_SYSTEM_WIDTH, systemHeight,
                    i, i == familyIndex, false);
        }
        drawThinScrollbar(graphics, KnowledgeUiContract.GENE_DATABASE_SCROLL_X + 5,
                KnowledgeUiContract.GENE_DATABASE_BOX_Y + 4,
                KnowledgeUiContract.GENE_DATABASE_BOX_HEIGHT - 8,
                geneScroll, Math.max(116, geneContentHeight()), familyColor(familyIndex));
    }

    protected final void renderArboristFrame(GuiGraphics graphics, int mouseX, int mouseY) {
        int selectionWidth = databaseSelectionWidth();
        LegacyGeneticsGui.blackPanel(graphics, leftPos + KnowledgeUiContract.DATABASE_INFO_X,
                topPos + KnowledgeUiContract.DATABASE_INFO_Y,
                KnowledgeUiContract.DATABASE_INFO_WIDTH,
                KnowledgeUiContract.DATABASE_INFO_HEIGHT);
        LegacyGeneticsGui.blackPanel(graphics, leftPos + KnowledgeUiContract.DATABASE_LIST_X,
                topPos + KnowledgeUiContract.DATABASE_LIST_Y,
                selectionWidth,
                KnowledgeUiContract.DATABASE_LIST_HEIGHT);
        LegacyGeneticsGui.searchField(graphics, leftPos + KnowledgeUiContract.DATABASE_SEARCH_X,
                topPos + KnowledgeUiContract.DATABASE_SEARCH_Y,
                selectionWidth,
                KnowledgeUiContract.DATABASE_SEARCH_HEIGHT);

        List<ArboristMode> modes = availableDatabaseModes();
        int modeHeight = Math.min(28, KnowledgeUiContract.DATABASE_INFO_TABS_HEIGHT / Math.max(1, modes.size()));
        int modeX = KnowledgeUiContract.databaseModeTabsX(databaseFamily());
        for (int i = 0; i < modes.size(); i++) {
            ArboristMode mode = modes.get(i);
            drawVerticalModeTab(graphics, modeX,
                    KnowledgeUiContract.DATABASE_INFO_TABS_Y + i * modeHeight,
                    22, modeHeight - 1, mode, mode == arboristMode);
            renderMutedTabIcon(graphics, databaseModeIcon(mode), modeX + 4,
                    KnowledgeUiContract.DATABASE_INFO_TABS_Y + i * modeHeight + (modeHeight - 14) / 2,
                    14, mode == arboristMode);
        }

        List<String> infoPages = currentDatabaseInfoPages();
        int infoHeight = Math.min(28, KnowledgeUiContract.DATABASE_INFO_TABS_HEIGHT / Math.max(1, infoPages.size()));
        for (int i = 0; i < infoPages.size(); i++) {
            boolean selected = i == arboristInfoPage;
            int color = selected ? databaseAccentColor() : 0x668866;
            graphics.fill(leftPos + KnowledgeUiContract.DATABASE_INFO_TABS_X,
                    topPos + KnowledgeUiContract.DATABASE_INFO_TABS_Y + i * infoHeight,
                    leftPos + KnowledgeUiContract.DATABASE_INFO_TABS_X + KnowledgeUiContract.DATABASE_INFO_TABS_WIDTH,
                    topPos + KnowledgeUiContract.DATABASE_INFO_TABS_Y + (i + 1) * infoHeight - 1,
                    0x66000000 | color);
            renderMutedTabIcon(graphics, databasePageIcon(infoPages.get(i)),
                    KnowledgeUiContract.DATABASE_INFO_TABS_X + 2,
                    KnowledgeUiContract.DATABASE_INFO_TABS_Y + i * infoHeight + (infoHeight - 12) / 2,
                    12, selected);
            if (selected) LegacyGeneticsGui.tabOutline(graphics,
                    leftPos + KnowledgeUiContract.DATABASE_INFO_TABS_X,
                    topPos + KnowledgeUiContract.DATABASE_INFO_TABS_Y + i * infoHeight,
                    KnowledgeUiContract.DATABASE_INFO_TABS_WIDTH, infoHeight - 1);
        }
        drawThinScrollbar(graphics, KnowledgeUiContract.DATABASE_LIST_X + selectionWidth - 5,
                KnowledgeUiContract.DATABASE_LIST_Y + 4,
                KnowledgeUiContract.DATABASE_LIST_HEIGHT - 8, arboristScroll,
                Math.max(KnowledgeUiContract.DATABASE_LIST_HEIGHT,
                        databaseEntries().size() * databaseEntryRowHeight() + 6),
                databaseAccentColor());
    }

    private void drawGeneBankInventoryChrome(GuiGraphics graphics) {
        for (int row = 0; row < KnowledgeUiContract.GENE_DATABASE_INVENTORY_ROWS; row++) {
            for (int column = 0; column < KnowledgeUiContract.GENE_DATABASE_INVENTORY_COLUMNS; column++) {
                LegacyGeneticsGui.slot(graphics,
                        leftPos + KnowledgeUiContract.GENE_DATABASE_INVENTORY_X + column * 18,
                        topPos + KnowledgeUiContract.GENE_DATABASE_INVENTORY_Y + row * 18);
            }
        }
    }

    protected final void drawPlayerInventoryChrome(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) LegacyGeneticsGui.slot(graphics, leftPos + x + col * 18, topPos + y + row * 18);
        }
        for (int col = 0; col < 9; col++) LegacyGeneticsGui.slot(graphics, leftPos + x + col * 18, topPos + y + 58);
    }

    private void drawSystemTab(GuiGraphics graphics, int x, int y, int family, boolean selected) {
        int color = familyColor(family);
        drawColoredTab(graphics, x, y, 20, 20, color, selected);
        ItemStack icon = familyIcon(family);
        if (!icon.isEmpty()) graphics.renderItem(icon, leftPos + x + 2, topPos + y + 2);
    }

    private void drawVerticalSystemTab(GuiGraphics graphics, int x, int y, int w, int h, int family, boolean selected, boolean right) {
        int color = familyColor(family);
        graphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + w - 1, topPos + y + h - 1,
                (selected ? 0xA0000000 : 0x50000000) | color);
        LegacyGeneticsGui.tabOutline(graphics, leftPos + x, topPos + y, w, h);
        ItemStack icon = familyIcon(family);
        if (!icon.isEmpty()) graphics.renderItem(icon, leftPos + x + 4, topPos + y + 5);
    }

    private void drawVerticalModeTab(GuiGraphics graphics, int x, int y, int w, int h,
                                     ArboristMode mode, boolean selected) {
        int color = databaseAccentColor();
        graphics.fill(leftPos + x + 1, topPos + y + 1,
                leftPos + x + w - 1, topPos + y + h - 1,
                (selected ? 0xA0000000 : 0x50000000) | color);
        LegacyGeneticsGui.tabOutline(graphics, leftPos + x, topPos + y, w, h);
    }

    private void drawRightTab(GuiGraphics graphics, int x, int y, int w, int h, boolean selected, int color) {
        graphics.fill(leftPos + x, topPos + y + 1, leftPos + x + w - 1, topPos + y + h - 1,
                (selected ? 0xA0000000 : 0x50000000) | color);
        LegacyGeneticsGui.tabOutline(graphics, leftPos + x, topPos + y, w, h);
    }

    private void drawLeftTab(GuiGraphics graphics, int x, int y, int w, int h, boolean selected, int color) {
        graphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + w, topPos + y + h - 1,
                (selected ? 0xA0000000 : 0x50000000) | color);
        LegacyGeneticsGui.tabOutline(graphics, leftPos + x, topPos + y, w, h);
    }

    private void drawColoredTab(GuiGraphics graphics, int x, int y, int width, int height, int color, boolean selected) {
        graphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + width - 1, topPos + y + height - 1,
                (selected ? 0xB0000000 : 0x50000000) | color);
        LegacyGeneticsGui.tabOutline(graphics, leftPos + x, topPos + y, width, height);
    }

    /**
      * The original Binnie controls used small pictograms instead of text inside their page tabs.
      * Reuse existing item artwork and mute it beneath a translucent wash so it reads as chrome,
      * not as an inventory stack.
      */
    private void renderMutedTabIcon(GuiGraphics graphics, ItemStack icon,
                                    int x, int y, int size, boolean selected) {
        if (icon.isEmpty()) return;
        int absoluteX = leftPos + x;
        int absoluteY = topPos + y;
        float scale = size / 16.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(absoluteX, absoluteY, 100.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.renderItem(icon, 0, 0);
        graphics.pose().popPose();
        graphics.fill(absoluteX, absoluteY, absoluteX + size, absoluteY + size,
                selected ? 0x28000000 : 0x68585858);
    }

    private ItemStack analystPageIcon(AnalystPage page) {
        return switch (page) {
            case DATABASE -> stack(GeneticsContent.REGISTRY.get());
            case DESCRIPTION -> stack(Items.WRITABLE_BOOK);
            case ACTIVE_GENOME -> stack(GeneticsContent.DNA_DYE.get());
            case INACTIVE_GENOME -> stack(Items.GRAY_DYE);
            case KARYOGRAM -> stack(GeneticsContent.PUNNETT_SQUARE.get());
            case CLIMATE -> stack(Items.COMPASS);
            case PRODUCTS -> stack(Items.CHEST);
            case BIOLOGY -> stack(Items.WHEAT_SEEDS);
            case BEHAVIOUR -> stack(Items.CLOCK);
            case FRUIT -> stack(Items.APPLE);
            case WOOD -> stack(Items.OAK_LOG);
            case GROWTH -> stack(Items.OAK_SAPLING);
            case SPECIMEN -> familyIcon(familyIndex);
            case SOIL -> stack(Items.DIRT);
            case APPEARANCE -> stack(Items.MAGENTA_DYE);
            case MUTATIONS -> stack(Items.FERMENTED_SPIDER_EYE);
        };
    }

    private ItemStack databaseModeIcon(ArboristMode mode) {
        return switch (mode) {
            case SPECIES -> familyIcon(familyIndex);
            case BRANCHES -> stack(Items.OAK_SAPLING);
            case BREEDER -> stack(GeneticsContent.PUNNETT_SQUARE.get());
            case FRUIT -> stack(Items.APPLE);
            case WOOD -> stack(Items.OAK_LOG);
            case PLANKS -> stack(Items.OAK_PLANKS);
            case COLOR -> stack(Items.MAGENTA_DYE);
        };
    }

    private ItemStack databasePageIcon(String page) {
        return switch (page) {
            case "overview" -> stack(Items.PAPER);
            case "classification" -> stack(Items.BOOK);
            case "genome" -> stack(GeneticsContent.DNA_DYE.get());
            case "products" -> stack(Items.CHEST);
            case "climate" -> stack(Items.COMPASS);
            case "resultant" -> stack(Items.LIME_DYE);
            case "further" -> stack(Items.CYAN_DYE);
            case "specimen", "species" -> familyIcon(familyIndex);
            case "breeder" -> stack(GeneticsContent.PUNNETT_SQUARE.get());
            case "natural" -> stack(Items.OAK_SAPLING);
            case "potential" -> stack(Items.GLOWSTONE_DUST);
            default -> stack(Items.PAPER);
        };
    }

    private ItemStack geneDatabaseTabIcon(int tab) {
        return switch (tab) {
            case 1 -> stack(Items.COMPARATOR);
            case 2 -> stack(Items.GOLD_NUGGET);
            default -> stack(Items.PAPER);
        };
    }

    private static ItemStack stack(Item item) {
        return new ItemStack(item);
    }

    private void drawThinScrollbar(GuiGraphics graphics, int x, int y, int height, int scroll, int contentHeight, int color) {
        if (contentHeight <= height) return;
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 3, topPos + y + height, 0x44000000 | color);
        int thumb = Math.max(8, height * height / contentHeight);
        int maxScroll = Math.max(1, contentHeight - height);
        int thumbY = y + (height - thumb) * Mth.clamp(scroll, 0, maxScroll) / maxScroll;
        graphics.fill(leftPos + x, topPos + thumbY, leftPos + x + 3, topPos + thumbY + thumb, 0xFF000000 | color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (screenKind == ScreenKind.GENE_DATABASE) {
            drawCenteredNoShadow(graphics, Component.translatable("screen.binniegenetics.database.gene_bank"),
                    imageWidth / 2, 10, 0x303030);
        } else {
            graphics.drawString(font, title, 48, 9, 0x303030, false);
        }
        renderScreenContent(graphics, mouseX, mouseY);
    }

    protected final void renderAnalystContent(GuiGraphics graphics, int mouseX, int mouseY) {
        List<AnalystPage> pages = currentAnalystPages();
        if (screenKind == ScreenKind.ANALYST && !hasAnalystSpecimen()) {
            drawCenteredNoShadow(graphics, Component.translatable("screen.binniegenetics.analyst.title"), imageWidth / 2, 65, 0x444444);
            Component instructions = hasAnalystInput()
                    ? Component.translatable("screen.binniegenetics.analyst.requires_dna_dye")
                    : Component.translatable("screen.binniegenetics.analyst.instructions");
            graphics.drawWordWrap(font, instructions, 36, 84, imageWidth - 72, 0x444444);
            return;
        }
        ResolvedIndividual resolved = currentResolvedIndividual();
        if (resolved == null && screenKind != ScreenKind.REGISTRY) return;

        AnalystPage left = leftPage(pages);
        AnalystPage right = rightPage(pages);
        if (left != null) renderAnalystPage(graphics, left, resolved, KnowledgeUiContract.ANALYST_LEFT_PAGE_X,
                KnowledgeUiContract.ANALYST_PAGE_Y, leftScroll, mouseX, mouseY);
        if (right != null) renderAnalystPage(graphics, right, resolved, KnowledgeUiContract.ANALYST_RIGHT_PAGE_X,
                KnowledgeUiContract.ANALYST_PAGE_Y, rightScroll, mouseX, mouseY);
    }

    /** Local bottom edge available to Analyst pages; the inventory drawer overrides this while open. */
    protected int analystPageClipBottom() {
        return KnowledgeUiContract.ANALYST_PAGE_Y + KnowledgeUiContract.ANALYST_PAGE_HEIGHT - 2;
    }

    private void renderAnalystPage(GuiGraphics graphics, AnalystPage page, @Nullable ResolvedIndividual resolved,
                                   int x, int y, int scroll, int mouseX, int mouseY) {
        int contentX = x + 5;
        int contentY = y + 5 - scroll;
        int width = KnowledgeUiContract.ANALYST_PAGE_WIDTH - 10;
        int pageBottom = Math.min(y + KnowledgeUiContract.ANALYST_PAGE_HEIGHT - 2, analystPageClipBottom());
        if (pageBottom <= y + 2) return;
        graphics.enableScissor(leftPos + x + 2, topPos + y + 2,
                leftPos + x + KnowledgeUiContract.ANALYST_PAGE_WIDTH - 2,
                topPos + pageBottom);
        Component heading = Component.translatable("screen.binniegenetics.analyst.page." + page.name().toLowerCase(Locale.ROOT));
        drawCenteredNoShadow(graphics, heading.copy().withStyle(ChatFormatting.UNDERLINE), x + KnowledgeUiContract.ANALYST_PAGE_WIDTH / 2,
                contentY, pageColor(page));
        int bodyY = contentY + 16;
        if (page == AnalystPage.DATABASE) {
            renderRegistrySpeciesPage(graphics, contentX, bodyY, width, mouseX, mouseY);
        } else if (resolved == null) {
            drawCenteredWrapped(graphics, Component.translatable("screen.binniegenetics.knowledge.empty"),
                    contentX + 10, bodyY + 30, width - 20, 0x666666);
        } else {
            switch (page) {
                case DESCRIPTION -> renderDescriptionPage(graphics, resolved, contentX, bodyY, width);
                case ACTIVE_GENOME -> renderGenomePage(graphics, resolved.genome(), true, contentX, bodyY, width);
                case INACTIVE_GENOME -> renderGenomePage(graphics, resolved.genome(), false, contentX, bodyY, width);
                case KARYOGRAM -> renderKaryogramPage(graphics, resolved.genome(), contentX, bodyY, width, mouseX, mouseY);
                case CLIMATE -> renderClimatePage(graphics, resolved.species(), contentX, bodyY, width);
                case PRODUCTS, FRUIT -> renderProductsPage(graphics, resolved.species(), contentX, bodyY, width, page == AnalystPage.FRUIT);
                case WOOD -> renderWoodPage(graphics, resolved.species(), contentX, bodyY, width);
                case BIOLOGY -> renderBiologyPage(graphics, resolved, contentX, bodyY, width);
                case BEHAVIOUR, GROWTH, SPECIMEN, SOIL, APPEARANCE -> renderSpecialPage(graphics, page, resolved, contentX, bodyY, width);
                case MUTATIONS -> renderMutationPage(graphics, resolved.species(), contentX, bodyY, width);
                default -> {}
            }
        }
        graphics.disableScissor();
    }

    private void renderRegistrySpeciesPage(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY) {
        List<ResolvedSpecies> species = visibleSpecies();
        int listY = y + 24;
        LegacyGeneticsGui.tabOutline(graphics, x - 2, listY - 2, width + 4, 104);
        int columns = Math.max(1, (width - 6) / 18);
        for (int index = registrySpeciesScroll; index < species.size(); index++) {
            int visible = index - registrySpeciesScroll;
            int col = visible % columns;
            int row = visible / columns;
            if (row >= 5) break;
            int itemX = x + 3 + col * 18;
            int itemY = listY + 3 + row * 18;
            ResolvedSpecies entry = species.get(index);
            if (index == selectedSpeciesIndex) {
                graphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, 0x88777777 | familyColor(familyIndex));
                LegacyGeneticsGui.tabOutline(graphics, itemX - 2, itemY - 2, 20, 20);
            } else if (inside(mouseX, mouseY, itemX, itemY, 16, 16)) {
                graphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, 0x55FFFFFF);
            }
            graphics.renderItem(entry.icon(), itemX, itemY);
        }
        if (species.isEmpty()) {
            drawCenteredWrapped(graphics, Component.translatable("screen.binniegenetics.knowledge.empty"),
                    x + 14, listY + 28, width - 28, 0x777777);
        }
    }

    private void renderDescriptionPage(GuiGraphics graphics, ResolvedIndividual resolved, int x, int y, int width) {
        ISpecies<?> species = resolved.species();
        graphics.renderItem(resolved.icon(), x + width / 2 - 8, y);
        y += 20;
        drawCenteredNoShadow(graphics, truncate(species.getDisplayName(), width), x + width / 2, y,
                pageColor(AnalystPage.DESCRIPTION));
        y += 11;
        Component scientificName = Component.literal(species.getGenusName() + " " + species.getSpeciesName())
                .withStyle(ChatFormatting.ITALIC);
        drawCenteredNoShadow(graphics, truncate(scientificName, width), x + width / 2, y, 0x444444);
        y += 18;
        Component authority = Component.translatable("screen.binniegenetics.analyst.discovered_by", species.getAuthority());
        y += drawCenteredWrapped(graphics, authority, x + 4, y, width - 8, 0x444444) + 3;
        Component complexity = Component.translatable("screen.binniegenetics.analyst.complexity", species.getComplexity());
        y += drawCenteredWrapped(graphics, complexity, x + 4, y, width - 8, 0x444444) + 7;
        graphics.drawWordWrap(font, speciesDescription(species).copy().withStyle(ChatFormatting.ITALIC),
                x + 4, y, width - 8, 0x444444);
    }

    private void renderGenomePage(GuiGraphics graphics, IGenome genome, boolean active, int x, int y, int width) {
        int center = x + width / 2;
        float scale = 0.78F;
        int halfWidth = width / 2 - 5;
        for (IChromosome<?> chromosome : genome.getKaryotype().getChromosomes()) {
            AllelePair<?> pair = genome.getAllelePair(chromosome);
            Object value = active ? pair.active().value() : pair.inactive().value();
            Component chromosomeName = GeneticsUtil.getChromosomeName(chromosome);
            Component alleleName = alleleName(chromosome, value);
            List<net.minecraft.util.FormattedCharSequence> chromosomeLines = font.split(chromosomeName,
                    Math.max(1, Math.round(halfWidth / scale)));
            List<net.minecraft.util.FormattedCharSequence> alleleLines = font.split(alleleName,
                    Math.max(1, Math.round(halfWidth / scale)));
            int lineHeight = Math.max(7, Math.round(font.lineHeight * scale));
            int rowLines = Math.max(chromosomeLines.size(), alleleLines.size());
            for (int line = 0; line < chromosomeLines.size(); line++) {
                net.minecraft.util.FormattedCharSequence text = chromosomeLines.get(line);
                int textWidth = Math.round(font.width(text) * scale);
                drawScaledString(graphics, text, center - textWidth - 3, y + line * lineHeight, scale, 0x006699);
            }
            drawScaledString(graphics, Component.literal(":"), center - 1,
                    y + Math.max(0, rowLines - 1) * lineHeight / 2, scale, 0x006699);
            for (int line = 0; line < alleleLines.size(); line++) {
                drawScaledString(graphics, alleleLines.get(line), center + 4,
                        y + line * lineHeight, scale, 0x006699);
            }
            y += rowLines * lineHeight + 3;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Component alleleName(IChromosome chromosome, Object value) {
        try {
            Component translated = GeneticsUtil.getName(chromosome, value);
            return isRawTranslationKey(translated) ? alleleFallback(chromosome, value) : translated;
        } catch (RuntimeException exception) {
            return alleleFallback(chromosome, value);
        }
    }

    private void renderKaryogramPage(GuiGraphics graphics, IGenome genome, int x, int y, int width, int mouseX, int mouseY) {
        List<IChromosome<?>> chromosomes = genome.getKaryotype().getChromosomes();
        int columns = Math.max(1, (width + 4 - 16) / 22);
        int usedColumns = Math.min(columns, chromosomes.size());
        int startX = x + (width - (usedColumns * 18 - 4)) / 2;
        for (int index = 0; index < chromosomes.size(); index++) {
            int col = index % columns;
            int row = index / columns;
            IChromosome<?> chromosome = chromosomes.get(index);
            AllelePair<?> pair = genome.getAllelePair(chromosome);
            boolean homozygous = Objects.equals(pair.active().value(), pair.inactive().value());
            int iconX = startX + col * 22;
            int iconY = y + row * 28;
            graphics.blit(ANALYST_TEXTURE, iconX, iconY, homozygous ? 0 : 16, 0, 16, 22, 256, 256);
        }
    }

    private void renderClimatePage(GuiGraphics graphics, ISpecies<?> species, int x, int y, int width) {
        if (species instanceof IClimateSensitive climate) {
            centeredPair(graphics, x, y, width, Component.translatable("screen.binniegenetics.analyst.temperature"), Component.literal(prettyWords(climate.getTemperature().getSerializedName())));
            centeredPair(graphics, x, y + 24, width, Component.translatable("screen.binniegenetics.analyst.humidity"), Component.literal(prettyWords(climate.getHumidity().name())));
        } else if (species instanceof IFlowerSpecies flower) {
            centeredPair(graphics, x, y, width, Component.translatable("screen.binniegenetics.analyst.temperature"), Component.literal(prettyWords(flower.temperature().getSerializedName())));
            centeredPair(graphics, x, y + 24, width, Component.translatable("screen.binniegenetics.analyst.moisture"), Component.literal(prettyWords(flower.moisture().getSerializedName())));
        } else {
            drawCenteredNoShadow(graphics, Component.translatable("screen.binniegenetics.knowledge.not_applicable"), x + width / 2, y + 20, 0x777777);
        }
    }

    private void renderProductsPage(GuiGraphics graphics, ISpecies<?> species, int x, int y, int width, boolean fruitOnly) {
        ArrayList<IProduct> products = new ArrayList<>();
        if (species instanceof IProductProducer producer) products.addAll(producer.getProducts());
        if (!fruitOnly && species instanceof ISpecialtyProducer producer) products.addAll(producer.getSpecialties());
        int index = 0;
        for (IProduct product : products) {
            ItemStack stack = product.createStack();
            if (stack.isEmpty()) continue;
            int rowY = y + index * 20;
            graphics.renderItem(stack, x + 4, rowY);
            graphics.drawString(font, truncate(stack.getHoverName(), width - 30), x + 24, rowY + 1, 0x444444, false);
            graphics.drawString(font, Component.literal(String.format(Locale.ROOT, "%.1f%%", product.chance() * 100.0F)), x + 24, rowY + 11, 0x777777, false);
            if (++index >= 6) break;
        }
        if (index == 0) drawCenteredNoShadow(graphics, Component.translatable("screen.binniegenetics.analyst.no_products"), x + width / 2, y + 20, 0x777777);
    }

    private void renderWoodPage(GuiGraphics graphics, ISpecies<?> species, int x, int y, int width) {
        if (species instanceof ITreeSpecies tree) {
            ItemStack leaves = tree.getDecorativeLeaves();
            graphics.renderItem(leaves, x + width / 2 - 8, y);
            drawCenteredNoShadow(graphics, leaves.getHoverName(), x + width / 2, y + 20, 0x444444);
            drawCenteredNoShadow(graphics, Component.translatable("screen.binniegenetics.analyst.girth", tree.getGirth(tree.getDefaultGenome())), x + width / 2, y + 34, 0x555555);
            drawCenteredNoShadow(graphics, Component.translatable("screen.binniegenetics.analyst.height", tree.getHeightModifier(tree.getDefaultGenome())), x + width / 2, y + 47, 0x555555);
        } else {
            drawCenteredNoShadow(graphics, Component.translatable("screen.binniegenetics.knowledge.not_applicable"), x + width / 2, y + 20, 0x777777);
        }
    }

    private void renderBiologyPage(GuiGraphics graphics, ResolvedIndividual resolved, int x, int y, int width) {
        ISpecies<?> species = resolved.species();
        centeredPair(graphics, x, y, width, Component.translatable("screen.binniegenetics.analyst.dominance"),
                Component.translatable(species.isDominant() ? "screen.binniegenetics.analyst.dominant" : "screen.binniegenetics.analyst.recessive"));
        centeredPair(graphics, x, y + 24, width, Component.translatable("screen.binniegenetics.analyst.secret"),
                Component.translatable(species.isSecret() ? "gui.yes" : "gui.no"));
        centeredPair(graphics, x, y + 48, width, Component.translatable("screen.binniegenetics.analyst.family"),
                safeDisplay(resolved.type().getDisplayName()));
    }

    private void renderSpecialPage(GuiGraphics graphics, AnalystPage page, ResolvedIndividual resolved, int x, int y, int width) {
        ISpecies<?> species = resolved.species();
        switch (page) {
            case SOIL -> {
                if (species instanceof IFlowerSpecies flower) {
                    centeredPair(graphics, x, y, width, Component.translatable("screen.binniegenetics.analyst.moisture"), Component.literal(prettyWords(flower.moisture().getSerializedName())));
                    centeredPair(graphics, x, y + 24, width, Component.translatable("screen.binniegenetics.analyst.acidity"), Component.literal(prettyWords(flower.acidity().getSerializedName())));
                }
            }
            case APPEARANCE -> {
                if (species instanceof IFlowerSpecies flower) {
                    centeredPair(graphics, x, y, width,
                            Component.translatable("screen.binniegenetics.analyst.flower_type"),
                            Component.literal(prettyId(flower.flowerType())));
                    centeredPair(graphics, x, y + 24, width, Component.translatable("screen.binniegenetics.analyst.sections"), Component.literal(Integer.toString(flower.sections())));
                }
            }
            case SPECIMEN -> {
                graphics.renderItem(resolved.icon(), x + width / 2 - 8, y + 4);
                drawCenteredNoShadow(graphics, resolved.icon().getHoverName(), x + width / 2, y + 26, 0x444444);
            }
            case GROWTH -> {
                if (species instanceof ITreeSpecies tree) {
                    centeredPair(graphics, x, y, width, Component.translatable("screen.binniegenetics.analyst.girth"), Component.literal(Integer.toString(tree.getGirth(tree.getDefaultGenome()))));
                    centeredPair(graphics, x, y + 24, width, Component.translatable("screen.binniegenetics.analyst.height"), Component.literal(String.format(Locale.ROOT, "%.2f", tree.getHeightModifier(tree.getDefaultGenome()))));
                    centeredPair(graphics, x, y + 48, width, Component.translatable("screen.binniegenetics.analyst.rarity"), Component.literal(String.format(Locale.ROOT, "%.2f", tree.getRarity())));
                }
            }
            case BEHAVIOUR -> {
                graphics.drawWordWrap(font, Component.translatable("screen.binniegenetics.analyst.behaviour.description", species.getDisplayName()), x + 4, y, width - 8, 0x444444);
            }
            default -> drawCenteredNoShadow(graphics, Component.translatable("screen.binniegenetics.knowledge.not_applicable"), x + width / 2, y + 20, 0x777777);
        }
    }

    private void renderMutationPage(GuiGraphics graphics, ISpecies<?> species, int x, int y, int width) {
        List<IMutation<?>> mutations = visibleMutations(species);
        List<IMutation<?>> resultant = mutations.stream()
                .filter(mutation -> mutation.getResult().id().equals(species.id())).toList();
        List<IMutation<?>> further = mutations.stream().filter(mutation -> mutation.isPartner(species)).toList();
        int nextY = y;
        if (!resultant.isEmpty()) {
            drawCenteredNoShadow(graphics, Component.translatable("screen.binniegenetics.analyst.mutations.resultant"),
                    x + width / 2, nextY, 0x335520);
            nextY = renderMutationGroup(graphics, resultant, species, true, x, nextY + 11, width);
        }
        if (!further.isEmpty()) {
            if (!resultant.isEmpty()) nextY += 7;
            drawCenteredNoShadow(graphics, Component.translatable("screen.binniegenetics.analyst.mutations.further"),
                    x + width / 2, nextY, 0x335520);
            renderMutationGroup(graphics, further, species, false, x, nextY + 11, width);
        }
        if (resultant.isEmpty() && further.isEmpty()) {
            drawCenteredWrapped(graphics, Component.translatable("screen.binniegenetics.analyst.no_mutations"),
                    x + 5, y + 20, width - 10, 0x777777);
        }
    }

    private int renderMutationGroup(GuiGraphics graphics, List<IMutation<?>> mutations, ISpecies<?> species,
                                    boolean resultant, int x, int y, int width) {
        int gap = 8;
        int groupWidth = 44 * 2 + gap;
        int startX = x + (width - groupWidth) / 2;
        for (int index = 0; index < mutations.size(); index++) {
            IMutation<?> mutation = mutations.get(index);
            int cellX = startX + (index % 2) * (44 + gap);
            int cellY = y + (index / 2) * 18;
            ISpecies<?> left = resultant ? mutation.getFirstParent() : mutation.getPartner(species);
            ISpecies<?> right = resultant ? mutation.getSecondParent() : mutation.getResult();
            graphics.renderItem(left.createStack(mutation.getType().getTypeForMutation(resultant ? 0 : 1)), cellX, cellY);
            graphics.drawString(font, resultant ? "+" : "→", cellX + (resultant ? 19 : 17), cellY + 5,
                    mutationChanceColor(mutation.getChance()), false);
            graphics.renderItem(right.createStack(mutation.getType().getTypeForMutation(resultant ? 1 : 2)), cellX + 28, cellY);
        }
        return y + ((mutations.size() + 1) / 2) * 18;
    }

    private List<IMutation<?>> visibleMutations(ISpecies<?> species) {
        List<IMutation<?>> all = new ArrayList<>(species.getType().getMutations().getAllMutations());
        return all.stream()
                .filter(mutation -> mutation.isPartner(species) || mutation.getResult().id().equals(species.id()))
                .filter(mutation -> menu.mode().exposesCompleteMutationCatalog(menu.interfaceType())
                        || (!mutation.isSecret()
                        && knowsSpecies(mutation.getFirstParent())
                        && knowsSpecies(mutation.getSecondParent())
                        && knowsSpecies(mutation.getResult())))
                .toList();
    }

    private static int mutationChanceColor(float chance) {
        if (chance >= 0.20F) return 0x006600;
        if (chance >= 0.15F) return 0x229922;
        if (chance >= 0.10F) return 0xAAAA00;
        if (chance >= 0.05F) return 0xCC8800;
        return chance > 0.0F ? 0xCC2222 : 0x660000;
    }

    private void centeredPair(GuiGraphics graphics, int x, int y, int width, Component label, Component value) {
        drawCenteredNoShadow(graphics, label.copy().withStyle(ChatFormatting.UNDERLINE), x + width / 2, y, 0x555555);
        drawCenteredNoShadow(graphics, value, x + width / 2, y + 11, 0x333333);
    }

    protected final void renderGeneDatabaseContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int known = menu.knowledge().genes().size();
        int total = GeneticKnowledgeCatalog.masterGeneDatabase().genes().size();
        graphics.enableScissor(leftPos + KnowledgeUiContract.GENE_DATABASE_INFO_X + 2,
                topPos + KnowledgeUiContract.GENE_DATABASE_INFO_Y + 2,
                leftPos + KnowledgeUiContract.GENE_DATABASE_INFO_X + KnowledgeUiContract.GENE_DATABASE_INFO_WIDTH - 2,
                topPos + KnowledgeUiContract.GENE_DATABASE_INFO_Y + KnowledgeUiContract.GENE_DATABASE_INFO_HEIGHT - 2);
        Component heading = switch (geneInfoTab) {
            case 1 -> Component.translatable("screen.binniegenetics.database.stats");
            case 2 -> Component.translatable("screen.binniegenetics.database.ranking");
            default -> Component.translatable("screen.binniegenetics.database.project");
        };
        drawCenteredNoShadow(graphics, heading,
                KnowledgeUiContract.GENE_DATABASE_INFO_X + KnowledgeUiContract.GENE_DATABASE_INFO_WIDTH / 2,
                KnowledgeUiContract.GENE_DATABASE_INFO_Y + 7, 0xFFFFFF);
        Component detail = geneInfoTab == 2
                ? Component.translatable("screen.binniegenetics.database.ranking_value", total == 0 ? 0 : known * 100 / total)
                : Component.translatable("screen.binniegenetics.database.sequenced", known, total);
        drawCenteredNoShadow(graphics, detail,
                KnowledgeUiContract.GENE_DATABASE_INFO_X + KnowledgeUiContract.GENE_DATABASE_INFO_WIDTH / 2,
                KnowledgeUiContract.GENE_DATABASE_INFO_Y + 24, 0xB0B0B0);
        if (transientMessage != null && System.currentTimeMillis() < transientMessageUntil) {
            drawCenteredNoShadow(graphics, transientMessage,
                    KnowledgeUiContract.GENE_DATABASE_INFO_X + KnowledgeUiContract.GENE_DATABASE_INFO_WIDTH / 2,
                    KnowledgeUiContract.GENE_DATABASE_INFO_Y + 37, 0x88FF88);
        }
        graphics.disableScissor();

        List<GeneCell> cells = geneCells();
        int hoveredOrdinal = GeneBankGridLayout.hitTest(cells.stream().map(GeneCell::layout).toList(),
                mouseX - leftPos, mouseY - topPos, geneScroll);
        int clipX = KnowledgeUiContract.GENE_DATABASE_BOX_X + 2;
        int clipY = KnowledgeUiContract.GENE_DATABASE_BOX_Y + 2;
        graphics.enableScissor(leftPos + clipX, topPos + clipY,
                leftPos + KnowledgeUiContract.GENE_DATABASE_BOX_X + KnowledgeUiContract.GENE_DATABASE_BOX_WIDTH - 2,
                topPos + KnowledgeUiContract.GENE_DATABASE_BOX_Y + KnowledgeUiContract.GENE_DATABASE_BOX_HEIGHT - 2);
        for (GeneCell cell : cells) {
            GeneBankGridLayout.Cell layout = cell.layout();
            int y = layout.y() - geneScroll;
            if (layout.firstInChromosome()) {
                graphics.drawString(font, chromosomeDisplay(cell.gene()),
                        GeneBankGridLayout.CONTENT_X, layout.headingY() - geneScroll, 0xFFFFFF, false);
            }
            if (y >= KnowledgeUiContract.GENE_DATABASE_BOX_Y - GeneBankGridLayout.CELL_SIZE
                    && y < KnowledgeUiContract.GENE_DATABASE_BOX_Y + KnowledgeUiContract.GENE_DATABASE_BOX_HEIGHT) {
                if (layout.ordinal() == hoveredOrdinal && canApplyGene(cell.gene())) {
                    graphics.fill(layout.x(), y, layout.x() + 16, y + 16, 0xFFFFFFFF);
                    graphics.fill(layout.x() + 1, y + 1, layout.x() + 15, y + 15, 0xFF444444);
                }
                graphics.blit(GENE_DNA, layout.x(), y, 0, 0, 16, 16, 16, 16);
            }
        }
        graphics.disableScissor();
    }

    protected final void renderArboristContent(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ArboristEntry> entries = databaseEntries();
        int selectionWidth = databaseSelectionWidth();
        int rowHeight = databaseEntryRowHeight();
        int start = arboristScroll / rowHeight;
        int yOffset = -(arboristScroll % rowHeight);
        graphics.enableScissor(leftPos + KnowledgeUiContract.DATABASE_LIST_X + 2,
                topPos + KnowledgeUiContract.DATABASE_LIST_Y + 2,
                leftPos + KnowledgeUiContract.DATABASE_LIST_X + selectionWidth - 2,
                topPos + KnowledgeUiContract.DATABASE_LIST_Y + KnowledgeUiContract.DATABASE_LIST_HEIGHT - 2);
        for (int index = start; index < entries.size(); index++) {
            int y = KnowledgeUiContract.DATABASE_LIST_Y + 3 + yOffset + (index - start) * rowHeight;
            if (y >= KnowledgeUiContract.DATABASE_LIST_Y + KnowledgeUiContract.DATABASE_LIST_HEIGHT) break;
            ArboristEntry entry = entries.get(index);
            if (index == arboristSelection) graphics.fill(KnowledgeUiContract.DATABASE_LIST_X + 2, y - 1,
                    KnowledgeUiContract.DATABASE_LIST_X + selectionWidth - 3, y + rowHeight - 2, 0x66467B31);
            if (!entry.icon().isEmpty()) {
                graphics.renderItem(entry.icon(), KnowledgeUiContract.DATABASE_LIST_X + 4, y + (rowHeight - 18) / 2 - 1);
            } else if (arboristMode == ArboristMode.COLOR) {
                int color = flowerRgb(entry.key());
                graphics.fill(KnowledgeUiContract.DATABASE_LIST_X + 5, y,
                        KnowledgeUiContract.DATABASE_LIST_X + 17, y + 10, 0xFF000000 | color);
                LegacyGeneticsGui.outline(graphics, KnowledgeUiContract.DATABASE_LIST_X + 4, y - 1, 14, 12);
            }
            graphics.drawString(font, truncate(entry.name(), selectionWidth - 28),
                    KnowledgeUiContract.DATABASE_LIST_X + 23, y + (rowHeight - font.lineHeight) / 2, 0xEEEEEE, false);
        }
        graphics.disableScissor();

        graphics.enableScissor(leftPos + KnowledgeUiContract.DATABASE_INFO_X + 2,
                topPos + KnowledgeUiContract.DATABASE_INFO_Y + 2,
                leftPos + KnowledgeUiContract.DATABASE_INFO_X + KnowledgeUiContract.DATABASE_INFO_WIDTH - 2,
                topPos + KnowledgeUiContract.DATABASE_INFO_Y + KnowledgeUiContract.DATABASE_INFO_HEIGHT - 2);
        renderArboristInfo(graphics, selectedArboristEntry(entries), mouseX - leftPos, mouseY - topPos);
        graphics.disableScissor();
    }

    private void renderArboristInfo(GuiGraphics graphics, @Nullable ArboristEntry entry, int mouseX, int mouseY) {
        int x = KnowledgeUiContract.DATABASE_INFO_X + 8;
        int y = KnowledgeUiContract.DATABASE_INFO_Y + 8;
        int width = KnowledgeUiContract.DATABASE_INFO_WIDTH - 16;
        List<String> pages = currentDatabaseInfoPages();
        String page = pages.get(Math.clamp(arboristInfoPage, 0, pages.size() - 1));
        boolean pageOwnsHeading = (arboristMode == ArboristMode.BRANCHES && "overview".equals(page))
                || (arboristMode == ArboristMode.PLANKS && "overview".equals(page));
        if (!pageOwnsHeading) {
            Component heading = Component.translatable("screen.binnietrees.arborist.page." + page)
                    .withStyle(ChatFormatting.UNDERLINE);
            drawCenteredNoShadow(graphics, heading, x + width / 2, y, databaseAccentColor());
            y += 15;
        }
        if (entry == null) {
            drawCenteredWrapped(graphics, Component.translatable("screen.binniegenetics.knowledge.empty"),
                    x + 10, y + 24, width - 20, 0xAAAAAA);
            return;
        }
        boolean sourceOwnsHeading = arboristMode == ArboristMode.BRANCHES
                || arboristMode == ArboristMode.FRUIT
                || arboristMode == ArboristMode.WOOD
                || arboristMode == ArboristMode.PLANKS;
        if (!sourceOwnsHeading && !entry.icon().isEmpty()) {
            graphics.renderItem(entry.icon(), x + width / 2 - 8, y);
            y += 20;
        } else if (!sourceOwnsHeading && arboristMode == ArboristMode.COLOR) {
            int color = flowerRgb(entry.key());
            graphics.fill(x + width / 2 - 8, y, x + width / 2 + 8, y + 16, 0xFF000000 | color);
            LegacyGeneticsGui.outline(graphics, x + width / 2 - 9, y - 1, 18, 18);
            y += 20;
        }
        if (!sourceOwnsHeading) {
            drawCenteredNoShadow(graphics, truncate(entry.name(), width), x + width / 2, y, 0xFFFFFF);
            y += 13;
        }
        switch (arboristMode) {
            case SPECIES -> renderDatabaseSpeciesInfo(graphics, entry.species(), page, x, y, width);
            case BRANCHES -> renderArboristBranchInfo(graphics, entry, page, x, y, width, mouseX, mouseY);
            case BREEDER -> renderBreederInfo(graphics, x, y, width);
            case FRUIT, WOOD, PLANKS -> renderArboristItemInfo(graphics, entry, page, x, y, width, mouseX, mouseY);
            case COLOR -> renderFlowerColorInfo(graphics, entry, page, x, y, width);
        }
    }

    private void renderDatabaseSpeciesInfo(GuiGraphics graphics, @Nullable ISpecies<?> species,
                                           String page, int x, int y, int width) {
        if (species == null) return;
        switch (page) {
            case "overview" -> {
                Component scientificName = Component.literal(species.getGenusName() + " " + species.getSpeciesName())
                        .withStyle(ChatFormatting.ITALIC);
                drawCenteredNoShadow(graphics, truncate(scientificName, width), x + width / 2, y, 0xDDDDDD);
                int textY = y + 13;
                Component authority = Component.translatable("screen.binniegenetics.analyst.discovered_by", species.getAuthority());
                textY += drawCenteredWrapped(graphics, authority, x, textY, width, 0xBBBBBB) + 7;
                graphics.drawWordWrap(font, speciesDescription(species), x, textY, width, 0xDDDDDD);
            }
            case "genome" -> renderGenomePage(graphics, species.getDefaultGenome(), true, x, y, width);
            case "classification" -> {
                graphics.drawString(font,
                        Component.translatable("screen.binnietrees.arborist.genus", species.getGenusName()),
                        x, y, 0xDDDDDD, false);
                graphics.drawString(font,
                        Component.translatable("screen.binnietrees.arborist.species", species.getSpeciesName()),
                        x, y + 13, 0xDDDDDD, false);
                graphics.drawString(font,
                        Component.translatable("screen.binniegenetics.analyst.complexity", species.getComplexity()),
                        x, y + 26, 0xDDDDDD, false);
            }
            case "products" -> renderProductsPage(graphics, species, x, y, width, false);
            case "climate" -> renderClimatePage(graphics, species, x, y, width);
            case "specimen" -> {
                ItemStack specimen = species.createStack(species.getType().getDefaultStage());
                graphics.renderItem(specimen, x + width / 2 - 8, y + 4);
                drawCenteredNoShadow(graphics, specimen.getHoverName(), x + width / 2, y + 26, 0xDDDDDD);
            }
            case "resultant" -> renderMutationListForSpecies(graphics, species, true, x, y, width);
            case "further" -> renderMutationListForSpecies(graphics, species, false, x, y, width);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void renderMutationListForSpecies(GuiGraphics graphics, ISpecies<?> species,
                                              boolean into, int x, int y, int width) {
        ISpeciesType rawType = (ISpeciesType) species.getType();
        ISpecies rawSpecies = (ISpecies) species;
        List<IMutation> mutations = into
                ? new ArrayList<>((List<IMutation>) rawType.getMutations().getMutationsInto(rawSpecies))
                : new ArrayList<>((List<IMutation>) rawType.getMutations().getMutationsFrom(rawSpecies));
        if (!menu.mode().master()) {
            mutations.removeIf(mutation -> mutation.isSecret()
                    || !knowsSpecies(mutation.getFirstParent())
                    || !knowsSpecies(mutation.getSecondParent())
                    || !knowsSpecies(mutation.getResult()));
        }
        for (int i = 0; i < Math.min(7, mutations.size()); i++) {
            IMutation mutation = mutations.get(i);
            Component line = into
                    ? Component.literal(mutation.getFirstParent().getDisplayName().getString()
                    + " + " + mutation.getSecondParent().getDisplayName().getString())
                    : mutation.getResult().getDisplayName();
            graphics.drawString(font, truncate(line, width), x, y + i * 12, 0xDDDDDD, false);
        }
        if (mutations.isEmpty()) {
            drawCenteredWrapped(graphics, Component.translatable("screen.binniegenetics.analyst.no_mutations"),
                    x, y + 20, width, 0xAAAAAA);
        }
    }

    private boolean knowsSpecies(ISpecies<?> species) {
        return menu.knowledge().species().stream().anyMatch(discovery ->
                discovery.speciesType().equals(species.getType().id())
                        && discovery.species().equals(species.id()));
    }



    private void renderArboristBranchInfo(GuiGraphics graphics, ArboristEntry entry,
                                           String page, int x, int y, int width, int mouseX, int mouseY) {
        if ("overview".equals(page)) {
            List<ResolvedSpecies> members = branchMembers(entry);
            drawCenteredNoShadow(graphics, entry.name().copy().withStyle(ChatFormatting.UNDERLINE),
                    x + width / 2, y, 0xFFFFFF);
            drawCenteredNoShadow(graphics, Component.literal(entry.key()).withStyle(ChatFormatting.ITALIC),
                    x + width / 2, y + 24, 0xDDDDDD);
            if (!members.isEmpty()) {
                Component authority = Component.translatable("screen.binniegenetics.analyst.discovered_by",
                        members.getFirst().species().getAuthority());
                drawCenteredWrapped(graphics, authority, x, y + 36, width, 0xBBBBBB);
            }
            drawCenteredWrapped(graphics, Component.translatable("screen.binniegenetics.knowledge.no_description")
                            .withStyle(ChatFormatting.ITALIC),
                    x, y + 76, width, 0xAAAAAA);
        } else {
            renderSpeciesRelationBox(graphics, branchMembers(entry), x, y, width, mouseX, mouseY);
        }
    }

    private void renderBreederInfo(GuiGraphics graphics, int x, int y, int width) {
        int known = databaseSpecies().size();
        ISpeciesType<?, ?> type = IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypeSafe(databaseFamily());
        int total = type == null ? 0 : type.getSpeciesCount();
        Component discovery = Component.translatable("screen.binnietrees.arborist.breeder.progress", known, total);
        int discoveryHeight = drawCenteredWrapped(graphics, discovery, x + 4, y + 6, width - 8, 0xDDDDDD);
        int barY = y + 10 + discoveryHeight;
        int bar = total == 0 ? 0 : (width - 8) * known / total;
        graphics.fill(x + 4, barY, x + width - 4, barY + 8, 0xFF222222);
        graphics.fill(x + 4, barY, x + 4 + bar, barY + 8, 0xFF5E9B45);
    }

    private void renderArboristItemInfo(GuiGraphics graphics, ArboristEntry entry, String page,
                                        int x, int y, int width, int mouseX, int mouseY) {
        if ("overview".equals(page)) {
            renderPlankOverview(graphics, entry, x, y, width);
            return;
        }
        List<ResolvedSpecies> related = relatedSpecies(entry);
        renderSpeciesRelationBox(graphics, related, x, y, width, mouseX, mouseY);
        if (related.isEmpty()) {
            drawCenteredWrapped(graphics, Component.translatable("screen.binnietrees.arborist.no_natural_species"),
                    x, y + 20, width, 0xAAAAAA);
        }
    }

    /** Current replacement for the removed Forestry classification/list controls used by every 1.12 database. */
    private void renderSpeciesRelationBox(GuiGraphics graphics, List<ResolvedSpecies> species,
                                          int x, int y, int width, int mouseX, int mouseY) {
        int rows = Math.min(7, species.size());
        if (rows == 0) return;
        graphics.fill(x, y, x + width, y + rows * 20 + 2, 0xFF555555);
        LegacyGeneticsGui.outline(graphics, x, y, width, rows * 20 + 2);
        for (int i = 0; i < rows; i++) {
            ResolvedSpecies value = species.get(i);
            int rowY = y + 1 + i * 20;
            if (inside(mouseX, mouseY, x + 1, rowY, width - 2, 20)) {
                graphics.fill(x + 1, rowY, x + width - 1, rowY + 20, 0x665E9B45);
            }
            graphics.renderItem(value.icon(), x + 3, rowY + 2);
            graphics.drawString(font, truncate(value.species().getDisplayName(), width - 25),
                    x + 23, rowY + 6, 0xEEEEEE, false);
        }
    }

    private void renderPlankOverview(GuiGraphics graphics, ArboristEntry entry, int x, int y, int width) {
        drawCenteredNoShadow(graphics, truncate(entry.name(), width), x + width / 2, y, 0xFFFFFF);
        graphics.renderItem(entry.icon(), x + width / 2 - 8, y + 14);
        graphics.drawString(font, Component.translatable("screen.binnietrees.arborist.planks.use"),
                x + 4, y + 39, 0xDDDDDD, false);
        List<ItemStack> uses = plankUses(entry.icon());
        for (int i = 0; i < uses.size(); i++) {
            graphics.renderItem(uses.get(i), plankUseX(x, i), plankUseY(y, i));
        }
    // PagePlanksTrees in 1.12 consisted of the plank heading plus its related-tree list.
    // It did not provide prose descriptions, so do not fill this page with a misleading
    // "No description available" message.
    }

    private List<ItemStack> plankUses(ItemStack planks) {
        LinkedHashMap<Item, ItemStack> uses = new LinkedHashMap<>();
        for (BinnieTreeWoodType type : BinnieTreeWoodType.values()) {
            BinnieTreeWoodContent.WoodSet set = BinnieTreeWoodContent.get(type);
            if (!planks.is(set.planksItem().get())) continue;
            List.of(set.stairsItem().get(), set.slabItem().get(), set.fenceItem().get(),
                    set.fenceGateItem().get(), set.doorItem().get(), set.trapdoorItem().get(),
                    set.pressurePlateItem().get(), set.buttonItem().get())
                    .forEach(item -> uses.put(item, new ItemStack(item)));
            break;
        }
    // The 1.12 page also listed Forestry and vanilla plank families. Resolve their modern
    // derivatives from the live recipe manager instead of guessing IDs, which also lets
    // third-party Forestry wood sets participate when their recipes use the selected plank.
        if (Minecraft.getInstance().level != null) {
            var level = Minecraft.getInstance().level;
            for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                var recipe = holder.value();
                if (recipe.getIngredients().stream().noneMatch(ingredient -> ingredient.test(planks))) continue;
                ItemStack output = recipe.getResultItem(level.registryAccess());
                if (!isPlankDerivative(output)) continue;
                uses.putIfAbsent(output.getItem(), output.copyWithCount(1));
            }
        }
        ArrayList<ItemStack> result = new ArrayList<>(uses.values());
        result.sort(Comparator.comparingInt(AbstractGeneticKnowledgeScreen::plankUseOrder)
                .thenComparing(stack -> safeDisplay(stack.getHoverName()).getString(), String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private static boolean isPlankDerivative(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        return blockItem.getBlock() instanceof StairBlock || blockItem.getBlock() instanceof SlabBlock
                || blockItem.getBlock() instanceof FenceBlock || blockItem.getBlock() instanceof FenceGateBlock
                || blockItem.getBlock() instanceof DoorBlock || blockItem.getBlock() instanceof TrapDoorBlock
                || blockItem.getBlock() instanceof PressurePlateBlock || blockItem.getBlock() instanceof ButtonBlock;
    }

    private static int plankUseOrder(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return 99;
        if (blockItem.getBlock() instanceof StairBlock) return 0;
        if (blockItem.getBlock() instanceof SlabBlock) return 1;
        if (blockItem.getBlock() instanceof FenceBlock) return 2;
        if (blockItem.getBlock() instanceof FenceGateBlock) return 3;
        if (blockItem.getBlock() instanceof DoorBlock) return 4;
        if (blockItem.getBlock() instanceof TrapDoorBlock) return 5;
        if (blockItem.getBlock() instanceof PressurePlateBlock) return 6;
        if (blockItem.getBlock() instanceof ButtonBlock) return 7;
        return 99;
    }

    private static int plankUseX(int x, int index) {
        return x + 8 + index % 4 * 22;
    }

    private static int plankUseY(int y, int index) {
        return y + 52 + index / 4 * 22;
    }

    private int databaseEntryRowHeight() {
        return arboristMode == ArboristMode.WOOD || arboristMode == ArboristMode.PLANKS ? 20 : 14;
    }

    private List<ResolvedSpecies> branchMembers(ArboristEntry entry) {
        return databaseSpecies().stream()
                .filter(value -> value.species().getGenusName().equals(entry.key())).toList();
    }

    private List<ResolvedSpecies> relatedSpecies(ArboristEntry entry) {
        return databaseSpecies().stream().filter(species -> relates(entry, species.species())).toList();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderScreenOverlay(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderKnowledgeTooltip(graphics, mouseX, mouseY);
    }

    private void renderKnowledgeTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        renderScreenTooltip(graphics, mouseX, mouseY);
    }

    protected final void renderAnalystTabTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<AnalystPage> pages = currentAnalystPages();
        int x = screenKind == ScreenKind.REGISTRY ? 16 + screenFamilyOrder().size() * 22 : 64;
        if (pages.isEmpty()) return;
        int tabWidth = Math.max(1, (imageWidth - 16 - x) / pages.size());
        for (int i = 0; i < pages.size(); i++) {
            int tabX = x + i * tabWidth;
            int width = i == pages.size() - 1 ? imageWidth - 16 - tabX : tabWidth;
            if (inside(mouseX - leftPos, mouseY - topPos, tabX, 28, width, 20)) {
                graphics.renderTooltip(font, Component.translatable("screen.binniegenetics.analyst.page." + pages.get(i).name().toLowerCase(Locale.ROOT)), mouseX, mouseY);
                return;
            }
        }
    }

    protected final void renderAnalystContentTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ResolvedIndividual resolved = currentResolvedIndividual();
        if (resolved == null) return;
        List<AnalystPage> pages = currentAnalystPages();
        if (renderAnalystPageTooltip(graphics, leftPage(pages), resolved,
                KnowledgeUiContract.ANALYST_LEFT_PAGE_X, leftScroll, mouseX, mouseY)) return;
        renderAnalystPageTooltip(graphics, rightPage(pages), resolved,
                KnowledgeUiContract.ANALYST_RIGHT_PAGE_X, rightScroll, mouseX, mouseY);
    }

    private boolean renderAnalystPageTooltip(GuiGraphics graphics, @Nullable AnalystPage page,
                                             ResolvedIndividual resolved, int pageX, int scroll,
                                             int mouseX, int mouseY) {
        if (page != AnalystPage.KARYOGRAM && page != AnalystPage.MUTATIONS) return false;
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;
        if (!inside(localX, localY, pageX + 2, KnowledgeUiContract.ANALYST_PAGE_Y + 2,
                KnowledgeUiContract.ANALYST_PAGE_WIDTH - 4, KnowledgeUiContract.ANALYST_PAGE_HEIGHT - 4)) return false;
        int contentX = pageX + 5;
        int contentY = KnowledgeUiContract.ANALYST_PAGE_Y + 21 - scroll;
        int width = KnowledgeUiContract.ANALYST_PAGE_WIDTH - 10;
        if (page == AnalystPage.KARYOGRAM) {
            return renderKaryogramTooltip(graphics, resolved.genome(), contentX, contentY,
                    width, localX, localY, mouseX, mouseY);
        }
        return renderMutationTooltip(graphics, resolved.species(), contentX, contentY,
                width, localX, localY, mouseX, mouseY);
    }

    private boolean renderKaryogramTooltip(GuiGraphics graphics, IGenome genome, int x, int y, int width,
                                           double localX, double localY, int mouseX, int mouseY) {
        List<IChromosome<?>> chromosomes = genome.getKaryotype().getChromosomes();
        int columns = Math.max(1, (width + 4 - 16) / 22);
        int usedColumns = Math.min(columns, chromosomes.size());
        int startX = x + (width - (usedColumns * 18 - 4)) / 2;
        for (int index = 0; index < chromosomes.size(); index++) {
            int iconX = startX + (index % columns) * 22;
            int iconY = y + (index / columns) * 28;
            if (!inside(localX, localY, iconX, iconY, 16, 22)) continue;
            IChromosome<?> chromosome = chromosomes.get(index);
            AllelePair<?> pair = genome.getAllelePair(chromosome);
            ArrayList<Component> tooltip = new ArrayList<>();
            tooltip.add(GeneticsUtil.getChromosomeName(chromosome));
            if (Objects.equals(pair.active().value(), pair.inactive().value())) {
                tooltip.add(alleleName(chromosome, pair.active().value()).copy().withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("screen.binniegenetics.analyst.karyogram.active",
                        alleleName(chromosome, pair.active().value())).withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.translatable("screen.binniegenetics.analyst.karyogram.inactive",
                        alleleName(chromosome, pair.inactive().value())).withStyle(ChatFormatting.DARK_GRAY));
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return true;
        }
        return false;
    }

    private boolean renderMutationTooltip(GuiGraphics graphics, ISpecies<?> species, int x, int y, int width,
                                          double localX, double localY, int mouseX, int mouseY) {
        List<IMutation<?>> mutations = visibleMutations(species);
        List<IMutation<?>> resultant = mutations.stream()
                .filter(mutation -> mutation.getResult().id().equals(species.id())).toList();
        List<IMutation<?>> further = mutations.stream().filter(mutation -> mutation.isPartner(species)).toList();
        int cellY = y + (resultant.isEmpty() ? 0 : 11);
        MutationHit hit = mutationHit(resultant, true, x, cellY, width, localX, localY);
        if (hit == null && !further.isEmpty()) {
            cellY += resultant.isEmpty() ? 11 : ((resultant.size() + 1) / 2) * 18 + 18;
            hit = mutationHit(further, false, x, cellY, width, localX, localY);
        }
        if (hit == null) return false;
        IMutation<?> mutation = hit.mutation();
        ArrayList<Component> tooltip = new ArrayList<>();
        if (!hit.resultant()) tooltip.add(mutation.getResult().getDisplayName());
        ISpecies<?> first = hit.resultant() ? mutation.getFirstParent() : species;
        ISpecies<?> second = hit.resultant() ? mutation.getSecondParent() : mutation.getPartner(species);
        tooltip.add(first.getDisplayName().copy().append(" + ").append(second.getDisplayName()));
        tooltip.add(Component.translatable("screen.binniegenetics.analyst.mutations.chance",
                String.format(Locale.ROOT, "%.1f%%", mutation.getChance() * 100.0F))
                .withStyle(ChatFormatting.GRAY));
        for (Object conditionObject : mutation.getSpecialConditions()) {
            if (conditionObject instanceof Component condition) {
                tooltip.add(safeDisplay(condition).copy().withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        return true;
    }

    @Nullable
    private MutationHit mutationHit(List<IMutation<?>> mutations, boolean resultant, int x, int y, int width,
                                    double mouseX, double mouseY) {
        int startX = x + (width - (44 * 2 + 8)) / 2;
        for (int index = 0; index < mutations.size(); index++) {
            int cellX = startX + (index % 2) * 52;
            int cellY = y + (index / 2) * 18;
            if (inside(mouseX, mouseY, cellX, cellY, 44, 16)) {
                return new MutationHit(mutations.get(index), resultant);
            }
        }
        return null;
    }

    protected final void renderRegistryTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        renderAnalystTabTooltip(graphics, mouseX, mouseY);
        for (int i = 0; i < screenFamilyOrder().size(); i++) {
            if (inside(mouseX - leftPos, mouseY - topPos, 16 + i * 22, 28, 20, 20)) {
                graphics.renderTooltip(font, safeDisplay(familyType(i).getDisplayName()), mouseX, mouseY);
                return;
            }
        }
        List<ResolvedSpecies> species = visibleSpecies();
        int width = KnowledgeUiContract.ANALYST_PAGE_WIDTH - 10;
        int columns = Math.max(1, (width - 6) / 18);
        int listX = KnowledgeUiContract.ANALYST_LEFT_PAGE_X + 8;
        int listY = KnowledgeUiContract.ANALYST_PAGE_Y + 45;
        for (int index = registrySpeciesScroll; index < species.size(); index++) {
            int visible = index - registrySpeciesScroll;
            int col = visible % columns;
            int row = visible / columns;
            if (row >= 5) break;
            if (inside(mouseX - leftPos, mouseY - topPos, listX + col * 18, listY + row * 18, 16, 16)) {
                graphics.renderTooltip(font, species.get(index).species().getDisplayName(), mouseX, mouseY);
                return;
            }
        }
    }

    protected final void renderGeneTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;
        int systemHeight = KnowledgeUiContract.GENE_DATABASE_SYSTEM_HEIGHT / screenFamilyOrder().size();
        for (int i = 0; i < screenFamilyOrder().size(); i++) {
            if (!inside(localX, localY, KnowledgeUiContract.GENE_DATABASE_SYSTEM_X,
                    KnowledgeUiContract.GENE_DATABASE_SYSTEM_Y + i * systemHeight,
                    KnowledgeUiContract.GENE_DATABASE_SYSTEM_WIDTH, systemHeight)) continue;
            ResourceLocation family = screenFamilyOrder().get(i);
            int known = familyGeneCount(menu.knowledge().genes(), family);
            int total = completeFamilyGeneCount(family);
            graphics.renderComponentTooltip(font, List.of(
                    safeDisplay(familyType(i).getDisplayName()),
                    Component.translatable("screen.binniegenetics.database.gene_count", known, total)), mouseX, mouseY);
            return;
        }

        int geneTabHeight = KnowledgeUiContract.GENE_DATABASE_INFO_TABS_HEIGHT / 3;
        for (int i = 0; i < 3; i++) {
            if (inside(localX, localY, KnowledgeUiContract.GENE_DATABASE_INFO_TABS_X,
                    KnowledgeUiContract.GENE_DATABASE_INFO_TABS_Y + i * geneTabHeight,
                    KnowledgeUiContract.GENE_DATABASE_INFO_TABS_WIDTH, geneTabHeight)) {
                graphics.renderTooltip(font,
                        Component.translatable("screen.binniegenetics.database.tab."
                                + KnowledgeUiContract.geneDatabaseInfoTabs().get(i)), mouseX, mouseY);
                return;
            }
        }

        List<GeneCell> cells = geneCells();
        int ordinal = GeneBankGridLayout.hitTest(cells.stream().map(GeneCell::layout).toList(), localX, localY, geneScroll);
        if (ordinal < 0 || ordinal >= cells.size()) return;
        GeneCell cell = cells.get(ordinal);
        ArrayList<Component> tooltip = new ArrayList<>();
        tooltip.add(chromosomeDisplay(cell.gene()).copy().append(": ").append(geneDisplay(cell.gene())));
        if (canApplyGene(cell.gene())) {
            tooltip.add(Component.translatable("screen.binniegenetics.database.assign").withStyle(ChatFormatting.GRAY));
            GeneticKnowledgeEntry replaced = replacedGene(cell.gene());
            if (replaced != null) tooltip.add(Component.translatable("screen.binniegenetics.database.replace",
                    chromosomeDisplay(replaced), geneDisplay(replaced)).withStyle(ChatFormatting.YELLOW));
        }
        graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    protected final void renderArboristTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;
        List<ArboristEntry> entries = databaseEntries();
        int selectionWidth = databaseSelectionWidth();
        int rowHeight = databaseEntryRowHeight();
        int start = arboristScroll / rowHeight;
        int yOffset = -(arboristScroll % rowHeight);
        for (int index = start; index < entries.size(); index++) {
            int y = KnowledgeUiContract.DATABASE_LIST_Y + 3 + yOffset + (index - start) * rowHeight;
            if (y >= KnowledgeUiContract.DATABASE_LIST_Y + KnowledgeUiContract.DATABASE_LIST_HEIGHT) break;
            if (inside(mouseX - leftPos, mouseY - topPos, KnowledgeUiContract.DATABASE_LIST_X + 2, y - 1,
                    selectionWidth - 5, rowHeight - 1)) {
                graphics.renderTooltip(font, entries.get(index).name(), mouseX, mouseY);
                return;
            }
        }
        ArboristEntry selected = selectedArboristEntry(entries);
        if (arboristMode == ArboristMode.PLANKS && arboristInfoPage == 0 && selected != null) {
            List<ItemStack> uses = plankUses(selected.icon());
            int infoX = KnowledgeUiContract.DATABASE_INFO_X + 8;
            int infoY = KnowledgeUiContract.DATABASE_INFO_Y + 8;
            for (int i = 0; i < uses.size(); i++) {
                if (!inside(localX, localY, plankUseX(infoX, i), plankUseY(infoY, i), 16, 16)) continue;
                ArrayList<Component> tooltip = new ArrayList<>();
                tooltip.add(safeDisplay(uses.get(i).getHoverName()));
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
                    tooltip.add(Component.translatable("gui.binniecore.jei.show_recipes")
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
                return;
            }
        }
        List<ArboristMode> modes = availableDatabaseModes();
        int modeHeight = Math.min(28, KnowledgeUiContract.DATABASE_INFO_TABS_HEIGHT / Math.max(1, modes.size()));
        int modeX = KnowledgeUiContract.databaseModeTabsX(databaseFamily());
        for (int i = 0; i < modes.size(); i++) {
            if (inside(mouseX - leftPos, mouseY - topPos, modeX,
                    KnowledgeUiContract.DATABASE_INFO_TABS_Y + i * modeHeight,
                    22, modeHeight - 1)) {
                graphics.renderTooltip(font, Component.translatable(
                        "screen.binnietrees.arborist.mode." + modes.get(i).name().toLowerCase(Locale.ROOT)),
                        mouseX, mouseY);
                return;
            }
        }
        List<String> infoPages = currentDatabaseInfoPages();
        int infoHeight = Math.min(28,
                KnowledgeUiContract.DATABASE_INFO_TABS_HEIGHT / Math.max(1, infoPages.size()));
        for (int i = 0; i < infoPages.size(); i++) {
            if (!inside(localX, localY, KnowledgeUiContract.DATABASE_INFO_TABS_X,
                    KnowledgeUiContract.DATABASE_INFO_TABS_Y + i * infoHeight,
                    KnowledgeUiContract.DATABASE_INFO_TABS_WIDTH, infoHeight - 1)) continue;
            String page = infoPages.get(i);
            ArrayList<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable(databasePageTranslationKey(page)));
            if (Screen.hasShiftDown()) {
                String helpKey = databasePageTranslationKey(page) + ".help";
                Component help = Component.translatable(helpKey);
                if (!help.getString().equals(helpKey)) tooltip.add(help.copy().withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("screen.binniegenetics.database.hold_shift")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return;
        }
        List<ResolvedSpecies> relations = visibleInformationSpecies();
        int relationY = KnowledgeUiContract.DATABASE_INFO_Y + 23;
        for (int i = 0; i < Math.min(7, relations.size()); i++) {
            if (!inside(localX, localY, KnowledgeUiContract.DATABASE_INFO_X + 8,
                    relationY + 1 + i * 20, KnowledgeUiContract.DATABASE_INFO_WIDTH - 16, 20)) continue;
            ISpecies<?> species = relations.get(i).species();
            graphics.renderComponentTooltip(font, List.of(
                    species.getDisplayName(),
                    Component.literal(species.getGenusName() + " " + species.getSpeciesName())
                            .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (handleScreenClick(x, y, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected final boolean handleAnalystClick(double x, double y, int button) {
        if (button != 0) return false;
        boolean registry = screenKind == ScreenKind.REGISTRY;
        if (registry) {
            for (int i = 0; i < screenFamilyOrder().size(); i++) {
                if (inside(x, y, 16 + i * 22, 28, 20, 20)) {
                    familyIndex = i;
                    selectedSpeciesIndex = 0;
                    registrySpeciesScroll = 0;
                    leftPageIndex = 0;
                    rightPageIndex = 1;
                    if (search != null) search.setValue("");
                    normalizeSelection();
                    return true;
                }
            }
        }
        List<AnalystPage> pages = currentAnalystPages();
        int tabXStart = registry ? 16 + screenFamilyOrder().size() * 22 : 64;
        if (!pages.isEmpty() && y >= 28 && y < 48 && x >= tabXStart && x < imageWidth - 16) {
            int tabWidth = Math.max(1, (imageWidth - 16 - tabXStart) / pages.size());
            int clicked = Math.clamp((int) ((x - tabXStart) / tabWidth), 0, pages.size() - 1);
            if (registry) {
                if (clicked != 0) rightPageIndex = clicked;
                leftPageIndex = 0;
            } else {
                if (clicked == 0) clicked = 1;
                rightPageIndex = Math.min(clicked, pages.size() - 1);
                leftPageIndex = Math.max(0, rightPageIndex - 1);
            }
            leftScroll = rightScroll = 0;
            return true;
        }
        if (registry && inside(x, y, KnowledgeUiContract.ANALYST_LEFT_PAGE_X + 8,
                KnowledgeUiContract.ANALYST_PAGE_Y + 45,
                KnowledgeUiContract.ANALYST_PAGE_WIDTH - 16, 90)) {
            List<ResolvedSpecies> species = visibleSpecies();
            int columns = Math.max(1, (KnowledgeUiContract.ANALYST_PAGE_WIDTH - 16) / 18);
            int col = (int) ((x - (KnowledgeUiContract.ANALYST_LEFT_PAGE_X + 8)) / 18);
            int row = (int) ((y - (KnowledgeUiContract.ANALYST_PAGE_Y + 45)) / 18);
            int index = registrySpeciesScroll + row * columns + col;
            if (index >= 0 && index < species.size()) {
                selectedSpeciesIndex = index;
                rightPageIndex = Math.max(1, rightPageIndex);
                rightScroll = 0;
                return true;
            }
        }
        return false;
    }

    protected final boolean handleGeneClick(double x, double y, int button) {
        if (button != 0) return false;
        int geneTabHeight = KnowledgeUiContract.GENE_DATABASE_INFO_TABS_HEIGHT / 3;
        for (int i = 0; i < 3; i++) {
            if (inside(x, y, KnowledgeUiContract.GENE_DATABASE_INFO_TABS_X,
                    KnowledgeUiContract.GENE_DATABASE_INFO_TABS_Y + i * geneTabHeight,
                    KnowledgeUiContract.GENE_DATABASE_INFO_TABS_WIDTH, geneTabHeight)) {
                geneInfoTab = i;
                return true;
            }
        }
        for (int i = 0; i < screenFamilyOrder().size(); i++) {
            if (inside(x, y, KnowledgeUiContract.GENE_DATABASE_SYSTEM_X,
                    KnowledgeUiContract.GENE_DATABASE_SYSTEM_Y + i * 30, 24, 30)) {
                familyIndex = i;
                geneScroll = 0;
                if (search != null) search.setValue("");
                return true;
            }
        }
        List<GeneCell> cells = geneCells();
        int ordinal = GeneBankGridLayout.hitTest(cells.stream().map(GeneCell::layout).toList(), x, y, geneScroll);
        if (ordinal >= 0 && ordinal < cells.size()) {
            GeneCell cell = cells.get(ordinal);
            if (!canApplyGene(cell.gene())) return true;
            PacketDistributor.sendToServer(new SelectDatabaseGenePayload(menu.containerId, cell.index()));
            transientMessage = Component.translatable("screen.binniegenetics.database.assigned", geneDisplay(cell.gene()));
            transientMessageUntil = System.currentTimeMillis() + 2500L;
            return true;
        }
        return false;
    }

    protected final boolean handleArboristClick(double x, double y, int button) {
        if (button != 0) return false;
        List<ArboristMode> modes = availableDatabaseModes();
        int modeHeight = Math.min(28, KnowledgeUiContract.DATABASE_INFO_TABS_HEIGHT / Math.max(1, modes.size()));
        int modeX = KnowledgeUiContract.databaseModeTabsX(databaseFamily());
        for (int i = 0; i < modes.size(); i++) {
            if (inside(x, y, modeX,
                    KnowledgeUiContract.DATABASE_INFO_TABS_Y + i * modeHeight,
                    22, modeHeight - 1)) {
                arboristMode = modes.get(i);
                arboristInfoPage = 0;
                arboristSelection = 0;
                arboristScroll = 0;
                if (search != null) search.setValue("");
                normalizeSelection();
                return true;
            }
        }
        List<String> infoPages = currentDatabaseInfoPages();
        int infoHeight = Math.min(28, KnowledgeUiContract.DATABASE_INFO_TABS_HEIGHT / Math.max(1, infoPages.size()));
        for (int i = 0; i < infoPages.size(); i++) {
            if (inside(x, y, KnowledgeUiContract.DATABASE_INFO_TABS_X,
                    KnowledgeUiContract.DATABASE_INFO_TABS_Y + i * infoHeight,
                    KnowledgeUiContract.DATABASE_INFO_TABS_WIDTH, infoHeight - 1)) {
                arboristInfoPage = i;
                return true;
            }
        }
        List<ResolvedSpecies> relations = visibleInformationSpecies();
        int relationY = KnowledgeUiContract.DATABASE_INFO_Y + 23;
        if (inside(x, y, KnowledgeUiContract.DATABASE_INFO_X + 8, relationY,
                KnowledgeUiContract.DATABASE_INFO_WIDTH - 16, Math.min(7, relations.size()) * 20 + 2)) {
            int row = ((int) y - relationY - 1) / 20;
            if (row >= 0 && row < Math.min(7, relations.size())) {
                ResourceLocation selected = relations.get(row).species().id();
                arboristMode = ArboristMode.SPECIES;
                arboristInfoPage = 0;
                arboristScroll = 0;
                if (search != null) search.setValue("");
                List<ArboristEntry> speciesEntries = databaseEntries();
                for (int i = 0; i < speciesEntries.size(); i++) {
                    if (speciesEntries.get(i).key().equals(selected.toString())) {
                        arboristSelection = i;
                        break;
                    }
                }
                return true;
            }
        }
        if (inside(x, y, KnowledgeUiContract.DATABASE_LIST_X, KnowledgeUiContract.DATABASE_LIST_Y,
                databaseSelectionWidth(), KnowledgeUiContract.DATABASE_LIST_HEIGHT)) {
            int index = (arboristScroll + (int) y - KnowledgeUiContract.DATABASE_LIST_Y - 3)
                    / databaseEntryRowHeight();
            if (index >= 0 && index < databaseEntries().size()) {
                arboristSelection = index;
                return true;
            }
        }
        if (arboristMode == ArboristMode.PLANKS && arboristInfoPage == 0
                && net.neoforged.fml.ModList.get().isLoaded("jei")) {
            ArboristEntry selected = selectedArboristEntry(databaseEntries());
            if (selected != null) {
                int infoX = KnowledgeUiContract.DATABASE_INFO_X + 8;
                int infoY = KnowledgeUiContract.DATABASE_INFO_Y + 8;
                List<ItemStack> uses = plankUses(selected.icon());
                for (int i = 0; i < uses.size(); i++) {
                    if (inside(x, y, plankUseX(infoX, i), plankUseY(infoY, i), 16, 16)) {
                        return BinnieTreesJeiPlugin.showCraftingRecipe(uses.get(i));
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        int delta = (int) Math.signum(-scrollY) * 18;
        if (screenKind == ScreenKind.GENE_DATABASE
                && inside(x, y, KnowledgeUiContract.GENE_DATABASE_BOX_X, KnowledgeUiContract.GENE_DATABASE_BOX_Y,
                KnowledgeUiContract.GENE_DATABASE_BOX_WIDTH + KnowledgeUiContract.GENE_DATABASE_SCROLL_WIDTH,
                KnowledgeUiContract.GENE_DATABASE_BOX_HEIGHT)) {
            int geneDelta = (int) Math.signum(-scrollY) * 12;
            geneScroll = Math.clamp(geneScroll + geneDelta, 0,
                    Math.max(0, geneContentHeight() - GeneBankGridLayout.VIEWPORT_HEIGHT));
            return true;
        }
        if (screenKind == ScreenKind.SPECIES_DATABASE
                && inside(x, y, KnowledgeUiContract.DATABASE_LIST_X, KnowledgeUiContract.DATABASE_LIST_Y,
                databaseSelectionWidth(), KnowledgeUiContract.DATABASE_LIST_HEIGHT)) {
            arboristScroll = Math.clamp(arboristScroll + delta, 0,
                    Math.max(0, databaseEntries().size() * databaseEntryRowHeight()
                            - KnowledgeUiContract.DATABASE_LIST_HEIGHT + 6));
            return true;
        }
        if (inside(x, y, KnowledgeUiContract.ANALYST_LEFT_PAGE_X, KnowledgeUiContract.ANALYST_PAGE_Y,
                KnowledgeUiContract.ANALYST_PAGE_WIDTH, KnowledgeUiContract.ANALYST_PAGE_HEIGHT)) {
            if (screenKind == ScreenKind.REGISTRY
                    && leftPage(currentAnalystPages()) == AnalystPage.DATABASE) {
                int columns = Math.max(1, (KnowledgeUiContract.ANALYST_PAGE_WIDTH - 16) / 18);
                registrySpeciesScroll = Math.clamp(registrySpeciesScroll
                                + (int) Math.signum(-scrollY) * columns,
                        0, Math.max(0, visibleSpecies().size() - columns * 5));
            } else {
                AnalystPage page = leftPage(currentAnalystPages());
                leftScroll = genomePage(page)
                        ? Math.clamp(leftScroll + delta, 0, analystPageMaxScroll(page))
                        : Math.max(0, leftScroll + delta);
            }
            return true;
        }
        if (inside(x, y, KnowledgeUiContract.ANALYST_RIGHT_PAGE_X, KnowledgeUiContract.ANALYST_PAGE_Y,
                KnowledgeUiContract.ANALYST_PAGE_WIDTH, KnowledgeUiContract.ANALYST_PAGE_HEIGHT)) {
            AnalystPage page = rightPage(currentAnalystPages());
            rightScroll = genomePage(page)
                    ? Math.clamp(rightScroll + delta, 0, analystPageMaxScroll(page))
                    : Math.max(0, rightScroll + delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search != null && search.isFocused()) return super.keyPressed(keyCode, scanCode, modifiers);
        if (screenKind == ScreenKind.ANALYST
                || screenKind == ScreenKind.REGISTRY) {
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                shiftPages(true);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                shiftPages(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void shiftPages(boolean right) {
        List<AnalystPage> pages = currentAnalystPages();
        if (pages.size() < 2) return;
        boolean locked = screenKind == ScreenKind.REGISTRY;
        if (right) {
            if (rightPageIndex + 1 >= pages.size()) return;
            rightPageIndex++;
            leftPageIndex = locked ? 0 : rightPageIndex - 1;
        } else {
            if (locked) {
                if (rightPageIndex <= 1) return;
                rightPageIndex--;
                leftPageIndex = 0;
            } else {
                if (leftPageIndex <= 0) return;
                leftPageIndex--;
                rightPageIndex = leftPageIndex + 1;
            }
        }
        leftScroll = rightScroll = 0;
    }

    private List<AnalystPage> currentAnalystPages() {
        boolean registry = screenKind == ScreenKind.REGISTRY;
        ResourceLocation type = registry ? currentFamily() : currentAnalystType();
        if (type == null) return registry ? List.of(AnalystPage.DATABASE) : List.of();
        return KnowledgeUiContract.analystPages(type, registry);
    }

    @Nullable
    private ResourceLocation currentAnalystType() {
        ResolvedIndividual resolved = resolveAnalysedSpecimen();
        return resolved == null ? null : resolved.type().id();
    }

    protected final boolean hasAnalystInput() {
        return GeneticOrganismAdapters.supports(
                menu.analystSlots().getItem(GeneticKnowledgeMenu.ANALYST_INDIVIDUAL_SLOT));
    }

    /** True only after the server has consumed DNA Dye and written an analyzed individual. */
    protected final boolean hasAnalystSpecimen() {
        return resolveAnalysedSpecimen() != null;
    }

    @Nullable
    private ResolvedIndividual resolveAnalysedSpecimen() {
        ItemStack stack = menu.analystSlots().getItem(GeneticKnowledgeMenu.ANALYST_INDIVIDUAL_SLOT);
        if (!GeneticOrganismAdapters.supports(stack)) return null;
        try {
            IIndividual individual = GeneticOrganismAdapters.forStack(stack).read(stack);
            if (!individual.isAnalyzed()) return null;
            return new ResolvedIndividual(individual.getType(), individual.getSpecies(), individual.getGenome(), stack.copyWithCount(1));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private ResolvedIndividual currentResolvedIndividual() {
        if (screenKind == ScreenKind.ANALYST) {
            return resolveAnalysedSpecimen();
        }
        ResolvedSpecies selected = selectedRegistrySpecies();
        if (selected == null) return null;
        IIndividual individual = selected.species().createIndividual();
        return new ResolvedIndividual(selected.type(), selected.species(), individual.getGenome(), selected.icon());
    }

    @Nullable
    private AnalystPage leftPage(List<AnalystPage> pages) {
        return pages.isEmpty() ? null : pages.get(Math.clamp(leftPageIndex, 0, pages.size() - 1));
    }

    @Nullable
    private AnalystPage rightPage(List<AnalystPage> pages) {
        return pages.size() < 2 ? null : pages.get(Math.clamp(rightPageIndex, 1, pages.size() - 1));
    }

    private List<ResourceLocation> screenFamilyOrder() {
        return screenKind == ScreenKind.GENE_DATABASE
                ? KnowledgeUiContract.geneBankFamilyOrder()
                : KnowledgeUiContract.familyOrder();
    }

    private ResourceLocation currentFamily() {
        return screenFamilyOrder().get(Math.clamp(familyIndex, 0, screenFamilyOrder().size() - 1));
    }

    private ISpeciesType<?, ?> familyType(int index) {
        ResourceLocation id = screenFamilyOrder().get(Math.clamp(index, 0, screenFamilyOrder().size() - 1));
        return IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(id);
    }

    private ItemStack familyIcon(int index) {
        try {
            return familyType(index).createDefaultStack();
        } catch (RuntimeException exception) {
            return ItemStack.EMPTY;
        }
    }

    private int familyColor(int index) {
        return FAMILY_COLORS[Math.clamp(index, 0, FAMILY_COLORS.length - 1)];
    }

    private int pageColor(AnalystPage page) {
        return switch (page) {
            case DATABASE -> familyColor(familyIndex);
            case DESCRIPTION -> 0x333333;
            case ACTIVE_GENOME, INACTIVE_GENOME -> 0x006699;
            case KARYOGRAM -> 0x9933FF;
            case CLIMATE -> 0x6699CC;
            case PRODUCTS, FRUIT -> 0xCC9900;
            case BIOLOGY -> 0x339933;
            case BEHAVIOUR, GROWTH, SPECIMEN -> 0x996633;
            case WOOD -> 0x704214;
            case SOIL -> 0x76552B;
            case APPEARANCE -> 0xCC3377;
            case MUTATIONS -> 0xCC3333;
        };
    }

    private List<ResolvedSpecies> visibleSpecies() {
        String filter = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        ResourceLocation family = currentFamily();
        ISpeciesType<?, ?> type = IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypeSafe(family);
        if (type == null) return List.of();
        ArrayList<ResolvedSpecies> result = new ArrayList<>();
        if (menu.mode().exposesCompleteSpeciesCatalog(menu.interfaceType())) {
            // In 1.12, AnalystPageDatabase(master=true) used system.getAllSpecies(),
            // independently of the player's breeding tracker or the portable item's snapshot.
            for (ISpecies<?> species : type.getAllSpecies()) {
                if (!matchesSpeciesSearch(species, filter)) continue;
                result.add(new ResolvedSpecies(type, species, species.createStack(type.getDefaultStage())));
            }
        } else {
            for (GeneticSpeciesDiscovery discovery : menu.knowledge().species()) {
                if (!discovery.speciesType().equals(family)) continue;
                ISpecies<?> species = type.getSpeciesSafe(discovery.species());
                if (species == null || !matchesSpeciesSearch(species, filter)) continue;
                result.add(new ResolvedSpecies(type, species, species.createStack(type.getDefaultStage())));
            }
        }
        result.sort(Comparator.comparing(value -> value.species().getDisplayName().getString(), String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private static boolean matchesSpeciesSearch(ISpecies<?> species, String filter) {
        return filter.isEmpty()
                || species.getDisplayName().getString().toLowerCase(Locale.ROOT).contains(filter)
                || species.id().toString().toLowerCase(Locale.ROOT).contains(filter);
    }

    @Nullable
    private ResolvedSpecies selectedRegistrySpecies() {
        List<ResolvedSpecies> species = visibleSpecies();
        return species.isEmpty() ? null : species.get(Math.clamp(selectedSpeciesIndex, 0, species.size() - 1));
    }

    private List<GeneticKnowledgeEntry> visibleFamilyGenes(boolean filtered) {
        ResourceLocation family = currentFamily();
        String query = filtered && search != null ? search.getValue().toLowerCase(Locale.ROOT) : "";
        ArrayList<GeneticKnowledgeEntry> result = new ArrayList<>();
        for (GeneticKnowledgeEntry gene : menu.knowledge().genes()) {
            if (!gene.speciesType().equals(family)) continue;
            if (!query.isEmpty()) {
                String haystack = chromosomeDisplay(gene).getString() + " " + geneDisplay(gene).getString() + " " + gene.value();
                if (!haystack.toLowerCase(Locale.ROOT).contains(query)) continue;
            }
            result.add(gene);
        }
        Map<ResourceLocation, Integer> chromosomeOrder = chromosomeOrder(family);
        result.sort(Comparator.comparingInt((GeneticKnowledgeEntry gene) -> chromosomeOrder.getOrDefault(gene.chromosome(), Integer.MAX_VALUE))
                .thenComparing(gene -> geneDisplay(gene).getString(), String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private Map<ResourceLocation, Integer> chromosomeOrder(ResourceLocation family) {
        LinkedHashMap<ResourceLocation, Integer> result = new LinkedHashMap<>();
        try {
            List<IChromosome<?>> chromosomes = GeneticOrganismAdapters.get(family).chromosomes();
            for (int i = 0; i < chromosomes.size(); i++) result.put(chromosomes.get(i).id(), i);
        } catch (RuntimeException ignored) {}
        return result;
    }

    private List<GeneCell> geneCells() {
        List<GeneticKnowledgeEntry> genes = visibleFamilyGenes(true);
        List<GeneBankGridLayout.Cell> layout = GeneBankGridLayout.layout(
                genes.stream().map(GeneticKnowledgeEntry::chromosome).toList());
        ArrayList<GeneCell> cells = new ArrayList<>(genes.size());
        for (int ordinal = 0; ordinal < genes.size(); ordinal++) {
            GeneticKnowledgeEntry gene = genes.get(ordinal);
            int index = menu.knowledge().genes().indexOf(gene);
            cells.add(new GeneCell(index, gene, layout.get(ordinal)));
        }
        return List.copyOf(cells);
    }

    private int geneContentHeight() {
        return GeneBankGridLayout.contentHeight(geneCells().stream().map(GeneCell::layout).toList());
    }

    private static int familyGeneCount(List<GeneticKnowledgeEntry> genes, ResourceLocation family) {
        return (int) genes.stream().filter(gene -> gene.speciesType().equals(family)).count();
    }

    private int completeFamilyGeneCount(ResourceLocation family) {
        return familyGeneCount(GeneticKnowledgeCatalog.masterGeneDatabase().genes(), family);
    }


    private Component chromosomeDisplay(GeneticKnowledgeEntry gene) {
        try {
            IChromosome<?> chromosome = GeneticOrganismAdapters.get(gene.speciesType()).speciesType().getKaryotype().getChromosome(gene.chromosome());
            if (chromosome != null) {
                Component translated = GeneticsUtil.getChromosomeName(chromosome);
                return isRawTranslationKey(translated)
                        ? Component.literal(prettyId(chromosome.id()))
                        : translated;
            }
        } catch (RuntimeException ignored) {}
        return Component.literal(prettyId(gene.chromosome()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Component geneDisplay(GeneticKnowledgeEntry gene) {
        try {
            IChromosome chromosome = GeneticOrganismAdapters.get(gene.speciesType()).speciesType().getKaryotype().getChromosome(gene.chromosome());
            if (chromosome != null) {
                Object value = gene.value().javaValue();
                Component translated = GeneticsUtil.getName(chromosome, value);
                return isRawTranslationKey(translated) ? alleleFallback(chromosome, value) : translated;
            }
        } catch (RuntimeException ignored) {}
        return alleleFallback(gene.chromosome(), gene.value().javaValue());
    }

    private boolean canApplyGene(GeneticKnowledgeEntry gene) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty() || carried.getCount() != 1 || !(carried.getItem() instanceof GeneticMaterialItem material)) return false;
        GeneticMaterialItem.Kind kind;
        try { kind = material.kind(carried); } catch (RuntimeException exception) { return false; }
        if (kind != GeneticMaterialItem.Kind.SERUM && kind != GeneticMaterialItem.Kind.SERUM_ARRAY) return false;
        GeneticSerumData serum = carried.get(BinnieDataComponents.GENETIC_SERUM);
        if (serum != null && serum.charges() != 0) return false;
        return kind != GeneticMaterialItem.Kind.SERUM_ARRAY || serum == null || serum.alleles().isEmpty()
                || serum.alleles().getFirst().speciesType().equals(gene.speciesType());
    }

    @Nullable
    private GeneticKnowledgeEntry replacedGene(GeneticKnowledgeEntry selected) {
        GeneticSerumData serum = menu.getCarried().get(BinnieDataComponents.GENETIC_SERUM);
        if (serum == null) return null;
        for (GeneticAlleleData allele : serum.alleles()) {
            if (allele.chromosome().equals(selected.chromosome())) return GeneticKnowledgeEntry.fromAllele(allele);
        }
        return null;
    }






    protected final ResourceLocation databaseFamily() {
        if (fixedSpeciesType != null) return fixedSpeciesType;
        ResourceLocation filter = menu.speciesTypeFilter();
        return filter == null ? ForestrySpeciesTypes.TREE : filter;
    }

    private boolean arboristInterface() {
        return menu.interfaceType() == GeneticKnowledgeMenu.InterfaceType.ARBORIST_DATABASE;
    }

    private int databaseSelectionWidth() {
        return KnowledgeUiContract.databaseSelectionWidth(databaseFamily());
    }

    private List<ArboristMode> availableDatabaseModes() {
        return KnowledgeUiContract.databaseModes(databaseFamily(), arboristInterface());
    }

    private List<String> currentDatabaseInfoPages() {
        return KnowledgeUiContract.databaseInfoPages(databaseFamily(), arboristMode, arboristInterface());
    }

    private int databaseAccentColor() {
        ResourceLocation family = databaseFamily();
        if (ForestrySpeciesTypes.BEE.equals(family)) return 0xFFD69B;
        if (ForestrySpeciesTypes.BUTTERFLY.equals(family)) return 0xA993C6;
        if (FlowerSpeciesTypes.FLOWER.equals(family)) return 0xD69AB9;
        return 0x92B789;
    }

    private String databasePageTranslationKey(String page) {
        return "screen.binnietrees.arborist.page." + page;
    }

    private List<ResolvedSpecies> databaseSpecies() {
        ISpeciesType<?, ?> type = IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypeSafe(databaseFamily());
        if (type == null) return List.of();

        ArrayList<ResolvedSpecies> result = new ArrayList<>();
        if (menu.mode().master()) {
            for (ISpecies<?> species : type.getAllSpecies()) {
                result.add(new ResolvedSpecies(type, species, species.createStack(type.getDefaultStage())));
            }
        } else {
            for (GeneticSpeciesDiscovery discovery : menu.knowledge().species()) {
                if (!discovery.speciesType().equals(databaseFamily())) continue;
                ISpecies<?> species = type.getSpeciesSafe(discovery.species());
                if (species != null) {
                    result.add(new ResolvedSpecies(type, species, species.createStack(type.getDefaultStage())));
                }
            }
        }
        result.sort(Comparator.comparing(value -> value.species().getDisplayName().getString(),
                String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private List<ArboristEntry> databaseEntries() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        ArrayList<ArboristEntry> result = new ArrayList<>();
        switch (arboristMode) {
            case SPECIES -> {
                for (ResolvedSpecies species : databaseSpecies()) {
                    result.add(new ArboristEntry(species.species().id().toString(),
                            species.species().getDisplayName(), species.icon(), species.species()));
                }
            }
            case BRANCHES -> {
                LinkedHashMap<String, ResolvedSpecies> branches = new LinkedHashMap<>();
                for (ResolvedSpecies species : databaseSpecies()) {
                    branches.putIfAbsent(species.species().getGenusName(), species);
                }
                for (Map.Entry<String, ResolvedSpecies> branch : branches.entrySet()) {
                    result.add(new ArboristEntry(branch.getKey(), Component.literal(prettyWords(branch.getKey())),
                            branch.getValue().icon(), null));
                }
            }
            case BREEDER -> result.add(new ArboristEntry("progress",
                    Component.translatable("screen.binnietrees.arborist.breeder"), ItemStack.EMPTY, null));
            case FRUIT -> {
                LinkedHashMap<String, ItemStack> items = new LinkedHashMap<>();
        // The 1.12 Arborist database populated this selector from getAllFruits();
        // discovery only filters the species shown on the Natural/Potential pages.
                for (ResolvedSpecies species : allDatabaseSpecies()) {
                    if (!(species.species() instanceof IProductProducer producer)) continue;
                    for (IProduct product : producer.getProducts()) {
                        ItemStack stack = product.createStack();
                        if (!stack.isEmpty()) {
                            items.putIfAbsent(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack);
                        }
                    }
                }
                for (Map.Entry<String, ItemStack> item : items.entrySet()) {
                    result.add(new ArboristEntry(item.getKey(), item.getValue().getHoverName(), item.getValue(), null));
                }
            }
            case WOOD -> {
                LinkedHashMap<Item, ItemStack> logs = new LinkedHashMap<>();
                for (ResolvedSpecies species : allDatabaseSpecies()) {
                    ItemStack stack = treeLogForSpecies(species.species());
                    if (!stack.isEmpty()) logs.putIfAbsent(stack.getItem(), stack);
                }
                for (ItemStack stack : logs.values()) {
                    result.add(new ArboristEntry(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                            safeDisplay(stack.getHoverName()), stack, null));
                }
            }
            case PLANKS -> {
                LinkedHashMap<Item, ItemStack> planks = new LinkedHashMap<>();
                for (ResolvedSpecies species : allDatabaseSpecies()) {
                    ItemStack stack = BinnieTreesMachineProcesses.lumbermillOutput(
                            treeLogForSpecies(species.species()));
                    if (!stack.isEmpty()) planks.putIfAbsent(stack.getItem(), stack.copyWithCount(1));
                }
                for (ItemStack stack : planks.values()) {
                    result.add(new ArboristEntry(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                            safeDisplay(stack.getHoverName()), stack, null));
                }
            }
            case COLOR -> {
                for (LegacyFlowerCatalog.ColorDef color : LegacyFlowerCatalog.INSTANCE.colors()) {
                    result.add(new ArboristEntry(color.id().toString(),
                            Component.literal(prettyId(color.id())), ItemStack.EMPTY, null));
                }
            }
        }
        if (!query.isEmpty()) {
            result.removeIf(entry -> !entry.name().getString().toLowerCase(Locale.ROOT).contains(query)
                    && !entry.key().toLowerCase(Locale.ROOT).contains(query));
        }
        result.sort(Comparator.comparing(entry -> entry.name().getString(), String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private List<ResolvedSpecies> allDatabaseSpecies() {
        ISpeciesType<?, ?> type = IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypeSafe(databaseFamily());
        if (type == null) return List.of();
        ArrayList<ResolvedSpecies> result = new ArrayList<>();
        for (ISpecies<?> species : type.getAllSpecies()) {
            result.add(new ResolvedSpecies(type, species, species.createStack(type.getDefaultStage())));
        }
        result.sort(Comparator.comparing(value -> value.species().getDisplayName().getString(),
                String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    @Nullable
    private LegacyFlowerCatalog.ColorDef flowerColorDef(String key) {
        ResourceLocation id;
        try {
            id = ResourceLocation.parse(key);
        } catch (RuntimeException exception) {
            return null;
        }
        return LegacyFlowerCatalog.INSTANCE.colors().stream()
                .filter(color -> color.id().equals(id))
                .findFirst().orElse(null);
    }

    private int flowerRgb(String key) {
        LegacyFlowerCatalog.ColorDef color = flowerColorDef(key);
        return color == null ? 0xFFFFFF : color.rgb();
    }

    private void renderFlowerColorInfo(GuiGraphics graphics, ArboristEntry entry, String page,
                                       int x, int y, int width) {
        LegacyFlowerCatalog.ColorDef selected = flowerColorDef(entry.key());
        if (selected == null) return;
        graphics.fill(x + width / 2 - 12, y, x + width / 2 + 12, y + 24, 0xFF000000 | selected.rgb());
        drawCenteredNoShadow(graphics, entry.name(), x + width / 2, y + 30, 0xDDDDDD);

        List<LegacyFlowerCatalog.ColorMixDef> mixes = LegacyFlowerCatalog.INSTANCE.colorMixes().stream()
                .filter(mix -> "resultant".equals(page)
                        ? mix.result().equals(selected.id())
                        : mix.first().equals(selected.id()) || mix.second().equals(selected.id()))
                .toList();
        int lineY = y + 47;
        for (int i = 0; i < Math.min(7, mixes.size()); i++) {
            LegacyFlowerCatalog.ColorMixDef mix = mixes.get(i);
            Component line = "resultant".equals(page)
                    ? Component.literal(prettyId(mix.first()) + " + " + prettyId(mix.second()))
                    : Component.literal(prettyId(mix.result()) + " (" + mix.chance() + "%)");
            graphics.drawString(font, truncate(line, width), x, lineY + i * 12, 0xDDDDDD, false);
        }
        if (mixes.isEmpty()) {
            drawCenteredWrapped(graphics, Component.translatable("screen.binniegenetics.analyst.no_mutations"),
                    x, y + 65, width, 0xAAAAAA);
        }
    }

    private void drawCenteredNoShadow(GuiGraphics graphics, String text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private void drawCenteredNoShadow(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        net.minecraft.util.FormattedCharSequence visual = text.getVisualOrderText();
        drawCenteredNoShadow(graphics, visual, centerX, y, color);
    }

    private void drawScaledString(GuiGraphics graphics, Component text, int x, int y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void drawScaledString(GuiGraphics graphics, net.minecraft.util.FormattedCharSequence text,
                                  int x, int y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private int analystPageContentHeight(@Nullable AnalystPage page) {
        if (page != AnalystPage.ACTIVE_GENOME && page != AnalystPage.INACTIVE_GENOME) return 160;
        ResolvedIndividual resolved = currentResolvedIndividual();
        if (resolved == null) return KnowledgeUiContract.ANALYST_PAGE_HEIGHT - 10;
        boolean active = page == AnalystPage.ACTIVE_GENOME;
        float scale = 0.78F;
        int halfWidth = (KnowledgeUiContract.ANALYST_PAGE_WIDTH - 10) / 2 - 5;
        int lineHeight = Math.max(7, Math.round(font.lineHeight * scale));
        int height = 26;
        for (IChromosome<?> chromosome : resolved.genome().getKaryotype().getChromosomes()) {
            AllelePair<?> pair = resolved.genome().getAllelePair(chromosome);
            Object value = active ? pair.active().value() : pair.inactive().value();
            int labelLines = font.split(GeneticsUtil.getChromosomeName(chromosome),
                    Math.max(1, Math.round(halfWidth / scale))).size();
            int valueLines = font.split(alleleName(chromosome, value),
                    Math.max(1, Math.round(halfWidth / scale))).size();
            height += Math.max(labelLines, valueLines) * lineHeight + 3;
        }
        return Math.max(KnowledgeUiContract.ANALYST_PAGE_HEIGHT - 10, height);
    }

    private int analystPageMaxScroll(@Nullable AnalystPage page) {
        return Math.max(0, analystPageContentHeight(page) - KnowledgeUiContract.ANALYST_PAGE_HEIGHT + 8);
    }

    private static boolean genomePage(@Nullable AnalystPage page) {
        return page == AnalystPage.ACTIVE_GENOME || page == AnalystPage.INACTIVE_GENOME;
    }

    private static Component safeDisplay(Component component) {
        String value = component.getString();
        if (!value.isBlank() && value.indexOf(' ') < 0 && (value.contains(".") || value.contains(":"))) {
            int separator = Math.max(value.lastIndexOf('.'), value.lastIndexOf(':'));
            return Component.literal(prettyWords(value.substring(separator + 1)));
        }
        return component;
    }

    private static boolean isRawTranslationKey(Component component) {
        String value = component.getString();
        return value.startsWith("allele.") || value.startsWith("chromosome.")
                || value.startsWith("species.") || value.startsWith("mutation.");
    }

    private static Component alleleFallback(IChromosome<?> chromosome, Object value) {
        return alleleFallback(chromosome.id(), value);
    }

    private static Component alleleFallback(ResourceLocation chromosome, Object value) {
        if (value instanceof ResourceLocation id) return Component.literal(prettyId(id));
        if (value instanceof Vec3i vector) {
            return Component.literal(vector.getX() + " × " + vector.getY() + " × " + vector.getZ());
        }
        if (value instanceof Float number && "sappiness".equals(chromosome.getPath())) {
            return Component.literal(Math.round(number * 100.0F) + "%");
        }
        if (value instanceof Enum<?> enumeration) return Component.literal(prettyWords(enumeration.name()));
        return Component.literal(String.valueOf(value));
    }

    private void drawCenteredNoShadow(GuiGraphics graphics,
                                      net.minecraft.util.FormattedCharSequence text,
                                      int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private int drawCenteredWrapped(GuiGraphics graphics, Component text,
                                    int x, int y, int width, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, Math.max(1, width));
        int lineY = y;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            drawCenteredNoShadow(graphics, line, x + width / 2, lineY, color);
            lineY += font.lineHeight + 1;
        }
        return lines.size() * (font.lineHeight + 1);
    }

    /**
      * Forestry exposes a description key for every species, including species that never had a
      * description entry in either Forestry or Binnie. Rendering that component directly exposes
      * the raw key. The old database instead left those entries without descriptive prose.
      */
    private Component speciesDescription(ISpecies<?> species) {
        String key = species.getDescriptionTranslationKey();
        if (key == null || key.isBlank() || !I18n.exists(key)) {
            return Component.translatable("screen.binniegenetics.knowledge.no_description");
        }
        String description = I18n.get(key);
        int attributionSeparator = description.indexOf('|');
        if (attributionSeparator >= 0) {
            description = description.substring(0, attributionSeparator);
        }
        if (description.isBlank()) {
            return Component.translatable("screen.binniegenetics.knowledge.no_description");
        }
        return Component.literal(description);
    }

    @Nullable
    private ArboristEntry selectedArboristEntry(List<ArboristEntry> entries) {
        return entries.isEmpty() ? null : entries.get(Math.clamp(arboristSelection, 0, entries.size() - 1));
    }

    private boolean relates(ArboristEntry entry, ISpecies<?> species) {
        if (arboristMode == ArboristMode.FRUIT && species instanceof IProductProducer producer) {
            return producer.getProducts().stream().map(IProduct::createStack).anyMatch(stack -> !stack.isEmpty() && stack.getItem() == entry.icon().getItem());
        }
        if (arboristMode == ArboristMode.WOOD || arboristMode == ArboristMode.PLANKS) {
            ItemStack log = treeLogForSpecies(species);
            if (!log.isEmpty()) {
                ItemStack product = arboristMode == ArboristMode.PLANKS
                        ? BinnieTreesMachineProcesses.lumbermillOutput(log) : log;
                return !product.isEmpty() && product.is(entry.icon().getItem());
            }
            // Forestry no longer exposes the 1.12 species wood-provider relation. Retain a
            // conservative fallback for external tree species whose registered ID embeds its wood identity.
            String speciesPath = species.id().getPath().replace('_', ' ');
            String itemPath = entry.key().substring(entry.key().indexOf(':') + 1).replace('_', ' ');
            for (String token : speciesPath.split(" ")) if (token.length() > 3 && itemPath.contains(token)) return true;
        }
        return false;
    }

    private ItemStack treeLogForSpecies(ISpecies<?> species) {
        String path = species.id().getPath();
        for (LegacyTreeCatalog.TreeBinding tree : LEGACY_TREE_CATALOG.species()) {
            if (!tree.id().equals(path)) continue;
            BinnieTreeWoodType binnieTreesWood = BinnieTreeWoodType.fromLegacyExpression(tree.wood());
            if (binnieTreesWood != null) return new ItemStack(BinnieTreeWoodContent.get(binnieTreesWood).logItem().get());
            int separator = tree.wood().lastIndexOf('.');
            String wood = (separator < 0 ? tree.wood() : tree.wood().substring(separator + 1))
                    .toLowerCase(Locale.ROOT);
            Item forestryLog = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                    "forestry", wood + "_log"));
            return forestryLog == Items.AIR ? ItemStack.EMPTY : new ItemStack(forestryLog);
        }
        String speciesPath = path.toLowerCase(Locale.ROOT);
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.LOGS)) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(holder.value());
            String itemPath = id.getPath();
            String stem = itemPath.endsWith("_log") ? itemPath.substring(0, itemPath.length() - 4)
                    : itemPath.endsWith("_wood") ? itemPath.substring(0, itemPath.length() - 5) : itemPath;
            if (stem.length() > 3 && speciesPath.contains(stem)) return new ItemStack(holder.value());
        }
        return ItemStack.EMPTY;
    }

    private List<ResolvedSpecies> visibleInformationSpecies() {
        ArboristEntry entry = selectedArboristEntry(databaseEntries());
        if (entry == null) return List.of();
        String page = currentDatabaseInfoPages().get(Math.clamp(arboristInfoPage, 0,
                currentDatabaseInfoPages().size() - 1));
        if (arboristMode == ArboristMode.BRANCHES && "species".equals(page)) return branchMembers(entry);
        if ((arboristMode == ArboristMode.FRUIT || arboristMode == ArboristMode.WOOD
                || arboristMode == ArboristMode.PLANKS) && !"overview".equals(page)) {
            return relatedSpecies(entry);
        }
        return List.of();
    }

    private void normalizeSelection() {
        familyIndex = Math.clamp(familyIndex, 0, screenFamilyOrder().size() - 1);
        selectedSpeciesIndex = Math.clamp(selectedSpeciesIndex, 0, Math.max(0, visibleSpecies().size() - 1));
        List<AnalystPage> pages = currentAnalystPages();
        if (!pages.isEmpty()) {
            leftPageIndex = Math.clamp(leftPageIndex, 0, pages.size() - 1);
            rightPageIndex = pages.size() < 2 ? 0 : Math.clamp(rightPageIndex, 1, pages.size() - 1);
        }
        List<ArboristMode> modes = availableDatabaseModes();
        if (!modes.contains(arboristMode)) arboristMode = modes.getFirst();
        arboristSelection = Math.clamp(arboristSelection, 0, Math.max(0, databaseEntries().size() - 1));
        arboristInfoPage = Math.clamp(arboristInfoPage, 0, Math.max(0, currentDatabaseInfoPages().size() - 1));
    }


    protected final boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String prettyId(ResourceLocation id) {
        return prettyWords(id.getPath());
    }

    private static String prettyValue(Object value) {
        if (value instanceof ResourceLocation id) return prettyId(id);
        return prettyWords(String.valueOf(value));
    }

    private static String prettyWords(String value) {
        String[] parts = value.replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private Component truncate(Component component, int width) {
        if (font.width(component) <= width) return component;
        return Component.literal(font.plainSubstrByWidth(component.getString(), Math.max(1, width - font.width("…"))) + "…");
    }

    private record ResolvedSpecies(ISpeciesType<?, ?> type, ISpecies<?> species, ItemStack icon) {}
    private record ResolvedIndividual(ISpeciesType<?, ?> type, ISpecies<?> species, IGenome genome, ItemStack icon) {}
    private record GeneCell(int index, GeneticKnowledgeEntry gene, GeneBankGridLayout.Cell layout) {}
    private record ArboristEntry(String key, Component name, ItemStack icon, @Nullable ISpecies<?> species) {}
    private record MutationHit(IMutation<?> mutation, boolean resultant) {}
}
