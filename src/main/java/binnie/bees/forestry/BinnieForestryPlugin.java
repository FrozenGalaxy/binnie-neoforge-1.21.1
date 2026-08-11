package binnie.bees.forestry;

import binnie.bees.BinnieBees;
import binnie.bees.alveary.StimulatorCircuit;
import binnie.bees.alveary.StimulatorProgram;
import binnie.bees.block.BinnieBeesAlvearyBlockEntity;
import binnie.bees.registry.BinnieBeesContent;
import binnie.botany.client.BinnieForestryClientRegistration;
import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.genetics.FlowerSpeciesType;
import binnie.botany.genetics.FlowerSpeciesTypes;
import binnie.botany.genetics.LegacyFlowerCatalog;
import binnie.botany.genetics.NoFlowerEffect;
import binnie.botany.farming.BotanyFlowerFarmable;
import binnie.botany.farming.BotanyGardenFarmLogic;
import binnie.botany.farming.BotanyFarmVariants;
import binnie.botany.registry.BotanyContent;
import binnie.botany.registry.BotanyWorldContent;
import forestry.api.core.ToleranceType;
import forestry.api.client.plugin.IClientRegistration;
import forestry.api.core.genetics.ForestryTaxa;
import forestry.api.core.genetics.alleles.Allele;
import net.minecraft.core.Vec3i;
import binnie.trees.forestry.LegacyHeightMutationCondition;
import binnie.trees.forestry.LegacyPodFruit;
import binnie.trees.forestry.LegacyTreeCatalog;
import binnie.trees.forestry.LegacyTreeGenerator;
import binnie.trees.registry.BinnieTreeWoodContent;
import binnie.trees.registry.BinnieTreesContent;
import binnie.trees.wood.BinnieTreeWoodType;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.genetics.IFruit;
import forestry.api.core.Product;
import forestry.api.core.genetics.IEffectData;
import forestry.api.core.genetics.IGenome;
import forestry.api.plugin.IArboricultureRegistration;
import forestry.api.plugin.IApicultureRegistration;
import forestry.api.plugin.IForestryPlugin;
import forestry.api.plugin.IFarmingRegistration;
import forestry.api.plugin.ILepidopterologyRegistration;
import forestry.api.plugin.IGeneticRegistration;
import binnie.botany.farming.BotanyGardenCircuit;
import binnie.botany.farming.BotanyTubeVariants;
import forestry.api.core.circuits.ForestryCircuitLayouts;
import forestry.api.plugin.ICircuitRegistration;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.fruit.RipeningFruit;
import forestry.arboriculture.wood.VanillaWoodType;
import forestry.agriculture.farmlogic.FarmLogicCrops;
import forestry.arboriculture.fruit.ForestryPodType;
import forestry.core.engine.genetics.mutations.MutationConditionTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** ForestryCE ServiceLoader entry point for the full Binnie's Mods suite. */
public final class BinnieForestryPlugin implements IForestryPlugin {
    private static final List<String> EFFECTS = List.of(
            "ectoplasm", "acid", "spawn_zombie", "spawn_skeleton", "spawn_creeper",
            "lightning", "radioactive", "meteor", "hunger", "food", "blindness",
            "confusion", "fireworks", "festival", "birthday", "teleport", "gravity",
            "thief", "wither", "water", "slow", "bonemeal_sapling", "bonemeal_fruit",
            "bonemeal_mushroom"
    );
    private static final AtomicBoolean HEIGHT_CONDITION_REGISTERED = new AtomicBoolean();
    private static final LegacyTreeCatalog TREES = LegacyTreeCatalog.load();

