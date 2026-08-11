package binnie.genetics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.ToleranceType;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;

/**
  * Closed, typed representation of every inline allele value used by ForestryCE and Binnie Botany.
  * This avoids stringifying arbitrary allele objects or storing an unstructured NBT blob.
  */
public sealed interface AlleleValue permits AlleleValue.ResourceValue, AlleleValue.IntValue,
        AlleleValue.FloatValue, AlleleValue.BooleanValue, AlleleValue.ToleranceValue, AlleleValue.VectorValue {
    Codec<AlleleValue> CODEC = Codec.STRING.dispatch("type", AlleleValue::type, AlleleValue::codecFor);

    String type();

    Object javaValue();

    default boolean isValidFor(IChromosome<?> chromosome) {
        return encodeFor(chromosome).result().isPresent();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default DataResult<?> encodeFor(IChromosome<?> chromosome) {
        return ((Codec) chromosome.valueCodec()).encodeStart(NbtOps.INSTANCE, javaValue());
    }

    static AlleleValue fromJava(Object value) {
        return switch (value) {
            case ResourceLocation resource -> new ResourceValue(resource);
            case Integer integer -> new IntValue(integer);
            case Float number -> new FloatValue(number);
            case Boolean bool -> new BooleanValue(bool);
            case ToleranceType tolerance -> new ToleranceValue(tolerance);
            case Vec3i vector -> new VectorValue(vector);
            default -> throw new IllegalArgumentException("Unsupported allele value type: " + value.getClass().getName());
        };
    }

    private static MapCodec<? extends AlleleValue> codecFor(String type) {
        return switch (type) {
            case ResourceValue.TYPE -> ResourceValue.CODEC;
            case IntValue.TYPE -> IntValue.CODEC;
            case FloatValue.TYPE -> FloatValue.CODEC;
            case BooleanValue.TYPE -> BooleanValue.CODEC;
            case ToleranceValue.TYPE -> ToleranceValue.CODEC;
            case VectorValue.TYPE -> VectorValue.CODEC;
            default -> throw new IllegalArgumentException("Unknown allele value type: " + type);
        };
    }

    record ResourceValue(ResourceLocation value) implements AlleleValue {
        static final String TYPE = "resource";
        static final MapCodec<ResourceValue> CODEC = ResourceLocation.CODEC.fieldOf("value")
                .xmap(ResourceValue::new, ResourceValue::value);
        @Override public String type() { return TYPE; }
        @Override public Object javaValue() { return value; }
    }

    record IntValue(int value) implements AlleleValue {
        static final String TYPE = "integer";
        static final MapCodec<IntValue> CODEC = Codec.INT.fieldOf("value")
                .xmap(IntValue::new, IntValue::value);
        @Override public String type() { return TYPE; }
        @Override public Object javaValue() { return value; }
    }

    record FloatValue(float value) implements AlleleValue {
        static final String TYPE = "float";
        static final MapCodec<FloatValue> CODEC = Codec.FLOAT.fieldOf("value")
                .xmap(FloatValue::new, FloatValue::value);
        @Override public String type() { return TYPE; }
        @Override public Object javaValue() { return value; }
    }

    record BooleanValue(boolean value) implements AlleleValue {
        static final String TYPE = "boolean";
        static final MapCodec<BooleanValue> CODEC = Codec.BOOL.fieldOf("value")
                .xmap(BooleanValue::new, BooleanValue::value);
        @Override public String type() { return TYPE; }
        @Override public Object javaValue() { return value; }
    }

    record ToleranceValue(ToleranceType value) implements AlleleValue {
        static final String TYPE = "tolerance";
        static final MapCodec<ToleranceValue> CODEC = Codec.STRING.comapFlatMap(name -> {
                    try {
                        return DataResult.success(new ToleranceValue(ToleranceType.valueOf(name)));
                    } catch (IllegalArgumentException exception) {
                        return DataResult.error(() -> "Unknown tolerance allele: " + name);
                    }
                }, value -> value.value().name()).fieldOf("value");
        @Override public String type() { return TYPE; }
        @Override public Object javaValue() { return value; }
    }

    record VectorValue(Vec3i value) implements AlleleValue {
        static final String TYPE = "vector";
        static final MapCodec<VectorValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(value -> value.value().getX()),
                Codec.INT.fieldOf("y").forGetter(value -> value.value().getY()),
                Codec.INT.fieldOf("z").forGetter(value -> value.value().getZ())
        ).apply(instance, (x, y, z) -> new VectorValue(new Vec3i(x, y, z))));
        @Override public String type() { return TYPE; }
        @Override public Object javaValue() { return value; }
    }
}
