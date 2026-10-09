package binnie.core.machine;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Process handlers keyed by the stable machine block ID. */
public class MachineProcessRegistry {
    private static final Map<ResourceLocation, MachineProcessHandler> HANDLERS = new LinkedHashMap<>();

    private MachineProcessRegistry() {}

    public static void register(ResourceLocation machine, MachineProcessHandler handler) {
        MachineProcessHandler previous = HANDLERS.putIfAbsent(machine, handler);
        if (previous != null) throw new IllegalStateException("Duplicate dynamic machine handler for " + machine);
    }

    public static MachineProcessHandler get(ResourceLocation machine) {
        return HANDLERS.get(machine);
    }
}
