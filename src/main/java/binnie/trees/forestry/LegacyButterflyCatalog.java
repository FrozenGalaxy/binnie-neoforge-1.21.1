package binnie.trees.forestry;

import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import forestry.api.plugin.ILepidopterologyRegistration;

import java.util.List;

/** 1.12 Binnie's Trees butterfly catalog from ButterflySpecies. */
public final class LegacyButterflyCatalog {
    public record Species(String id, String genus, String species, int color) {
        public ResourceLocation resourceId() {
            return ResourceLocation.fromNamespaceAndPath("binnietrees", "butterfly_" + id);
        }
    }

    public static final List<Species> SPECIES = List.of(
            s("white_admiral", "Limenitis", "camilla", 16448250),
            s("purple_emperor", "Apatura", "iris", 4338374),
            s("red_admiral", "Vanessa", "atalanta", 15101764),
            s("painted_lady", "Vanessa", "cardui", 15573064),
            s("small_tortoiseshell", "Aglais", "urticae", 15365387),
            s("camberwell_beauty", "Aglais", "antiopa", 9806540),
            s("peacock", "Inachis", "io", 13842434),
            s("wall", "Lasiommata", "megera", 15707678),
            s("crimson_rose", "Atrophaneura", "hector", 16736891),
            s("kaiser_i_hind", "Teinopalpus", "imperialis", 7839808),
            s("golden_birdwing", "Troides", "aeacus", 16374814),
            s("marsh_fritillary", "Euphydryas", "aurinia", 16747520),
            s("pearl_bordered_fritillary", "Boloria", "euphrosyne", 16747267),
            s("queen_of_spain_fritillary", "Issoria", "lathonia", 16765247),
            s("speckled_wood", "Pararge", "aegeria", 16119949),
            s("scotch_angus", "Erebia", "aethiops", 12735523),
            s("gatekeeper", "Pyronia", "tithonus", 16433962),
            s("meadow_brown", "Maniola", "jurtina", 14914841),
            s("small_heath", "Coenonympha", "pamphilus", 16754226),
            s("ringlet", "Aphantopus", "hyperantus", 9919799),
            s("monarch", "Danaus", "plexippus", 16757254),
            s("marbled_white", "Melanargia", "galathea", 15527148)
    );

    private LegacyButterflyCatalog() {}

    public static int register(ILepidopterologyRegistration registration) {
        for (Species species : SPECIES) {
            registration.registerSpecies(species.resourceId(), species.genus(), species.species(), true,
                            TextColor.fromRgb(species.color()), 0.5F)
                    .setAuthority("Binnie")
                    .setRarity(0.5F);
        }
        return SPECIES.size();
    }

    private static Species s(String id, String genus, String species, int color) {
        return new Species(id, genus, species, color);
    }
}
