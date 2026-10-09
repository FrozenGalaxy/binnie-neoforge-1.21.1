package binnie.trees.item;

import binnie.core.item.VariantItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/** Dynamic food values for the 1.12 Binnie's Trees fruit/nut catalog. */
public final class BinnieTreesFoodItem extends VariantItem {
    private static final FoodProperties DEFAULT_FOOD = food(2);
    private static final Map<String, Integer> NUTRITION = Map.ofEntries(
            Map.entry("crabapple", 2), Map.entry("orange", 4), Map.entry("kumquat", 2), Map.entry("lime", 2),
            Map.entry("wild_cherry", 2), Map.entry("sour_cherry", 2), Map.entry("black_cherry", 2), Map.entry("blackthorn", 3),
            Map.entry("cherry_plum", 3), Map.entry("almond", 1), Map.entry("apricot", 4), Map.entry("grapefruit", 4),
            Map.entry("peach", 4), Map.entry("satsuma", 3), Map.entry("buddha_hand", 3), Map.entry("citron", 3),
            Map.entry("finger_lime", 3), Map.entry("key_lime", 2), Map.entry("manderin", 3), Map.entry("nectarine", 3),
            Map.entry("pomelo", 3), Map.entry("tangerine", 3), Map.entry("pear", 4), Map.entry("sand_pear", 2),
            Map.entry("hazelnut", 2), Map.entry("butternut", 1), Map.entry("beechnut", 0), Map.entry("pecan", 0),
            Map.entry("banana", 4), Map.entry("red_banana", 4), Map.entry("plantain", 2), Map.entry("brazil_nut", 0),
            Map.entry("fig", 2), Map.entry("acorn", 0), Map.entry("elderberry", 1), Map.entry("olive", 1),
            Map.entry("gingko_nut", 1), Map.entry("coffee", 0), Map.entry("osange_orange", 1), Map.entry("clove", 0),
            Map.entry("papayimar", 8), Map.entry("blackcurrant", 2), Map.entry("redcurrant", 2), Map.entry("blackberry", 2),
            Map.entry("raspberry", 2), Map.entry("blueberry", 2), Map.entry("cranberry", 2), Map.entry("juniper", 0),
            Map.entry("gooseberry", 2), Map.entry("golden_raspberry", 2), Map.entry("coconut", 2), Map.entry("cashew", 0),
            Map.entry("avocado", 2), Map.entry("nutmeg", 0), Map.entry("allspice", 0), Map.entry("chilli", 2),
            Map.entry("star_anise", 0), Map.entry("mango", 4), Map.entry("starfruit", 2), Map.entry("candlenut", 0)
    );

    public BinnieTreesFoodItem(Properties properties, List<String> variants) {
        super(properties.food(DEFAULT_FOOD), "binnietrees.food", variants);
        if (!NUTRITION.keySet().equals(SetSupport.copyOf(variants))) {
            throw new IllegalStateException("Binnie's Trees food catalog and nutrition table differ");
        }
    }

    public BinnieTreesFoodItem(Properties properties, String variant) {
        super(properties.food(food(NUTRITION.getOrDefault(variant, 0))), "binnietrees.food", List.of(variant));
        if (!NUTRITION.containsKey(variant)) throw new IllegalArgumentException("Unknown Binnie's Trees food variant " + variant);
    }

    @Override
    public FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        return food(NUTRITION.getOrDefault(value(stack), 0));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.binnietrees.food." + value(stack));
    }

    public int nutrition(ItemStack stack) {
        return NUTRITION.getOrDefault(value(stack), 0);
    }

    @Override
    public ItemStack createStack(String value) {
        ItemStack stack = super.createStack(value);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(variants().indexOf(value) + 1));
        return stack;
    }

    private static FoodProperties food(int nutrition) {
        float seconds = (8 + nutrition * 2) / 20.0F;
        return new FoodProperties(nutrition, nutrition * nutrition / 5.0F, false, seconds,
                java.util.Optional.empty(), java.util.List.of());
    }

    private static final class SetSupport {
        private SetSupport() {}
        static java.util.Set<String> copyOf(List<String> values) {
            return java.util.Set.copyOf(values);
        }
    }
}
