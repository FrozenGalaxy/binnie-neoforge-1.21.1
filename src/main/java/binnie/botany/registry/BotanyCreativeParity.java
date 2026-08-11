package binnie.botany.registry;

import binnie.core.data.DesignData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/** Source-ordered ceramic examples that replaced 1.12 metadata-backed creative subitems. */
public final class BotanyCreativeParity {
    private BotanyCreativeParity() {}
    public static void acceptCeramicDesigns(CreativeModeTab.Output output) {
        ResourceLocation ceramic = ResourceLocation.fromNamespaceAndPath("binniebotany", "ceramic");
        for (String design : binnie.core.catalog.LegacyCatalogs.values("binniebotany.ceramic_pattern")) {
            output.accept(BotanyWorldContent.CERAMIC_TILE_ITEM.get().create(
                    new DesignData(design, design, ceramic, ceramic, 0, false, "brown", "gold"), 1));
        }
    }
    public static void acceptCeramicBricks(CreativeModeTab.Output output) {
        var colors = binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color");
        for (String color : colors) output.accept(BotanyWorldContent.CERAMIC_BRICK_ITEM.get().createStack(
                binnie.botany.block.CeramicBrickType.TILE, color, color));
        for (var type : binnie.botany.block.CeramicBrickType.values()) {
            if (type.canDouble()) output.accept(BotanyWorldContent.CERAMIC_BRICK_ITEM.get().createStack(type, "brown", "gold"));
        }
        output.accept(BotanyWorldContent.CERAMIC_BRICK_ITEM.get().createStack(binnie.botany.block.CeramicBrickType.SPLIT, "gold", "gold"));
        output.accept(BotanyWorldContent.CERAMIC_BRICK_ITEM.get().createStack(binnie.botany.block.CeramicBrickType.CHEQUERED, "brown", "brown"));
        output.accept(BotanyWorldContent.CERAMIC_BRICK_ITEM.get().createStack(binnie.botany.block.CeramicBrickType.LARGE_BRICK, "gold", "brown"));
    }

}
