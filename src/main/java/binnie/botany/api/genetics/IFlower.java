package binnie.botany.api.genetics;

import forestry.api.core.genetics.IIndividualLiving;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

/** A genetically typed Binnie flower individual. */
public interface IFlower extends IIndividualLiving {
    @Override
    IFlowerSpecies getSpecies();

    @Override
    IFlowerSpecies getInactiveSpecies();

    int getGeneration();

    int getAge();

    void setAge(int age);

    boolean hasFlowered();

    void setFlowered(boolean flowered);

    boolean isWilted();

    void setWilted(boolean wilted);

    default void pollinate(IFlower mate) {
        setMate(mate == null ? null : mate.getGenome());
    }

    /** Creates the equivalent seed/cutting offspring for this flower. */
    List<IFlower> createOffspring(Level level, BlockPos pos, IFlower mate, int count);
}
