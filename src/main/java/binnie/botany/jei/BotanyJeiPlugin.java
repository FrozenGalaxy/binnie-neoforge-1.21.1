package binnie.botany.jei;

import binnie.botany.BinnieBotany;
import binnie.botany.machine.BotanyMachineProcesses;
import binnie.botany.registry.BotanyContent;
import binnie.botany.registry.BotanyMachines;
import binnie.botany.registry.BotanyWorldContent;
import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.api.genetics.IFlower;
import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.genetics.FlowerSpeciesType;
import binnie.botany.genetics.LegacyFlowerCatalog;
import binnie.botany.item.BotanyPigments;
import binnie.botany.recipe.FlowerPigmentRecipe;
import binnie.design.client.screen.TileworkerScreen;
import binnie.design.data.DesignCatalog;
import binnie.design.data.DesignerMaterialTags;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import java.util.*;

@JeiPlugin
public final class BotanyJeiPlugin implements IModPlugin {
    public static final RecipeType<TileworkerJeiRecipe> TILEWORKER = RecipeType.create(BinnieBotany.MOD_ID, "tileworker", TileworkerJeiRecipe.class);
    public static final RecipeType<GardeningJeiRecipe> GARDENING = RecipeType.create(BinnieBotany.MOD_ID, "gardening", GardeningJeiRecipe.class);
    public static final ClickArea CLICK_AREA = ClickArea.fixed(92,50,18,18);
    public static final ClickArea TITLE_CLICK_AREA = ClickArea.title(320);
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, "jei"); }
    @Override public void registerItemSubtypes(ISubtypeRegistration r) {
        ISubtypeInterpreter<ItemStack> components = new ISubtypeInterpreter<>() {
            @Override public Object getSubtypeData(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return stack.getComponentsPatch();
            }
            @Override @SuppressWarnings("deprecation")
            public String getLegacyStringSubtypeInfo(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return stack.getComponentsPatch().toString();
            }
        };
        r.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyContent.PIGMENT.get(), components);
        r.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyContent.CLAY.get(), components);
        r.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyContent.INSULATED_TUBE.get(), components);
        r.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyWorldContent.CERAMIC_ITEM.get(), components);
        r.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyWorldContent.STAINED_GLASS_ITEM.get(), components);
        r.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyWorldContent.CERAMIC_BRICK_ITEM.get(), components);
        r.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyWorldContent.CERAMIC_TILE_ITEM.get(), components);
        r.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyWorldContent.SOIL_ITEM.get(), components);
        r.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyWorldContent.LOAM_ITEM.get(), components);
        r.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyWorldContent.FLOWERBED_ITEM.get(), components);
    }
    @Override public void registerCategories(IRecipeCategoryRegistration r) {
        var gui = r.getJeiHelpers().getGuiHelper();
        r.addRecipeCategories(new TileworkerJeiCategory(TILEWORKER, gui));
        r.addRecipeCategories(new GardeningJeiCategory(GARDENING, gui));
    }
    @Override public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration r) {
        r.getCraftingCategory().addExtension(FlowerPigmentRecipe.class, new FlowerPigmentCraftingExtension());
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r) { r.addRecipeCatalyst(BotanyMachines.REGISTRY.block("tileworker").get(), TILEWORKER); }
    @Override public void registerRecipes(IRecipeRegistration r) {
        List<ItemStack> materials = binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color").stream()
                .map(BotanyWorldContent.CERAMIC_ITEM.get()::createStack).toList();
        ArrayList<ItemStack> outputs = new ArrayList<>(DesignCatalog.SELECTABLE_DESIGNS.size());
        for (int index = 0; index < DesignCatalog.SELECTABLE_DESIGNS.size(); index++) {
            ItemStack first = materials.get(index % materials.size());
            ItemStack second = materials.get((index * 37 + 1) % materials.size());
            ItemStack output = BotanyMachineProcesses.tileworkerOutput(first, second, DesignCatalog.SELECTABLE_DESIGNS.get(index));
            if (!output.isEmpty()) outputs.add(output);
        }
        Ingredient material = Ingredient.of(DesignerMaterialTags.CERAMIC);
        r.addRecipes(TILEWORKER, List.of(new TileworkerJeiRecipe(
                ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, "jei/tileworker/operation"),
                material, material, materials, new ItemStack(BotanyContent.MORTAR.get()), outputs)));
        r.addRecipes(GARDENING, gardeningRecipes());
    }

    static List<GardeningJeiRecipe> gardeningRecipes() {
        ArrayList<GardeningJeiRecipe> recipes = new ArrayList<>();
        List<ItemStack> trowels = List.of(BotanyContent.TROWEL_WOOD, BotanyContent.TROWEL_STONE,
                BotanyContent.TROWEL_IRON, BotanyContent.TROWEL_GOLD, BotanyContent.TROWEL_DIAMOND)
                .stream().map(item -> new ItemStack(item.get())).toList();
        List<ItemStack> dirt = List.of(new ItemStack(Items.DIRT), new ItemStack(Items.GRASS_BLOCK),
                new ItemStack(Items.DIRT_PATH), new ItemStack(Items.COARSE_DIRT), new ItemStack(Items.ROOTED_DIRT));
        ArrayList<ItemStack> acquiredSoils = new ArrayList<>();
        for (SoilMoisture moisture : SoilMoisture.values()) for (SoilAcidity acidity : SoilAcidity.values())
            acquiredSoils.add(soil("soil", moisture, acidity, false));
        recipes.add(guide("acquire", List.of(trowels, dirt), acquiredSoils,
                "jei.binniebotany.gardening.acquire"));
        for (String family : List.of("soil", "loam", "flowerbed")) {
            for (SoilMoisture moisture : SoilMoisture.values()) for (SoilAcidity acidity : SoilAcidity.values()) {
                ItemStack plain = soil(family, moisture, acidity, false);
                ItemStack treated = soil(family, moisture, acidity, true);
                recipes.add(guide("weedkiller/" + family + '/' + acidity.getSerializedName() + '/' + moisture.getSerializedName(),
                        List.of(List.of(withCount(plain, 4)), List.of(new ItemStack(BotanyContent.WEEDKILLER.get()))),
                        List.of(withCount(treated, 4)), "jei.binniebotany.gardening.weedkiller"));
                addAmendments(recipes, family, moisture, acidity, false);
                addAmendments(recipes, family, moisture, acidity, true);
            }
        }
        return List.copyOf(recipes);
    }

    private static void addAmendments(List<GardeningJeiRecipe> recipes, String family, SoilMoisture moisture,
                                      SoilAcidity acidity, boolean treated) {
        ItemStack input = soil(family, moisture, acidity, treated);
        String suffix = family + '/' + acidity.getSerializedName() + '/' + moisture.getSerializedName() + '/' + treated;
        if (acidity != SoilAcidity.ACID) {
            SoilAcidity target = SoilAcidity.values()[acidity.ordinal() - 1];
            addAmendment(recipes, "mulch/" + suffix, input, item("forestry:mulch"), soil(family, moisture, target, treated), "acid");
        }
        if (acidity != SoilAcidity.ALKALINE) {
            SoilAcidity target = SoilAcidity.values()[acidity.ordinal() + 1];
            addAmendment(recipes, "ash/" + suffix, input, item("forestry:ash"), soil(family, moisture, target, treated), "alkaline");
            addAmendment(recipes, "wood_pulp/" + suffix, input, item("forestry:wood_pulp"), soil(family, moisture, target, treated), "alkaline");
        }
        if (!family.equals("flowerbed")) {
            String target = family.equals("soil") ? "loam" : "flowerbed";
            addAmendment(recipes, "fertilizer/" + suffix, input, item("forestry:fertilizer"),
                    soil(target, moisture, acidity, treated), "nutrient");
        }
    }

    private static void addAmendment(List<GardeningJeiRecipe> recipes, String id, ItemStack soil,
                                     ItemStack reagent, ItemStack output, String kind) {
        recipes.add(guide(id, List.of(List.of(withCount(soil, 4)), List.of(reagent)),
                List.of(withCount(output, 4)), "jei.binniebotany.gardening." + kind));
    }

    private static GardeningJeiRecipe guide(String id, List<List<ItemStack>> inputs, List<ItemStack> outputs, String text) {
        return new GardeningJeiRecipe(ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, "jei/gardening/" + id),
                inputs, outputs, net.minecraft.network.chat.Component.translatable(text));
    }

    private static ItemStack soil(String family, SoilMoisture moisture, SoilAcidity acidity, boolean treated) {
        var item = switch (family) {
            case "loam" -> BotanyWorldContent.LOAM_ITEM.get();
            case "flowerbed" -> BotanyWorldContent.FLOWERBED_ITEM.get();
            default -> BotanyWorldContent.SOIL_ITEM.get();
        };
        int fertility = family.equals("soil") ? 1 : family.equals("loam") ? 2 : 3;
        return BotanyWorldContent.soilStack(item, moisture, acidity, fertility, treated);
    }

    private static ItemStack item(String id) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))); }
    private static ItemStack withCount(ItemStack stack, int count) { ItemStack copy = stack.copy(); copy.setCount(count); return copy; }

    static List<PigmentPair> pigmentPairs() {
        var type = FlowerSpeciesType.get();
        var species = type.getDefaultSpecies();
        ArrayList<PigmentPair> recipes = new ArrayList<>(LegacyFlowerCatalog.INSTANCE.colors().size());
        for (var color : LegacyFlowerCatalog.INSTANCE.colors()) {
            var pair = AllelePair.both(new Allele<>(color.id(), color.dominant()));
            var genome = species.getDefaultGenome().copyWithPairs(Map.of(FlowerChromosomes.PRIMARY_COLOR, pair));
            IFlower flower = species.createIndividual(genome);
            flower.setAge(1);
            String colorId = color.id().getPath();
            recipes.add(new PigmentPair(flower.createStack(FlowerLifeStage.FLOWER),
                    BotanyPigments.stackFor(colorId, BotanyContent.PIGMENT.get())));
        }
        return List.copyOf(recipes);
    }
    record PigmentPair(ItemStack flower, ItemStack pigment) {
        PigmentPair {
            flower = flower.copy();
            pigment = pigment.copy();
        }
    }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration r) { r.addGuiContainerHandler(TileworkerScreen.class, new mezz.jei.api.gui.handlers.IGuiContainerHandler<>() {
        @Override public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(TileworkerScreen s) { return s.ledgerAreas(); }
        @Override public Collection<IGuiClickableArea> getGuiClickableAreas(TileworkerScreen s, double x, double y) {
            if (!s.getMenu().machineId().equals(ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID,"tileworker"))) return List.of();
            ClickArea title = TITLE_CLICK_AREA.resolve(net.minecraft.client.Minecraft.getInstance().font.width(s.getTitle()));
            return List.of(
                    IGuiClickableArea.createBasic(CLICK_AREA.x(),CLICK_AREA.y(),CLICK_AREA.width(),CLICK_AREA.height(),TILEWORKER),
                    IGuiClickableArea.createBasic(title.x(),title.y(),title.width(),title.height(),TILEWORKER));
        }}); }
    public record ClickArea(int x,int y,int width,int height,int titleGuiWidth) {
        public static ClickArea fixed(int x,int y,int width,int height) { return new ClickArea(x,y,width,height,0); }
        public static ClickArea title(int guiWidth) { return new ClickArea(0,8,0,12,guiWidth); }
        public boolean isTitle() { return titleGuiWidth > 0; }
        public ClickArea resolve(int titleWidth) {
            return isTitle() ? fixed((titleGuiWidth - titleWidth) / 2, y, Math.max(1, titleWidth), height) : this;
        }
    }
}
