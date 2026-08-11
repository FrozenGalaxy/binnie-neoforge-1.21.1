package binnie.botany.item;

import binnie.botany.block.GardenPlantBlock;
import binnie.botany.block.GardenPlantType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

import java.util.Map;

/** Metadata-compatible item form for the five 1.12 Botany plant states. */
public final class GardenPlantItem extends BlockItem {
    public GardenPlantItem(Block block, Properties properties) {
        super(block, properties);
    }

    public ItemStack createStack(GardenPlantType type) {
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.BLOCK_STATE,
                new BlockItemStateProperties(Map.of(GardenPlantBlock.TYPE.getName(), type.getSerializedName())));
        return stack;
    }

    public GardenPlantType type(ItemStack stack) {
        String value = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                .properties().get(GardenPlantBlock.TYPE.getName());
        if (value == null) return GardenPlantType.WEEDS;
        for (GardenPlantType type : GardenPlantType.values()) {
            if (type.getSerializedName().equals(value)) return type;
        }
        return GardenPlantType.WEEDS;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.binniebotany.garden_plant." + type(stack).getSerializedName());
    }
}
