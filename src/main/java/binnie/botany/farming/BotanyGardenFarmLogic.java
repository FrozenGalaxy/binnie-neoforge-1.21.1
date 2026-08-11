package binnie.botany.farming;

import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import binnie.botany.block.BotanySoilBlock;
import forestry.api.agriculture.IFarmHousing;
import forestry.api.agriculture.IFarmType;
import forestry.api.agriculture.IFarmable;
import forestry.agriculture.farmlogic.FarmLogicCrops;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Garden farm logic honoring the selected 1.12 moisture, pH and fertilisation profile. */
public final class BotanyGardenFarmLogic extends FarmLogicCrops {
    private final SoilMoisture moisture;
    private final SoilAcidity acidity;
    private final boolean fertilised;
    public BotanyGardenFarmLogic(IFarmType type, boolean manual, SoilMoisture moisture, SoilAcidity acidity, boolean fertilised) {
        super(type, manual); this.moisture = moisture; this.acidity = acidity; this.fertilised = fertilised;
    }
    @Override
    protected boolean maintainCrops(Level level, IFarmHousing housing, BlockPos pos, Direction direction, int extent) {
        for (int i = 0; i < extent; i++) {
            BlockPos plant = translateWithOffset(pos, direction, i);
            if (!level.hasChunkAt(plant) || !level.getBlockState(plant).canBeReplaced()) continue;
            BlockState soil = level.getBlockState(plant.below());
            if (!(soil.getBlock() instanceof BotanySoilBlock)
                    || soil.getValue(BotanySoilBlock.MOISTURE) != moisture
                    || (acidity != null && soil.getValue(BotanySoilBlock.ACIDITY) != acidity)
                    || (fertilised && soil.getValue(BotanySoilBlock.FERTILITY) <= 0)) continue;
            for (IFarmable farmable : getFarmables()) if (housing.plantGermling(farmable, level, plant, direction)) return true;
        }
        return false;
    }
}
