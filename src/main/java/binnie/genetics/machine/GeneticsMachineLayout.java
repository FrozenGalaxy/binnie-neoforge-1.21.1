package binnie.genetics.machine;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/**
  * 1.12 presentation coordinates for the six save-compatible live
  * slots in a Genetics laboratory. The supplemental 1.12 arrays are defined
  * by {@link GeneticsMachineInventoryLayout}.
  */
public class GeneticsMachineLayout {
    private GeneticsMachineLayout() {
    }

    public record Layout(
            int width,
            int height,
            int inputX, int inputY,
            int catalystX, int catalystY,
            int reagentX, int reagentY,
            int outputX, int outputY,
            int secondaryX, int secondaryY,
            int tertiaryX, int tertiaryY,
            int playerX, int playerY
    ) {
    }

    public static Optional<Layout> forMachine(ResourceLocation id) {
        if (!"binniegenetics".equals(id.getNamespace())) {
            return Optional.empty();
        }
        return Optional.of(switch (id.getPath()) {
            case "isolator" -> new Layout(330, 208, 80, 35, 240, 71, 80, 71, 240, 35, -1_000, -1_000, -1_000, -1_000, 84, 120);
            case "sequencer" -> new Layout(226, 224, 112, 48, -1_000, -1_000, 34, 112, 190, 32, -1_000, -1_000, -1_000, -1_000, 32, 136);
            case "polymeriser" -> new Layout(278, 212, 50, 38, 120, 64, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, 58, 124);
            case "inoculator" -> new Layout(266, 240, 176, 58, 70, 32, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, 52, 152);
            case "splicer" -> new Layout(280, 240, 131, 74, 68, 62, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, 59, 152);
            case "analyser" -> new Layout(220, 210, 101, 48, -1_000, -1_000, 44, 94, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, 29, 122);
            case "genepool" -> new Layout(280, 198, 98, 53, -1_000, -1_000, 121, 82, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, 59, 110);
            case "incubator" -> new Layout(228, 196, -1_000, -1_000, -1_000, -1_000, 91, 45, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, 33, 108);
            case "acclimatiser" -> new Layout(280, 198, 88, 32, 70, 68, 88, 68, -1_000, -1_000, -1_000, -1_000, -1_000, -1_000, 59, 110);
            default -> new Layout(176, 166, 26, 35, 62, 35, 80, 58, 116, 35, 140, 35, 140, 58, 8, 84);
        });
    }
}
