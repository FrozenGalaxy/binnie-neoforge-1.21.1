package binnie.bees.block;

import forestry.apiculture.hives.HiveBlockEntity;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Marker entity used by Forestry's spectacles renderer for Binnie's wild hives. */
public final class BinnieBeeHiveBlockEntity extends HiveBlockEntity {
    private IBee defaultBee;

    public BinnieBeeHiveBlockEntity(BlockEntityType<? extends HiveBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public IBee getContainedBee() {
        if (defaultBee == null) {
            ResourceLocation speciesId = getBlockState().getBlock() instanceof BinnieBeeHiveBlock hive
                    ? ResourceLocation.fromNamespaceAndPath("binniebees", hive.hiveType().beeSpeciesId())
                    : ForestryBeeSpecies.FOREST;
            IBeeSpecies species = SpeciesUtil.getBeeSpecies(speciesId);
            if (species == null) species = SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST);
            defaultBee = species.createIndividual().cast();
        }
        return defaultBee;
    }
}
