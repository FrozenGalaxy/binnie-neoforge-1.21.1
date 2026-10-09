package binnie.botany.gardening;

import binnie.botany.api.genetics.SoilAcidity;
import binnie.botany.api.genetics.SoilMoisture;
import binnie.botany.block.BotanySoilBlock;
import binnie.botany.block.SoilQuality;
import binnie.botany.registry.BotanyWorldContent;
import forestry.api.IForestryApi;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 1.12 gardening calculations and soil-family transitions shared by tools and automation. */
public class BotanyGardening {
    private BotanyGardening() {}

    public static SoilMoisture naturalMoisture(Level level, BlockPos pos) {
        HumidityType humidity = IForestryApi.INSTANCE.getClimateManager().getHumidity(level.getBiome(pos));
        int score = humidity.ordinal();
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            if (nearby.equals(pos)) continue;
            Block block = level.getBlockState(nearby).getBlock();
            if (block == Blocks.WATER) score += 2;
            else if (block == Blocks.SAND || block == Blocks.RED_SAND) score -= 1;
            else if (block == Blocks.GRAVEL && score > 0) score--;
        }
        if (level.isRainingAt(pos.above())) score += 2;
        return SoilMoisture.values()[Math.max(0, Math.min(2, score))];
    }

    public static SoilAcidity naturalAcidity(Level level, BlockPos pos) {
        var climates = IForestryApi.INSTANCE.getClimateManager();
        HumidityType humidity = climates.getHumidity(level.getBiome(pos));
        TemperatureType temperature = climates.getTemperature(level.getBiome(pos));
        int score = 1;
        if (humidity == HumidityType.DAMP) score--;
        else if (humidity == HumidityType.ARID) score++;
        if (temperature.isWarmerOrEqual(TemperatureType.HOT)) score++;
        else if (temperature.isCoolerOrEqual(TemperatureType.COLD)) score--;
        if (pos.getY() > 96) score++;
        else if (pos.getY() < 40) score--;
        return SoilAcidity.values()[Math.max(0, Math.min(2, score))];
    }

    public static BlockState createSoilState(Level level, BlockPos pos, SoilQuality quality) {
        return blockFor(quality).defaultBlockState()
                .setValue(BotanySoilBlock.MOISTURE, naturalMoisture(level, pos))
                .setValue(BotanySoilBlock.ACIDITY, naturalAcidity(level, pos))
                .setValue(BotanySoilBlock.FERTILITY, quality.initialFertility())
                .setValue(BotanySoilBlock.WEEDKILLED, false);
    }

    public static BotanySoilBlock blockFor(SoilQuality quality) {
        return switch (quality) {
            case SOIL -> BotanyWorldContent.SOIL.get();
            case LOAM -> BotanyWorldContent.LOAM.get();
            case FLOWERBED -> BotanyWorldContent.FLOWERBED.get();
        };
    }

    /** Upgrades soil while preserving any stronger nutrient charge already present. */
    public static BlockState changeQuality(BlockState state, SoilQuality quality) {
        return copySoilState(state, quality,
                Math.max(quality.initialFertility(), state.getValue(BotanySoilBlock.FERTILITY)));
    }

    /** Downgrades soil without carrying a higher-grade fertility charge into the lower soil family. */
    public static BlockState degradeQuality(BlockState state, SoilQuality quality) {
        return copySoilState(state, quality,
                Math.min(quality.initialFertility(), state.getValue(BotanySoilBlock.FERTILITY)));
    }

    private static BlockState copySoilState(BlockState state, SoilQuality quality, int fertility) {
        return blockFor(quality).defaultBlockState()
                .setValue(BotanySoilBlock.MOISTURE, state.getValue(BotanySoilBlock.MOISTURE))
                .setValue(BotanySoilBlock.ACIDITY, state.getValue(BotanySoilBlock.ACIDITY))
                .setValue(BotanySoilBlock.FERTILITY, Math.max(0, Math.min(3, fertility)))
                .setValue(BotanySoilBlock.WEEDKILLED, state.getValue(BotanySoilBlock.WEEDKILLED));
    }
}
