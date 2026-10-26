package binnie.genetics.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Data-pack extensible reagents for the Genetics machines. */
public final class GeneticsItemTags {
    public static final TagKey<Item> INCUBATOR_BACTERIA_REAGENTS = tag("incubator/bacteria_reagents");
    public static final TagKey<Item> INCUBATOR_ENZYME_REAGENTS = tag("incubator/enzyme_reagents");
    public static final TagKey<Item> INCUBATOR_POLYMERISING_REAGENTS = tag("incubator/polymerising_bacteria_reagents");
    public static final TagKey<Item> INCUBATOR_VECTOR_REAGENTS = tag("incubator/vector_bacteria_reagents");
    public static final TagKey<Item> ACCLIMATISER_TEMPERATURE_UP = tag("acclimatiser/temperature_up");
    public static final TagKey<Item> ACCLIMATISER_TEMPERATURE_DOWN = tag("acclimatiser/temperature_down");
    public static final TagKey<Item> ACCLIMATISER_HUMIDITY_UP = tag("acclimatiser/humidity_up");
    public static final TagKey<Item> ACCLIMATISER_HUMIDITY_DOWN = tag("acclimatiser/humidity_down");

    private GeneticsItemTags() {}

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("binniegenetics", path));
    }
}
