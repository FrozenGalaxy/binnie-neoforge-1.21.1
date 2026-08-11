package binnie.trees.forestry;

import forestry.api.core.IProduct;
import forestry.arboriculture.fruit.PodFruit;
import forestry.arboriculture.fruit.ForestryPodType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Objects;

/** Pod fruit with the 1.12 tree's actual host-log tag instead of Forestry's built-in host families. */
public final class LegacyPodFruit extends PodFruit {
    private final TagKey<Block> hostLogs;

    public LegacyPodFruit(boolean dominant, ForestryPodType type, List<IProduct> products, TagKey<Block> hostLogs) {
        super(dominant, type, products);
        this.hostLogs = Objects.requireNonNull(hostLogs, "hostLogs");
    }

    @Override
    public TagKey<Block> getLogTag() {
        return this.hostLogs;
    }
}
