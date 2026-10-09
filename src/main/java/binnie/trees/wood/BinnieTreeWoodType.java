package binnie.trees.wood;

import com.mojang.authlib.GameProfile;
import forestry.api.arboriculture.IWoodType;
import forestry.api.core.genetics.IGenome;
import binnie.trees.forestry.LegacyTreeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** The 36 1.12 Binnie's Trees wood product types. */
public enum BinnieTreeWoodType implements IWoodType {
    APPLE("apple", 6305064),
    FIG("fig", 13142058),
    BUTTERNUT("butternut", 15510138),
    WHITEBEAM("whitebeam", 13222585),
    ROWAN("rowan", 13610394),
    HEMLOCK("hemlock", 13088108),
    ASH("ash", 16107368),
    ALDER("alder", 12092755),
    BEECH("beech", 14784849),
    HAWTHORN("hawthorn", 13402978),
    BANANA("banana", 9466691),
    YEW("yew", 14722426),
    CYPRESS("cypress", 16169052),
    FIR("fir", 12815444),
    HAZEL("hazel", 13480341),
    HICKORY("hickory", 14333070),
    ELM("elm", 15772004),
    ELDER("elder", 12489337),
    HOLLY("holly", 16512743),
    HORNBEAM("hornbeam", 12818528),
    CEDAR("cedar", 14181940),
    OLIVE("olive", 11578760),
    SWEETGUM("sweetgum", 13997656),
    LOCUST("locust", 12816736),
    PEAR("pear", 12093805),
    MACLURA("maclura", 15970862),
    BRAZILWOOD("brazilwood", 7487063),
    LOGWOOD("logwood", 10762028),
    ROSEWOOD("rosewood", 7738624),
    PURPLEHEART("purpleheart", 5970991),
    IROKO("iroko", 7681024),
    GINGKO("gingko", 16050106),
    EUCALYPTUS("eucalyptus", 16165771),
    BOX("box", 16511430),
    SYZGIUM("syzgium", 15123393),
    PINKIVORY("pinkivory", 15502496);

    private static final Map<String, BinnieTreeWoodType> BY_NAME = new HashMap<>();
    private static final Set<String> FORESTRY_OWNED = Set.of("beech", "olive", "pear");
    static {
        for (BinnieTreeWoodType type : values()) BY_NAME.put(type.name, type);
    }

    private final String name;
    private final int legacyColor;

    BinnieTreeWoodType(String name, int legacyColor) {
        this.name = name;
        this.legacyColor = legacyColor;
    }

    public static BinnieTreeWoodType fromLegacyExpression(String expression) {
        if (!expression.startsWith("EnumETLog.")) return null;
        String legacy = expression.substring(expression.indexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (legacy.equals("eucalyptus2") || legacy.equals("eucalyptus3")) legacy = "eucalyptus";
        if (legacy.equals("pinkivory")) legacy = "pinkivory";
        return BY_NAME.get(legacy);
    }

    public boolean isForestryOwned() {
        return FORESTRY_OWNED.contains(name);
    }

    @Override
    public float getHardness() {
        return 2.0F;
    }

    @Override
    public boolean setDefaultLeaves(LevelAccessor level, BlockPos pos, IGenome genome, RandomSource random,
                                    @Nullable GameProfile owner) {
    // Always keep this tree's species/genome instead of delegating to vanilla oak leaves.
        return LegacyTreeGenerator.placeGeneticLeaves(genome, level, pos);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** 1.12 Design material colour from BinnieTreePlanks. */
    public int legacyColor() {
        return legacyColor;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
