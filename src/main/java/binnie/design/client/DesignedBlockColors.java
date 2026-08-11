package binnie.design.client;

import binnie.core.data.DesignData;
import binnie.core.registry.BinnieDataComponents;
import binnie.design.api.DesignedBlockKind;
import binnie.design.block.DesignedBlock;
import binnie.design.block.DesignedBlockEntity;
import binnie.design.data.DesignMaterialCatalog;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Vanilla block/item tint handlers for the two matching 1.12 material passes. */
public final class DesignedBlockColors {
    private DesignedBlockColors() {}

    public static int blockColor(BlockState state, @Nullable BlockAndTintGetter level,
                                 @Nullable net.minecraft.core.BlockPos pos, int tintIndex) {
        if (!(state.getBlock() instanceof DesignedBlock block)) return 0xFFFFFFFF;
        DesignData data = DesignedBlockEntity.defaultData(block.kind());
        if (level != null && pos != null && level.getBlockEntity(pos) instanceof DesignedBlockEntity designed) {
            data = designed.data();
        }
        return 0xFF000000 | materialColor(data, block.kind(), tintIndex);
    }

    public static int itemColor(ItemStack stack, int tintIndex, DesignedBlockKind kind) {
        DesignData data = stack.getOrDefault(BinnieDataComponents.DESIGN, DesignedBlockEntity.defaultData(kind));
        return 0xFF000000 | materialColor(data, kind, tintIndex);
    }

    static int materialColor(DesignData data, DesignedBlockKind kind, int tintIndex) {
        boolean secondary = tintIndex > 0;
        var material = secondary ? data.secondaryMaterial() : data.primaryMaterial();
        String variant = secondary ? data.secondaryVariant() : data.primaryVariant();
        return switch (kind) {
            case GLASS -> DesignMaterialCatalog.glassColor(material, variant)
                    .orElse(DesignMaterialCatalog.defaultGlassColor());
            case CERAMIC -> DesignMaterialCatalog.ceramicColor(variant)
                    .orElse(DesignMaterialCatalog.defaultGlassColor());
            case WOOD, PANEL -> DesignMaterialCatalog.woodColor(material)
                    .orElse(DesignMaterialCatalog.defaultWoodColor());
        };
    }
}
