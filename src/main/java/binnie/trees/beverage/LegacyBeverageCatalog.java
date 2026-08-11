package binnie.trees.beverage;

import binnie.core.data.BeverageData;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Identity, color, and alcohol metadata for completed 1.12 beverage content. */
public final class LegacyBeverageCatalog {
    private static final Map<String, Definition> DEFINITIONS = create();

    private LegacyBeverageCatalog() {}

    public static Definition get(String category, String value) {
        Definition definition = DEFINITIONS.get(category + ":" + value);
        if (definition == null) throw new IllegalArgumentException("Unknown legacy beverage " + category + ":" + value);
        return definition;
    }

    public static Map<String, Definition> definitions() { return DEFINITIONS; }

    public static Definition find(String beverageId) {
        ResourceLocation id = ResourceLocation.tryParse(beverageId);
        if (id == null || !id.getNamespace().equals("binnietrees")) return null;
        String[] path = id.getPath().split("/", 2);
        return path.length == 2 ? DEFINITIONS.get(path[0] + ":" + path[1]) : null;
    }

    public static String translationKey(BeverageData data) {
        Definition definition = find(data.beverage());
        if (definition == null) return "fluid.binnietrees.beverage";
        if (definition.category().equals("cocktail")) {
            return "item.binnietrees.cocktail." + definition.value();
        }
        String sourceCategory = definition.category().equals("misc_fluid") ? "miscfluid" : definition.category();
        return "binnietrees.fluid." + sourceCategory + "." + definition.value().replace("_", "");
    }

