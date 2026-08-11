package binnie.trees.forestry;

import forestry.api.arboriculture.ITreeGenData;
import forestry.api.core.genetics.IGenome;
import forestry.arboriculture.worldgen.FeatureTree;
import forestry.arboriculture.worldgen.TreeBlockTypeLeaf;
import forestry.arboriculture.worldgen.TreeContour;
import forestry.arboriculture.worldgen.FeatureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
  * Modern, 1.12 tree features for every generator token used by Binnie's Trees 1.12.
  *
  * <p>The old module supplied dozens of small generator subclasses. Keeping one feature class with an explicit
  * token-to-profile table keeps their distinct height/canopy families without silently routing unknown trees to
  * vanilla oak generation.</p>
  */
public final class LegacyTreeFeature extends FeatureTree {
    private static final Map<String, GeneratorProfile> PROFILES = createProfiles();

    private final GeneratorProfile profile;

    public LegacyTreeFeature(ITreeGenData tree, GeneratorProfile profile) {
        super(tree, profile.baseHeight(), profile.heightVariation(), profile.minimumHeight());
        this.profile = profile;
    }

    public static GeneratorProfile profile(String generator) {
        GeneratorProfile profile = PROFILES.get(generator);
        if (profile == null) {
            throw new IllegalArgumentException("Unsupported Binnie's Trees generator token: " + generator);
        }
        return profile;
    }

    public static Set<String> supportedGenerators() {
        return PROFILES.keySet();
    }

    @Override
    protected void generateLeaves(IGenome genome, LevelAccessor level, RandomSource random, TreeBlockTypeLeaf leaf,
                                  TreeContour contour, BlockPos startPos) {
        switch (this.profile.shape()) {
            case ORCHARD -> orchard(level, random, leaf, contour, startPos);
            case SPHERE -> sphere(level, random, leaf, contour, startPos);
            case SHRUB -> shrub(level, leaf, contour, startPos);
            case BANANA -> banana(level, leaf, contour, startPos, 1.5F);
            case PALM -> banana(level, leaf, contour, startPos, 0.5F);
            case CONIFER -> conifer(level, random, leaf, contour, startPos, false);
            case FIR -> conifer(level, random, leaf, contour, startPos, true);
            case MONKEY_PUZZLE -> monkeyPuzzle(level, leaf, contour, startPos);
            case COLUMNAR -> columnar(level, random, leaf, contour, startPos);
            case JUNGLE -> jungle(level, random, leaf, contour, startPos);
            case BROAD -> broad(level, random, leaf, contour, startPos);
            case DENSE -> dense(level, random, leaf, contour, startPos);
            case DEFAULT -> defaultCanopy(level, random, leaf, contour, startPos);
        }
    }

    private void defaultCanopy(LevelAccessor level, RandomSource random, TreeBlockTypeLeaf leaf,
                               TreeContour contour, BlockPos startPos) {
        int y = this.height;
        float width = Math.max(1.55F, this.height * between(random, 0.35F, 0.40F));
        cylinder(level, leaf, contour, startPos, y--, Math.max(this.girth, width - 1.0F));
        cylinder(level, leaf, contour, startPos, y--, width);
        cylinder(level, leaf, contour, startPos, y, Math.max(this.girth, width - 0.5F));
    }

    private void orchard(LevelAccessor level, RandomSource random, TreeBlockTypeLeaf leaf,
                         TreeContour contour, BlockPos startPos) {
        int y = this.height + 1;
        float width = Math.max(2.0F, this.height * between(random, 0.32F, 0.42F));
        cylinder(level, leaf, contour, startPos, y--, Math.max(1.0F, width - 1.25F));
        cylinder(level, leaf, contour, startPos, y--, width - 0.35F);
        cylinder(level, leaf, contour, startPos, y--, width);
        cylinder(level, leaf, contour, startPos, y, Math.max(1.0F, width - 0.65F));
    }

    private void sphere(LevelAccessor level, RandomSource random, TreeBlockTypeLeaf leaf,
                        TreeContour contour, BlockPos startPos) {
        int centerY = Math.max(3, this.height - this.girth);
        int randomGirth = Math.max(1, this.girth);
        int radius = Math.round((2 + random.nextInt(randomGirth)) * (this.height / 4.0F));
        radius = Math.max(2, Math.min(4, radius));
        FeatureHelper.generateSphereFromTreeStartPos(level, startPos.offset(0, centerY, 0), this.girth, radius,
                leaf, FeatureHelper.EnumReplaceMode.AIR, contour);
    }

