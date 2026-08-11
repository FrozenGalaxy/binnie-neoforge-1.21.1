package binnie.design.gametest;

import binnie.core.data.DesignData;
import binnie.core.registry.BinnieDataComponents;
import binnie.design.api.DesignedBlockKind;
import binnie.design.block.DesignedBlockEntity;
import binnie.trees.registry.BinnieTreesDesignContent;
import binnie.botany.registry.BotanyWorldContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("binniecore")
@PrefixGameTestTemplate(false)
public final class DesignWorldGameTests {
    private DesignWorldGameTests() {}

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 100)
    public static void pairedMaterialsTransformAndSaveReloadLosslessly(GameTestHelper helper) {
        BlockPos first = new BlockPos(2, 2, 2);
        helper.setBlock(first, BinnieTreesDesignContent.PATTERNED_WOOD_BLOCK.get());
        DesignedBlockEntity original = (DesignedBlockEntity) helper.getBlockEntity(first);
        DesignData data = new DesignData("diamond", "diamond", BuiltInRegistries.ITEM.getKey(Items.OAK_PLANKS),
                BuiltInRegistries.ITEM.getKey(Items.BIRCH_PLANKS), 2, true);
        original.setData(data);
        var tag = original.saveWithoutMetadata(helper.getLevel().registryAccess());

        BlockPos second = new BlockPos(5, 2, 2);
        helper.setBlock(second, BinnieTreesDesignContent.PATTERNED_WOOD_BLOCK.get());
        DesignedBlockEntity restored = (DesignedBlockEntity) helper.getBlockEntity(second);
        restored.loadWithComponents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(restored.data().equals(data), "DesignData changed across block-entity save/load");
        restored.rotate();
        restored.mirror();
        helper.assertTrue(restored.data().rotation() == 3 && !restored.data().mirrored(), "Rotation/mirror actions were not persisted");
        ItemStack clone = BinnieTreesDesignContent.PATTERNED_WOOD.get().create(restored.data(), 1);
        helper.assertTrue(clone.get(BinnieDataComponents.DESIGN).equals(restored.data()), "Clone item lost DesignData");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 100)
    public static void allFourDynamicBlockFamiliesCarryTypedData(GameTestHelper helper) {
        var kinds = new DesignedBlockKind[]{DesignedBlockKind.WOOD, DesignedBlockKind.PANEL, DesignedBlockKind.GLASS, DesignedBlockKind.CERAMIC};
        for (int i = 0; i < kinds.length; i++) {
            DesignedBlockKind kind = kinds[i];
            BlockPos pos = new BlockPos(2 + i * 3, 2, 2);
            helper.setBlock(pos, switch (kind) {
                case WOOD -> BinnieTreesDesignContent.PATTERNED_WOOD_BLOCK.get();
                case PANEL -> BinnieTreesDesignContent.PATTERNED_PANEL_BLOCK.get();
                case GLASS -> BinnieTreesDesignContent.PATTERNED_GLASS_BLOCK.get();
                case CERAMIC -> BotanyWorldContent.CERAMIC_TILE.get();
            });
            DesignedBlockEntity entity = (DesignedBlockEntity) helper.getBlockEntity(pos);
            DesignData data = kind == DesignedBlockKind.CERAMIC
                    ? new DesignData("diamond", "diamond", net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("binniebotany", "ceramic"),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("binniebotany", "ceramic"), 0, false, "white", "black")
                    : BinnieTreesDesignContent.example(kind, i == 0 ? "octagon" : i == 1 ? "barred" : "ringed",
                    kind == DesignedBlockKind.GLASS ? Items.GLASS : Items.SPRUCE_PLANKS,
                    kind == DesignedBlockKind.GLASS ? Items.WHITE_STAINED_GLASS : Items.CHERRY_PLANKS);
            entity.setData(data);
            helper.assertTrue(entity.data().equals(data), kind + " block rejected valid dynamic design data");
        }
        helper.succeed();
    }
}
