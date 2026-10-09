package binnie.botany.item;

import binnie.core.catalog.LegacyCatalogs;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Current pigment identities: vanilla dyes replace matching duplicate Botany colours. */
public class BotanyPigments {
    private static final String RESOURCE = "data/binniebotany/binnie_catalog/pigment_substitutions.json";
    private static final Map<String, ResourceLocation> SUBSTITUTIONS = loadSubstitutions();
    private static final List<String> CUSTOM_COLORS = LegacyCatalogs.values("binniebotany.flower_color").stream()
            .filter(color -> !SUBSTITUTIONS.containsKey(color)).toList();

    private BotanyPigments() {}

    public static List<String> customColors() {
        return CUSTOM_COLORS;
    }

    public static Map<String, ResourceLocation> substitutions() {
        return SUBSTITUTIONS;
    }

    public static boolean isSubstituted(String color) {
        return SUBSTITUTIONS.containsKey(color);
    }

    public static ItemStack stackFor(String color, FlowerColorVariantItem customPigments) {
        ResourceLocation substitute = SUBSTITUTIONS.get(color);
        if (substitute != null) return new ItemStack(BuiltInRegistries.ITEM.get(substitute));
        return customPigments.createStack(color);
    }

    private static Map<String, ResourceLocation> loadSubstitutions() {
        var stream = BotanyPigments.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing Botany pigment substitution catalog: " + RESOURCE);
        JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        LinkedHashMap<String, ResourceLocation> result = new LinkedHashMap<>();
        for (var entry : root.entrySet()) {
            if (!LegacyCatalogs.values("binniebotany.flower_color").contains(entry.getKey())) {
                throw new IllegalStateException("Pigment substitution is not a flower color: " + entry.getKey());
            }
            result.put(entry.getKey(), ResourceLocation.parse(entry.getValue().getAsString()));
        }
        return Map.copyOf(result);
    }
}
