package binnie.botany.block;

import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.api.genetics.IFlower;
import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.genetics.FlowerIndividual;
import binnie.botany.genetics.FlowerSpeciesType;
import binnie.botany.registry.BotanyWorldContent;
import binnie.botany.gardening.BotanyGardening;
import forestry.api.IForestryApi;
import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.ToleranceType;
import forestry.api.core.genetics.ClimateHelper;
import forestry.api.core.genetics.IGenome;
import forestry.core.engine.genetics.ItemGE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Canonical persistent container for a planted genetic flower. */
public final class FlowerBlockEntity extends BlockEntity {
    private static final int TICK_INTERVAL = 20;
    private ItemStack flowerStack = ItemStack.EMPTY;
    @Nullable private UUID owner;
    private long lastPollinationTick;

    public FlowerBlockEntity(BlockPos pos, BlockState state) {
        super(BotanyWorldContent.FLOWER_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStack getFlowerStack() {
        ensureDefaultFlower();
        return flowerStack;
    }

    public void setFlower(ItemStack stack, @Nullable UUID owner) {
        if (!(ItemGE.getIndividual(stack) instanceof IFlower)) throw new IllegalArgumentException("Stack does not contain a flower genome");
        this.flowerStack = stack.copyWithCount(1);
        this.owner = owner;
        syncFromIndividual();
    }

    @Nullable
    public IFlower getFlower() {
        ensureDefaultFlower();
        return ItemGE.getIndividual(flowerStack) instanceof IFlower flower ? flower : null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FlowerBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % TICK_INTERVAL != 0) return;
        IFlower flower = blockEntity.getFlower();
        if (flower == null) return;

        boolean suitable = blockEntity.isEnvironmentSuitable(flower);
        if (suitable) {
            float agingChance = (float) Math.min(1.0,
                    Math.sqrt(2.0 / Math.max(1, flower.getGenome().getActiveValue(FlowerChromosomes.LIFESPAN)))
                            / blockEntity.soilLifespanMultiplier());
            if (server.random.nextFloat() < agingChance) flower.age(server, 1.0F);
            if (flower.getAge() > 1 && flower.isAlive()) flower.setFlowered(true);
        } else {
            flower.setHealth(flower.getHealth() - 1);
            if (flower.getHealth() <= Math.max(1, flower.getMaxHealth() / 4)) flower.setWilted(true);
        }

        if (!flower.isAlive()) {
            blockEntity.expire(server, flower, false);
            return;
        }

        if (flower.hasFlowered() && !flower.isWilted()) {
            blockEntity.tryPollination(server, flower);
            blockEntity.tryProduceOffspring(server, flower);
        }
        flower.saveToStack(blockEntity.flowerStack);
        blockEntity.syncFromIndividual();
    }

    private boolean isEnvironmentSuitable(IFlower flower) {
        if (level == null || level.getMaxLocalRawBrightness(worldPosition.above()) < 8) return false;
        IFlowerSpecies species = flower.getSpecies();
        IClimateProvider climate = IForestryApi.INSTANCE.getClimateManager().createClimateProvider(level, worldPosition);
        if (!ClimateHelper.isWithinLimits(climate.temperature(), species.temperature(),
                flower.getGenome().getActiveValue(FlowerChromosomes.TEMPERATURE_TOLERANCE))) return false;
        BlockState soil = level.getBlockState(worldPosition.below());
        if (!(soil.getBlock() instanceof BotanySoilBlock)) return true;
        ToleranceType humidityTolerance = flower.getGenome().getActiveValue(FlowerChromosomes.HUMIDITY_TOLERANCE);
        ToleranceType phTolerance = flower.getGenome().getActiveValue(FlowerChromosomes.PH_TOLERANCE);
        SoilMoisture moisture = soil.getValue(BotanySoilBlock.MOISTURE);
        SoilAcidity acidity = soil.getValue(BotanySoilBlock.ACIDITY);
        return BotanySoilBlock.withinTolerance(moisture.ordinal(), species.moisture().ordinal(), humidityTolerance)
                && BotanySoilBlock.withinTolerance(acidity.ordinal(), species.acidity().ordinal(), phTolerance)
                && soil.getValue(BotanySoilBlock.FERTILITY) > 0;
    }

    private void tryPollination(ServerLevel level, IFlower flower) {
        if (flower.getMate() != null || level.getGameTime() - lastPollinationTick < 100) return;
        lastPollinationTick = level.getGameTime();
        Vec3i territory = flower.getGenome().getActiveValue(FlowerChromosomes.TERRITORY);
        int radiusX = Math.min(8, Math.max(1, territory.getX() / 2));
        int radiusY = Math.min(3, Math.max(1, territory.getY() / 2));
        int radiusZ = Math.min(8, Math.max(1, territory.getZ() / 2));
        ArrayList<FlowerBlockEntity> candidates = new ArrayList<>();
        for (BlockPos candidatePos : BlockPos.betweenClosed(worldPosition.offset(-radiusX, -radiusY, -radiusZ),
                worldPosition.offset(radiusX, radiusY, radiusZ))) {
            if (candidatePos.equals(worldPosition)) continue;
            if (level.getBlockEntity(candidatePos) instanceof FlowerBlockEntity candidate) {
                IFlower mate = candidate.getFlower();
                if (mate != null && mate.hasFlowered() && !mate.isWilted()) candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            if (level.random.nextInt(4) == 0) flower.setMate(flower.getGenome());
        } else {
            Collections.shuffle(candidates, new java.util.Random(level.random.nextLong()));
            IFlower mate = candidates.getFirst().getFlower();
            if (mate != null) flower.pollinate(mate);
        }
    }

    private void tryProduceOffspring(ServerLevel level, IFlower flower) {
        IGenome mate = flower.getMate();
        if (mate == null || level.random.nextInt(20) != 0) return;
        IFlower mateIndividual = mate.getActiveSpecies().createIndividual(mate).cast();
        List<IFlower> children = flower.createOffspring(level, worldPosition, mateIndividual, 1);
        if (children.isEmpty()) return;
        IFlower child = children.getFirst();
        ItemStack childStack = child.createStack(FlowerLifeStage.SEED);
        for (BlockPos target : nearbyPlantingPositions(level)) {
            if (level.isEmptyBlock(target) && BotanySoilBlock.isSuitableSoil(level.getBlockState(target.below()))) {
                level.setBlockAndUpdate(target, BotanyWorldContent.FLOWER_BLOCK.get().defaultBlockState());
                if (level.getBlockEntity(target) instanceof FlowerBlockEntity placed) placed.setFlower(childStack, owner);
                flower.setMate(null);
                return;
            }
        }
        level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, childStack));
        flower.setMate(null);
    }

