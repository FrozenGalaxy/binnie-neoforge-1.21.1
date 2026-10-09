package binnie.botany.farming;

import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;

import java.util.ArrayList;
import java.util.List;

/** 1.12 physical tube palette plus mappings for the current farm-profile circuit identities. */
public class BotanyTubeVariants {
    private static final String[] MATERIALS = {"copper", "tin", "bronze", "iron"};
    private static final int[] MATERIAL_COLORS = {0xe3b78e, 0xe1eef4, 0xddc276, 0xd8d8d8};
    private static final String[] INSULATIONS = {"clay", "cobblestone", "sand", "hardened_clay", "stone", "sandstone"};
    private static final int[] INSULATION_COLORS = {0xa1aacc, 0x7b7b7b, 0xefeab5, 0x935c43, 0x6d6d6d, 0xc1b989};

    private BotanyTubeVariants() {}

    public static List<String> physicalIds() {
        ArrayList<String> values = new ArrayList<>();
        for (String insulation : INSULATIONS) for (String material : MATERIALS) values.add(material + "_" + insulation);
        return List.copyOf(values);
    }

    public static List<String> allIds() {
        ArrayList<String> values = new ArrayList<>(physicalIds());
        values.addAll(BotanyFarmVariants.variantIds());
        return List.copyOf(values);
    }

    /** The two 1.12 circuit layouts encoded by one physical tube's material and insulation. */
    public static List<BotanyFarmVariants.Variant> circuitsFor(String tubeId) {
        int index = physicalIds().indexOf(tubeId);
        if (index < 0) return List.of();
        int material = index % MATERIALS.length;
        int insulation = index / MATERIALS.length;
        SoilMoisture moisture = SoilMoisture.values()[2 - insulation % 3];
        SoilAcidity acidity = material == 0 ? null : SoilAcidity.values()[material - 1];
        boolean fertilised = insulation >= 3;
        return List.of(new BotanyFarmVariants.Variant(moisture, acidity, false, fertilised),
                new BotanyFarmVariants.Variant(moisture, acidity, true, fertilised));
    }

    /** pH selects the metal; moisture and fertilizer select the insulation. */
    public static String physicalId(BotanyFarmVariants.Variant circuit) {
        int material = circuit.acidity() == null ? 0 : circuit.acidity().ordinal() + 1;
        int insulation = 2 - circuit.moisture().ordinal() + (circuit.fertilised() ? 3 : 0);
        return MATERIALS[material] + "_" + INSULATIONS[insulation];
    }

    public static Physical physical(String id) {
        for (int insulation = 0; insulation < INSULATIONS.length; insulation++) {
            for (int material = 0; material < MATERIALS.length; material++) {
                if (id.equals(MATERIALS[material] + "_" + INSULATIONS[insulation])) {
                    return new Physical(MATERIALS[material], MATERIAL_COLORS[material], INSULATIONS[insulation], INSULATION_COLORS[insulation]);
                }
            }
        }
        for (BotanyFarmVariants.Variant variant : BotanyFarmVariants.all()) {
            if (!variant.id().equals(id)) continue;
            int material = variant.acidity() == null ? 0 : variant.acidity().ordinal() + 1;
            int insulation = 2 - variant.moisture().ordinal() + (variant.fertilised() ? 3 : 0);
            return new Physical(MATERIALS[material], MATERIAL_COLORS[material], INSULATIONS[insulation], INSULATION_COLORS[insulation]);
        }
        return new Physical(MATERIALS[0], MATERIAL_COLORS[0], INSULATIONS[0], INSULATION_COLORS[0]);
    }

    public record Physical(String material, int materialColor, String insulation, int insulationColor) {}
}
