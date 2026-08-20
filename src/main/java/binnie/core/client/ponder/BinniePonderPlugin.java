package binnie.core.client.ponder;

import binnie.core.BinnieCore;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class BinniePonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return BinnieCore.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        BinniePonderScenes.register(helper);
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("required_inputs", "Use %s.");
        helper.registerSharedText("fluid_supply", "Fluid supply: %s");
        helper.registerSharedText("process_output", "Process output: %s");
    }
}
