package binnie.design.data;

import binnie.botany.genetics.LegacyFlowerCatalog;
import binnie.core.data.VariantData;
import binnie.core.registry.BinnieDataComponents;
import binnie.trees.wood.BinnieTreeWoodType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
  * Matching material palette used by the 1.12 carpentry systems.
  *
  * <p>Designs were coloured masks, not copies of arbitrary wood or glass block
  * textures. The palette therefore contains only materials that
  * {@code CarpentryInterface} and {@code GlassType} registered in the source
  * release. Later Minecraft wood families and arbitrary slabs are not implicit
  * designer inputs.</p>
  */
public final class DesignMaterialCatalog {
    private static final Map<ResourceLocation, Integer> WOOD_COLORS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Integer> GLASS_COLORS = new LinkedHashMap<>();

    static {
    // VanillaPlanks.java
        wood("minecraft", "oak_planks", 11833434);
        wood("minecraft", "spruce_planks", 8412726);
        wood("minecraft", "birch_planks", 14139781);
        wood("minecraft", "jungle_planks", 11632732);
        wood("minecraft", "acacia_planks", 12215095);
        wood("minecraft", "dark_oak_planks", 4599061);

    // Forestry's fireproof forms are the current representation returned by
    // the 1.12 material's getStack(true) path.
        wood("forestry", "oak_fireproof_planks", 11833434);
        wood("forestry", "spruce_fireproof_planks", 8412726);
        wood("forestry", "birch_fireproof_planks", 14139781);
        wood("forestry", "jungle_fireproof_planks", 11632732);
        wood("forestry", "acacia_fireproof_planks", 12215095);
        wood("forestry", "dark_oak_fireproof_planks", 4599061);

    // BinnieTreePlanks.java
        for (BinnieTreeWoodType type : BinnieTreeWoodType.values()) {
            wood("binnietrees", type.getSerializedName() + "_planks", type.legacyColor());
            wood("binnietrees", type.getSerializedName() + "_fireproof_planks", type.legacyColor());
        }

    // ForestryPlanks.java. ForestryCE renamed a small number of its legacy
    // tree IDs while retaining their species; each mapping remains tied to
    // the 1.12 material colour instead of a current map colour.
        forestryWood("larch", 14131085);
        forestryWood("teak", 8223075);
        forestryWood("camelthorn", 9745287); // legacy Forestry ACACIA
        forestryWood("lime", 13544048);
        forestryWood("chestnut", 12298845);
        forestryWood("wenge", 6182474);
        forestryWood("baobab", 9608290);
        forestryWood("sequoia", 10050135);
        forestryWood("kapok", 8156212);
        forestryWood("ebony", 3946288);
        forestryWood("mahogany", 7749432);
        forestryWood("balsa", 11117209);
        forestryWood("willow", 11710818);
        forestryWood("walnut", 6836802);
        forestryWood("greenheart", 5144156);
        forestryWood("sour_cherry", 11895348); // legacy Forestry CHERRY
        forestryWood("mahoe", 8362154);
        forestryWood("poplar", 13619074);
        forestryWood("palm", 13271115);
        forestryWood("papaya", 14470005);
        forestryWood("pine", 12885585);
        forestryWood("plum", 11364479);
        forestryWood("maple", 11431211);
        forestryWood("lemon", 10266653); // legacy Forestry CITRUS
        forestryWood("giant_sequoia", 5186590); // legacy Forestry GIGANTEUM
        forestryWood("ipe", 5057822);
        forestryWood("padauk", 11756341);
        forestryWood("cocobolo", 7541506);
        forestryWood("zebrano", 10912334); // legacy Forestry ZEBRAWOOD

    // GlassType.StandardColor, in the original metadata order.
        glass("white_stained_glass", 16777215);
        glass("orange_stained_glass", 14188339);
        glass("magenta_stained_glass", 11685080);
        glass("light_blue_stained_glass", 6724056);
        glass("yellow_stained_glass", 15066419);
        glass("lime_stained_glass", 8375321);
        glass("pink_stained_glass", 15892389);
        glass("gray_stained_glass", 5000268);
        glass("light_gray_stained_glass", 10066329);
        glass("cyan_stained_glass", 5013401);
        glass("purple_stained_glass", 8339378);
        glass("blue_stained_glass", 3361970);
        glass("brown_stained_glass", 6704179);
        glass("green_stained_glass", 6717235);
        glass("red_stained_glass", 10040115);
        glass("black_stained_glass", 1644825);
    }