    private List<BlockPos> nearbyPlantingPositions(ServerLevel level) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (dx != 0 || dz != 0) positions.add(worldPosition.offset(dx, 0, dz));
        }
        Collections.shuffle(positions, new java.util.Random(level.random.nextLong()));
        return positions;
    }

    public boolean applyPollen(IFlower pollen) {
        IFlower flower = getFlower();
        if (flower == null || flower.isWilted() || !flower.hasFlowered()) return false;
        flower.pollinate(pollen);
        flower.saveToStack(flowerStack);
        setChangedAndSync();
        return true;
    }

    public boolean isHarvestable() {
        IFlower flower = getFlower();
        return flower != null && flower.hasFlowered() && !flower.isWilted();
    }

    /** Bonemeal moves an established, healthy plant into its flowering state. */
    public boolean applyBonemeal() {
        IFlower flower = getFlower();
        if (flower == null || flower.isWilted()) return false;
        if (flower.getAge() > 1) flower.setFlowered(true);
        flower.saveToStack(flowerStack);
        syncFromIndividual();
        return true;
    }

    public void harvest(ServerPlayer player) {
        for (ItemStack output : harvestForAutomation()) give(player, output);
    }

    public List<ItemStack> harvestForAutomation() {
        IFlower flower = getFlower();
        if (flower == null || !isHarvestable()) return List.of();
        ArrayList<ItemStack> outputs = new ArrayList<>();
        outputs.add(flower.createStack(FlowerLifeStage.FLOWER));
        outputs.add(flower.createStack(FlowerLifeStage.POLLEN));
        int fertility = Math.max(1, flower.getGenome().getActiveValue(FlowerChromosomes.FERTILITY));
        for (int i = 0; i < Math.max(1, fertility / 2); i++) outputs.add(flower.createStack(FlowerLifeStage.SEED));
        flower.setFlowered(false);
        flower.setAge(Math.max(1, flower.getAge() - 1));
        consumeSoilNutrients();
        flower.saveToStack(flowerStack);
        syncFromIndividual();
        return List.copyOf(outputs);
    }

    public boolean shear(ServerPlayer player) {
        IFlower flower = getFlower();
        if (flower == null || flower.getAge() <= 1) return false;
        IFlower cutting = flower.copy().cast();
        cutting.setAge(0);
        cutting.setFlowered(false);
        cutting.setWilted(false);
        give(player, cutting.createStack(FlowerLifeStage.SEED));
        int agingRolls = Math.max(1, flower.getMaxHealth());
        for (int i = 0; i < agingRolls; i++) {
            if (player.getRandom().nextFloat() < 0.5F / soilLifespanMultiplier()) flower.age(player.level(), 1.0F);
            if (!flower.isAlive()) {
                expire((ServerLevel) player.level(), flower, true);
                return true;
            }
        }
        flower.saveToStack(flowerStack);
        syncFromIndividual();
        return true;
    }

    public Component inspectionSummary() {
        IFlower flower = getFlower();
        if (flower == null) return Component.translatable("message.binniebotany.field_kit.invalid");
        int lifespan = Math.max(1, flower.getGenome().getActiveValue(FlowerChromosomes.LIFESPAN));
        int agePercent = Math.min(100, Math.round(flower.getAge() * 100.0F / lifespan));
        return Component.translatable("message.binniebotany.field_kit.summary",
                flower.getSpecies().getDisplayName(), flower.getInactiveSpecies().getDisplayName(),
                flower.getGeneration(), agePercent,
                Component.translatable(flower.hasFlowered() ? "gui.yes" : "gui.no"),
                Component.translatable(flower.isWilted() ? "gui.yes" : "gui.no"));
    }

    private void consumeSoilNutrients() {
        if (level == null) return;
        BlockPos soilPos = worldPosition.below();
        BlockState soilState = level.getBlockState(soilPos);
        if (!(soilState.getBlock() instanceof BotanySoilBlock soil)) return;
        int fertility = soilState.getValue(BotanySoilBlock.FERTILITY);
        if (fertility > 0) {
            level.setBlock(soilPos, soilState.setValue(BotanySoilBlock.FERTILITY, fertility - 1),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        } else if (soil.quality().previous() != soil.quality()) {
            level.setBlock(soilPos, BotanyGardening.changeQuality(soilState, soil.quality().previous()),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    private float soilLifespanMultiplier() {
        if (level == null) return 1.0F;
        BlockState soilState = level.getBlockState(worldPosition.below());
        if (soilState.getBlock() instanceof BotanySoilBlock soil) {
            return 1.0F + soil.quality().ordinal() * 0.25F;
        }
        return 1.0F;
    }

    private void expire(ServerLevel server, IFlower flower, boolean wasCut) {
        if (!wasCut && flower.getMate() != null) {
            IGenome mateGenome = flower.getMate();
            IFlower mate = mateGenome.getActiveSpecies().createIndividual(mateGenome).cast();
            List<IFlower> offspring = flower.createOffspring(server, worldPosition.below(), mate, 1);
            if (!offspring.isEmpty()) {
                IFlower child = offspring.getFirst();
                server.setBlock(worldPosition, BotanyWorldContent.FLOWER_BLOCK.get().defaultBlockState(),
                        net.minecraft.world.level.block.Block.UPDATE_ALL);
                BlockEntity blockEntity = server.getBlockEntity(worldPosition);
                if (blockEntity instanceof FlowerBlockEntity replacement) {
                    replacement.setFlower(child.createStack(FlowerLifeStage.FLOWER), owner);
                    return;
                }
            }
        }
    // ForestryCE models remaining lifespan as health, independently of Binnie's
    // visual growth age. An unsuitable flower can therefore reach zero health
    // while its growth age is still zero; it has already entered the wilted
    // lifecycle and must leave the 1.12 dead-flower block behind.
        if (flower.getAge() > 0 || flower.isWilted()) {
            server.setBlock(worldPosition, BotanyWorldContent.GARDEN_PLANT.get().defaultBlockState()
                    .setValue(GardenPlantBlock.TYPE, GardenPlantType.DEAD_FLOWER),
                    net.minecraft.world.level.block.Block.UPDATE_ALL);
        } else {
            server.destroyBlock(worldPosition, false);
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private void ensureDefaultFlower() {
        if (!flowerStack.isEmpty()) return;
        try {
            IFlowerSpecies species = FlowerSpeciesType.get().getDefaultSpecies();
            flowerStack = species.createStack(species.createIndividual(), FlowerLifeStage.FLOWER);
        } catch (RuntimeException ignored) {
            // Species registration can lag behind construction during early startup.
        }
    }

    private void syncFromIndividual() {
        IFlower flower = getFlower();
        if (flower == null || level == null) return;
        BlockState old = getBlockState();
        BlockState changed = old.setValue(FlowerBlock.AGE, Math.min(15, flower.getAge()))
                .setValue(FlowerBlock.FLOWERED, flower.hasFlowered())
                .setValue(FlowerBlock.WILTED, flower.isWilted());
        if (changed != old) level.setBlock(worldPosition, changed, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ensureDefaultFlower();
        if (!flowerStack.isEmpty()) tag.put("Flower", flowerStack.saveOptional(registries));
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putLong("LastPollination", lastPollinationTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        flowerStack = tag.contains("Flower") ? ItemStack.parse(registries, tag.getCompound("Flower")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        lastPollinationTick = tag.getLong("LastPollination");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
}
