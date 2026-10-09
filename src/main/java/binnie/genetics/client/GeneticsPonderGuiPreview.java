package binnie.genetics.client;

import binnie.genetics.jei.GeneticsJeiRecipe;
import binnie.genetics.machine.GeneticsMachineInventoryLayout;
import binnie.genetics.machine.GeneticsMachineLayout;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedOverlayElementBase;
import net.createmod.ponder.foundation.instruction.FadeInOutInstruction;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * Small, non-interactive preview of the real Genetics machine GUI used from Ponder.
 *
 * <p>The preview reuses the live legacy GUI artwork and machine layout coordinates,
 * while its item and fluid contents come from the same component-bearing examples
 * used by Genetics JEI. It is deliberately only a visual explanation: no fake
 * buttons are clickable and no machine state is mutated.</p>
 */
public class GeneticsPonderGuiPreview {
    private GeneticsPonderGuiPreview() {
    }

    public static void show(SceneBuilder scene, ResourceLocation machineId, GeneticsJeiRecipe recipe, int duration) {
        PreviewElement element = new PreviewElement(machineId, recipe);
        scene.addInstruction(new FadeInOutInstruction(duration) {
            @Override
            protected void show(PonderScene ponderScene) {
                ponderScene.addElement(element);
                element.setVisible(true);
            }

            @Override
            protected void hide(PonderScene ponderScene) {
                element.setVisible(false);
            }

            @Override
            protected void applyFade(PonderScene ponderScene, float fade) {
                element.setFade(fade);
            }
        });
    }

    private static class PreviewElement extends AnimatedOverlayElementBase {
        private final ResourceLocation machineId;
        private final GeneticsJeiRecipe recipe;
        private final GeneticsMachineLayout.Layout layout;

        private PreviewElement(ResourceLocation machineId, GeneticsJeiRecipe recipe) {
            this.machineId = machineId;
            this.recipe = recipe;
            this.layout = GeneticsMachineLayout.forMachine(machineId)
                    .orElseThrow(() -> new IllegalArgumentException("Not a Genetics machine: " + machineId));
        }

        @Override
        public void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float partialTicks, float fade) {
            if (fade < 1.0F / 16.0F) return;

            int originalWidth = layout.width();
            int originalHeight = layout.height();
            // Keep the real GUI readable without allowing it to dominate the scene.
            // Ponder's own title occupies the upper strip, so this sits directly below
            // it in the upper-left rather than covering most of the world view.
            float scale = Math.min(0.42F, Math.min(170.0F / originalWidth, 105.0F / originalHeight));
            int x = 18;
            int y = 62;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 700);
            graphics.pose().scale(scale, scale, scale);
            graphics.setColor(1.0F, 1.0F, 1.0F, fade);

            LegacyGeneticsGui.window(graphics, 0, 0, originalWidth, originalHeight);
            renderMachineFace(graphics, machineId, recipe);
            renderSlots(graphics, machineId, layout, recipe);

