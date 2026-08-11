package binnie.botany;

import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.api.genetics.IFlowerSpecies;
import binnie.botany.block.BotanySoilBlock;
import binnie.botany.block.FlowerBlockEntity;
import binnie.botany.gardening.VanillaFlowerConversions;
import binnie.botany.registry.BotanyWorldContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Common gameplay hooks retained from the 1.12 Flowers module. */
@EventBusSubscriber(modid = BinnieBotany.MOD_ID)
public final class BotanyCommonEvents {
    private BotanyCommonEvents() {}

    @SubscribeEvent
    public static void onVanillaFlowerPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player player)) return;
        BlockPos pos = event.getPos();
        if (!(level.getBlockState(pos.below()).getBlock() instanceof BotanySoilBlock)) return;

        BlockState placed = event.getPlacedBlock();
        if (placed.getBlock() instanceof DoublePlantBlock
                && placed.hasProperty(DoublePlantBlock.HALF)
                && placed.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) return;
        var speciesId = VanillaFlowerConversions.speciesId(placed.getBlock());
        if (speciesId == null) return;

        IFlowerSpecies species;
        try {
            species = binnie.botany.genetics.FlowerSpeciesType.get().getSpecies(speciesId);
        } catch (RuntimeException exception) {
            return;
        }
        if (placed.getBlock() instanceof DoublePlantBlock) level.removeBlock(pos.above(), false);
        level.setBlock(pos, BotanyWorldContent.FLOWER_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof FlowerBlockEntity flower) {
            flower.setFlower(species.createStack(species.createIndividual(), FlowerLifeStage.FLOWER), player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.getBlockEntity(event.getPos()) instanceof FlowerBlockEntity flower && flower.applyBonemeal()) {
            event.setSuccessful(true);
            event.setCanceled(true);
        }
    }
}
