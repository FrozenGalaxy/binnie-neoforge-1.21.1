package binnie.core.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public record MachineRecipe(
        ResourceLocation machine,
        Ingredient input,
        int inputCount,
        ItemStack output,
        int duration,
        int energyPerTick,
        FluidStack fluidInput
) implements Recipe<RecipeInput> {
    public static final MapCodec<MachineRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("machine").forGetter(MachineRecipe::machine),
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(MachineRecipe::input),
            Codec.INT.optionalFieldOf("input_count", 1).forGetter(MachineRecipe::inputCount),
            ItemStack.CODEC.fieldOf("output").forGetter(MachineRecipe::output),
            Codec.INT.optionalFieldOf("duration", 200).forGetter(MachineRecipe::duration),
            Codec.INT.optionalFieldOf("energy_per_tick", 20).forGetter(MachineRecipe::energyPerTick),
            FluidStack.OPTIONAL_CODEC.optionalFieldOf("fluid_input", FluidStack.EMPTY).forGetter(MachineRecipe::fluidInput)
    ).apply(instance, MachineRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> STREAM_CODEC = StreamCodec.of(
            MachineRecipe::encode,
            MachineRecipe::decode
    );

    public MachineRecipe {
        if (inputCount <= 0 || duration <= 0 || energyPerTick < 0 || output.isEmpty()) {
            throw new IllegalArgumentException("Invalid machine recipe for " + machine);
        }
        output = output.copy();
        fluidInput = fluidInput.copy();
    }

    public boolean matches(ResourceLocation machineId, ItemStack stack, FluidStack fluid) {
        if (!machine.equals(machineId) || stack.getCount() < inputCount || !input.test(stack)) {
            return false;
        }
        return fluidInput.isEmpty() || (FluidStack.isSameFluidSameComponents(fluidInput, fluid) && fluid.getAmount() >= fluidInput.getAmount());
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(input);
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MachineRegistries.MACHINE_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return MachineRegistries.MACHINE_RECIPE_TYPE.get();
    }

    private static void encode(RegistryFriendlyByteBuf buffer, MachineRecipe recipe) {
        ResourceLocation.STREAM_CODEC.encode(buffer, recipe.machine);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
        ByteBufCodecs.VAR_INT.encode(buffer, recipe.inputCount);
        ItemStack.STREAM_CODEC.encode(buffer, recipe.output);
        ByteBufCodecs.VAR_INT.encode(buffer, recipe.duration);
        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energyPerTick);
        FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.fluidInput);
    }

    private static MachineRecipe decode(RegistryFriendlyByteBuf buffer) {
        return new MachineRecipe(
                ResourceLocation.STREAM_CODEC.decode(buffer),
                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ItemStack.STREAM_CODEC.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer)
        );
    }

    public static final class Serializer implements RecipeSerializer<MachineRecipe> {
        @Override
        public MapCodec<MachineRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
