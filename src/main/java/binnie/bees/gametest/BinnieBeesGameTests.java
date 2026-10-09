package binnie.bees.gametest;

import binnie.core.data.VariantData;
import binnie.core.registry.BinnieDataComponents;
import binnie.bees.BinnieBees;
import binnie.bees.block.AlvearyPartType;
import binnie.bees.block.BinnieBeeHiveBlock;
import binnie.bees.block.BinnieBeeHiveDrops;
import binnie.bees.block.BinnieBeesAlvearyBlockEntity;
import binnie.bees.registry.BinnieBeesContent;
import binnie.bees.registry.BinnieBeesWorldContent;
import binnie.bees.item.HiveFrameItem;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.core.IProduct;
import forestry.api.core.machines.ICentrifugeRecipe;
import forestry.api.core.machines.ISqueezerRecipe;
import forestry.apiculture.bees.BeeHousingInventory;
import forestry.core.engine.genetics.ItemGE;
import forestry.core.platform.util.SpeciesUtil;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server-side regression coverage for Binnie's Bees world content and powered alveary parts. */
@GameTestHolder("binniecore")
@PrefixGameTestTemplate(false)
public class BinnieBeesGameTests {
    private BinnieBeesGameTests() {}

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void marbleBeeLineIsAbsentWithoutMarbleBlocks(GameTestHelper helper) {
        TagKey<net.minecraft.world.level.block.Block> marbleBlocks = TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("binniebees", "marble_hive_replaceables"));
        boolean hasMarble = helper.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK)
                .get(marbleBlocks).map(net.minecraft.core.HolderSet::size).orElse(0) > 0;
        if (!hasMarble) {
            Set<String> marbleLine = Set.of("marble", "roman", "greek", "classical");
            helper.assertTrue(SpeciesUtil.BEE_TYPE.get().getAllSpecies().stream()
                            .noneMatch(species -> species.id().getNamespace().equals("binniebees")
                                    && marbleLine.contains(species.id().getPath())),
                    "Marble bee lineage remained registered without marble blocks");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void everyWildHiveUsesItsLegacyDropTable(GameTestHelper helper) {
        Map<BinnieBeeHiveBlock.HiveType, String> primarySpecies = Map.of(
                BinnieBeeHiveBlock.HiveType.WATER, "water",
                BinnieBeeHiveBlock.HiveType.ROCK, "rock",
                BinnieBeeHiveBlock.HiveType.NETHER, "basalt",
                BinnieBeeHiveBlock.HiveType.MARBLE, "marble");
        for (BinnieBeeHiveBlock.HiveType type : BinnieBeeHiveBlock.HiveType.values()) {
            List<BinnieBeeHiveDrops.HiveDrop> table = BinnieBeeHiveDrops.dropTable(type);
            helper.assertTrue(table.size() == 2, type + " hive must have its legacy species and Valiant drops");
            helper.assertTrue(table.get(0).species().equals(ResourceLocation.fromNamespaceAndPath("binniebees", primarySpecies.get(type)))
                            && table.get(0).chance() == 0.80 && table.get(0).ignobleChance() == 0.5,
                    type + " hive has incorrect primary bee odds");
            helper.assertTrue(table.get(1).species().equals(ResourceLocation.fromNamespaceAndPath("forestry", "valiant"))
                            && table.get(1).chance() == 0.03 && table.get(1).ignobleChance() == 0.5,
                    type + " hive has incorrect Valiant odds");

            if (type == BinnieBeeHiveBlock.HiveType.MARBLE
                    && SpeciesUtil.BEE_TYPE.get().getAllSpecies().stream()
                            .noneMatch(species -> species.id().equals(ResourceLocation.fromNamespaceAndPath("binniebees", "marble")))) continue;
            List<ItemStack> drops = BinnieBeeHiveDrops.createDrops(type, helper.getLevel().getRandom(), 0);
            helper.assertTrue(!drops.isEmpty() && drops.size() <= 2, type + " hive produced an invalid number of bee stacks");
            helper.assertTrue(ItemGE.getLifeStage(drops.get(0)) == BeeLifeStage.PRINCESS,
                    type + " hive did not create a princess");
            for (ItemStack drop : drops) {
                helper.assertTrue(table.stream().anyMatch(entry -> entry.species().equals(ItemGE.getIndividual(drop).getSpecies().id())),
                        type + " hive produced a bee outside its legacy drop table");
            }
            if (drops.size() == 2) {
                helper.assertTrue(ItemGE.getLifeStage(drops.get(1)) == BeeLifeStage.DRONE,
                        type + " hive's second bee stack was not a drone");
                helper.assertTrue(drops.get(1).getCount() == 1, type + " hive produced more than one drone");
            }
        }
        helper.succeed();
    }


    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void everyBinnieBeeProducesAndStoresItsRealCombVariant(GameTestHelper helper) {
        Set<String> observed = new HashSet<>();
        int combStacks = 0;

        for (var species : SpeciesUtil.getAllBeeSpecies()) {
            if (!BinnieBees.MOD_ID.equals(species.id().getNamespace())) continue;
            combStacks += inspectCombProducts(helper, species.id().toString(), species.getProducts(), observed);
            combStacks += inspectCombProducts(helper, species.id() + " specialty", species.getSpecialties(), observed);
        }

        helper.assertTrue(combStacks > 100,
                "Too few Binnie's Bees comb products reached the live Forestry species registry: " + combStacks);
        helper.assertTrue(observed.size() > 40, "Live Forestry comb products collapsed to " + observed);
        helper.assertTrue(observed.contains("barren") && observed.contains("coal")
                        && observed.contains("iron") && observed.contains("diamond"),
                "Representative live comb variants are missing: " + observed);
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void blazeCombCentrifugeRecipeUsesItsRealVariantAndLegacyOutputs(GameTestHelper helper) {
        ICentrifugeRecipe recipe = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(FactoryRecipeTypes.CENTRIFUGE.type()).stream()
                .filter(holder -> holder.id().equals(ResourceLocation.fromNamespaceAndPath("binniebees", "centrifuge/blaze_comb")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Binnie's Bees Blaze comb centrifuge recipe"))
                .value();

        ItemStack blazeComb = BinnieBeesContent.HONEY_COMB.get().createStack("blaze");
        helper.assertTrue(recipe.getInput().test(blazeComb),
                "Blaze centrifuge recipe does not match the Blaze comb's variant component");
        helper.assertTrue(recipe.getProcessingTime() == 20 && recipe.getAllProducts().size() == 2,
                "Blaze comb must retain its two source centrifuge products");
        IProduct wax = recipe.getAllProducts().stream().filter(product -> product.createStack().is(net.minecraft.world.item.Items.AIR) == false
                        && product.createStack().is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("forestry", "beeswax"))))
                .findFirst().orElseThrow(() -> new AssertionError("Blaze comb is missing its 1.12 beeswax output"));
        IProduct blazePowder = recipe.getAllProducts().stream().filter(product -> product.createStack().is(Items.BLAZE_POWDER))
                .findFirst().orElseThrow(() -> new AssertionError("Blaze comb is missing its 1.12 blaze-powder output"));
        helper.assertTrue(wax.chance() == 0.75F && blazePowder.chance() == 1.0F,
                "Blaze comb output chances differ from the 1.12 source table");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void sawdustCombUsesThePortedSawdustFallback(GameTestHelper helper) {
        ICentrifugeRecipe recipe = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(FactoryRecipeTypes.CENTRIFUGE.type()).stream()
                .filter(holder -> holder.id().equals(ResourceLocation.fromNamespaceAndPath("binniebees", "centrifuge/sawdust_comb")))
                .findFirst().orElseThrow(() -> new AssertionError("Missing Binnie's Bees sawdust comb centrifuge recipe"))
                .value();
        helper.assertTrue(recipe.getInput().test(BinnieBeesContent.HONEY_COMB.get().createStack("sawdust")),
                "Sawdust centrifuge recipe does not match the sawdust comb variant");
        IProduct sawdust = recipe.getAllProducts().stream()
                .filter(product -> product.createStack().is(net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .get(ResourceLocation.fromNamespaceAndPath("binnietrees", "sawdust"))))
                .findFirst().orElseThrow(() -> new AssertionError("Sawdust comb did not use the ported sawdust fallback"));
        helper.assertTrue(sawdust.chance() == 1.0F,
                "Sawdust comb must retain the source fallback's guaranteed sawdust output");
        helper.succeed();
    }

    /** The 1.12 {@code pulpWood} ore entry is exposed through current common tags. */
    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void sawdustIsPublishedThroughModernCommonTags(GameTestHelper helper) {
        ItemStack sawdust = new ItemStack(binnie.trees.registry.BinnieTreesContent.SAWDUST.get());
        TagKey<net.minecraft.world.item.Item> sawdustTag = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("c", "sawdust"));
        TagKey<net.minecraft.world.item.Item> pulpWoodTag = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("c", "pulp_wood"));
        helper.assertTrue(sawdust.is(sawdustTag), "Binnie's Trees sawdust is missing the modern c:sawdust tag");
        helper.assertTrue(sawdust.is(pulpWoodTag), "Binnie's Trees sawdust is missing c:pulp_wood, the 1.12 pulpWood successor");
        helper.succeed();
    }

    /**
      * Recipe shapes are the active 1.12 definitions: cocoa/clay surround the
      * impregnated frame; cage/soul are shapeless two-input upgrades. Verify
      * that all four result variants survive the data-driven recipe boundary.
      */
    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void legacyHiveFrameCraftingRecipesProduceTheirExactVariants(GameTestHelper helper) {
        assertFrameRecipe(helper, "cocoa_frame", "cocoa");
        assertFrameRecipe(helper, "cage_frame", "cage");
        assertFrameRecipe(helper, "soul_frame", "soul");
        assertFrameRecipe(helper, "clay_frame", "clay");
        helper.succeed();
    }

    private static void assertFrameRecipe(GameTestHelper helper, String recipePath, String variant) {
        CraftingRecipe recipe = helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(holder -> holder.id().equals(ResourceLocation.fromNamespaceAndPath("binniebees", "crafting/" + recipePath)))
                .findFirst().orElseThrow(() -> new AssertionError("Missing 1.12 hive-frame recipe " + recipePath)).value();
        ItemStack result = recipe.getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(BinnieBeesContent.HIVE_FRAME.get()), recipePath + " does not produce an Binnie's Bees hive frame");
        VariantData data = result.get(BinnieDataComponents.VARIANT);
        helper.assertTrue(data != null && "binniebees.hive_frame".equals(data.category()) && variant.equals(data.value()),
                recipePath + " lost its source frame variant: " + data);
    // Crafting JSON supplies the variant component; the item lifecycle
    // supplies its variant-specific runtime durability/model data.
        BinnieBeesContent.HIVE_FRAME.get().onCraftedBy(result, helper.getLevel(), null);
        helper.assertTrue(result.getMaxDamage() == HiveFrameItem.maxDamage(variant),
                recipePath + " did not receive the source " + HiveFrameItem.maxDamage(variant) + "-use frame durability");
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void legacyMaterialCombsKeepTheirPortedDustAndShardOutputs(GameTestHelper helper) {
        assertCombProduct(helper, "coal", "coal_dust", 1.0F);
        assertCombProduct(helper, "iron", "iron_dust", 1.0F);
        assertCombProduct(helper, "diamond", "diamond_shard", 1.0F);
        assertCombProduct(helper, "yellorium", "yellorium_dust", .25F);
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void everyBinnieBeesProcessingVariantHasARealOutput(GameTestHelper helper) {
        List<ICentrifugeRecipe> centrifuge = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(FactoryRecipeTypes.CENTRIFUGE.type()).stream().map(holder -> holder.value()).toList();
        for (String variant : BinnieBeesContent.HONEY_COMB.get().variants()) {
            // This comb is only produced by AE2-gated bee specialties, so it is
            // intentionally not processable when AE2 is absent.
            if (variant.equals("enderpearl") && !ModList.get().isLoaded("ae2")) continue;
            ICentrifugeRecipe recipe = centrifuge.stream()
                    .filter(candidate -> candidate.getInput().test(BinnieBeesContent.HONEY_COMB.get().createStack(variant)))
                    .findFirst().orElseThrow(() -> new AssertionError("Comb variant has no centrifuge recipe: " + variant));
            helper.assertTrue(!recipe.getAllProducts().isEmpty(), "Comb variant has no centrifuge output: " + variant);
            for (String other : BinnieBeesContent.HONEY_COMB.get().variants()) {
                if (!other.equals(variant)) helper.assertTrue(!recipe.getInput().test(BinnieBeesContent.HONEY_COMB.get().createStack(other)),
                        "Comb recipe for " + variant + " also matches " + other);
            }
        }

        List<ISqueezerRecipe> squeezer = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(FactoryRecipeTypes.SQUEEZER.type()).stream().map(holder -> holder.value()).toList();
        assertEverySqueezerVariant(helper, squeezer, BinnieBeesContent.HONEY_DROP.get(), "honey drop");
        assertEverySqueezerVariant(helper, squeezer, BinnieBeesContent.PROPOLIS.get(), "propolis");
        helper.succeed();
    }

    private static void assertEverySqueezerVariant(GameTestHelper helper, List<ISqueezerRecipe> recipes,
                                                   binnie.core.item.VariantItem item, String kind) {
        for (String variant : item.variants()) {
            ISqueezerRecipe recipe = recipes.stream()
                    .filter(candidate -> candidate.getInputs().stream().anyMatch(input -> input.test(item.createStack(variant))))
                    .findFirst().orElseThrow(() -> new AssertionError(kind + " variant has no squeezer recipe: " + variant));
            helper.assertTrue(!recipe.getFluidOutput().createFluidStack().isEmpty()
                            && recipe.getFluidOutput().createFluidStack().getAmount() > 0,
                    kind + " variant has no squeezer output: " + variant);
            for (String other : item.variants()) {
                if (!other.equals(variant)) helper.assertTrue(recipe.getInputs().stream().noneMatch(input -> input.test(item.createStack(other))),
                        kind + " recipe for " + variant + " also matches " + other);
            }
        }
    }

    private static void assertCombProduct(GameTestHelper helper, String comb, String expectedProduct, float chance) {
        ICentrifugeRecipe recipe = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(FactoryRecipeTypes.CENTRIFUGE.type()).stream()
                .filter(holder -> holder.id().equals(ResourceLocation.fromNamespaceAndPath("binniebees", "centrifuge/" + comb + "_comb")))
                .findFirst().orElseThrow(() -> new AssertionError("Missing " + comb + " centrifuge recipe")).value();
        helper.assertTrue(recipe.getInput().test(BinnieBeesContent.HONEY_COMB.get().createStack(comb)),
                comb + " recipe does not match its component-aware comb stack");
        IProduct product = recipe.getAllProducts().stream()
                .filter(candidate -> candidate.createStack().is(net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .get(ResourceLocation.fromNamespaceAndPath("binniebees", expectedProduct))))
                .findFirst().orElseThrow(() -> new AssertionError(comb + " comb is missing " + expectedProduct));
        helper.assertTrue(product.chance() == chance, comb + " product chance differs from the source table");
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void legacyDropAndPropolisSqueezerRecipesAreRegistered(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(FactoryRecipeTypes.SQUEEZER.type());
        ISqueezerRecipe redDrop = recipes.stream()
                .filter(holder -> holder.id().equals(ResourceLocation.fromNamespaceAndPath("binniebees", "squeezer/red_honey_drop")))
                .findFirst().orElseThrow(() -> new AssertionError("Missing legacy red honey-drop squeezer recipe")).value();
        ItemStack red = BinnieBeesContent.HONEY_DROP.get().createStack("red");
        helper.assertTrue(redDrop.getInputs().size() == 1 && redDrop.getInputs().getFirst().test(red),
                "Red honey-drop squeezer recipe does not match its variant");
        helper.assertTrue(redDrop.getProcessingTime() == 10 && redDrop.getFluidOutput().createFluidStack().getAmount() == 200
                        && redDrop.getRemnants().is(Items.RED_DYE) && redDrop.getRemnantsChance() == 1.0F,
                "Red honey-drop recipe differs from the 1.12 honey/dye conversion");

        ISqueezerRecipe waterPropolis = recipes.stream()
                .filter(holder -> holder.id().equals(ResourceLocation.fromNamespaceAndPath("binniebees", "squeezer/water_propolis")))
                .findFirst().orElseThrow(() -> new AssertionError("Missing legacy water propolis squeezer recipe")).value();
        helper.assertTrue(waterPropolis.getInputs().getFirst().test(BinnieBeesContent.PROPOLIS.get().createStack("water"))
                        && waterPropolis.getProcessingTime() == 20
                        && waterPropolis.getFluidOutput().createFluidStack().getAmount() == 500,
                "Water propolis recipe differs from its 1.12 processing table");
        helper.succeed();
    }

    private static int inspectCombProducts(GameTestHelper helper, String owner, List<IProduct> products,
                                           Set<String> observed) {
        int combStacks = 0;
        for (IProduct product : products) {
            ItemStack produced = product.createStack();
            if (!produced.is(BinnieBeesContent.HONEY_COMB.get())) continue;
            combStacks++;

            VariantData expected = produced.get(BinnieDataComponents.VARIANT);
            helper.assertTrue(expected != null, owner + " produced a comb without variant data");
            helper.assertTrue("binniebees.honey_comb".equals(expected.category()),
                    owner + " produced the wrong comb category " + expected);
            helper.assertTrue(BinnieBeesContent.HONEY_COMB.get().variants().contains(expected.value()),
                    owner + " produced an unknown comb variant " + expected.value());
            observed.add(expected.value());

            BeeHousingInventory inventory = new BeeHousingInventory(9);
            helper.assertTrue(inventory.addProduct(produced.copy(), true),
                    owner + " comb could not enter a Forestry bee housing output");
            ItemStack stored = inventory.getItem(BeeHousingInventory.SLOT_PRODUCT_1);
            helper.assertTrue(ItemStack.isSameItemSameComponents(produced, stored),
                    owner + " comb lost its variant during Forestry inventory insertion: "
                            + expected + " -> " + stored.get(BinnieDataComponents.VARIANT));
        }
        return combStacks;
    }

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void transmissionAcceptsEnergyWithoutModifyingBees(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        helper.setBlock(relative, BinnieBeesWorldContent.alveary(AlvearyPartType.TRANSMISSION).get());
        BlockPos absolute = helper.absolutePos(relative);
        BinnieBeesAlvearyBlockEntity transmission =
                (BinnieBeesAlvearyBlockEntity) helper.getLevel().getBlockEntity(absolute);
        helper.assertTrue(transmission != null, "Transmission block entity was not created");

        var energy = Capabilities.EnergyStorage.BLOCK.getCapability(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), transmission, null);
        helper.assertTrue(energy != null, "Transmission did not expose the NeoForge energy capability");
        helper.assertTrue(energy.receiveEnergy(400, false) == 400, "Transmission rejected valid FE input");

        var genome = SpeciesUtil.BEE_TYPE.get().getDefaultSpecies().createIndividual().getGenome();
        helper.assertTrue(transmission.getBeeModifier().modifyProductionSpeed(genome, 1.0F) == 1.0F,
                "Unpowered Transmission modified production");
        transmission.updateServer(1);
        helper.assertTrue(transmission.isTransmissionPowered(), "Transmission did not retain received energy");
    // In 1.12 Transmission was an energy distributor only; it never
    // changed bee production or any other bee modifier.
        helper.assertTrue(transmission.getBeeModifier().modifyProductionSpeed(genome, 1.0F) == 1.0F,
                "Transmission must not modify bee production");
        helper.succeed();
    }
}
