package binnie.design.data;

import binnie.core.data.DesignData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Direction;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Matching six-face EnumDesign layout extracted from the released 1.12 source. */
public class DesignLayoutCatalog {
    public record Layout(String pattern, boolean inverted) {}

    private static final Map<String, Map<Direction, Layout>> DESIGNS;
    private static final Set<String> RENDER_PATTERNS;

    static {
        Map<String, Map<Direction, Layout>> designs = new LinkedHashMap<>();
        Set<String> patterns = new LinkedHashSet<>(DesignCatalog.PATTERNS);
        try (InputStream stream = DesignLayoutCatalog.class.getResourceAsStream("/data/binniedesign/design_layouts.json")) {
            if (stream == null) throw new IllegalStateException("Missing design_layouts.json");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var designEntry : root.getAsJsonObject("designs").entrySet()) {
                EnumMap<Direction, Layout> faces = new EnumMap<>(Direction.class);
                JsonObject faceData = designEntry.getValue().getAsJsonObject();
                for (Direction direction : Direction.values()) {
                    JsonObject layout = faceData.getAsJsonObject(direction.getName());
                    Layout value = new Layout(layout.get("pattern").getAsString(), layout.get("inverted").getAsBoolean());
                    faces.put(direction, value);
                    patterns.add(value.pattern());
                }
                designs.put(designEntry.getKey(), Collections.unmodifiableMap(faces));
            }
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
        DESIGNS = Collections.unmodifiableMap(designs);
        RENDER_PATTERNS = Collections.unmodifiableSet(patterns);
    }

    private DesignLayoutCatalog() {}

    public static Layout layout(DesignData data, Direction face) {
        if (!DesignCatalog.usesLegacyLayout(data)) {
            return new Layout(data.pattern(), false);
        }
        Map<Direction, Layout> faces = DESIGNS.get(data.design());
        if (faces == null) return new Layout("blank", false);
        return faces.getOrDefault(face, new Layout("blank", false));
    }

    public static Set<String> renderPatterns() {
        return RENDER_PATTERNS;
    }
}
