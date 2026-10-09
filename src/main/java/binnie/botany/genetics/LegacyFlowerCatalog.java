package binnie.botany.genetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable 1.12 catalog for all 1.12 Botany binniegenetics data. */
public class LegacyFlowerCatalog {
    private static final String RESOURCE = "binniebotany/legacy_flower_catalog.json";
    public static final LegacyFlowerCatalog INSTANCE = load();

    private final List<ColorDef> colors;
    private final List<SpeciesDef> species;
    private final List<MutationDef> mutations;
    private final List<ColorMixDef> colorMixes;
    private final Map<ResourceLocation, ColorDef> colorsById;
    private final Map<ResourceLocation, SpeciesDef> speciesById;
    private final Map<ColorPair, ColorMixDef> mixesByPair;

    private LegacyFlowerCatalog(List<ColorDef> colors, List<SpeciesDef> species, List<MutationDef> mutations, List<ColorMixDef> colorMixes) {
        this.colors = List.copyOf(colors);
        this.species = List.copyOf(species);
        this.mutations = List.copyOf(mutations);
        this.colorMixes = List.copyOf(colorMixes);

        LinkedHashMap<ResourceLocation, ColorDef> colorMap = new LinkedHashMap<>();
        for (ColorDef color : colors) {
            if (colorMap.put(color.id(), color) != null) {
                throw new IllegalStateException("Duplicate flower color: " + color.id());
            }
        }
        this.colorsById = Map.copyOf(colorMap);

        LinkedHashMap<ResourceLocation, SpeciesDef> speciesMap = new LinkedHashMap<>();
        for (SpeciesDef value : species) {
            if (speciesMap.put(value.id(), value) != null) {
                throw new IllegalStateException("Duplicate flower species: " + value.id());
            }
        }
        this.speciesById = Map.copyOf(speciesMap);

        LinkedHashMap<ColorPair, ColorMixDef> mixMap = new LinkedHashMap<>();
        for (ColorMixDef mix : colorMixes) {
            ColorPair pair = ColorPair.of(mix.first(), mix.second());
            if (mixMap.put(pair, mix) != null) {
                throw new IllegalStateException("Duplicate flower color pair: " + pair);
            }
        }
        this.mixesByPair = Map.copyOf(mixMap);

        requireCount("colors", colors.size(), 80);
        requireCount("species", species.size(), 47);
        requireCount("executable species mutations", mutations.size(), 37);
        requireCount("executable color mixes", colorMixes.size(), 2815);
    }

    private static void requireCount(String label, int actual, int expected) {
        if (actual != expected) {
            throw new IllegalStateException("Expected " + expected + ' ' + label + ", got " + actual);
        }
    }

    public List<ColorDef> colors() {
        return this.colors;
    }

    public List<SpeciesDef> species() {
        return this.species;
    }

    public List<MutationDef> mutations() {
        return this.mutations;
    }

    public List<ColorMixDef> colorMixes() {
        return this.colorMixes;
    }

    public ColorDef color(ResourceLocation id) {
        ColorDef value = this.colorsById.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown flower color: " + id);
        return value;
    }

    public SpeciesDef species(ResourceLocation id) {
        SpeciesDef value = this.speciesById.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown flower species: " + id);
        return value;
    }

    public ColorMixDef colorMix(ResourceLocation first, ResourceLocation second) {
        return this.mixesByPair.get(ColorPair.of(first, second));
    }