            String title = BuiltInRegistries.BLOCK.get(machineId).getName().getString();
            graphics.drawCenteredString(Minecraft.getInstance().font, title, originalWidth / 2, 10, 0xFF555555);

            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.pose().popPose();
        }
    }

    private static void renderSlots(GuiGraphics graphics, ResourceLocation machineId,
                                    GeneticsMachineLayout.Layout layout, GeneticsJeiRecipe recipe) {
        ItemStack input = first(recipe.inputs());
        ItemStack catalyst = first(recipe.catalysts());
        ItemStack reagent = first(recipe.reagents());
        ItemStack output = first(recipe.outputs());

        slot(graphics, layout.inputX(), layout.inputY(), input);
        slot(graphics, layout.catalystX(), layout.catalystY(), catalyst);
        slot(graphics, layout.reagentX(), layout.reagentY(), reagent);
        slot(graphics, layout.outputX(), layout.outputY(), output);
        slot(graphics, layout.secondaryX(), layout.secondaryY(), ItemStack.EMPTY);
        slot(graphics, layout.tertiaryX(), layout.tertiaryY(), ItemStack.EMPTY);

        for (GeneticsMachineInventoryLayout.SlotDefinition definition : GeneticsMachineInventoryLayout.extraSlots(machineId)) {
            if (definition.x() <= -512 || definition.y() <= -512) continue;
            boolean firstInGroup = GeneticsMachineInventoryLayout.slots(machineId, definition.group()).stream()
                    .findFirst().orElse(-1) == definition.slot();
            if (!firstInGroup) {
                slot(graphics, definition.x(), definition.y(), ItemStack.EMPTY);
                continue;
            }

            ItemStack stack = switch (definition.group()) {
                case INPUT_RESERVE -> input;
                case SERUM_RESERVE -> machineId.getPath().equals("polymeriser") ? input : catalyst;
                case SERUM_EXPENDED -> ItemStack.EMPTY;
                case FINISHED -> output;
                case QUEUE -> reagent;
                case INCUBATOR_OUTPUT -> output;
                case ACCLIMATISER -> reagent;
            };
            slot(graphics, definition.x(), definition.y(), stack);
        }
    }

    private static void slot(GuiGraphics graphics, int x, int y, ItemStack stack) {
        if (x <= -512 || y <= -512) return;
        LegacyGeneticsGui.slot(graphics, x - 1, y - 1);
        if (!stack.isEmpty()) graphics.renderItem(stack, x, y);
    }

    private static void renderMachineFace(GuiGraphics graphics, ResourceLocation id, GeneticsJeiRecipe recipe) {
        int progress = 65;
        int max = 100;
        switch (id.getPath()) {
            case "isolator" -> {
                LegacyGeneticsGui.arrow(graphics, "right", 62, 36);
                LegacyGeneticsGui.arrow(graphics, "right", 258, 36);
                LegacyGeneticsGui.arrow(graphics, "up", 258, 53);
                LegacyGeneticsGui.tank(graphics, 16, 32, visibleFluid(recipe.fluidInputs(), 0, 700), 1_000);
                LegacyGeneticsGui.progress(graphics, LegacyGeneticsGui.PROCESS_3, 98, 35,
                        0, 218, 0, 201, 142, 17, progress, max);
                LegacyGeneticsGui.errorState(graphics, 153, 81);
                LegacyGeneticsGui.charge(graphics, 99, 70, 7_500, 0xFFEFEF6F);
            }
            case "sequencer" -> {
                LegacyGeneticsGui.arrow(graphics, "right", 62, 48);
                LegacyGeneticsGui.arrow(graphics, "right", 174, 48);
                LegacyGeneticsGui.grayPanel(graphics, 80, 32, 100, 52);
                LegacyGeneticsGui.sequencerReadout(graphics, Minecraft.getInstance().font, 80, 32,
                        first(recipe.inputs()), progress, max);
                LegacyGeneticsGui.errorState(graphics, 178, 113);
                LegacyGeneticsGui.charge(graphics, 53, 111, 7_500, 0xFFFF9900);
            }
            case "polymeriser" -> {
                LegacyGeneticsGui.progress(graphics, LegacyGeneticsGui.PROCESS, 68, 32,
                        76, 170, 76, 91, 160, 79, progress, max);
                LegacyGeneticsGui.arrow(graphics, "right", 34, 39);
                LegacyGeneticsGui.tank(graphics, 50, 72, visibleFluid(recipe.fluidInputs(), 0, 700), 1_000, true);
                LegacyGeneticsGui.tank(graphics, 50, 98, visibleFluid(recipe.fluidInputs(), 1, 700), 1_000, true);
                LegacyGeneticsGui.errorState(graphics, 244, 97);
                LegacyGeneticsGui.charge(graphics, 139, 63, 8_000, 0xFFFFCC00);
            }
            case "inoculator" -> {
                LegacyGeneticsGui.arrow(graphics, "right", 52, 32);
                LegacyGeneticsGui.arrow(graphics, "right", 88, 32);
                LegacyGeneticsGui.arrow(graphics, "upleft", 194, 52);
                LegacyGeneticsGui.arrow(graphics, "leftdown", 194, 68);
                LegacyGeneticsGui.tank(graphics, 16, 66, visibleFluid(recipe.fluidInputs(), 0, 700), 1_000);
                LegacyGeneticsGui.progress(graphics, LegacyGeneticsGui.PROCESS_2, 34, 56,
                        0, 72, 0, 0, 142, 72, progress, max);
                LegacyGeneticsGui.errorState(graphics, 161, 118);
            }
            case "splicer" -> {
                LegacyGeneticsGui.arrow(graphics, "right", 52, 62);
                LegacyGeneticsGui.arrow(graphics, "down", 132, 54);
                LegacyGeneticsGui.arrow(graphics, "down", 132, 96);
                LegacyGeneticsGui.arrow(graphics, "upleft", 68, 86);
                LegacyGeneticsGui.splicerProgress(graphics, 84, 32, 108, 102, progress, max);
                LegacyGeneticsGui.errorState(graphics, 218, 86);
            }
            case "analyser" -> {
                LegacyGeneticsGui.arrow(graphics, "right", 54, 50);
                LegacyGeneticsGui.arrow(graphics, "right", 150, 50);
                LegacyGeneticsGui.tintedPanel(graphics, 72, 32, 76, 50);
                LegacyGeneticsGui.progress(graphics, LegacyGeneticsGui.PROCESS, 77, 37,
                        0, 51, 66, 51, 66, 40, progress, max);
                LegacyGeneticsGui.errorState(graphics, 160, 95);
                LegacyGeneticsGui.charge(graphics, 63, 93, 7_500, 0xFF9933FF);
            }
            case "genepool" -> {
                LegacyGeneticsGui.arrow(graphics, "right", 80, 53);
                LegacyGeneticsGui.tank(graphics, 16, 32, visibleFluid(recipe.fluidInputs(), 0, 700), 1_000);
                LegacyGeneticsGui.tank(graphics, 246, 32, visibleFluid(recipe.fluidOutputs(), 0, 500), 1_000);
                LegacyGeneticsGui.progress(graphics, LegacyGeneticsGui.PROCESS_3, 116, 51,
                        64, 0, 64, 21, 130, 21, progress, max);
                LegacyGeneticsGui.errorState(graphics, 181, 83);
                LegacyGeneticsGui.charge(graphics, 140, 81, 7_500, 0xFFEFEF6F);
            }
            case "incubator" -> {
                LegacyGeneticsGui.arrow(graphics, "right", 72, 45);
                LegacyGeneticsGui.arrow(graphics, "right", 122, 45);
                LegacyGeneticsGui.tank(graphics, 16, 32, visibleFluid(recipe.fluidInputs(), 0, 800), 1_000);
                LegacyGeneticsGui.tank(graphics, 164, 32, visibleFluid(recipe.fluidOutputs(), 0, 500), 1_000);
                LegacyGeneticsGui.progress(graphics, LegacyGeneticsGui.PROCESS, 80, 38,
                        0, 91, 38, 91, 38, 32, progress, max);
                LegacyGeneticsGui.errorState(graphics, 91, 82);
            }
            case "acclimatiser" -> {
                LegacyGeneticsGui.progress(graphics, LegacyGeneticsGui.PROCESS_3, 65, 28,
                        0, 64, 0, 64, 64, 64, progress, max);
                LegacyGeneticsGui.errorState(graphics, 181, 83);
            }
            default -> { }
        }
    }

    private static ItemStack first(List<ItemStack> stacks) {
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.getFirst();
    }

    /** Keep fluid identity exact while filling enough of the illustrative tank to remain legible. */
    private static FluidStack visibleFluid(List<FluidStack> fluids, int index, int amount) {
        if (index >= fluids.size() || fluids.get(index).isEmpty()) return FluidStack.EMPTY;
        return fluids.get(index).copyWithAmount(amount);
    }
}
