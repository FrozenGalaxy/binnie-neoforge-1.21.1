package binnie.core.client;

import binnie.core.ponder.PonderFluidPipeBlock;
import binnie.core.ponder.PonderFluidPipeBlockEntity;
import binnie.core.ponder.PonderFluidSourceBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Renders any assigned fluid with its own animated atlas sprite and tint. */
public final class PonderFluidPipeRenderer implements BlockEntityRenderer<PonderFluidPipeBlockEntity> {
    public PonderFluidPipeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PonderFluidPipeBlockEntity pipe, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        FluidStack fluid = pipe.displayedFluid();
        if (fluid.isEmpty()) return;

        IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(extension.getStillTexture(fluid));
        int tint = extension.getTintColor(fluid);
        int alpha = tint >>> 24;
        if (alpha == 0) alpha = 255;
        int red = tint >> 16 & 255;
        int green = tint >> 8 & 255;
        int blue = tint & 255;

        poseStack.pushPose();
        boolean source = pipe.getBlockState().getBlock() instanceof PonderFluidSourceBlock;
        if (!source) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            switch (pipe.getBlockState().getValue(PonderFluidPipeBlock.FACING)) {
                case NORTH -> { }
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
                case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(270));
            }
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        if (source) {
            renderBox(vertices, pose, sprite, red, green, blue, alpha, packedLight, packedOverlay,
                    1.01F / 16F, 14.99F / 16F,
                    1.01F / 16F, 13.99F / 16F,
                    1.01F / 16F, 14.99F / 16F);
        } else {
            renderBox(vertices, pose, sprite, red, green, blue, alpha, packedLight, packedOverlay,
                    5.51F / 16F, 10.49F / 16F,
                    5.51F / 16F, 10.49F / 16F,
                    0.01F / 16F, 15.99F / 16F);
        }
        poseStack.popPose();
    }

    private static void renderBox(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite sprite,
                                  int red, int green, int blue, int alpha, int light, int overlay,
                                  float minX, float maxX, float minY, float maxY, float start, float end) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();

        quad(out, pose, minX, minY, start, maxX, minY, start, maxX, maxY, start, minX, maxY, start,
                u0, u1, v0, v1, 0, 0, -1, red, green, blue, alpha, light, overlay);
        quad(out, pose, maxX, minY, end, minX, minY, end, minX, maxY, end, maxX, maxY, end,
                u0, u1, v0, v1, 0, 0, 1, red, green, blue, alpha, light, overlay);

        // Fluid sprites share the block atlas, so UV wrapping would leak into adjacent
        // sprites. Repeat the sprite with individual square-ish quads instead, cropping
        // only the final segment. This retains animation without stretching it along Z.
        float tileLength = Math.min(maxX - minX, maxY - minY);
        for (float z0 = start; z0 < end; z0 += tileLength) {
            float z1 = Math.min(z0 + tileLength, end);
            float segmentFraction = (z1 - z0) / tileLength;
            float segmentU1 = u0 + (u1 - u0) * segmentFraction;

            quad(out, pose, minX, minY, z1, minX, minY, z0, minX, maxY, z0, minX, maxY, z1,
                    u0, segmentU1, v0, v1, -1, 0, 0, red, green, blue, alpha, light, overlay);
            quad(out, pose, maxX, minY, z0, maxX, minY, z1, maxX, maxY, z1, maxX, maxY, z0,
                    u0, segmentU1, v0, v1, 1, 0, 0, red, green, blue, alpha, light, overlay);
            quad(out, pose, minX, maxY, z0, maxX, maxY, z0, maxX, maxY, z1, minX, maxY, z1,
                    u0, segmentU1, v0, v1, 0, 1, 0, red, green, blue, alpha, light, overlay);
            quad(out, pose, minX, minY, z1, maxX, minY, z1, maxX, minY, z0, minX, minY, z0,
                    u0, segmentU1, v0, v1, 0, -1, 0, red, green, blue, alpha, light, overlay);
        }
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float u0, float u1, float v0, float v1, float nx, float ny, float nz,
                             int red, int green, int blue, int alpha, int light, int overlay) {
        vertex(out, pose, x1, y1, z1, u0, v1, nx, ny, nz, red, green, blue, alpha, light, overlay);
        vertex(out, pose, x2, y2, z2, u1, v1, nx, ny, nz, red, green, blue, alpha, light, overlay);
        vertex(out, pose, x3, y3, z3, u1, v0, nx, ny, nz, red, green, blue, alpha, light, overlay);
        vertex(out, pose, x4, y4, z4, u0, v0, nx, ny, nz, red, green, blue, alpha, light, overlay);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz, int red, int green, int blue,
                               int alpha, int light, int overlay) {
        out.addVertex(pose.pose(), x, y, z).setColor(red, green, blue, alpha).setUv(u, v)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
