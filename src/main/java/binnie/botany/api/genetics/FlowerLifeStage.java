package binnie.botany.api.genetics;

import binnie.botany.registry.BotanyContent;
import forestry.api.core.genetics.ILifeStage;
import net.minecraft.world.item.Item;

import java.util.Locale;
import java.util.function.Supplier;

public enum FlowerLifeStage implements ILifeStage {
    FLOWER(() -> BotanyContent.FLOWER.get()),
    POLLEN(() -> BotanyContent.POLLEN.get()),
    SEED(() -> BotanyContent.SEED.get());

    private final String name;
    private final Supplier<Item> item;

    FlowerLifeStage(Supplier<Item> item) {
        this.name = name().toLowerCase(Locale.ROOT);
        this.item = item;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Override
    public Item getItemForm() {
        return this.item.get();
    }
}
