package binnie.botany.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record GardeningJeiRecipe(ResourceLocation id, List<List<ItemStack>> inputs,
                                 List<ItemStack> outputs, Component description) {
    public GardeningJeiRecipe {
        inputs = inputs.stream().map(list -> list.stream().map(ItemStack::copy).toList()).toList();
        outputs = outputs.stream().map(ItemStack::copy).toList();
    }
}