    private static LegacyFlowerCatalog load() {
        InputStream stream = LegacyFlowerCatalog.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing flower catalog: " + RESOURCE);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return new LegacyFlowerCatalog(
                    parseColors(root.getAsJsonArray("colors")),
                    parseSpecies(root.getAsJsonArray("species")),
                    parseMutations(root.getAsJsonArray("mutations")),
                    parseColorMixes(root.getAsJsonArray("color_mixes"))
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed reading flower catalog: " + RESOURCE, exception);
        }
    }

    private static List<ColorDef> parseColors(JsonArray array) {
        ArrayList<ColorDef> result = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            JsonObject value = element.getAsJsonObject();
            result.add(new ColorDef(id(value.get("id").getAsString()), value.get("rgb").getAsInt(),
                    value.get("wilted_rgb").getAsInt(), value.get("dominant").getAsBoolean()));
        }
        return result;
    }

    private static List<SpeciesDef> parseSpecies(JsonArray array) {
        ArrayList<SpeciesDef> result = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            JsonObject value = element.getAsJsonObject();
            LinkedHashMap<String, AlleleDef> alleles = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject("alleles").entrySet()) {
                JsonObject allele = entry.getValue().getAsJsonObject();
                alleles.put(entry.getKey(), new AlleleDef(allele.get("value").deepCopy(), allele.get("dominant").getAsBoolean(),
                        allele.get("kind").getAsString(), allele.get("legacy").getAsString()));
            }
            result.add(new SpeciesDef(
                    id(value.get("id").getAsString()), value.get("display_name").getAsString(), value.get("genus").getAsString(),
                    value.get("species").getAsString(), value.get("dominant").getAsBoolean(), value.get("authority").getAsString(),
                    id(value.get("type").getAsString()), value.get("sections").getAsInt(), value.get("temperature").getAsString(),
                    value.get("moisture").getAsString(), value.get("acidity").getAsString(), Map.copyOf(alleles)));
        }
        return result;
    }

    private static List<MutationDef> parseMutations(JsonArray array) {
        ArrayList<MutationDef> result = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            JsonObject value = element.getAsJsonObject();
            result.add(new MutationDef(id(value.get("id").getAsString()), id(value.get("first").getAsString()),
                    id(value.get("second").getAsString()), id(value.get("result").getAsString()), value.get("chance").getAsInt(),
                    value.get("source_entry").getAsString(), value.get("source_ordinal").getAsInt()));
        }
        return result;
    }

    private static List<ColorMixDef> parseColorMixes(JsonArray array) {
        ArrayList<ColorMixDef> result = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            JsonObject value = element.getAsJsonObject();
            result.add(new ColorMixDef(id(value.get("first").getAsString()), id(value.get("second").getAsString()),
                    id(value.get("result").getAsString()), value.get("chance").getAsInt(), value.get("source_ordinal").getAsInt()));
        }
        return result;
    }

    private static ResourceLocation id(String pathOrId) {
        return pathOrId.indexOf(':') >= 0 ? ResourceLocation.parse(pathOrId) : ResourceLocation.fromNamespaceAndPath("binniebotany", pathOrId);
    }

    public record ColorDef(ResourceLocation id, int rgb, int wiltedRgb, boolean dominant) {}

    public record AlleleDef(JsonElement value, boolean dominant, String kind, String legacy) {
        public int intValue() { return this.value.getAsInt(); }
        public float floatValue() { return this.value.getAsFloat(); }
        public String stringValue() { return this.value.getAsString(); }
        public ResourceLocation resourceValue() { return ResourceLocation.parse(stringValue()); }
        public int[] intArray() {
            JsonArray values = this.value.getAsJsonArray();
            return new int[]{values.get(0).getAsInt(), values.get(1).getAsInt(), values.get(2).getAsInt()};
        }
    }

    public record SpeciesDef(ResourceLocation id, String displayName, String genus, String species, boolean dominant,
                             String authority, ResourceLocation type, int sections, String temperature, String moisture,
                             String acidity, Map<String, AlleleDef> alleles) {
        public AlleleDef allele(String chromosome) {
            AlleleDef allele = this.alleles.get(chromosome);
            if (allele == null) throw new IllegalArgumentException("Missing " + chromosome + " for " + this.id);
            return allele;
        }
    }

    public record MutationDef(ResourceLocation id, ResourceLocation first, ResourceLocation second, ResourceLocation result,
                              int chance, String sourceEntry, int sourceOrdinal) {}

    public record ColorMixDef(ResourceLocation first, ResourceLocation second, ResourceLocation result, int chance, int sourceOrdinal) {}

    private record ColorPair(ResourceLocation low, ResourceLocation high) {
        static ColorPair of(ResourceLocation first, ResourceLocation second) {
            return first.toString().compareTo(second.toString()) <= 0 ? new ColorPair(first, second) : new ColorPair(second, first);
        }
    }
}
