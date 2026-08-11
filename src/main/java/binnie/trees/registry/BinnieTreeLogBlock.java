package binnie.trees.registry;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.arboriculture.wood.BlockForestryLog;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/** Forestry-compatible Binnie's Trees log/wood block with an explicit axe-stripping target. */
final class BinnieTreeLogBlock extends BlockForestryLog {
    @Nullable
    private final Supplier<? extends Block> strippedTarget;

    BinnieTreeLogBlock(WoodBlockKind kind, boolean fireproof, IWoodType woodType,
                      @Nullable Supplier<? extends Block> strippedTarget) {
        super(kind, fireproof, woodType);
        this.strippedTarget = strippedTarget;
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context,
                                                     ItemAbility itemAbility, boolean simulate) {
        if (itemAbility == ItemAbilities.AXE_STRIP && this.strippedTarget != null) {
            Block stripped = Objects.requireNonNull(this.strippedTarget.get(), "stripped Binnie's Trees wood block");
            return stripped.defaultBlockState().setValue(AXIS, state.getValue(AXIS));
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}
