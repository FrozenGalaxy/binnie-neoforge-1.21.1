package binnie.core.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** Dynamic processor for component-sensitive operations that cannot be represented by static recipe JSON. */
public interface MachineProcessHandler {
    Optional<MachineProcessPlan> createPlan(Level level, MachineBlockEntity machine);

    /**
      * Server-side inventory housekeeping before a process evaluates its
      * inputs. Machine packages use this for reserve/restock and
      * completed-item transfer groups.
      */
    default void beforeTick(Level level, MachineBlockEntity machine) {
    }

    /** Initial progress for a plan whose input already carries partial work. */
    default int initialProgress(MachineBlockEntity machine, MachineProcessPlan plan) { return 0; }

    /** Called exactly once after a dynamic plan is locked and before its first progress tick. */
    default void onStarted(Level level, MachineBlockEntity machine, MachineProcessPlan plan) { }

    /** Returns the current server-authoritative reason this machine cannot work. */
    default MachineProcessError diagnose(Level level, MachineBlockEntity machine) { return MachineProcessError.NONE; }

    /** Called on the terminal tick before inputs are consumed. */
    default void beforeCompletion(Level level, MachineBlockEntity machine, MachineProcessPlan plan) {
    }

    default boolean isItemValid(MachineBlockEntity machine, int slot, ItemStack stack) {
        return slot != MachineBlockEntity.OUTPUT_SLOT;
    }

    /** Highest user-selectable mode for this machine. Zero means no mode selector. */
    default int maxMode(MachineBlockEntity machine) { return 0; }

    /** Called server-side after all inputs are consumed and outputs are inserted, before progress is reset. */
    default void onCompleted(Level level, MachineBlockEntity machine, MachineProcessPlan plan) {
    }

    /**
      * Lets a 1.12-compatible dynamic machine manage its own progressive fluid
      * checks/drains instead of requiring every process fluid up front.
      */
    default boolean controlsFluidProgress() { return false; }

    /** Called before a dynamic progress tick. False pauses progress without cancelling it. */
    default boolean canProgress(Level level, MachineBlockEntity machine, MachineProcessPlan plan) { return true; }

    /** Precise diagnostic used when {@link #canProgress} pauses an otherwise valid plan. */
    default MachineProcessError cannotProgressError(Level level, MachineBlockEntity machine, MachineProcessPlan plan) {
        return MachineProcessError.INVALID_RECIPE;
    }

    /** Called after a successful dynamic progress tick, including the terminal tick. */
    default void onProgress(Level level, MachineBlockEntity machine, MachineProcessPlan plan, int progress, int maxProgress) {
    }

    /** True when a process emits fluid continuously instead of on completion. */
    default boolean controlsFluidOutputProgress() { return false; }

    /**
      * True for processors that have no finite completion time. Their
      * handler performs work attempts directly on each powered
      * tick instead of being coerced into a synthetic timed recipe.
      */
    default boolean handlesIndefiniteProcess(MachineBlockEntity machine) { return false; }

    /** Invoked server-side once per tick for an indefinite processor. */
    default void tickIndefiniteProcess(Level level, MachineBlockEntity machine) {
    }

    /**
      * True for machines whose result is taken directly from a preview
      * slot, instead of produced by the common timed processor.
      */
    default boolean isManualCrafting(MachineBlockEntity machine) { return false; }

    /** Validates and applies a server-authoritative selection for a manual machine. */
    default boolean setManualSelection(MachineBlockEntity machine, int selection) { return false; }

    /** The current manual preview. This must be recomputed from authoritative inputs. */
    default ItemStack getManualPreview(MachineBlockEntity machine) { return ItemStack.EMPTY; }

    /**
      * Attempts the 1.12-style preview-slot action. The returned non-empty
      * stack has already consumed inputs; an empty result leaves state intact.
      */
    default ItemStack takeManualPreview(MachineBlockEntity machine, ItemStack carried) { return ItemStack.EMPTY; }

    /**
      * CraftGUI controls that are neither mode selectors nor designer
      * actions arrive here. The block entity remains the authority: screens
      * only request an action and the handler validates its current process
      * state before changing anything.
      */
    default boolean handleScreenAction(Level level, MachineBlockEntity machine, String action) { return false; }
}
