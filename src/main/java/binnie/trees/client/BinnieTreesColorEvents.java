package binnie.trees.client;

import binnie.trees.BinnieTrees;
import binnie.trees.block.DecorativeLeavesBlock;
import binnie.trees.registry.BinnieTreesWorldContent;
import binnie.trees.forestry.LegacyTreeCatalog;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.platform.item.IColoredItem;
import forestry.core.platform.util.SpeciesUtil;
import forestry.arboriculture.features.ArboricultureItems;
import net.neoforged.bus.api.EventPriority;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = BinnieTrees.MOD_ID, value = Dist.CLIENT)
public final class BinnieTreesColorEvents {
    private static final Map<String, LegacyTreeCatalog.TreeBinding> TREES = LegacyTreeCatalog.load().species().stream()
            .collect(Collectors.toUnmodifiableMap(LegacyTreeCatalog.TreeBinding::id, Function.identity()));

    private BinnieTreesColorEvents() {}
    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var holder : BinnieTreesWorldContent.decorativeLeaves().values()) {
            event.register((state, level, pos, tint) -> tint == 0 ? ((DecorativeLeavesBlock) state.getBlock()).color() : 0xFFFFFF,
                    holder.get());
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void items(RegisterColorHandlersEvent.Item event) {
        for (var entry : BinnieTreesWorldContent.decorativeLeafItems().entrySet()) {
            int color = BinnieTreesWorldContent.decorativeLeaves().get(entry.getKey()).get().color();
            event.register((stack, tint) -> tint == 0 ? 0xFF000000 | color : 0xFFFFFFFF, entry.getValue().get());
        }
    // ForestryCE returns white for all sapling render passes. The 1.12 Binnie models were
    // grayscale two-layer masks tinted with each species' leaf and wood colors, so keep that contract here.
        event.register((stack, tint) -> {
            var species = IIndividualHandlerItem.getSpecies(stack, SpeciesUtil.TREE_TYPE.get());
            if (species != null && species.id().getNamespace().equals(BinnieTrees.MOD_ID)) {
                LegacyTreeCatalog.TreeBinding tree = TREES.get(species.id().getPath());
                if (tree != null) {
                    return switch (tint) {
                        case 0 -> 0xFF000000 | tree.leafColor();
                        case 1 -> 0xFF000000 | tree.woodColor();
                        default -> 0xFFFFFFFF;
                    };
                }
            }
            return stack.getItem() instanceof IColoredItem colored
                    ? 0xFF000000 | colored.getColorFromItemStack(stack, tint)
                    : 0xFFFFFFFF;
        }, ArboricultureItems.TREE_SAPLING.item());
    }
}