    private static Map<String, Definition> create() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        put(values, "juice", "apple", "juice.apple", 0xFFCA32, 0);
        put(values, "juice", "apricot", "juice.apricot", 0xFFB51E, 0);
        put(values, "juice", "banana", "juice.banana", 0xE9D483, 0);
        put(values, "juice", "cherry", "juice.cherry", 0xC70B1F, 0);
        put(values, "juice", "elderberry", "juice.elderberry", 0x402629, 0);
        put(values, "juice", "lemon", "juice.lemon", 0xDEDA52, 0);
        put(values, "juice", "lime", "juice.lime", 0xB9CE6F, 0);
        put(values, "juice", "orange", "juice.orange", 0xF9A22F, 0);
        put(values, "juice", "peach", "juice.peach", 0xFAC55D, 0);
        put(values, "juice", "plum", "juice.plum", 0xA11F11, 0);
        put(values, "juice", "carrot", "juice.carrot", 0xFB8E17, 0);
        put(values, "juice", "tomato", "juice.tomato", 0xC2442E, 0);
        put(values, "juice", "cranberry", "juice.cranberry", 0xC52735, 0);
        put(values, "juice", "grapefruit", "juice.grapefruit", 0xF29255, 0);
        put(values, "juice", "olive", "juice.olive", 0xF4C900, 0);
        put(values, "juice", "pineapple", "juice.pineapple", 0xE7C547, 0);
        put(values, "juice", "pear", "juice.pear", 0xD8BF65, 0);
        put(values, "juice", "white_grape", "juice.white.grape", 0xFBE379, 0);
        put(values, "juice", "red_grape", "juice.red.grape", 0x952934, 0);
        put(values, "alcohol", "apple", "cider.apple", 0xFABE3C, 5);
        put(values, "alcohol", "apricot", "wine.apricot", 0xF0CF36, 10);
        put(values, "alcohol", "banana", "wine.banana", 0xE4C84D, 10);
        put(values, "alcohol", "cherry", "wine.cherry", 0xAB0416, 10);
        put(values, "alcohol", "elderberry", "wine.elderberry", 0x950001, 10);
        put(values, "alcohol", "peach", "cider.peach", 0xEA661B, 5);
        put(values, "alcohol", "pear", "ciderpear", 0xE5D067, 5);
        put(values, "alcohol", "plum", "wine.plum", 0xB81408, 10);
        put(values, "alcohol", "carrot", "wine.carrot", 0xF77D02, 10);
        put(values, "alcohol", "white_wine", "wine.white", 0xEDDA95, 10);
        put(values, "alcohol", "red_wine", "wine.red", 0x750B0B, 10);
        put(values, "alcohol", "sparkling_wine", "wine.sparkling", 0xFEF7BE, 10);
        put(values, "alcohol", "agave", "wine.agave", 0xD4AE64, 10);
        put(values, "alcohol", "potato", "fermented.potatoes", 0xB78950, 10);
        put(values, "alcohol", "citrus", "wine.citrus", 0xFFFF00, 10);
        put(values, "alcohol", "cranberry", "wine.cranberry", 0xB10002, 10);
        put(values, "alcohol", "pineapple", "wine.pineapple", 0xE0AC36, 10);
        put(values, "alcohol", "tomato", "wine.tomato", 0xBE1A19, 10);
        put(values, "alcohol", "fruit", "juice", 0xFABE3C, 10);
        put(values, "alcohol", "ale", "beer.ale", 0xC63A21, 5);
        put(values, "alcohol", "lager", "beer.lager", 0xE97C05, 5);
        put(values, "alcohol", "wheat_beer", "beer.wheat", 0xDB6E08, 5);
        put(values, "alcohol", "rye_beer", "beer.rye", 0xA55827, 5);
        put(values, "alcohol", "corn_beer", "beer.corn", 0xCCA424, 5);
        put(values, "alcohol", "stout", "beer.stout", 0x592901, 5);
        put(values, "alcohol", "barley", "mash.grain", 0xC63A21, 5);
        put(values, "alcohol", "wheat", "mash.wheat", 0xC63A21, 5);
        put(values, "alcohol", "rye", "mash.rye", 0xA55827, 5);
        put(values, "alcohol", "corn", "mash.corn", 0xCCA424, 5);
        put(values, "misc_fluid", "carbonated_water", "water.carbonated", 0xCCCCFF, 0);
        put(values, "misc_fluid", "tonic_water", "water.tonic", 0xCCCCFF, 0);
        put(values, "misc_fluid", "cream", "cream", 0xEAEADE, 0);
        put(values, "misc_fluid", "ginger_ale", "gingerAle", 0xFFFFFF, 0);
        put(values, "misc_fluid", "coffee", "coffee", 0x5A3105, 0);
        put(values, "misc_fluid", "sugar_syrup", "syrup.simple", 0xF5F8F1, 0);
        put(values, "misc_fluid", "agave_nectar", "syrup.agave", 0xCF7E25, 0);
        put(values, "misc_fluid", "grenadine_syrup", "syrup.grenadine", 0xF44965, 0);
        put(values, "spirit", "neutral_spirit", "spirit.neutral", 0xFFFFFF, 80);
        put(values, "spirit", "vodka", "vodka", 0xF4F5F7, 40);
        put(values, "spirit", "whiskey", "whiskey", 0xCF6F00, 40);
        put(values, "spirit", "corn_whiskey", "whiskey.corn", 0x961201, 40);
        put(values, "spirit", "rye_whiskey", "whiskey.rye", 0xF57328, 40);
        put(values, "spirit", "wheat_whiskey", "whiskey.wheat", 0xE48612, 40);
        put(values, "spirit", "tequila", "tequila", 0xF5E9C0, 40);
        put(values, "spirit", "brandy", "brandy.grape", 0xF79F20, 40);
        put(values, "spirit", "apple_brandy", "brandy.apple", 0xE4AA3E, 40);
        put(values, "spirit", "pear_brandy", "brandy.pear", 0xFEC633, 40);
        put(values, "spirit", "apricot_brandy", "brandy.apricot", 0xCB7F43, 40);
        put(values, "spirit", "plum_brandy", "brandy.plum", 0x912311, 40);
        put(values, "spirit", "cherry_brandy", "brandy.cherry", 0x830B1E, 40);
        put(values, "spirit", "elderberry_brandy", "brandy.elderberry", 0xBE2B47, 40);
        put(values, "spirit", "citrus_brandy", "brandy.citrus", 0xCB7F43, 40);
        put(values, "spirit", "fruit_brandy", "brandy.fruit", 0xE4AA3E, 40);
        // The legacy enum left every completed liquor at the same placeholder grey.
        // Preserve the color of the fermented fruit stream feeding each Distillery route.
        put(values, "spirit", "apple_liquor", "liquor.apple", 0xFABE3C, 40);
        put(values, "spirit", "pear_liquor", "liquor.pear", 0xE5D067, 40);
        put(values, "spirit", "cherry_liquor", "liquor.cherry", 0xAB0416, 40);
        put(values, "spirit", "elderberry_liquor", "liquor.elderberry", 0x950001, 40);
        put(values, "spirit", "apricot_liquor", "liquor.apricot", 0xF0CF36, 40);
        put(values, "spirit", "fruit_liquor", "liquor.fruit", 0xFABE3C, 40);
        put(values, "cocktail", "bellini", "Bellini", 0xF3C16C, 7);
        put(values, "cocktail", "black_russian", "BlackRussian", 0x331618, 34);
        put(values, "cocktail", "bloody_mary", "BloodyMary", 0xC12F00, 12);
        put(values, "cocktail", "cosmopolitan", "Cosmopolitan", 0xE20000, 20);
        put(values, "cocktail", "cuba_libre", "CubaLibre", 0x8A0404, 11);
        put(values, "cocktail", "french_connection", "FrenchConnection", 0xF6AD27, 30);
        put(values, "cocktail", "god_father", "GodFather", 0xC8691D, 27);
        put(values, "cocktail", "god_mother", "GodMother", 0xDF7601, 40);
        put(values, "cocktail", "grasshopper", "Grasshopper", 0x43C46A, 13);
        put(values, "cocktail", "french75", "French75", 0xECD89D, 17);
        put(values, "cocktail", "harvey_wallbanger", "HarveyWallbanger", 0xF9D100, 14);
        put(values, "cocktail", "hemingway_special", "HemingwaySpecial", 0xE7F3B5, 20);
        put(values, "cocktail", "horses_neck", "HorsesNeck", 0xFEB500, 10);
        put(values, "cocktail", "irish_coffee", "IrishCoffee", 0x440E02, 10);
        put(values, "cocktail", "kir", "Kir", 0xF4361E, 11);
        put(values, "cocktail", "bramble", "Bramble", 0xC3636E, 22);
        put(values, "cocktail", "b52", "B52", 0xC55E00, 20);
        put(values, "cocktail", "dark_nstormy", "DarkNStormy", 0xBA6628, 10);
        put(values, "cocktail", "dirty_martini", "DirtyMartini", 0xDAB253, 33);
        put(values, "cocktail", "expresso_martini", "ExpressoMartini", 0x63282A, 28);
        put(values, "cocktail", "french_martini", "FrenchMartini", 0xC12D4D, 28);
        put(values, "cocktail", "kamikaze", "Kamikaze", 0xE1D8AD, 20);
        put(values, "cocktail", "lemon_drop_martini", "LemonDropMartini", 0xF9DE8D, 23);
        put(values, "cocktail", "pisco_sour", "PiscoSour", 0xEAE69F, 21);
        put(values, "cocktail", "russian_spring_punch", "RussianSpringPunch", 0xB4242C, 19);
        put(values, "cocktail", "spritz_veneziano", "SpritzVeneziano", 0xEA4F00, 9);
        put(values, "cocktail", "tommys_margarita", "TommysMargarita", 0xE2DC88, 27);
        put(values, "cocktail", "vesper", "Vesper", 0xEEEEEC, 38);
        put(values, "cocktail", "sex_on_the_beach", "SexOnTheBeach", 0xFE7A32, 14);
        if (values.size() != 107) throw new IllegalStateException("Expected 107 completed beverages, got " + values.size());
        return Map.copyOf(values);
    }

    private static void put(Map<String, Definition> values, String category, String value, String legacyIdentifier, int color, int alcohol) {
        String key = category + ":" + value;
        Definition previous = values.put(key, new Definition(category, value, legacyIdentifier, color, alcohol));
        if (previous != null) throw new IllegalStateException("Duplicate beverage " + key);
    }

    public record Definition(String category, String value, String legacyIdentifier, int color, int alcohol) {
        public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath("binnietrees", category + "/" + value); }
        public BeverageData data(int volume) { return new BeverageData(id().toString(), color, alcohol, volume); }
    }
}
