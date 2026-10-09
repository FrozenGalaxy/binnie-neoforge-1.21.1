package binnie.bees.forestry;

import binnie.core.data.VariantData;
import binnie.core.registry.BinnieDataComponents;
import binnie.bees.BinnieBees;
import binnie.bees.registry.BinnieBeesContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.core.ToleranceType;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.Product;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.plugin.IApicultureRegistration;
import forestry.api.plugin.IBeeSpeciesBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Loads the generated 1.12-compatible Binnie's Bees species definitions into ForestryCE. */
public class LegacyBeeSpeciesCatalog {
    private static final String INDEX = "/data/binniebees/binnie_catalog/bee_species.json";

    private LegacyBeeSpeciesCatalog() {}

    public static int register(IApicultureRegistration apiculture) {
        List<String> ids = loadSpeciesIds();
        int registered = 0;
        for (String id : ids) {
            JsonObject json = readJson("/data/binniebees/bee_species/" + id + ".json");
            if (json.has("required_mod") && !ModList.get().isLoaded(json.get("required_mod").getAsString())) {
                BinnieBees.LOGGER.info("Skipped Binnie's Bees species {}: required mod {} is unavailable",
                        id, json.get("required_mod").getAsString());
                continue;
            }
            registerSpecies(apiculture, id, json);
            registered++;
        }
        return registered;
    }

