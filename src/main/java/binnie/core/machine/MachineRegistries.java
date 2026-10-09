package binnie.core.machine;

import binnie.core.BinnieCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MachineRegistries {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BinnieCore.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, BinnieCore.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, BinnieCore.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE_MENU = MENUS.register(
            "machine", () -> new MenuType<>((IContainerFactory<MachineMenu>) MachineMenu::fromNetwork, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipe.Serializer> MACHINE_RECIPE_SERIALIZER = SERIALIZERS.register(
            "machine", MachineRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> MACHINE_RECIPE_TYPE = TYPES.register(
            "machine", MachineRegistries::createMachineRecipeType);

    private static RecipeType<MachineRecipe> createMachineRecipeType() {
        return new RecipeType<MachineRecipe>() {
                @Override
                public String toString() {
                    return BinnieCore.MOD_ID + ":machine";
                }
            };
    }

    private MachineRegistries() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
        SERIALIZERS.register(bus);
        TYPES.register(bus);
    }
}
