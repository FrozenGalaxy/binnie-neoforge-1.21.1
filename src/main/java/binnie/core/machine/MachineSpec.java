package binnie.core.machine;

import net.minecraft.resources.ResourceLocation;

public record MachineSpec(ResourceLocation id, int defaultDuration, int energyPerTick, int energyCapacity, int fluidCapacity) {
    public MachineSpec {
        if (defaultDuration <= 0 || energyPerTick < 0 || energyCapacity < 0 || fluidCapacity < 0) {
            throw new IllegalArgumentException("Invalid machine limits for " + id);
        }
    }

    public static MachineSpec standard(String namespace, String path) {
        return new MachineSpec(ResourceLocation.fromNamespaceAndPath(namespace, path), 200, 20, 100_000, 16_000);
    }
}
