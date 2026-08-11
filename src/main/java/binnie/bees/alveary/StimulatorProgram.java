package binnie.bees.alveary;

import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import net.minecraft.core.Vec3i;

/** The nine original Binnie's Bees alveary-stimulator programs. */
public enum StimulatorProgram implements IBeeModifier {
    LOW_VOLTAGE("low_voltage", "iron_electron_tube", 10) {
        @Override public float modifyProductionSpeed(IGenome genome, float value) { return value * 1.5F; }
    },
    HIGH_VOLTAGE("high_voltage", "diamantine_electron_tube", 20) {
        @Override public float modifyProductionSpeed(IGenome genome, float value) { return value * 2.5F; }
    },
    PLANT("plant", "lapis_electron_tube", 10) {
        @Override public float modifyPollination(IGenome genome, float value) { return value * 1.5F; }
    },
    DEATH("death", "obsidian_electron_tube", 10) {
        @Override public float modifyAging(IGenome genome, IGenome mate, float value) { return value / 0.8F; }
    },
    LIFE("life", "apatine_electron_tube", 10) {
        @Override public float modifyAging(IGenome genome, IGenome mate, float value) { return value / 1.5F; }
    },
    NETHER("nether", "blazing_electron_tube", 15) {
        @Override public boolean isHellish() { return true; }
    },
    MUTATION("mutation", "golden_electron_tube", 15) {
        @Override
        public float modifyMutationChance(IGenome genome, IGenome mate, IMutation<IBeeSpecies> mutation, float value) {
            return value * 1.5F;
        }
    },
    INHIBITOR("inhibitor", "tin_electron_tube", 10) {
        @Override public Vec3i modifyTerritory(IGenome genome, Vec3i value) {
            return new Vec3i(Math.max(1, Math.round(value.getX() * 0.4F)),
                    Math.max(1, Math.round(value.getY() * 0.4F)),
                    Math.max(1, Math.round(value.getZ() * 0.4F)));
        }
        @Override public float modifyProductionSpeed(IGenome genome, float value) { return value * 0.9F; }
    },
    TERRITORY("territory", "bronze_electron_tube", 10) {
        @Override public Vec3i modifyTerritory(IGenome genome, Vec3i value) {
            return new Vec3i(Math.max(1, Math.round(value.getX() * 1.5F)),
                    Math.max(1, Math.round(value.getY() * 1.5F)),
                    Math.max(1, Math.round(value.getZ() * 1.5F)));
        }
    };

    private final String id;
    private final String electronTubeId;
    private final int powerUsage;

    StimulatorProgram(String id, String electronTubeId, int powerUsage) {
        this.id = id;
        this.electronTubeId = electronTubeId;
        this.powerUsage = powerUsage;
    }

    public String id() { return id; }
    /** Matching Forestry electron-tube item that created this 1.12 circuit. */
    public String electronTubeId() { return electronTubeId; }
    public int powerUsage() { return powerUsage; }

    public static StimulatorProgram byId(String id) {
        for (StimulatorProgram value : values()) if (value.id.equals(id)) return value;
        return null;
    }

    @Override
    public float modifyGeneticDecay(IGenome genome, float value) {
        return value * 1.5F;
    }
}
