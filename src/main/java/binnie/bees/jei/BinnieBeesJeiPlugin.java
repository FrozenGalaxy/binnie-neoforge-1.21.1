package binnie.bees.jei;

import binnie.bees.BinnieBees;
import binnie.bees.block.AlvearyPartType;
import binnie.bees.client.screen.AlvearyComponentScreen;
import binnie.bees.client.screen.AlvearyFrameScreen;
import binnie.bees.client.screen.AlvearyHatcheryScreen;
import binnie.bees.client.screen.AlvearyMutatorScreen;
import binnie.bees.client.screen.AlvearyStimulatorScreen;
import binnie.bees.item.HiveFrameItem;
import binnie.core.data.VariantData;
import binnie.core.registry.BinnieDataComponents;
import binnie.bees.registry.BinnieBeesContent;
import binnie.bees.registry.BinnieBeesWorldContent;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.core.platform.util.SpeciesUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Client-only JEI ownership for the one Binnie's Bees component with a recipe-like output. */
@JeiPlugin
public final class BinnieBeesJeiPlugin implements IModPlugin {
    public static final int HATCHERY_ATTEMPT_DENOMINATOR = 2_400;
    public static final RecipeType<AlvearyHatcheryJeiRecipe> HATCHERY =
            RecipeType.create(BinnieBees.MOD_ID, "alveary_hatchery", AlvearyHatcheryJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, "jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> variants = new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return stack.getComponentsPatch();
            }

            @Override
            @SuppressWarnings("deprecation")
            public String getLegacyStringSubtypeInfo(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return stack.getComponentsPatch().toString();
            }
        };
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BinnieBeesContent.HONEY_COMB.get(), variants);
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BinnieBeesContent.HONEY_DROP.get(), variants);
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BinnieBeesContent.PROPOLIS.get(), variants);
    // Crafting recipes carry only Binnie's persistent variant component.
    // Durability and custom-model components are installed by HiveFrameItem
    // when the result is crafted, so including the full component patch
    // here prevented JEI from relating its visible frame stack to that
    // recipe result.
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BinnieBeesContent.HIVE_FRAME.get(), new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return stack.get(BinnieDataComponents.VARIANT);
            }

            @Override
            @SuppressWarnings("deprecation")
            public String getLegacyStringSubtypeInfo(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                VariantData variant = stack.get(BinnieDataComponents.VARIANT);
                return variant == null ? "" : variant.category() + ":" + variant.value();
            }
        });
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BinnieBeesContent.INDUSTRIAL_FRAME.get(), variants);
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BinnieBeesContent.ALVEARY_COMPONENT.get(), variants);
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
    // VariantItem has one registry entry, so explicitly contribute all
    // player-facing 1.12 frame stacks to JEI's visible ingredient list.
        registration.addExtraItemStacks(HiveFrameItem.VARIANTS.stream()
                .map(value -> BinnieBeesContent.HIVE_FRAME.get().createStack(value))
                .toList());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AlvearyHatcheryJeiCategory(HATCHERY, registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(BinnieBeesWorldContent.alveary(AlvearyPartType.HATCHERY).get(), HATCHERY);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var species = SpeciesUtil.BEE_TYPE.get().getDefaultSpecies();
        var individual = species.createIndividual();
        registration.addRecipes(HATCHERY, List.of(new AlvearyHatcheryJeiRecipe(
                ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, "jei/alveary_hatchery/dynamic_bee"),
                individual.createStack(BeeLifeStage.QUEEN), individual.createStack(BeeLifeStage.LARVAE),
                HATCHERY_ATTEMPT_DENOMINATOR)));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registerLedgerAreas(registration, AlvearyMutatorScreen.class);
        registerLedgerAreas(registration, AlvearyFrameScreen.class);
        registerLedgerAreas(registration, AlvearyStimulatorScreen.class);
        registerLedgerAreas(registration, AlvearyHatcheryScreen.class);
    }

    private static <S extends AlvearyComponentScreen> void registerLedgerAreas(IGuiHandlerRegistration registration,
                                                                                 Class<S> screenClass) {
        registration.addGuiContainerHandler(screenClass, new mezz.jei.api.gui.handlers.IGuiContainerHandler<>() {
            @Override public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(S screen) {
                return screen.ledgerAreas();
            }
        });
    }
}
