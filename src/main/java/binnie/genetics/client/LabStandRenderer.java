package binnie.genetics.client;

import binnie.core.machine.MachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Displays the mounted database on the 1.12 Lab Stand. */
public final class LabStandRenderer implements BlockEntityRenderer<MachineBlockEntity> {
    public LabStandRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MachineBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (!machine.spec().id().getNamespace().equals("binniegenetics")
                || !machine.spec().id().getPath().equals("laboratory_machine")) return;
        ItemStack mounted = machine.inventory().getStackInSlot(MachineBlockEntity.INPUT_SLOT);
        if (mounted.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5D, 1.03D, 0.5D);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
        pose.scale(0.55F, 0.55F, 0.55F);
        Minecraft.getInstance().getItemRenderer().renderStatic(mounted, ItemDisplayContext.FIXED,
                packedLight, packedOverlay, pose, buffers, machine.getLevel(), 0);
        pose.popPose();
    }
}