    private static void registerSpecies(IApicultureRegistration apiculture, String path, JsonObject json) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, path);
        IBeeSpeciesBuilder builder = apiculture.registerSpecies(
                id,
                json.get("genus").getAsString(),
                json.get("species").getAsString(),
                json.get("dominant").getAsBoolean(),
                TextColor.fromRgb(json.get("outline").getAsInt()))
        // 1.12's DefaultBeeSpriteColourProvider used the primary colour only
        // for render pass 0 (outline), and its secondary colour for pass 1
        // (body). The current model splits the old rear detail into its own
        // layer; its 1.12 texture rendered black.
                .setBody(TextColor.fromRgb(json.get("body").getAsInt()))
                .setStripes(TextColor.fromRgb(json.get("stripes").getAsInt()))
                .setAuthority(json.get("authority").getAsString());

        if (json.has("temperature")) builder.setTemperature(TemperatureType.valueOf(json.get("temperature").getAsString().toUpperCase(java.util.Locale.ROOT)));
        if (json.has("humidity")) builder.setHumidity(HumidityType.valueOf(json.get("humidity").getAsString().toUpperCase(java.util.Locale.ROOT)));
        if (json.has("products")) addProducts(builder, json.getAsJsonArray("products"), false);
        if (json.has("specialties")) addProducts(builder, json.getAsJsonArray("specialties"), true);
        if (json.has("genome")) {
            JsonObject genome = json.getAsJsonObject("genome");
            builder.setGenome(target -> applyGenome(target, genome));
        }
    }

    private static void addProducts(IBeeSpeciesBuilder builder, JsonArray products, boolean specialty) {
        for (JsonElement element : products) {
            JsonObject product = element.getAsJsonObject();
            if (product.has("required_mod")
                    && !ModList.get().isLoaded(product.get("required_mod").getAsString())) {
                continue;
            }
            ResourceLocation itemId = ResourceLocation.parse(product.get("item").getAsString());
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                BinnieBees.LOGGER.warn("Skipping missing Binnie's Bees product item {}", itemId);
                continue;
            }

            int count = product.has("count") ? product.get("count").getAsInt() : 1;
            ItemStack stack;
            if (itemId.equals(ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, "honey_comb"))) {
                VariantData variant = requireCombVariant(product);
                stack = BinnieBeesContent.HONEY_COMB.get().createStack(variant.value());
                stack.setCount(count);
            } else {
                stack = new ItemStack(item, count);
                applyComponents(stack, product);
            }

            float chance = product.get("chance").getAsFloat();
            Product registered = new Product(stack.getItem(), stack.getCount(), stack.getComponentsPatch(), chance);
            assertRoundTrip(itemId, stack, registered);
            if (specialty) builder.addSpecialty(registered); else builder.addProduct(registered);
        }
    }

    private static VariantData requireCombVariant(JsonObject product) {
        if (!product.has("tag")) {
            throw new IllegalStateException("Binnie's Bees honey-comb product has no binniecore:variant tag: " + product);
        }
        JsonObject variant = product.getAsJsonObject("tag").getAsJsonObject("binniecore:variant");
        if (variant == null) {
            throw new IllegalStateException("Binnie's Bees honey-comb product has no binniecore:variant component: " + product);
        }
        VariantData data = new VariantData(variant.get("category").getAsString(), variant.get("value").getAsString());
        if (!data.category().equals("binniebees.honey_comb")) {
            throw new IllegalStateException("Wrong Binnie's Bees comb variant category " + data.category() + " for " + data.value());
        }
        if (!BinnieBeesContent.HONEY_COMB.get().variants().contains(data.value())) {
            throw new IllegalStateException("Unknown Binnie's Bees comb variant " + data.value());
        }
        return data;
    }

    private static void applyComponents(ItemStack stack, JsonObject product) {
        if (!product.has("tag")) return;
        JsonObject variant = product.getAsJsonObject("tag").getAsJsonObject("binniecore:variant");
        if (variant != null) {
            stack.set(BinnieDataComponents.VARIANT, new VariantData(
                    variant.get("category").getAsString(), variant.get("value").getAsString()));
        }
    }

    private static void assertRoundTrip(ResourceLocation itemId, ItemStack expected, Product product) {
        ItemStack recreated = product.createStack();
        if (!ItemStack.isSameItemSameComponents(expected, recreated) || expected.getCount() != recreated.getCount()) {
            throw new IllegalStateException("Forestry product round-trip changed " + itemId + ": "
                    + expected.getComponentsPatch() + " -> " + recreated.getComponentsPatch());
        }
        if (itemId.equals(ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, "honey_comb"))) {
            VariantData wanted = expected.get(BinnieDataComponents.VARIANT);
            VariantData actual = recreated.get(BinnieDataComponents.VARIANT);
            if (!Objects.equals(wanted, actual)) {
                throw new IllegalStateException("Forestry product lost Binnie's Bees comb variant " + wanted + " -> " + actual);
            }
        }
    }

    private static void applyGenome(forestry.api.plugin.IGenomeBuilder target, JsonObject genome) {
        for (var entry : genome.entrySet()) {
            JsonObject allele = entry.getValue().getAsJsonObject();
            boolean dominant = !allele.has("dominant") || allele.get("dominant").getAsBoolean();
            JsonElement value = allele.get("value");
            switch (entry.getKey()) {
                case "forestry:speed" -> target.set(BeeChromosomes.SPEED, new Allele<>(value.getAsFloat(), dominant));
                case "forestry:lifespan" -> target.set(BeeChromosomes.LIFESPAN, new Allele<>(value.getAsInt(), dominant));
                case "forestry:fertility" -> target.set(BeeChromosomes.FERTILITY, new Allele<>(value.getAsInt(), dominant));
                case "forestry:temperature_tolerance" -> target.set(BeeChromosomes.TEMPERATURE_TOLERANCE,
                        new Allele<>(ToleranceType.valueOf(value.getAsString()), dominant));
                case "forestry:humidity_tolerance" -> target.set(BeeChromosomes.HUMIDITY_TOLERANCE,
                        new Allele<>(ToleranceType.valueOf(value.getAsString()), dominant));
                case "forestry:activity" -> target.set(BeeChromosomes.ACTIVITY, ResourceLocation.parse(value.getAsString()));
                case "forestry:cave_dwelling" -> target.set(BeeChromosomes.CAVE_DWELLING, new Allele<>(value.getAsBoolean(), dominant));
                case "forestry:tolerates_rain" -> target.set(BeeChromosomes.TOLERATES_RAIN, new Allele<>(value.getAsBoolean(), dominant));
                case "forestry:flower_type" -> target.set(BeeChromosomes.FLOWER_TYPE, ResourceLocation.parse(value.getAsString()));
                case "forestry:bee_effect" -> target.set(BeeChromosomes.EFFECT, ResourceLocation.parse(value.getAsString()));
                case "forestry:pollination" -> target.set(BeeChromosomes.POLLINATION, new Allele<>(value.getAsInt(), dominant));
                case "forestry:territory" -> {
                    JsonArray array = value.getAsJsonArray();
                    target.set(BeeChromosomes.TERRITORY, new Allele<>(new Vec3i(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt()), dominant));
                }
                default -> BinnieBees.LOGGER.warn("Ignoring unknown Binnie's Bees chromosome {}", entry.getKey());
            }
        }
    }

    private static List<String> loadSpeciesIds() {
        JsonArray values = readJson(INDEX).getAsJsonArray("values");
        List<String> ids = new ArrayList<>(values.size());
        values.forEach(value -> ids.add(value.getAsString()));
        return List.copyOf(ids);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = LegacyBeeSpeciesCatalog.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing bundled Binnie's Bees definition " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read bundled Binnie's Bees definition " + path, exception);
        }
    }
}
