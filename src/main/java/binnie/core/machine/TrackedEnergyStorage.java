package binnie.core.machine;

import net.neoforged.neoforge.energy.EnergyStorage;

final class TrackedEnergyStorage extends EnergyStorage {
    private final Runnable changed;

    TrackedEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable changed) {
        super(capacity, maxReceive, maxExtract);
        this.changed = changed;
    }

    int maxReceive() {
        return maxReceive;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (!simulate && received > 0) changed.run();
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (!simulate && extracted > 0) changed.run();
        return extracted;
    }
}
