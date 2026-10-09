package binnie.trees.forestry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Source-generated, immutable runtime bindings for the full Binnie's Trees genetics catalog. */
public record LegacyTreeCatalog(List<TreeBinding> species, List<FruitBinding> fruits) {
    private static final String RESOURCE = "binnietrees/legacy_tree_bindings.json";

    public static LegacyTreeCatalog load() {
        InputStream stream = LegacyTreeCatalog.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing generated tree catalog: " + RESOURCE);
        }
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            LegacyTreeCatalog catalog = new Gson().fromJson(reader, LegacyTreeCatalog.class);
            if (catalog == null || catalog.species == null || catalog.fruits == null) {
                throw new IllegalStateException("Invalid generated tree catalog: " + RESOURCE);
            }
            return catalog;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read generated tree catalog: " + RESOURCE, exception);
        }
    }

    public static boolean isForestryOwned(String id) {
        return id.equals("coconut");
    }

    public record TreeBinding(
            String id,
            String genus,
            String species,
            boolean dominant,
            String authority,
            String temperature,
            String humidity,
            @SerializedName("leaf_color") int leafColor,
            @SerializedName("pollinated_color") int pollinatedColor,
            @SerializedName("wood_color") int woodColor,
            @SerializedName("leaf_type") String leafType,
            @SerializedName("sapling_type") String saplingType,
            String wood,
            String generator,
            Map<String, AlleleBinding> genome,
            float rarity
    ) {
        public TreeBinding {
            genome = genome == null ? Map.of() : Map.copyOf(genome);
        }
    }

    public record AlleleBinding(JsonElement value, Boolean dominant) {
        public boolean dominantOr(boolean fallback) {
            return dominant == null ? fallback : dominant;
        }
    }

    public record FruitBinding(
            String id,
            int period,
            @SerializedName("callow_color") int callowColor,
            @SerializedName("ripe_color") int ripeColor,
            String sprite,
            boolean pod,
            @SerializedName("pod_type") String podType,
            @SerializedName("product_variant") String productVariant,
            @SerializedName("product_count") int productCount,
            boolean dominant
    ) {
    }
}
