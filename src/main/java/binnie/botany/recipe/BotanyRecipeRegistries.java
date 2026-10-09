package binnie.botany.recipe;

import binnie.botany.BinnieBotany;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BotanyRecipeRegistries {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BinnieBotany.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<?>> FLOWER_PIGMENT =
            SERIALIZERS.register("flower_pigment", () -> new SimpleCraftingRecipeSerializer<>(FlowerPigmentRecipe::new));

    private BotanyRecipeRegistries() {}

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
