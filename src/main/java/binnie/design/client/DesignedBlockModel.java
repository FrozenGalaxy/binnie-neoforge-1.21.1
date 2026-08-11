package binnie.design.client;

import binnie.core.data.DesignData;
import binnie.core.registry.BinnieDataComponents;
import binnie.design.api.DesignedBlockKind;
import binnie.design.block.DesignedBlock;
import binnie.design.block.DesignedBlockEntity;
import binnie.design.block.DesignedBlockItem;
import binnie.design.data.DesignCatalog;
import binnie.design.data.DesignLayoutCatalog;
import com.mojang.math.Transformation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
  * Component/model-data aware baked model for all four 1.12 Design systems.
  *
  * <p>The old mod built ordinary shaded quads with two tint passes. Rebuilding
  * those quads here lets the vanilla block renderer provide real AO, directional
  * light and translucent sorting instead of imitating lighting in a BER.</p>
  */
@SuppressWarnings("deprecation") // BakedModel's 1.21.1 compatibility methods remain required overrides.
public final class DesignedBlockModel implements BakedModel {
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final ItemTransforms BLOCK_TRANSFORMS = new ItemTransforms(
            new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5F / 16F, 0), new Vector3f(0.375F)),
            new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5F / 16F, 0), new Vector3f(0.375F)),
            new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(), new Vector3f(0.4F)),
            new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(), new Vector3f(0.4F)),
            ItemTransform.NO_TRANSFORM,
            new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(), new Vector3f(0.625F)),
            new ItemTransform(new Vector3f(), new Vector3f(0, 3F / 16F, 0), new Vector3f(0.25F)),
            new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(0.5F))
    );

    private final DesignedBlockKind kind;
    private final Map<SpriteKey, TextureAtlasSprite> sprites;
    private final Map<ModelKey, BakedModel> cache = new ConcurrentHashMap<>();
    private final ItemOverrides overrides = new DesignedOverrides();
    private final TextureAtlasSprite fallbackParticle;

    public DesignedBlockModel(DesignedBlockKind kind, Map<ModelResourceLocation, BakedModel> bakedModels) {
        this.kind = kind;
        this.sprites = new HashMap<>();
        for (String pattern : DesignLayoutCatalog.renderPatterns()) {
            for (int pass = 0; pass < 2; pass++) {
                ModelResourceLocation location = ModelResourceLocation.standalone(spriteModel(kind, pattern, pass));
                BakedModel spriteModel = bakedModels.get(location);
                if (spriteModel == null) {
                    throw new IllegalStateException("Missing stitched Design sprite model " + location);
                }
                sprites.put(new SpriteKey(pattern, pass), spriteModel.getParticleIcon());
            }
        }
        this.fallbackParticle = sprite("blank", 0);
    }

    public static ResourceLocation spriteModel(DesignedBlockKind kind, String pattern, int pass) {
        String passName = pass == 0 ? "primary" : "secondary";
        return switch (kind) {
            case WOOD, PANEL -> ResourceLocation.fromNamespaceAndPath("binnietrees", "design_sprite/wood/" + pattern + "_" + passName);
            case GLASS -> ResourceLocation.fromNamespaceAndPath("binnietrees", "design_sprite/glass/" + pattern + "_" + passName);
            case CERAMIC -> ResourceLocation.fromNamespaceAndPath("binniebotany", "design_sprite/ceramic/" + pattern + "_" + passName);
        };
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData modelData, @Nullable RenderType renderType) {
        if (state == null || !(state.getBlock() instanceof DesignedBlock block) || block.kind() != kind) {
            return List.of();
        }
        DesignData data = modelData.get(DesignedBlockEntity.DESIGN_DATA);
        if (data == null) data = DesignedBlockEntity.defaultData(kind);
        Direction facing = state.getValue(DesignedBlock.FACING);
        BakedModel concrete = model(new ModelKey(DesignCatalog.validate(data), facing, false));
        return concrete.getQuads(state, side, random, modelData, renderType);
    }

    private BakedModel model(ModelKey key) {
        return cache.computeIfAbsent(key, this::bake);
    }

    private BakedModel bake(ModelKey key) {
        EnumMap<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) culled.put(direction, new ArrayList<>());

        Vector3f from;
        Vector3f to;
        if (kind.isPanel()) {
            // 1.12 panel thickness. World panels occupy the supported outer
            // face; inventory panels use the old bottom-slab item bounds.
            from = key.inventory ? new Vector3f(0, 0, 0) : new Vector3f(0, 15, 0);
            to = key.inventory ? new Vector3f(16, 1, 16) : new Vector3f(16, 16, 16);
        } else {
            from = new Vector3f(0, 0, 0);
            to = new Vector3f(16, 16, 16);
        }

        var modelState = new SimpleModelState(new Transformation(transform(key.facing, key.data, key.inventory)), false);
        for (Direction localFace : Direction.values()) {
            DesignLayoutCatalog.Layout layout = DesignLayoutCatalog.layout(key.data, localFace);
            float[] uvs = faceUvs(localFace, from, to);
            for (int materialPass = 0; materialPass < 2; materialPass++) {
                int texturePass = layout.inverted() ? 1 - materialPass : materialPass;
                BlockFaceUV uv = new BlockFaceUV(uvs.clone(), 0);
                BlockElementFace face = new BlockElementFace(localFace, materialPass, "", uv);
                BakedQuad quad = FACE_BAKERY.bakeQuad(from, to, face,
                        sprite(layout.pattern(), texturePass), localFace, modelState, null, true);
                culled.get(quad.getDirection()).add(quad);
            }
        }

        DesignLayoutCatalog.Layout particleLayout = DesignLayoutCatalog.layout(key.data, Direction.DOWN);
        int particlePass = particleLayout.inverted() ? 1 : 0;
        TextureAtlasSprite particle = sprite(particleLayout.pattern(), particlePass);
        RenderTypeGroup types = renderTypes(kind);
        return new SimpleBakedModel(List.of(), culled, true, true, true, particle,
                BLOCK_TRANSFORMS, ItemOverrides.EMPTY, types);
    }

    /**
      * Rotation/mirror mapping the 1.12 local designed face (UP) to the stored
      * outward-facing side.
      *
      * <p>{@link FaceBakery} already subtracts the block centre, applies the model
      * transformation, and adds the centre back. This matrix must therefore contain
      * rotation/reflection only. Embedding another +/-0.5 translation here pivots
      * the geometry twice and can move the rendered cube exactly one block away from
      * its block state and collision shape.</p>
      */
    static Matrix4f transform(Direction facing, DesignData data, boolean inventory) {
        Matrix4f matrix = new Matrix4f();
        if (!inventory) {
            switch (facing) {
                case DOWN -> matrix.rotateX((float) Math.PI);
                case NORTH -> matrix.rotateX((float) -Math.PI / 2F);
                case SOUTH -> matrix.rotateX((float) Math.PI / 2F);
                case WEST -> matrix.rotateZ((float) Math.PI / 2F);
                case EAST -> matrix.rotateZ((float) -Math.PI / 2F);
                case UP -> { }
            }
        }
        matrix.rotateY((float) Math.toRadians(90.0 * data.rotation()));
        if (data.mirrored()) matrix.scale(-1.0F, 1.0F, 1.0F);
        return matrix;
    }

    private static float[] faceUvs(Direction face, Vector3f from, Vector3f to) {
        float minU, minV, maxU, maxV;
        switch (face) {
            case NORTH, SOUTH -> { minU = from.x; minV = from.y; maxU = to.x; maxV = to.y; }
            case WEST, EAST -> { minU = from.z; minV = from.y; maxU = to.z; maxV = to.y; }
            case UP, DOWN -> { minU = from.x; minV = from.z; maxU = to.x; maxV = to.z; }
            default -> throw new IllegalStateException("Unexpected face " + face);
        }
        return new float[]{16 - minU, 16 - minV, 16 - maxU, 16 - maxV};
    }

    private TextureAtlasSprite sprite(String pattern, int pass) {
        TextureAtlasSprite sprite = sprites.get(new SpriteKey(pattern, pass));
        return sprite != null ? sprite : fallbackParticle;
    }

    private static RenderTypeGroup renderTypes(DesignedBlockKind kind) {
        if (kind == DesignedBlockKind.GLASS) {
            return new RenderTypeGroup(RenderType.translucent(), Sheets.translucentItemSheet(), Sheets.translucentItemSheet());
        }
        return new RenderTypeGroup(RenderType.cutoutMipped(), Sheets.cutoutBlockSheet(), Sheets.cutoutBlockSheet());
    }

    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return fallbackParticle; }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        DesignData design = data.get(DesignedBlockEntity.DESIGN_DATA);
        if (design == null) return fallbackParticle;
        Direction facing = data.get(DesignedBlockEntity.DESIGN_FACING);
        if (facing == null) facing = Direction.UP;
        return model(new ModelKey(DesignCatalog.validate(design), facing, false)).getParticleIcon();
    }

    @Override public ItemTransforms getTransforms() { return BLOCK_TRANSFORMS; }
    @Override public ItemOverrides getOverrides() { return overrides; }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return kind == DesignedBlockKind.GLASS
                ? ChunkRenderTypeSet.of(RenderType.translucent())
                : ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return List.of(kind == DesignedBlockKind.GLASS ? Sheets.translucentItemSheet() : Sheets.cutoutBlockSheet());
    }

    private final class DesignedOverrides extends ItemOverrides {
        @Nullable
        @Override
        public BakedModel resolve(BakedModel original, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            if (!(stack.getItem() instanceof DesignedBlockItem item) || item.kind() != kind) return original;
            DesignData data = stack.getOrDefault(BinnieDataComponents.DESIGN, DesignedBlockEntity.defaultData(kind));
            return model(new ModelKey(DesignCatalog.validate(data), Direction.UP, true));
        }
    }

    private record SpriteKey(String pattern, int pass) {}
    private record ModelKey(DesignData data, Direction facing, boolean inventory) {}
}
