package binnie.genetics.jei;

import binnie.botany.registry.BotanyContent;
import binnie.genetics.client.GeneticsMachineScreen;
import binnie.genetics.client.AcclimatiserScreen;
import binnie.genetics.client.AnalyserScreen;
import binnie.genetics.client.GenePoolScreen;
import binnie.genetics.client.IncubatorScreen;
import binnie.genetics.client.InoculatorScreen;
import binnie.genetics.client.IsolatorScreen;
import binnie.genetics.client.PolymeriserScreen;
import binnie.genetics.client.SequencerScreen;
import binnie.genetics.client.SplicerScreen;
import binnie.genetics.client.GeneticsJeiAreas;
import binnie.genetics.BinnieGenetics;
import binnie.genetics.item.GeneticMaterialItem;
import binnie.genetics.registry.GeneticsContent;
import binnie.genetics.registry.GeneticsMachines;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

@JeiPlugin
public final class GeneticsJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(BinnieGenetics.MOD_ID, "jei");
    private static final Map<String, RecipeType<GeneticsJeiRecipe>> TYPES = createTypes();

    private static Map<String, RecipeType<GeneticsJeiRecipe>> createTypes() {
        LinkedHashMap<String, RecipeType<GeneticsJeiRecipe>> result = new LinkedHashMap<>();
        for (String machine : GeneticsJeiRecipes.MACHINES) {
            result.put(machine, RecipeType.create(BinnieGenetics.MOD_ID, machine, GeneticsJeiRecipe.class));
        }
        return Map.copyOf(result);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> componentInterpreter = new ISubtypeInterpreter<>() {
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
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, GeneticsContent.GENETIC_MATERIAL.get(), componentInterpreter);
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyContent.FLOWER.get(), componentInterpreter);
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyContent.SEED.get(), componentInterpreter);
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, BotanyContent.POLLEN.get(), componentInterpreter);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        for (String machine : GeneticsJeiRecipes.MACHINES) {
            registration.addRecipeCategories(new GeneticsJeiCategory(machine, TYPES.get(machine), gui));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        GeneticsJeiRecipes.build().forEach((machine, recipes) -> registration.addRecipes(TYPES.get(machine), recipes));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (String machine : GeneticsJeiRecipes.MACHINES) {
            registration.addRecipeCatalyst(GeneticsMachines.REGISTRY.block(machine).get(), TYPES.get(machine));
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registerHandler(registration, IsolatorScreen.class);
        registerHandler(registration, SequencerScreen.class);
        registerHandler(registration, PolymeriserScreen.class);
        registerHandler(registration, InoculatorScreen.class);
        registerHandler(registration, SplicerScreen.class);
        registerHandler(registration, AnalyserScreen.class);
        registerHandler(registration, GenePoolScreen.class);
        registerHandler(registration, IncubatorScreen.class);
        registerHandler(registration, AcclimatiserScreen.class);
    }

    private static <S extends GeneticsMachineScreen> void registerHandler(IGuiHandlerRegistration registration, Class<S> screenClass) {
        registration.addGuiContainerHandler(screenClass, new mezz.jei.api.gui.handlers.IGuiContainerHandler<>() {
            @Override
            public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(S screen) {
                return screen.ledgerAreas();
            }

            @Override
            public java.util.Collection<IGuiClickableArea> getGuiClickableAreas(S screen, double mouseX, double mouseY) {
                String path = screen.getMenu().machineId().getPath();
                RecipeType<GeneticsJeiRecipe> type = TYPES.get(path);
                if (type == null) return java.util.List.of();
                GeneticsJeiAreas.Area descriptor = GeneticsJeiAreas.AREAS.get(path);
                if (descriptor == null) return java.util.List.of();
                var area = descriptor.resolve(net.minecraft.client.Minecraft.getInstance().font.width(screen.getTitle()));
                return java.util.List.of(IGuiClickableArea.createBasic(area.x(), area.y(), area.width(), area.height(), type));
            }
        });
    }

    public static Map<String, RecipeType<GeneticsJeiRecipe>> recipeTypes() {
        return TYPES;
    }

}
