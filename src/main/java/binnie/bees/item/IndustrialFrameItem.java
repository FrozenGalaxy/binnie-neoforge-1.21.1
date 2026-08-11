package binnie.bees.item;

import binnie.core.data.VariantData;
import binnie.core.item.VariantItem;
import binnie.core.registry.BinnieDataComponents;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.hives.IHiveFrame;
import forestry.api.core.genetics.IGenome;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/** One component-backed item preserving all sixteen 1.12 Binnie's Bees frame identities. */
public final class IndustrialFrameItem extends VariantItem implements IHiveFrame {
    private static final Map<String, Modifier> MODIFIERS = Map.ofEntries(
            Map.entry("empty", new Modifier(1, 1, 1, 1, false, false, false)),
            Map.entry("light", new Modifier(1, 1, 1, 1, false, true, false)),
            Map.entry("rain", new Modifier(1, 1, 1, 1, true, false, false)),
            Map.entry("sunlight", new Modifier(1, 1, 1, 1, false, true, false)),
            Map.entry("soul", new Modifier(0.75f, 1, 1, 1, false, false, true)),
            Map.entry("uranium", new Modifier(1, 1.5f, 1, 1.2f, false, false, false)),
            Map.entry("cage", new Modifier(1, 1, 0.5f, 1, false, false, false)),
            Map.entry("freedom", new Modifier(1, 1, 2, 1, false, false, false)),
            Map.entry("honey", new Modifier(1.5f, 1, 1, 1, false, false, false)),
            Map.entry("jelly", new Modifier(2, 1, 1, 1, false, false, false)),
            Map.entry("leaf", new Modifier(1, 1, 1.5f, 1, false, false, false)),
            Map.entry("pollen", new Modifier(1, 1, 2, 1, false, false, false)),
            Map.entry("clay", new Modifier(0.75f, 0.75f, 0.75f, 0.5f, true, false, false)),
            Map.entry("emerald", new Modifier(1.25f, 1.25f, 1.25f, 0.75f, false, false, false)),
            Map.entry("nether_star", new Modifier(2.5f, 2, 2, 0.25f, true, true, true)),
            Map.entry("poison", new Modifier(0.5f, 2, 1, 1.5f, false, false, false))
    );

    public IndustrialFrameItem(Properties properties, List<String> variants) {
        super(properties.durability(240), "binniebees.industrial_frame", variants);
    }

    @Override
    public ItemStack frameUsed(IBeeHousing housing, ItemStack frame, IBee queen, int wear) {
        if (housing.getLevel() instanceof ServerLevel server) {
            int adjusted = Math.max(1, Math.round(wear * MODIFIERS.getOrDefault(value(frame), MODIFIERS.get("empty")).wear));
            frame.hurtAndBreak(adjusted, server, null, item -> {});
        }
        return frame.isEmpty() ? ItemStack.EMPTY : frame;
    }

    @Override
    public IBeeModifier getBeeModifier(ItemStack frame) {
        return MODIFIERS.getOrDefault(value(frame), MODIFIERS.get("empty"));
    }

    private record Modifier(float production, float mutation, float pollination, float wear,
                            boolean sealed, boolean sunlight, boolean hellish) implements IBeeModifier {
        @Override public float modifyProductionSpeed(IGenome genome, float current) { return current * production; }
        @Override public float modifyMutationChance(IGenome genome, IGenome mate, forestry.api.core.genetics.IMutation<forestry.api.apiculture.genetics.IBeeSpecies> mutation, float current) { return current * this.mutation; }
        @Override public float modifyPollination(IGenome genome, float current) { return current * pollination; }
        @Override public Vec3i modifyTerritory(IGenome genome, Vec3i current) { return current; }
        @Override public boolean isSealed() { return sealed; }
        @Override public boolean isSunlightSimulated() { return sunlight; }
        @Override public boolean isHellish() { return hellish; }
    }
}