    @Override
    public void registerGenetics(IGeneticRegistration binniegenetics) {
        LegacyBeeFlowerTypes.register(binniegenetics);
    // ForestryCE currently exposes mutation condition types through its public model but not through
    // IGeneticRegistration. Register the required minimum-height condition at the central dispatcher.
        if (HEIGHT_CONDITION_REGISTERED.compareAndSet(false, true)) {
            MutationConditionTypes.register(ResourceLocation.fromNamespaceAndPath("binnietrees", "height"),
                    LegacyHeightMutationCondition.TYPE);
        }

        LegacyFlowerCatalog.SpeciesDef defaultFlower = LegacyFlowerCatalog.INSTANCE.species().getFirst();
        var defaultPrimary = defaultFlower.allele("primary");
        var defaultSecondary = defaultFlower.allele("secondary");
        var defaultStem = defaultFlower.allele("stem");
        var defaultTerritory = defaultFlower.allele("territory").intArray();
        binniegenetics.registerSpeciesType(FlowerSpeciesTypes.FLOWER, FlowerSpeciesType::new)
                .addStages(FlowerLifeStage.values())
                .setDefaultStage(FlowerLifeStage.FLOWER)
                .setKaryotype(karyotype -> {
                    karyotype.setSpecies(FlowerChromosomes.SPECIES, defaultFlower.id());
                    karyotype.set(FlowerChromosomes.PRIMARY_COLOR, defaultPrimary.resourceValue());
                    karyotype.set(FlowerChromosomes.SECONDARY_COLOR, defaultSecondary.resourceValue());
                    karyotype.set(FlowerChromosomes.FERTILITY, new Allele<>(defaultFlower.allele("fertility").intValue(), defaultFlower.allele("fertility").dominant()));
                    karyotype.set(FlowerChromosomes.TERRITORY, new Allele<>(new Vec3i(defaultTerritory[0], defaultTerritory[1], defaultTerritory[2]), defaultFlower.allele("territory").dominant()));
                    karyotype.set(FlowerChromosomes.EFFECT, NoFlowerEffect.ID);
                    karyotype.set(FlowerChromosomes.LIFESPAN, new Allele<>(defaultFlower.allele("lifespan").intValue(), defaultFlower.allele("lifespan").dominant()));
                    karyotype.set(FlowerChromosomes.TEMPERATURE_TOLERANCE, new Allele<>(ToleranceType.valueOf(defaultFlower.allele("temperature_tolerance").stringValue()), defaultFlower.allele("temperature_tolerance").dominant())).setWeaklyInherited(true);
                    karyotype.set(FlowerChromosomes.HUMIDITY_TOLERANCE, new Allele<>(ToleranceType.valueOf(defaultFlower.allele("humidity_tolerance").stringValue()), defaultFlower.allele("humidity_tolerance").dominant())).setWeaklyInherited(true);
                    karyotype.set(FlowerChromosomes.PH_TOLERANCE, new Allele<>(ToleranceType.valueOf(defaultFlower.allele("ph_tolerance").stringValue()), defaultFlower.allele("ph_tolerance").dominant())).setWeaklyInherited(true);
                    karyotype.set(FlowerChromosomes.SAPPINESS, new Allele<>(defaultFlower.allele("sappiness").floatValue(), defaultFlower.allele("sappiness").dominant()));
                    karyotype.set(FlowerChromosomes.STEM, defaultStem.resourceValue());
                });
    }

    @Override
    public void registerClient(Consumer<Consumer<IClientRegistration>> registrar) {
    // Forestry invokes this only on the physical client; the registration object keeps client
    // renderer classes isolated from dedicated-server loading.
        registrar.accept(new BinnieForestryClientRegistration());
    }

    @Override
    public void registerApiculture(IApicultureRegistration apiculture) {
        for (String effect : EFFECTS) {
            apiculture.registerBeeEffect(id("bee_effect_" + effect), LegacyBeeEffects.create(effect));
        }
        int species = LegacyBeeSpeciesCatalog.register(apiculture);
        BinnieBees.LOGGER.info("Registered {} Binnie's Bees species with ForestryCE", species);
    }

    @Override
    public void registerLepidopterology(ILepidopterologyRegistration lepidopterology) {
        binnie.trees.forestry.LegacyButterflyCatalog.register(lepidopterology);
    }

