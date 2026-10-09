package binnie.design.data;

import binnie.core.catalog.LegacyCatalogs;
import binnie.core.data.DesignData;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

/** Immutable current registry view over all 1.12 designs and render patterns. */
public class DesignCatalog {
    public static final List<String> DESIGNS = List.copyOf(LegacyCatalogs.values("binniedesign.design"));
    public static final List<String> PATTERNS = List.copyOf(LegacyCatalogs.values("binniedesign.pattern"));
    private static final Set<String> DESIGN_IDS = Set.copyOf(new LinkedHashSet<>(DESIGNS));
    private static final Set<String> PATTERN_IDS = Set.copyOf(new LinkedHashSet<>(PATTERNS));
    /** Matching active EnumDesign.Category ordering from the 1.12 source. */
    public static final List<Map.Entry<String, List<String>>> CATEGORY_ORDER = List.of(
            Map.entry("Designs & Emblems", List.of("blank","octagon","diamond","ringed","squared","multiply","plus","circle","emblem1","emblem2","emblem3","emblem4","emblem5","creeper")),
            Map.entry("Squares & Stripes", List.of("chequered","chequered_b","tiled","tiled_b","striped","thin_striped","thin_striped_corner","striped_corner","ornate_striped_corner")),
            Map.entry("Edges", List.of("halved","corner","thick_corner","edged","thin_corner","thin_edged","very_thin_corner","thin_curved_corner","curved_corner")),
            Map.entry("Bars", List.of("thin_barred","thin_barred_corner","thin_t_section","thin_crossed","thin_barred_end","ornate_thin_barred","barred","barred_corner","t_section","crossed","barred_end","overlapped_barred","ornate_barred","thick_barred","thick_barred_corner","thick_t_section","thick_crossed","thick_barred_end","thin_curved_barred_corner","curved_barred_corner","thin_curved_t_section","curved_t_section","thin_curved_crossed","split_barred","split_barred_corner","split_barred_t_section","split_crossed","split_barred_end","overlapped_split_barred")),
            Map.entry("Letters", List.of("letter_a","letter_b","letter_c","letter_d","letter_e","letter_f","letter_g","letter_h","letter_i","letter_j","letter_k","letter_l","letter_m","letter_n","letter_o","letter_p","letter_q","letter_r","letter_s","letter_t","letter_u","letter_v","letter_w","letter_x","letter_y","letter_z")),
            Map.entry("Diagonals", List.of("thin_saltire","diagonal","diagonal_corner","diagonal_t_section","diagonal_curved_corner","diagonal_curved_t_section","saltire","thick_diagonal","thick_saltire","diagonal_halved","diagonal1_edged","diagonal2_edged","thick_diagonal1_edged")));
    /** Designs reachable through the 1.12 category selector; EnumDesign.Test was never registered. */
    public static final List<String> SELECTABLE_DESIGNS = CATEGORY_ORDER.stream()
            .flatMap(category -> category.getValue().stream()).toList();

    private DesignCatalog() {}

    public static boolean isDesign(String id) {
        return DESIGN_IDS.contains(id);
    }

    public static boolean isSelectableDesign(String id) {
        return SELECTABLE_DESIGNS.contains(id);
    }

    public static boolean isPattern(String id) {
        return PATTERN_IDS.contains(id);
    }

    public static DesignData validate(DesignData data) {
        String design = isDesign(data.design()) ? data.design() : "blank";
        String pattern = (isPattern(data.pattern()) || isDesign(data.pattern())) ? data.pattern() : design;
        return new DesignData(design, pattern, data.primaryMaterial(), data.secondaryMaterial(),
                Math.floorMod(data.rotation(), 4), data.mirrored(), data.primaryVariant(), data.secondaryVariant());
    }

    public static boolean usesLegacyLayout(DesignData data) {
        return isDesign(data.design()) && data.design().equals(data.pattern());
    }

}
