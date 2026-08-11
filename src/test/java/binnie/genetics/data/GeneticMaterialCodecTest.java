package binnie.genetics.data;

import com.mojang.serialization.JsonOps;
import forestry.api.core.ToleranceType;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class GeneticMaterialCodecTest {
    private static final ResourceLocation FLOWER = ResourceLocation.fromNamespaceAndPath("binniebotany", "flower");

    @Test
    void allAlleleValueKindsRoundTrip() {
        List<AlleleValue> values = List.of(
                new AlleleValue.ResourceValue(ResourceLocation.fromNamespaceAndPath("binniebotany", "blue")),
                new AlleleValue.IntValue(7),
                new AlleleValue.FloatValue(1.75F),
                new AlleleValue.BooleanValue(true),
                new AlleleValue.ToleranceValue(ToleranceType.BOTH_2),
                new AlleleValue.VectorValue(new Vec3i(9, 6, 9))
        );
        for (AlleleValue original : values) {
            var encoded = AlleleValue.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
            assertEquals(original, AlleleValue.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        }
    }

    @Test
    void geneSequenceAndSerumPersistentCodecsRoundTrip() {
        GeneticAlleleData allele = new GeneticAlleleData(
                FLOWER,
                ResourceLocation.fromNamespaceAndPath("binniebotany", "primary_color"),
                new AlleleValue.ResourceValue(ResourceLocation.fromNamespaceAndPath("binniebotany", "ultramarine")),
                true,
                GeneticAlleleData.Origin.INACTIVE
        );
        var geneJson = GeneticAlleleData.CODEC.encodeStart(JsonOps.INSTANCE, allele).getOrThrow();
        assertEquals(allele, GeneticAlleleData.CODEC.parse(JsonOps.INSTANCE, geneJson).getOrThrow());

        GeneticSequenceData sequence = new GeneticSequenceData(allele, 83, 47, true);
        var sequenceJson = GeneticSequenceData.CODEC.encodeStart(JsonOps.INSTANCE, sequence).getOrThrow();
        assertEquals(sequence, GeneticSequenceData.CODEC.parse(JsonOps.INSTANCE, sequenceJson).getOrThrow());

        GeneticSerumData serum = new GeneticSerumData(List.of(allele), 83, 12);
        var serumJson = GeneticSerumData.CODEC.encodeStart(JsonOps.INSTANCE, serum).getOrThrow();
        assertEquals(serum, GeneticSerumData.CODEC.parse(JsonOps.INSTANCE, serumJson).getOrThrow());
    }

    @Test
    void geneSequenceAndSerumNetworkCodecsRoundTrip() {
        GeneticAlleleData allele = new GeneticAlleleData(
                FLOWER,
                ResourceLocation.fromNamespaceAndPath("binniebotany", "ph_tolerance"),
                new AlleleValue.ToleranceValue(ToleranceType.UP_1),
                false,
                GeneticAlleleData.Origin.ACTIVE
        );
        var geneBuffer = Unpooled.buffer();
        GeneticAlleleData.STREAM_CODEC.encode(geneBuffer, allele);
        assertEquals(allele, GeneticAlleleData.STREAM_CODEC.decode(geneBuffer));

        GeneticSequenceData sequence = new GeneticSequenceData(allele, 100);
        var sequenceBuffer = Unpooled.buffer();
        GeneticSequenceData.STREAM_CODEC.encode(sequenceBuffer, sequence);
        assertEquals(sequence, GeneticSequenceData.STREAM_CODEC.decode(sequenceBuffer));

        GeneticSerumData serum = new GeneticSerumData(List.of(allele), 100, 16);
        var serumBuffer = Unpooled.buffer();
        GeneticSerumData.STREAM_CODEC.encode(serumBuffer, serum);
        assertEquals(serum, GeneticSerumData.STREAM_CODEC.decode(serumBuffer));
    }

    @Test
    void serumRejectsMixedOrganismTypes() {
        GeneticAlleleData flower = new GeneticAlleleData(FLOWER,
                ResourceLocation.fromNamespaceAndPath("binniebotany", "fertility"), new AlleleValue.IntValue(3), true,
                GeneticAlleleData.Origin.ACTIVE);
        GeneticAlleleData bee = new GeneticAlleleData(ResourceLocation.fromNamespaceAndPath("forestry", "bee_species"),
                ResourceLocation.fromNamespaceAndPath("forestry", "fertility"), new AlleleValue.IntValue(2), true,
                GeneticAlleleData.Origin.ACTIVE);
        assertThrows(IllegalArgumentException.class, () -> new GeneticSerumData(List.of(flower, bee), 100, 16));
    }


    @Test
    void legacySequenceCodecDefaultsToCompletedAndUnanalysed() {
        GeneticAlleleData allele = new GeneticAlleleData(FLOWER,
                ResourceLocation.fromNamespaceAndPath("binniebotany", "fertility"), new AlleleValue.IntValue(3), true,
                GeneticAlleleData.Origin.ACTIVE);
        var legacy = GeneticSequenceData.CODEC.encodeStart(JsonOps.INSTANCE,
                new GeneticSequenceData(allele, 81, 100, false)).getOrThrow().deepCopy();
        legacy.getAsJsonObject().remove("sequenced");
        legacy.getAsJsonObject().remove("analyzed");
        GeneticSequenceData decoded = GeneticSequenceData.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();
        assertEquals(100, decoded.sequenced());
        assertEquals(false, decoded.analyzed());
        assertEquals(allele, decoded.allele());
    }

    @Test
    void legacySerumChargeCodecNormalizesPreSourceCheckpointArrays() {
        GeneticAlleleData allele = new GeneticAlleleData(FLOWER,
                ResourceLocation.fromNamespaceAndPath("binniebotany", "fertility"), new AlleleValue.IntValue(3), true,
                GeneticAlleleData.Origin.ACTIVE);
        var json = GeneticSerumData.CODEC.encodeStart(JsonOps.INSTANCE, new GeneticSerumData(List.of(allele), 100, 16)).getOrThrow();
        var legacy = json.deepCopy();
        legacy.getAsJsonObject().addProperty("charges", 64);
        assertEquals(16, GeneticSerumData.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow().charges());
    }

    @Test
    void sequenceAndSerumRejectInvalidSourceState() {
        GeneticAlleleData allele = new GeneticAlleleData(FLOWER,
                ResourceLocation.fromNamespaceAndPath("binniebotany", "fertility"), new AlleleValue.IntValue(3), true,
                GeneticAlleleData.Origin.ACTIVE);
        GeneticAlleleData duplicateChromosome = new GeneticAlleleData(FLOWER,
                allele.chromosome(), new AlleleValue.IntValue(4), false, GeneticAlleleData.Origin.INACTIVE);
        assertThrows(IllegalArgumentException.class, () -> new GeneticSequenceData(allele, 100, 101, false));
        assertThrows(IllegalArgumentException.class, () -> new GeneticSerumData(List.of(allele), 0, 16));
        assertThrows(IllegalArgumentException.class, () -> new GeneticSerumData(List.of(allele, duplicateChromosome), 100, 16));
        assertThrows(IllegalArgumentException.class, () -> new GeneticSerumData(List.of(allele), 100, 17));
    }

    @Test
    void laboratoryChargePersistentAndNetworkCodecsPreservePartialCells() {
        LaboratoryChargeData charge = new LaboratoryChargeData(7_500);
        var json = LaboratoryChargeData.CODEC.encodeStart(JsonOps.INSTANCE, charge).getOrThrow();
        assertEquals(charge, LaboratoryChargeData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());

        var buffer = Unpooled.buffer();
        LaboratoryChargeData.STREAM_CODEC.encode(buffer, charge);
        assertEquals(charge, LaboratoryChargeData.STREAM_CODEC.decode(buffer));
        assertEquals(new LaboratoryChargeData(2_500), charge.consume(5_000));
        assertThrows(IllegalArgumentException.class, () -> charge.consume(7_501));
    }
}