    @Override
    public void registerArboriculture(IArboricultureRegistration arboriculture) {
        BinnieTreeWoodContent.registerForestryBindings(arboriculture);

        for (LegacyTreeCatalog.FruitBinding fruit : TREES.fruits()) {
            ItemStack output = fruit.productVariant().equals("apple")
                    ? new ItemStack(Items.APPLE)
                    : BinnieTreesContent.FOOD.get().createStack(fruit.productVariant());
            Product product = new Product(output.getItem(), fruit.productCount(), output.getComponentsPatch(), 1.0F);
            IFruit registeredFruit;
            if (fruit.pod()) {
                ForestryPodType podType = fruit.podType().equals("coconut")
                        ? ForestryPodType.COCONUT
                        : ForestryPodType.PAPAYA;
                registeredFruit = new LegacyPodFruit(fruit.dominant(), podType, List.of(product), podHostTag(fruit));
            } else {
                ResourceLocation sprite = ResourceLocation.fromNamespaceAndPath(
                        "binnietrees", "block/fruit/" + fruit.sprite());
                registeredFruit = new RipeningFruit(fruit.dominant(), fruit.period(), sprite,
                        fruit.ripeColor(), fruit.callowColor(), List.of(product));
            }
            arboriculture.registerFruit(treeId("fruit_" + fruit.id()), registeredFruit);
        }

        for (LegacyTreeCatalog.TreeBinding tree : TREES.species()) {
            IWoodType woodType = woodType(tree.wood());
            arboriculture.registerSpecies(treeId(tree.id()), tree.genus(), tree.species(), tree.dominant(),
                            TextColor.fromRgb(tree.leafColor()), woodType)
                    .setGenerator(new LegacyTreeGenerator(woodType, tree.generator()))
                    .setRarity(tree.rarity());
        }
    }

    @Override
    public void registerCircuits(ICircuitRegistration circuits) {
        circuits.registerLayout(BinnieBeesAlvearyBlockEntity.STIMULATOR_LAYOUT,
                BinnieBeesAlvearyBlockEntity.STIMULATOR_SOCKET);
        for (StimulatorProgram program : StimulatorProgram.values()) {
            ResourceLocation tubeId = ResourceLocation.fromNamespaceAndPath("forestry", program.electronTubeId());
            var tube = BuiltInRegistries.ITEM.get(tubeId);
            if (tube == Items.AIR) throw new IllegalStateException("Missing Forestry electron tube " + tubeId);
            circuits.registerCircuit(BinnieBeesAlvearyBlockEntity.STIMULATOR_LAYOUT,
                    new ItemStack(tube),
                    new StimulatorCircuit(program));
        }

        ResourceLocation baseType = ResourceLocation.fromNamespaceAndPath("binniebotany", "flowers");
        for (String physicalTube : BotanyTubeVariants.physicalIds()) {
            ItemStack tube = BotanyContent.INSULATED_TUBE.get().createStack(physicalTube);
            for (BotanyFarmVariants.Variant variant : BotanyTubeVariants.circuitsFor(physicalTube)) {
                String layout = variant.manual() ? ForestryCircuitLayouts.MANUAL_FARM : ForestryCircuitLayouts.MANAGED_FARM;
                circuits.registerCircuit(layout, tube, new BotanyGardenCircuit("binniebotany.garden." + variant.id(),
                        ResourceLocation.fromNamespaceAndPath("binniebotany", "garden_" + variant.id()), variant));
            }
        }
    }

