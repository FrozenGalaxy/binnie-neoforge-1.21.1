package binnie.genetics.recipe;

import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.item.GeneticMaterialItem;
import binnie.genetics.registry.GeneticsContent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

/** Matches a filled 1.12 sequence/serum vessel without accepting its blank current carrier. */
public record FilledGeneticMaterialIngredient(GeneticMaterialItem.Kind kind) implements ICustomIngredient {
    public static final MapCodec<FilledGeneticMaterialIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.xmap(GeneticMaterialItem.Kind::byId, GeneticMaterialItem.Kind::id)
                    .fieldOf("kind").forGetter(FilledGeneticMaterialIngredient::kind)
    ).apply(instance, FilledGeneticMaterialIngredient::new));

    @Override
    public boolean test(ItemStack stack) {
        if (!(stack.getItem() instanceof GeneticMaterialItem material) || material.kind(stack) != kind) return false;
        return switch (kind) {
            case SEQUENCE -> stack.has(BinnieDataComponents.GENETIC_SEQUENCE);
            case SERUM, SERUM_ARRAY -> stack.has(BinnieDataComponents.GENETIC_SERUM);
            case GENE -> stack.has(BinnieDataComponents.GENETIC_ALLELE);
        };
    }

    @Override
    public Stream<ItemStack> getItems() {
    // The accepted component value is unconstrained; no
    // fabricated allele is serialized into the recipe itself.
        return Stream.of(GeneticsContent.GENETIC_MATERIAL.get().blank(kind));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return GeneticsRecipeRegistries.FILLED_GENETIC_MATERIAL.get();
    }
}
