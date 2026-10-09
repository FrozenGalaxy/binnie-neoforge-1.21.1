package binnie.botany.farming;

import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import java.util.ArrayList;
import java.util.List;

/** Full 1.12 garden-circuit identity matrix: moisture × pH × manual × fertilised. */
public class BotanyFarmVariants {
    public record Variant(SoilMoisture moisture, SoilAcidity acidity, boolean manual, boolean fertilised) {
        public String id() {
            return moisture.getSerializedName() + "_" + (acidity == null ? "any" : acidity.getSerializedName())
                    + (manual ? "_manual" : "_managed") + (fertilised ? "_fertilised" : "_unfertilised");
        }
    }
    private BotanyFarmVariants() {}
    public static List<Variant> all() {
        ArrayList<Variant> out = new ArrayList<>();
        for (boolean manual : new boolean[]{false, true})
            for (boolean fertilised : new boolean[]{false, true})
                for (SoilMoisture moisture : SoilMoisture.values()) {
                    out.add(new Variant(moisture, null, manual, fertilised));
                    for (SoilAcidity acidity : SoilAcidity.values()) out.add(new Variant(moisture, acidity, manual, fertilised));
                }
        return List.copyOf(out);
    }
    public static List<String> variantIds() { return all().stream().map(Variant::id).toList(); }
}