    @Override
    public void registerFarming(IFarmingRegistration farming) {
    // The 1.12 gardening powders are the canonical fertilizer inputs. Keep the
    // three early-port aliases registered too so existing worlds remain compatible.
        farming.registerFertilizer(BotanyContent.POWDER_FERTILISER.get(), 500);
        farming.registerFertilizer(BotanyContent.POWDER_COMPOST.get(), 500);
        farming.registerFertilizer(BotanyContent.FERTILISER.get(), 500);
        BotanyFlowerFarmable farmable = new BotanyFlowerFarmable();
        for (BotanyFarmVariants.Variant variant : BotanyFarmVariants.all()) {
            var soilState = BotanyWorldContent.FLOWERBED.get().defaultBlockState()
                    .setValue(binnie.botany.block.BotanySoilBlock.MOISTURE, variant.moisture())
                    .setValue(binnie.botany.block.BotanySoilBlock.ACIDITY, variant.acidity() == null ? binnie.botany.api.genetics.SoilAcidity.NEUTRAL : variant.acidity())
                    .setValue(binnie.botany.block.BotanySoilBlock.FERTILITY, variant.fertilised() ? 3 : 0);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("binniebotany", "garden_" + variant.id());
            farming.createFarmType(id,
                            (type, manual) -> new BotanyGardenFarmLogic(type, variant.manual(), variant.moisture(), variant.acidity(), variant.fertilised()),
                            BotanyContent.INSULATED_TUBE.get().createStack(variant.id()))
                    .setWaterConsumption(hydrationModifier -> variant.moisture() == binnie.botany.api.genetics.SoilMoisture.DRY ? 5
                            : variant.moisture() == binnie.botany.api.genetics.SoilMoisture.NORMAL ? 20 : 40)
                    .setFertilizerConsumption(variant.fertilised() ? 5 : 0)
                    .addSoil(new ItemStack(BotanyWorldContent.FLOWERBED_ITEM.get()), soilState)
                    .addFarmable(farmable);
        }
    }

    @Override
    public ResourceLocation id() {
        return id("forestry_plugin");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BinnieBees.MOD_ID, path);
    }

    private static ResourceLocation treeId(String path) {
        return ResourceLocation.fromNamespaceAndPath("binnietrees", path);
    }

    private static TagKey<Block> podHostTag(LegacyTreeCatalog.FruitBinding fruit) {
        String path = fruit.podType().equals("coconut") ? "coconut_pod_logs" : "banana_pod_logs";
        return TagKey.create(Registries.BLOCK, treeId(path));
    }

    private static IWoodType woodType(String legacy) {
        if (legacy.equals("EnumShrubLog.INSTANCE")) return VanillaWoodType.OAK;
        if (legacy.equals("EnumETLog.Cinnamon")) return VanillaWoodType.JUNGLE;
        if (legacy.equals("EnumETLog.Cherry")) return ForestryWoodType.SOUR_CHERRY;
        if (legacy.endsWith(".JUNGLE")) return VanillaWoodType.JUNGLE;
        if (legacy.endsWith(".OAK")) return VanillaWoodType.OAK;
        if (legacy.startsWith("EnumForestryWoodType.")) {
            String name = legacy.substring(legacy.indexOf('.') + 1);
            if (name.equals("CITRUS")) return ForestryWoodType.LEMON;
            try {
                return ForestryWoodType.valueOf(name);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Unknown Forestry wood binding in Binnie's Trees catalog: " + legacy, exception);
            }
        }
        BinnieTreeWoodType binnieTreeWood = BinnieTreeWoodType.fromLegacyExpression(legacy);
        if (binnieTreeWood == null) {
            throw new IllegalStateException("Unknown Binnie's Trees wood binding: " + legacy);
        }
        return binnieTreeWood;
    }

    /** Stable code registration for all 1.12 effect allele identities. */
    private static final class LegacyBeeEffect implements IBeeEffect {
        private final String name;

        private LegacyBeeEffect(String name) {
            this.name = name;
        }

        @Override
        public boolean isDominant() {
            return true;
        }

        @Override
        public IEffectData doEffect(IGenome genome, IEffectData storedData, IBeeHousing housing) {
            return storedData;
        }

        @Override
        public String toString() {
            return "BinnieBeeEffect[" + this.name + "]";
        }
    }
}