    private DesignMaterialCatalog() {}

    public static boolean isWood(ItemStack stack) {
        return WOOD_COLORS.containsKey(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isGlass(ItemStack stack) {
        ResourceLocation item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return GLASS_COLORS.containsKey(item) || glassColor(item, variant(stack)).isPresent();
    }

    /** Concrete tag members JEI may cycle, in the 1.12 registration order. */
    public static List<ItemStack> woodMaterialStacks() {
        ArrayList<ItemStack> result = new ArrayList<>(WOOD_COLORS.size());
        for (ResourceLocation id : WOOD_COLORS.keySet()) {
            var item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) result.add(new ItemStack(item));
        }
        return List.copyOf(result);
    }

    /**
      * Concrete glass tag members, including all component-backed Botany
      * colours which cannot be represented as separate item IDs in a tag.
      */
    public static List<ItemStack> glassMaterialStacks() {
        ArrayList<ItemStack> result = new ArrayList<>(GLASS_COLORS.size() + LegacyFlowerCatalog.INSTANCE.colors().size());
        for (ResourceLocation id : GLASS_COLORS.keySet()) {
            var item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) result.add(new ItemStack(item));
        }
        var botanyGlass = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("binniebotany", "stained"));
        if (botanyGlass != Items.AIR) {
            for (var color : LegacyFlowerCatalog.INSTANCE.colors()) {
                ItemStack stack = new ItemStack(botanyGlass);
                stack.set(BinnieDataComponents.VARIANT,
                        new VariantData("binniebotany.flower_color", color.id().getPath()));
                result.add(stack);
            }
        }
        return List.copyOf(result);
    }

    public static OptionalInt woodColor(ResourceLocation item) {
        return color(WOOD_COLORS, item);
    }

    public static OptionalInt glassColor(ResourceLocation item) {
        return glassColor(item, "");
    }

    public static OptionalInt ceramicColor(String variant) {
        if (variant == null || variant.isBlank()) return OptionalInt.empty();
        return LegacyFlowerCatalog.INSTANCE.colors().stream()
                .filter(color -> color.id().getPath().equals(variant))
                .mapToInt(LegacyFlowerCatalog.ColorDef::rgb)
                .findFirst();
    }

    public static OptionalInt glassColor(ResourceLocation item, String variant) {
        OptionalInt standard = color(GLASS_COLORS, item);
        if (standard.isPresent()) return standard;
        if (!item.equals(ResourceLocation.fromNamespaceAndPath("binniebotany", "stained")) || variant.isBlank()) return OptionalInt.empty();
        return LegacyFlowerCatalog.INSTANCE.colors().stream()
                .filter(color -> color.id().getPath().equals(variant))
                .mapToInt(LegacyFlowerCatalog.ColorDef::rgb)
                .findFirst();
    }

    public static String variant(ItemStack stack) {
        VariantData data = stack.get(BinnieDataComponents.VARIANT);
        return data != null && "binniebotany.flower_color".equals(data.category()) ? data.value() : "";
    }

    public static int defaultWoodColor() {
        return BinnieTreeWoodType.FIR.legacyColor();
    }

    public static int defaultGlassColor() {
        return 16777215;
    }

    private static OptionalInt color(Map<ResourceLocation, Integer> colors, ResourceLocation item) {
        Integer color = colors.get(item);
        return color == null ? OptionalInt.empty() : OptionalInt.of(color);
    }

    private static void forestryWood(String id, int color) {
        wood("forestry", id + "_planks", color);
        wood("forestry", id + "_fireproof_planks", color);
    }

    private static void wood(String namespace, String path, int color) {
        WOOD_COLORS.put(ResourceLocation.fromNamespaceAndPath(namespace, path), color);
    }

    private static void glass(String path, int color) {
        GLASS_COLORS.put(ResourceLocation.withDefaultNamespace(path), color);
    }
}
