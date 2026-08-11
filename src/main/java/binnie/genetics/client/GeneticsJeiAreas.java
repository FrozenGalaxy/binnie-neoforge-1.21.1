package binnie.genetics.client;

import java.util.Map;

/** JEI-free title geometry shared by the screen hover affordance and optional plugin. */
public final class GeneticsJeiAreas {
    public static final Map<String, Area> AREAS = Map.ofEntries(
            Map.entry("isolator", new Area(330)), Map.entry("sequencer", new Area(226)),
            Map.entry("polymeriser", new Area(278)), Map.entry("inoculator", new Area(266)),
            Map.entry("splicer", new Area(280)), Map.entry("analyser", new Area(220)),
            Map.entry("genepool", new Area(280)), Map.entry("incubator", new Area(228)),
            Map.entry("acclimatiser", new Area(280)));
    private GeneticsJeiAreas() { }
    public record Area(int guiWidth) {
        public Resolved resolve(int titleWidth) {
            int width = Math.max(1, titleWidth);
            return new Resolved((guiWidth - width) / 2, 8, width, 12);
        }
    }
    public record Resolved(int x, int y, int width, int height) {
        public boolean contains(int x, int y) { return x >= this.x && x < this.x + width && y >= this.y && y < this.y + height; }
    }
}
