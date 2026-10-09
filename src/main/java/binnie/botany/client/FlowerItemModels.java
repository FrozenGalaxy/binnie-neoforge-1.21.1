package binnie.botany.client;

import binnie.botany.genetics.LegacyFlowerCatalog;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stable source-order flower-type to custom-model-data mapping. Safe to call on both logical sides. */
public class FlowerItemModels {
    private static final Map<String, Integer> INDEX = build();
    private FlowerItemModels() {}
    private static Map<String, Integer> build() {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (LegacyFlowerCatalog.SpeciesDef species : LegacyFlowerCatalog.INSTANCE.species()) {
            String type = normalize(species.type().getPath());
            result.putIfAbsent(type, result.size());
        }
        return Map.copyOf(result);
    }
    public static int index(String type) { return INDEX.getOrDefault(normalize(type), -1); }
    public static Map<String, Integer> all() { return INDEX; }
    private static String normalize(String value) {
        String compact = value.replace("_", "").toLowerCase(java.util.Locale.ROOT);
        return switch (compact) {
            case "dandelion" -> "dandelion";
            case "poppy" -> "poppy";
            case "orchid" -> "orchid";
            case "allium" -> "allium";
            case "azurebluet", "bluet" -> "bluet";
            case "tulip" -> "tulip";
            case "daisy" -> "daisy";
            case "cornflower" -> "cornflower";
            case "carnation" -> "carnation";
            case "lily" -> "lily";
            case "rose" -> "rose";
            case "peony" -> "peony";
            case "lilac" -> "lilac";
            case "hydrangea" -> "hydrangea";
            case "foxglove" -> "foxglove";
            case "lavender" -> "lavender";
            case "goldenrod" -> "goldenrod";
            case "althea" -> "althea";
            case "penstemon" -> "penstemon";
            case "delphinium" -> "delphinium";
            case "hollyhock" -> "hollyhock";
            case "camellia" -> "camellia";
            default -> value.toLowerCase(java.util.Locale.ROOT).replace("d_a_n_d_e_l_i_o_n", "dandelion");
        };
    }
}
