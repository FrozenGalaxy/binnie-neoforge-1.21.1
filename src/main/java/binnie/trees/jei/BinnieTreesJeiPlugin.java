package binnie.trees.jei;

import binnie.core.client.MachineScreen;
import binnie.design.client.screen.GlassworkerScreen;
import binnie.design.client.screen.PanelworkerScreen;
import binnie.design.client.screen.WoodworkerScreen;
import binnie.trees.client.screen.BreweryScreen;
import binnie.trees.client.screen.DistilleryScreen;
import binnie.trees.client.screen.LumbermillScreen;
import binnie.trees.client.screen.PressScreen;
import binnie.design.data.DesignCatalog;
import binnie.design.data.DesignMaterialCatalog;
import binnie.design.data.DesignerMaterialTags;
import binnie.trees.BinnieTrees;
import binnie.trees.machine.BinnieTreesMachineProcesses;
import binnie.trees.registry.BinnieTreesContent;
import binnie.trees.registry.BinnieTreesMachines;
import binnie.trees.registry.BinnieTreesFluids;
import binnie.core.registry.BinnieDataComponents;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

/** JEI is client-only and reads the same immutable route tables as the processors. */
@JeiPlugin
public final class BinnieTreesJeiPlugin implements IModPlugin {
    private static volatile IJeiRuntime runtime;
    public static final List<String> MACHINES = List.of("press", "brewery", "distillery", "lumbermill", "woodworker", "panelworker", "glassworker");
    public static final Map<String, ClickArea> CLICK_AREAS = Map.of(
            "press", ClickArea.title(194),
            "brewery", ClickArea.title(228),
            "distillery", ClickArea.title(224),
            "lumbermill", ClickArea.title(220),
            "woodworker", ClickArea.title(320),
            "panelworker", ClickArea.title(320),
            "glassworker", ClickArea.title(320));
    public static final ClickArea WORKER_OPERATION_AREA = ClickArea.fixed(92, 50, 18, 18);
    private static final Map<String, RecipeType<BinnieTreesJeiRecipe>> TYPES = types();
    private static Map<String, RecipeType<BinnieTreesJeiRecipe>> types() { Map<String, RecipeType<BinnieTreesJeiRecipe>> m = new LinkedHashMap<>(); for (String n : MACHINES) m.put(n, RecipeType.create(BinnieTrees.MOD_ID, n, BinnieTreesJeiRecipe.class)); return Map.copyOf(m); }
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, "jei"); }
    @Override public void onRuntimeAvailable(IJeiRuntime jeiRuntime) { runtime = jeiRuntime; }
    @Override public void onRuntimeUnavailable() { runtime = null; }

    /** Opens the crafting recipes that produce the selected database relation. */
    public static boolean showCraftingRecipe(ItemStack output) {
        IJeiRuntime current = runtime;
        if (current == null || output.isEmpty()) return false;
        var focus = current.getJeiHelpers().getFocusFactory().createFocus(
                RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, output.copyWithCount(1));
        current.getRecipesGui().show(focus);
        return true;
    }
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> variants = new ISubtypeInterpreter<>() {
            @Override public Object getSubtypeData(ItemStack stack, UidContext context) {
                var beverage = stack.get(BinnieDataComponents.BEVERAGE);
                return beverage != null ? beverage.beverage() : stack.get(BinnieDataComponents.VARIANT);
            }
            @SuppressWarnings("deprecation")
            @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                Object data = getSubtypeData(stack, context);
                return data == null ? "" : data.toString();
            }
        };
        registration.registerSubtypeInterpreter(BinnieTreesContent.FOOD.get(), variants);
        registration.registerSubtypeInterpreter(BinnieTreesContent.ALCOHOL.get(), variants);
        registration.registerSubtypeInterpreter(BinnieTreesContent.COCKTAIL.get(), variants);
        registration.registerSubtypeInterpreter(BinnieTreesContent.JUICE.get(), variants);
        registration.registerSubtypeInterpreter(BinnieTreesContent.LIQUEUR.get(), variants);
        registration.registerSubtypeInterpreter(BinnieTreesContent.MISC_FLUID.get(), variants);
        registration.registerSubtypeInterpreter(BinnieTreesContent.SPIRIT.get(), variants);
    }
    @Override
    public <T> void registerFluidSubtypes(ISubtypeRegistration registration, mezz.jei.api.helpers.IPlatformFluidHelper<T> helper) {
        registration.registerSubtypeInterpreter(NeoForgeTypes.FLUID_STACK,
                BinnieTreesFluids.beverageFluid(),
                new ISubtypeInterpreter<FluidStack>() {
                    @Override public Object getSubtypeData(FluidStack stack, UidContext context) {
                        var data = BinnieTreesFluids.data(stack);
                        return data == null ? "" : data.beverage();
                    }
                    @SuppressWarnings("deprecation")
                    @Override public String getLegacyStringSubtypeInfo(FluidStack stack, UidContext context) {
                        return (String) getSubtypeData(stack, context);
                    }
                });
    }
    @Override public void registerCategories(IRecipeCategoryRegistration r) { var g = r.getJeiHelpers().getGuiHelper(); for (String n : MACHINES) r.addRecipeCategories(new BinnieTreesJeiCategory(n, TYPES.get(n), g)); }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r) { for (String n : MACHINES) r.addRecipeCatalyst(BinnieTreesMachines.REGISTRY.block(n).get(), TYPES.get(n)); }
    @Override public void registerRecipes(IRecipeRegistration r) { build().forEach((n, v) -> r.addRecipes(TYPES.get(n), v)); }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration r) {
        r.addGuiContainerHandler(PressScreen.class, handler());
        r.addGuiContainerHandler(BreweryScreen.class, handler());
        r.addGuiContainerHandler(DistilleryScreen.class, handler());
        r.addGuiContainerHandler(LumbermillScreen.class, handler());
        r.addGuiContainerHandler(WoodworkerScreen.class, handler());
        r.addGuiContainerHandler(PanelworkerScreen.class, handler());
        r.addGuiContainerHandler(GlassworkerScreen.class, handler());
    }
    private static <S extends MachineScreen> mezz.jei.api.gui.handlers.IGuiContainerHandler<S> handler() {
        return new mezz.jei.api.gui.handlers.IGuiContainerHandler<>() { @Override public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(S s) { return s.ledgerAreas(); }
            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(S s, double x, double y) {
            String n = s.getMenu().machineId().getPath();
            RecipeType<BinnieTreesJeiRecipe> t = TYPES.get(n);
            ClickArea descriptor = CLICK_AREAS.get(n);
            if (t == null || descriptor == null) return List.of();
            ClickArea title = descriptor.resolve(Minecraft.getInstance().font.width(s.getTitle()));
            IGuiClickableArea titleArea = IGuiClickableArea.createBasic(title.x(), title.y(), title.width(), title.height(), t);
            return n.endsWith("worker")
                    ? List.of(titleArea, IGuiClickableArea.createBasic(WORKER_OPERATION_AREA.x(),
                            WORKER_OPERATION_AREA.y(), WORKER_OPERATION_AREA.width(),
                            WORKER_OPERATION_AREA.height(), t))
                    : List.of(titleArea);
        }};
    }
    private static Map<String,List<BinnieTreesJeiRecipe>> build() {
        Map<String,List<BinnieTreesJeiRecipe>> r = new LinkedHashMap<>(); for (String n : MACHINES) r.put(n,new ArrayList<>());
        BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(stack -> stack.is(ItemTags.LOGS))
                .sorted(java.util.Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()))
                .forEach(log -> {
                    ItemStack planks = BinnieTreesMachineProcesses.lumbermillOutput(log);
                    if (!planks.isEmpty()) add(r,"lumbermill",BuiltInRegistries.ITEM.getKey(log.getItem()).toString().replace(':','/'),
                            List.of(log),List.of(),List.of(),List.of(planks,new ItemStack(BinnieTreesContent.BARK.get(),2),new ItemStack(BinnieTreesContent.SAWDUST.get(),2)),
                            List.of(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,300)),List.of(),30,30,
                            "5 mB minimum to progress; drains up to 10 mB per tick (300 mB nominal total)");
                });
        BinnieTreesMachineProcesses.fruitPressOperations().forEach(operation -> add(r,"press",operation.inputVariant(),
                List.of(BinnieTreesContent.FOOD.get().createStack(operation.inputVariant())),List.of(),List.of(),List.of(),List.of(),
                List.of(BinnieTreesMachineProcesses.beverageStack("juice",operation.juice(),operation.outputAmount())),50,20,
                "Forestry squeezer provenance: " + operation.sourceSqueezerTime() + " ticks, mulch " + operation.sourceMulchChance() + "%"
                        + (operation.sourceWasSeedOil() ? "; source seed oil amount doubled for Fruit Press" : "")));
        BinnieTreesMachineProcesses.breweryFermentations().forEach(operation -> {
            String input = operation.inputBeverage().substring(operation.inputBeverage().lastIndexOf('/') + 1);
            add(r, "brewery", "fermentation_" + input, List.of(), List.of(),
                    List.of(new ItemStack(BinnieTreesContent.YEAST.get())), List.of(),
                    List.of(BinnieTreesMachineProcesses.beverageStack("juice", input, 1000)),
                    List.of(BinnieTreesMachineProcesses.beverageStack("alcohol", operation.outputAlcohol(), 1000)),
                    800, 20, "Source BreweryRecipe: one bucket, standard yeast, no grains or ingredient");
        });
        BinnieTreesMachineProcesses.breweryGrainOperations().forEach(operation -> {
            ItemStack grain = breweryGrain(operation.grain());
            ItemStack yeast = new ItemStack("lager_yeast".equals(operation.yeast())
                    ? BinnieTreesContent.LAGER_YEAST.get() : BinnieTreesContent.YEAST.get());
            List<ItemStack> inputs = operation.ingredient().isEmpty()
                    ? List.of(grain.copyWithCount(2)) : List.of(grain.copyWithCount(2), new ItemStack(BinnieTreesContent.HOPS.get()));
            add(r, "brewery", "grain_" + operation.outputAlcohol(), inputs, List.of(), List.of(yeast), List.of(),
                    List.of(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000)),
                    List.of(BinnieTreesMachineProcesses.beverageStack("alcohol", operation.outputAlcohol(), 1000)),
                    800, 20, "Source BrewedGrainRecipe: at least two matching grains; all occupied source crafting slots are consumed");
        });
        var distillation = BinnieTreesMachineProcesses.distillationRoutes();
        for (int index = 0; index < distillation.size(); index++) {
            var route = distillation.get(index);
            BinnieTreesMachineProcesses.BeverageIdentity identity = BinnieTreesMachineProcesses.beverageIdentity(route.inputBeverage());
            String category = identity.category(), value = identity.value();
            add(r, "distillery", String.format(java.util.Locale.ROOT, "%03d_%s_%d", index, value, route.mode()),
                    List.of(), List.of(), List.of(), List.of(),
                    List.of(BinnieTreesMachineProcesses.beverageStack(category, value, 1000)),
                    List.of(BinnieTreesMachineProcesses.beverageStack("spirit", route.outputSpirit(), route.outputAmount())),
                    2000 + 800 * route.mode(), 2, route.mode(), "distillery level " + (route.mode() + 1));
        }
        addDesignerRecipe(r, "woodworker", DesignerMaterialTags.WOOD,
                DesignMaterialCatalog.woodMaterialStacks(), new ItemStack(BinnieTreesContent.WOOD_WAX.get()));
        addDesignerRecipe(r, "panelworker", DesignerMaterialTags.WOOD,
                DesignMaterialCatalog.woodMaterialStacks(), new ItemStack(BinnieTreesContent.WOOD_WAX.get()));
        addDesignerRecipe(r, "glassworker", DesignerMaterialTags.GLASS,
                DesignMaterialCatalog.glassMaterialStacks(), new ItemStack(BinnieTreesContent.GLASS_FITTING.get()));
        r.replaceAll((k,v)->List.copyOf(v)); return Map.copyOf(r);
    }
    private static void addDesignerRecipe(Map<String,List<BinnieTreesJeiRecipe>> all, String machine,
                                          net.minecraft.tags.TagKey<net.minecraft.world.item.Item> materialTag,
                                          List<ItemStack> materials, ItemStack adhesive) {
        if (materials.isEmpty()) return;
        ArrayList<ItemStack> outputs = new ArrayList<>(DesignCatalog.SELECTABLE_DESIGNS.size());
        for (int index = 0; index < DesignCatalog.SELECTABLE_DESIGNS.size(); index++) {
            ItemStack first = materials.get(index % materials.size());
            ItemStack second = materials.get((index * 37 + 1) % materials.size());
            ItemStack output = BinnieTreesMachineProcesses.designerOutput(
                    machine, first, second, DesignCatalog.SELECTABLE_DESIGNS.get(index));
            if (!output.isEmpty()) outputs.add(output);
        }
        all.get(machine).add(new BinnieTreesJeiRecipe(
                ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, "jei/" + machine + "/operation"),
                machine, List.of(), List.of(), List.of(adhesive), List.of(), List.of(), List.of(),
                0, 0, -1,
                "Manual designer: the two tag-backed material slots accept every source material; "
                        + "consumes one primary, one adhesive, and secondary except for Blank",
                List.of(Ingredient.of(materialTag), Ingredient.of(materialTag)),
                List.of(materials, materials), List.of(List.copyOf(outputs))));
    }
    private static ItemStack breweryGrain(String grain) {
        return new ItemStack(switch (grain) {
            case "barley" -> BinnieTreesContent.GRAIN_BARLEY.get();
            case "roasted" -> BinnieTreesContent.GRAIN_ROASTED.get();
            case "wheat" -> BinnieTreesContent.GRAIN_WHEAT.get();
            case "rye" -> BinnieTreesContent.GRAIN_RYE.get();
            case "corn" -> BinnieTreesContent.GRAIN_CORN.get();
            default -> throw new IllegalArgumentException("Unknown source grain " + grain);
        });
    }
    private static void add(Map<String,List<BinnieTreesJeiRecipe>> all,String machine,String id,List<ItemStack> in,List<ItemStack> cat,List<ItemStack> reagent,List<ItemStack> out,List<FluidStack> fin,List<FluidStack> fout,int duration,int energy,String detail) {
        add(all, machine, id, in, cat, reagent, out, fin, fout, duration, energy, -1, detail);
    }
    private static void add(Map<String,List<BinnieTreesJeiRecipe>> all,String machine,String id,List<ItemStack> in,List<ItemStack> cat,List<ItemStack> reagent,List<ItemStack> out,List<FluidStack> fin,List<FluidStack> fout,int duration,int energy,int mode,String detail) {
        all.get(machine).add(new BinnieTreesJeiRecipe(ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID,"jei/"+machine+"/"+id),machine,in,cat,reagent,out,fin,fout,duration,energy,mode,detail,
                List.of(), List.of(), List.of()));
    }
    public record ClickArea(int x, int y, int width, int height, int titleGuiWidth) {
        public static ClickArea title(int guiWidth) { return new ClickArea(0, 8, 0, 12, guiWidth); }
        public static ClickArea fixed(int x, int y, int width, int height) { return new ClickArea(x, y, width, height, 0); }
        public boolean isTitle() { return titleGuiWidth > 0; }
        public ClickArea resolve(int titleWidth) {
            if (!isTitle()) return this;
            int width = Math.max(1, titleWidth);
            return fixed((titleGuiWidth - width) / 2, y, width, height);
        }
    }
}
