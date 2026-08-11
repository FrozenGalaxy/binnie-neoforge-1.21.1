package binnie.core.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MachineCapabilityTest {
    @Test
    void extractionOnlyItemViewRejectsDirectMutationWithoutThrowing() {
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND, 3));
        MachineSidedItemHandler output = new MachineSidedItemHandler(inventory, new int[]{1}, false, true);

        assertDoesNotThrow(() -> output.setStackInSlot(0, new ItemStack(Items.DIRT)));
        assertTrue(output.getStackInSlot(0).is(Items.DIAMOND));
        assertEquals(3, output.extractItem(0, 3, false).getCount());
    }

    @Test
    void unrestrictedFluidViewDrainsOutputThenMatchingInputs() {
        FluidTank primary = new FluidTank(1_000);
        FluidTank secondary = new FluidTank(1_000);
        FluidTank output = new FluidTank(1_000);
        primary.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
        secondary.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
        output.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);

        MachineFluidHandler full = new MachineFluidHandler(primary, secondary, output, MachineFluidHandler.Access.FULL);
        FluidStack drained = full.drain(1_500, IFluidHandler.FluidAction.EXECUTE);

        assertEquals(1_500, drained.getAmount());
        assertEquals(0, output.getFluidAmount());
        assertEquals(500, primary.getFluidAmount());
        assertEquals(1_000, secondary.getFluidAmount());
    }

    @Test
    void sidedFluidViewsKeepInputAndOutputRolesSeparate() {
        FluidTank primary = new FluidTank(1_000);
        FluidTank secondary = new FluidTank(1_000);
        FluidTank output = new FluidTank(1_000);
        output.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);
        MachineFluidHandler input = new MachineFluidHandler(primary, secondary, output, MachineFluidHandler.Access.INPUT);
        MachineFluidHandler outputOnly = new MachineFluidHandler(primary, secondary, output, MachineFluidHandler.Access.OUTPUT);

        assertTrue(input.drain(500, IFluidHandler.FluidAction.EXECUTE).isEmpty());
        assertEquals(500, input.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE));
        assertEquals(0, outputOnly.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE));
        assertEquals(500, outputOnly.drain(500, IFluidHandler.FluidAction.EXECUTE).getAmount());
    }
}
