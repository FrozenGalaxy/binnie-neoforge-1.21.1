package binnie.core.machine;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Immutable, recomputable description of one dynamic machine operation. */
public record MachineProcessPlan(
        ResourceLocation id,
        ItemStack output,
        ItemStack secondaryOutput,
        ItemStack tertiaryOutput,
        int duration,
        int energyPerTick,
        int inputCount,
        int catalystCount,
        int reagentCount,
        ItemStack catalystAfter,
        ItemStack reagentAfter,
        FluidStack primaryFluidInput,
        FluidStack secondaryFluidInput,
        FluidStack fluidOutput,
        int[] extraConsumedSlots
) {
    public MachineProcessPlan {
        output = output.copy();
        secondaryOutput = secondaryOutput.copy();
        tertiaryOutput = tertiaryOutput.copy();
        catalystAfter = catalystAfter.copy();
        reagentAfter = reagentAfter.copy();
        primaryFluidInput = primaryFluidInput.copy();
        secondaryFluidInput = secondaryFluidInput.copy();
        fluidOutput = fluidOutput.copy();
        extraConsumedSlots = extraConsumedSlots.clone();
        if ((output.isEmpty() && secondaryOutput.isEmpty() && tertiaryOutput.isEmpty() && fluidOutput.isEmpty())
                || duration <= 0 || energyPerTick < 0
                || inputCount < 0 || catalystCount < 0 || reagentCount < 0) {
            throw new IllegalArgumentException("Invalid dynamic process " + id);
        }
    }

    public MachineProcessPlan(ResourceLocation id, ItemStack output, ItemStack secondaryOutput, ItemStack tertiaryOutput,
                              int duration, int energyPerTick, int inputCount, int catalystCount, int reagentCount,
                              ItemStack catalystAfter, ItemStack reagentAfter, FluidStack primaryFluidInput,
                              FluidStack secondaryFluidInput, FluidStack fluidOutput) {
        this(id, output, secondaryOutput, tertiaryOutput, duration, energyPerTick, inputCount, catalystCount, reagentCount,
                catalystAfter, reagentAfter, primaryFluidInput, secondaryFluidInput, fluidOutput, new int[0]);
    }

    public static MachineProcessPlan simple(ResourceLocation id, ItemStack output, int duration, int energyPerTick,
                                            int inputCount, int catalystCount) {
        return new MachineProcessPlan(id, output, ItemStack.EMPTY, ItemStack.EMPTY, duration, energyPerTick,
                inputCount, catalystCount, 0, ItemStack.EMPTY, ItemStack.EMPTY,
                FluidStack.EMPTY, FluidStack.EMPTY, FluidStack.EMPTY);
    }
}
