package binnie.botany.farming;

import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.api.genetics.IFlower;
import binnie.botany.block.FlowerBlockEntity;
import binnie.botany.genetics.FlowerSpeciesType;
import binnie.botany.gardening.VanillaFlowerConversions;
import binnie.botany.registry.BotanyContent;
import binnie.botany.registry.BotanyWorldContent;
import forestry.api.agriculture.ICrop;
import forestry.api.agriculture.IFarmable;
import forestry.core.engine.genetics.ItemGE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/** Forestry multifarm bridge for genetic Botany seeds and mature planted flowers. */
public final class BotanyFlowerFarmable implements IFarmable {
    @Override
    public boolean isSaplingAt(Level level, BlockPos pos, BlockState state) {
        if (!state.is(BotanyWorldContent.FLOWER_BLOCK.get())) return false;
        return !(level.getBlockEntity(pos) instanceof FlowerBlockEntity flower) || !flower.isHarvestable();
    }

    @Nullable
    @Override
    public ICrop getCropAt(Level level, BlockPos pos, BlockState state) {
        if (!state.is(BotanyWorldContent.FLOWER_BLOCK.get())
                || !(level.getBlockEntity(pos) instanceof FlowerBlockEntity flower)
                || !flower.isHarvestable()) return null;
        return new ICrop() {
            @Nullable
            @Override
            public List<ItemStack> harvest() {
                return flower.harvestForAutomation();
            }

            @Override
            public BlockPos getPosition() {
                return pos;
            }
        };
    }

    @Override
    public boolean isGermling(ItemStack stack) {
        return (stack.is(BotanyContent.SEED.get()) && ItemGE.getIndividual(stack) instanceof IFlower)
                || VanillaFlowerConversions.create(stack) != null;
    }

    @Override
    public void addGermlings(Consumer<ItemStack> accumulator) {
        try {
            var species = FlowerSpeciesType.get().getDefaultSpecies();
            accumulator.accept(species.createStack(species.createIndividual(), FlowerLifeStage.SEED));
            accumulator.accept(new ItemStack(Blocks.DANDELION));
            accumulator.accept(new ItemStack(Blocks.POPPY));
            accumulator.accept(new ItemStack(Blocks.BLUE_ORCHID));
            accumulator.accept(new ItemStack(Blocks.ALLIUM));
            accumulator.accept(new ItemStack(Blocks.AZURE_BLUET));
            accumulator.accept(new ItemStack(Blocks.RED_TULIP));
            accumulator.accept(new ItemStack(Blocks.OXEYE_DAISY));
            accumulator.accept(new ItemStack(Blocks.LILAC));
            accumulator.accept(new ItemStack(Blocks.ROSE_BUSH));
            accumulator.accept(new ItemStack(Blocks.PEONY));
        } catch (RuntimeException ignored) {
            // JEI may query before Forestry finishes species registration.
        }
    }

    @Override
    public void addProducts(Consumer<ItemStack> accumulator) {
        try {
            var species = FlowerSpeciesType.get().getDefaultSpecies();
            accumulator.accept(species.createStack(species.createIndividual(), FlowerLifeStage.FLOWER));
            accumulator.accept(species.createStack(species.createIndividual(), FlowerLifeStage.POLLEN));
            accumulator.accept(species.createStack(species.createIndividual(), FlowerLifeStage.SEED));
        } catch (RuntimeException ignored) {
            // JEI may query before Forestry finishes species registration.
        }
    }

    @Override
    public boolean isWindfall(ItemStack stack) {
        return stack.is(BotanyContent.FLOWER.get()) || stack.is(BotanyContent.POLLEN.get()) || stack.is(BotanyContent.SEED.get());
    }

    @Override
    public boolean plantSaplingAt(Player player, ItemStack germling, Level level, BlockPos pos) {
        if (!level.getBlockState(pos).canBeReplaced()
                || !BotanyWorldContent.FLOWER_BLOCK.get().defaultBlockState().canSurvive(level, pos)) return false;
        IFlower individual = ItemGE.getIndividual(germling) instanceof IFlower genetic
                ? genetic : VanillaFlowerConversions.create(germling);
        if (individual == null) return false;
        if (!level.setBlock(pos, BotanyWorldContent.FLOWER_BLOCK.get().defaultBlockState(), 3)) return false;
        if (level.getBlockEntity(pos) instanceof FlowerBlockEntity flower) {
            flower.setFlower(individual.createStack(FlowerLifeStage.FLOWER), player == null ? null : player.getUUID());
            return true;
        }
        level.destroyBlock(pos, false);
        return false;
    }
}