    private void shrub(LevelAccessor level, TreeBlockTypeLeaf leaf, TreeContour contour, BlockPos startPos) {
        int y = this.height;
        cylinder(level, leaf, contour, startPos, y--, Math.max(1.0F, this.girth));
        cylinder(level, leaf, contour, startPos, y--, this.girth + 1.0F);
        int expansion = 0;
        while (y >= 0) {
            cylinder(level, leaf, contour, startPos, y--, this.girth + 2.0F + expansion++);
        }
    }

    private void banana(LevelAccessor level, TreeBlockTypeLeaf leaf, TreeContour contour, BlockPos startPos,
                        float middleExpansion) {
        int y = this.height + 1;
        cylinder(level, leaf, contour, startPos, y--, Math.max(1.0F, this.girth));
        cylinder(level, leaf, contour, startPos, y--, this.girth + middleExpansion);
        cylinder(level, leaf, contour, startPos, y, Math.max(1.0F, this.girth + middleExpansion - 0.5F));
    }

    private void conifer(LevelAccessor level, RandomSource random, TreeBlockTypeLeaf leaf,
                         TreeContour contour, BlockPos startPos, boolean full) {
        int top = this.height + 1;
        int bottom = Math.max(1, Math.round(this.height * (full ? 0.18F : 0.32F)));
        int depth = Math.max(1, top - bottom);
        for (int y = top; y >= bottom; y--) {
            float progress = (top - y) / (float) depth;
            float radius = this.girth + 0.4F + progress * (full ? 3.1F : 2.4F);
            if ((y & 1) == 0) {
                radius -= 0.45F;
            }
            radius *= between(random, 0.90F, 1.08F);
            cylinder(level, leaf, contour, startPos, y, Math.max(1.0F, radius));
        }
    }

    private void monkeyPuzzle(LevelAccessor level, TreeBlockTypeLeaf leaf, TreeContour contour, BlockPos startPos) {
        int top = this.height + 1;
        cylinder(level, leaf, contour, startPos, top, this.girth + 0.5F);
        cylinder(level, leaf, contour, startPos, top - 1, this.girth + 2.5F);
        cylinder(level, leaf, contour, startPos, top - 2, this.girth + 3.5F);
        for (int y = top - 4; y > this.height / 2; y -= 2) {
            cylinder(level, leaf, contour, startPos, y, this.girth + 1.25F);
        }
    }

    private void columnar(LevelAccessor level, RandomSource random, TreeBlockTypeLeaf leaf,
                          TreeContour contour, BlockPos startPos) {
        int top = this.height + 2;
        int bottom = Math.max(2, Math.round(this.height * 0.48F));
        cylinder(level, leaf, contour, startPos, top--, this.girth + 0.75F);
        cylinder(level, leaf, contour, startPos, top--, this.girth + 1.75F);
        while (top > bottom) {
            float radius = this.girth + between(random, 1.0F, 2.4F);
            cylinder(level, leaf, contour, startPos, top--, radius);
        }
    }

    private void jungle(LevelAccessor level, RandomSource random, TreeBlockTypeLeaf leaf,
                        TreeContour contour, BlockPos startPos) {
        int top = this.height + 1;
        int bottom = Math.max(1, Math.round(this.height * between(random, 0.32F, 0.48F)));
        float width = Math.max(2.0F, Math.min(6.0F, this.height * between(random, 0.25F, 0.34F)));
        cylinder(level, leaf, contour, startPos, top--, Math.max(1.0F, width - 1.0F));
        cylinder(level, leaf, contour, startPos, top--, width - 0.4F);
        while (top > bottom) {
            cylinder(level, leaf, contour, startPos, top--, width * between(random, 0.82F, 1.10F));
        }
        cylinder(level, leaf, contour, startPos, top, Math.max(1.0F, width - 0.7F));
    }

    private void broad(LevelAccessor level, RandomSource random, TreeBlockTypeLeaf leaf,
                       TreeContour contour, BlockPos startPos) {
        int top = this.height + 1;
        int bottom = Math.max(1, Math.round(this.height * 0.25F));
        float width = Math.max(2.0F, Math.min(7.0F, this.height * between(random, 0.45F, 0.62F)));
        cylinder(level, leaf, contour, startPos, top--, Math.max(1.0F, width * 0.45F));
        cylinder(level, leaf, contour, startPos, top--, width * 0.72F);
        while (top > bottom) {
            cylinder(level, leaf, contour, startPos, top--, width * between(random, 0.82F, 1.0F));
        }
    }

