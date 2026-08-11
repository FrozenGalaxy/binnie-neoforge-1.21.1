package binnie.core.machine;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Three-tank machine capability. The unrestricted view supports both process inputs and the output,
 * while sided views can be input-only or output-only without exposing internal tank mutation.
 */
final class MachineFluidHandler implements IFluidHandler {
    enum Access { FULL, INPUT, OUTPUT }

    private final FluidTank primaryInput;
    private final FluidTank secondaryInput;
    private final FluidTank output;
    private final Access access;

    MachineFluidHandler(FluidTank primaryInput, FluidTank secondaryInput, FluidTank output, Access access) {
        this.primaryInput = primaryInput;
        this.secondaryInput = secondaryInput;
        this.output = output;
        this.access = access;
    }

    @Override
    public int getTanks() {
        return 3;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank(tank).getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank(tank).getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return access != Access.OUTPUT && tank < 2 && tank(tank).isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (access == Access.OUTPUT || resource.isEmpty()) return 0;
        int accepted = primaryInput.fill(resource, action);
        if (accepted >= resource.getAmount()) return accepted;
        FluidStack remainder = resource.copyWithAmount(resource.getAmount() - accepted);
        return accepted + secondaryInput.fill(remainder, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (access == Access.INPUT || resource.isEmpty()) return FluidStack.EMPTY;
        if (access == Access.OUTPUT) return output.drain(resource, action);
        return drainMatching(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (access == Access.INPUT || maxDrain <= 0) return FluidStack.EMPTY;
        if (access == Access.OUTPUT) return output.drain(maxDrain, action);
        FluidStack template = firstDrainable();
        return template.isEmpty() ? FluidStack.EMPTY : drainMatching(template.copyWithAmount(maxDrain), action);
    }

    private FluidStack firstDrainable() {
        if (!output.getFluid().isEmpty()) return output.getFluid();
        if (!primaryInput.getFluid().isEmpty()) return primaryInput.getFluid();
        return secondaryInput.getFluid();
    }

    private FluidStack drainMatching(FluidStack requested, FluidAction action) {
        int remaining = requested.getAmount();
        FluidStack drained = FluidStack.EMPTY;
        for (FluidTank candidate : new FluidTank[]{output, primaryInput, secondaryInput}) {
            if (remaining <= 0) break;
            FluidStack present = candidate.getFluid();
            if (present.isEmpty() || !FluidStack.isSameFluidSameComponents(present, requested)) continue;
            FluidStack part = candidate.drain(requested.copyWithAmount(remaining), action);
            if (part.isEmpty()) continue;
            if (drained.isEmpty()) drained = part.copy();
            else drained.grow(part.getAmount());
            remaining -= part.getAmount();
        }
        return drained;
    }

    private FluidTank tank(int index) {
        return switch (index) {
            case 0 -> primaryInput;
            case 1 -> secondaryInput;
            case 2 -> output;
            default -> throw new IndexOutOfBoundsException("Unknown machine tank " + index);
        };
    }
}
