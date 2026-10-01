package binnie.core.forestry;

import forestry.api.IForestryApi;
import forestry.api.modules.ForestryModuleIds;

public final class ForestryModules {
    private ForestryModules() {}

    public static boolean butterfliesLoaded() {
        return IForestryApi.INSTANCE.getModuleManager().isModuleLoaded(ForestryModuleIds.LEPIDOPTEROLOGY);
    }
}
