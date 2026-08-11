package binnie.genetics.recipe;

import binnie.genetics.BinnieGenetics;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class GeneticsRecipeRegistries {
    private static final DeferredRegister<IngredientType<?>> INGREDIENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, BinnieGenetics.MOD_ID);

    public static final DeferredHolder<IngredientType<?>, IngredientType<FilledGeneticMaterialIngredient>> FILLED_GENETIC_MATERIAL =
            INGREDIENTS.register("filled_genetic_material", () -> new IngredientType<>(FilledGeneticMaterialIngredient.CODEC));

    private GeneticsRecipeRegistries() {}

    public static void register(IEventBus bus) {
        INGREDIENTS.register(bus);
    }
}
