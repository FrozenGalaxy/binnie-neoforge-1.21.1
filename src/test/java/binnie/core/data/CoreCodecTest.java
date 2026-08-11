package binnie.core.data;

import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CoreCodecTest {
    @Test
    void genomeDataPersistentCodecRoundTripsAllFields() {
        GenomeData original = new GenomeData(
                ResourceLocation.fromNamespaceAndPath("binniebotany", "flower"),
                ResourceLocation.fromNamespaceAndPath("binniebotany", "edelweiss"),
                Map.of("primary", "binniebotany:white", "inactive_primary", "binniebotany:blue", "fertility", "3"),
                true,
                17
        );
        var json = GenomeData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        assertEquals(original, GenomeData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void variantAndDesignDataRoundTrip() {
        VariantData variant = new VariantData("binniebotany.flower_color", "ultramarine");
        var variantJson = VariantData.CODEC.encodeStart(JsonOps.INSTANCE, variant).getOrThrow();
        assertEquals(variant, VariantData.CODEC.parse(JsonOps.INSTANCE, variantJson).getOrThrow());

        DesignData design = new DesignData(
                "chevron",
                "diagonal",
                ResourceLocation.fromNamespaceAndPath("binnietrees", "walnut"),
                ResourceLocation.fromNamespaceAndPath("binnietrees", "ebony"),
                3,
                true,
                "crimson",
                "royal_blue"
        );
        var designJson = DesignData.CODEC.encodeStart(JsonOps.INSTANCE, design).getOrThrow();
        assertEquals(design, DesignData.CODEC.parse(JsonOps.INSTANCE, designJson).getOrThrow());
    }

    @Test
    void beverageDataRoundTrip() {
        BeverageData beverage = new BeverageData("binnietrees:apple_brandy", 0xC57A32, 18, 250);
        var json = BeverageData.CODEC.encodeStart(JsonOps.INSTANCE, beverage).getOrThrow();
        assertEquals(beverage, BeverageData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }
}