    private void dense(LevelAccessor level, RandomSource random, TreeBlockTypeLeaf leaf,
                       TreeContour contour, BlockPos startPos) {
        int top = this.height + 1;
        int bottom = Math.max(1, Math.round(this.height * 0.35F));
        float width = Math.max(2.0F, this.girth + between(random, 1.4F, 2.7F));
        cylinder(level, leaf, contour, startPos, top--, width - 0.75F);
        while (top >= bottom) {
            cylinder(level, leaf, contour, startPos, top--, width);
        }
    }

    private void cylinder(LevelAccessor level, TreeBlockTypeLeaf leaf, TreeContour contour, BlockPos startPos,
                          int y, float radius) {
        FeatureHelper.generateCylinderFromTreeStartPos(level, leaf, startPos.offset(0, y, 0), this.girth,
                Math.max(0.75F, radius), 1, FeatureHelper.EnumReplaceMode.AIR, contour);
    }

    private static float between(RandomSource random, float minimum, float maximum) {
        return minimum + random.nextFloat() * (maximum - minimum);
    }

    private static Map<String, GeneratorProfile> createProfiles() {
        Map<String, GeneratorProfile> profiles = new HashMap<>();
        add(profiles, "WorldGenAlder.CommonAlder", 5, 2, Shape.DENSE);
        add(profiles, "WorldGenApple.FloweringCrabapple", 3, 6, Shape.ORCHARD);
        add(profiles, "WorldGenApple.OrchardApple", 3, 6, Shape.ORCHARD);
        add(profiles, "WorldGenApple.PrairieCrabapple", 4, 4, Shape.ORCHARD);
        add(profiles, "WorldGenApple.SweetCrabapple", 4, 4, Shape.ORCHARD);
        add(profiles, "WorldGenAsh.CommonAsh", 5, 2, Shape.BROAD);
        add(profiles, "WorldGenBanana", 6, 1, Shape.BANANA);
        add(profiles, "WorldGenBeech.CommonBeech", 5, 3, Shape.DENSE);
        add(profiles, "WorldGenBeech.CopperBeech", 5, 3, Shape.DENSE);
        add(profiles, "WorldGenConifer.Cedar", 6, 2, Shape.CONIFER);
        add(profiles, "WorldGenConifer.Cypress", 6, 2, Shape.CONIFER);
        add(profiles, "WorldGenConifer.LoblollyPine", 6, 2, Shape.CONIFER);
        add(profiles, "WorldGenConifer.MonkeyPuzzle", 9, 2, Shape.MONKEY_PUZZLE);
        add(profiles, "WorldGenConifer.WesternHemlock", 7, 3, Shape.CONIFER);
        add(profiles, "WorldGenConifer.Yew", 5, 2, Shape.CONIFER);
        add(profiles, "WorldGenEucalyptus.RainbowGum", 7, 3, Shape.COLUMNAR);
        add(profiles, "WorldGenEucalyptus.RoseGum", 9, 3, Shape.COLUMNAR);
        add(profiles, "WorldGenEucalyptus.SwampGum", 14, 3, Shape.COLUMNAR);
        add(profiles, "WorldGenFir.BalsamFir", 6, 2, Shape.FIR);
        add(profiles, "WorldGenFir.DouglasFir", 7, 3, Shape.FIR);
        add(profiles, "WorldGenFir.SilverFir", 7, 3, Shape.FIR);
        add(profiles, "WorldGenHolly.Holly", 4, 2, Shape.DENSE);
        add(profiles, "WorldGenJungle.BrazilNut", 7, 1, Shape.JUNGLE);
        add(profiles, "WorldGenJungle.Brazilwood", 4, 2, Shape.JUNGLE);
        add(profiles, "WorldGenJungle.Coffee", 3, 1, Shape.DENSE);
        add(profiles, "WorldGenJungle.Logwood", 4, 2, Shape.JUNGLE);
        add(profiles, "WorldGenJungle.OldFustic", 5, 2, Shape.JUNGLE);
        add(profiles, "WorldGenJungle.OsangeOrange", 5, 1, Shape.JUNGLE);
        add(profiles, "WorldGenJungle.Purpleheart", 7, 2, Shape.JUNGLE);
        add(profiles, "WorldGenJungle.Rosewood", 6, 2, Shape.JUNGLE);
        add(profiles, "WorldGenLazy.Tree", 4, 1, Shape.DEFAULT);
        add(profiles, "WorldGenLemon", 6, 3, Shape.SPHERE);
        add(profiles, "WorldGenMaple.RedMaple", 5, 2, Shape.BROAD);
        add(profiles, "WorldGenPalm.Coconut", 6, 1, Shape.PALM);
        add(profiles, "WorldGenPlum", 6, 3, Shape.SPHERE);
        add(profiles, "WorldGenPoplar.Aspen", 5, 2, Shape.COLUMNAR);
        add(profiles, "WorldGenShrub.Shrub", 1, 1, 1, Shape.SHRUB);
        add(profiles, "WorldGenSorbus.Rowan", 5, 3, Shape.BROAD);
        add(profiles, "WorldGenSorbus.Whitebeam", 5, 3, Shape.BROAD);
        add(profiles, "WorldGenTree2", 5, 2, Shape.DEFAULT);
        add(profiles, "WorldGenTree2.Box", 3, 1, Shape.DENSE);
        add(profiles, "WorldGenTree2.Clove", 4, 1, Shape.DENSE);
        add(profiles, "WorldGenTree2.Gingko", 6, 2, Shape.BROAD);
        add(profiles, "WorldGenTree2.Iroko", 6, 2, Shape.BROAD);
        add(profiles, "WorldGenTree2.Locust", 6, 2, Shape.BROAD);
        add(profiles, "WorldGenTree2.Olive", 4, 1, Shape.DEFAULT);
        add(profiles, "WorldGenTree2.Pear", 3, 1, Shape.ORCHARD);
        add(profiles, "WorldGenTree2.Sweetgum", 5, 1, Shape.BROAD);
        add(profiles, "WorldGenTree3.AcornOak", 6, 2, Shape.BROAD);
        add(profiles, "WorldGenTree3.Elder", 4, 1, Shape.DENSE);
        add(profiles, "WorldGenTree3.Elm", 6, 2, Shape.BROAD);
        add(profiles, "WorldGenTree3.Hawthorn", 4, 1, Shape.DENSE);
        add(profiles, "WorldGenTree3.Hazel", 5, 1, Shape.DENSE);
        add(profiles, "WorldGenTree3.Hornbeam", 5, 2, Shape.BROAD);
        add(profiles, "WorldGenTree3.Pecan", 6, 2, Shape.BROAD);
        add(profiles, "WorldGenTree3.Sallow", 4, 1, Shape.DENSE);
        add(profiles, "WorldGenTree3.Sycamore", 6, 2, Shape.BROAD);
        add(profiles, "WorldGenTropical.Mango", 5, 1, Shape.JUNGLE);
        add(profiles, "WorldGenWalnut.Butternut", 6, 3, Shape.BROAD);
        return Map.copyOf(profiles);
    }

    private static void add(Map<String, GeneratorProfile> profiles, String token, int baseHeight,
                            int variation, Shape shape) {
        add(profiles, token, baseHeight, variation, 3, shape);
    }

    private static void add(Map<String, GeneratorProfile> profiles, String token, int baseHeight,
                            int variation, int minimumHeight, Shape shape) {
        GeneratorProfile previous = profiles.put(token,
                new GeneratorProfile(token, baseHeight, variation, minimumHeight, shape));
        if (previous != null) {
            throw new IllegalStateException("Duplicate Binnie's Trees generator profile: " + token);
        }
    }

    public record GeneratorProfile(String token, int baseHeight, int heightVariation, int minimumHeight, Shape shape) {
        public GeneratorProfile {
            if (baseHeight < 1 || heightVariation < 1 || minimumHeight < 1) {
                throw new IllegalArgumentException("Invalid tree generator dimensions for " + token);
            }
        }
    }

    public enum Shape {
        DEFAULT,
        ORCHARD,
        SPHERE,
        SHRUB,
        BANANA,
        PALM,
        CONIFER,
        FIR,
        MONKEY_PUZZLE,
        COLUMNAR,
        JUNGLE,
        BROAD,
        DENSE
    }
}
