package binnie.bees.alveary;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Alveary mutagens, excluding the obsolete IC2 entries. */
public class AlvearyMutagens {
    public record Entry(ItemStack stack, float multiplier) {
        public Entry {
            stack = stack.copyWithCount(1);
        }
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry(new ItemStack(Blocks.SOUL_SAND), 1.5F),
            new Entry(new ItemStack(Items.ENDER_PEARL), 2.0F),
            new Entry(new ItemStack(Items.ENDER_EYE), 4.0F)
    );

    private AlvearyMutagens() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static float multiplier(ItemStack stack) {
        if (stack.isEmpty()) return 1.0F;
        for (Entry entry : ENTRIES) {
            if (ItemStack.isSameItemSameComponents(stack, entry.stack())) return entry.multiplier();
        }
        return 1.0F;
    }

    public static boolean accepts(ItemStack stack) {
        return multiplier(stack) > 1.0F;
    }
}
