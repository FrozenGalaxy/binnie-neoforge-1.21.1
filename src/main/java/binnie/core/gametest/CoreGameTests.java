package binnie.core.gametest;

import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.botany.genetics.FlowerSpeciesType;
import binnie.core.item.FieldKitItem;
import binnie.core.menu.FieldKitMenu;
import binnie.core.registry.CoreContent;
import binnie.core.registry.CoreRegistries;
import binnie.core.storage.CompartmentBlock;
import binnie.core.storage.CompartmentBlockEntity;
import binnie.core.storage.CompartmentTier;
import binnie.genetics.api.GeneticOrganismAdapters;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("binniecore")
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(templateNamespace = "forestry", template = "empty")
    public static void diamondCompartmentPersistsAllCapacityAndMetadata(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        var state = CoreRegistries.COMPARTMENT_BLOCK.get().defaultBlockState()
                .setValue(CompartmentBlock.TIER, CompartmentTier.DIAMOND);
        helper.setBlock(relative, state);
        CompartmentBlockEntity compartment = requireCompartment(helper, relative);
        compartment.backingInventory().setStackInSlot(399, new ItemStack(Items.DIAMOND, 32));
        compartment.setTabName(7, "Rare");
        compartment.setTabIcon(7, new ItemStack(Items.EMERALD));

        CompoundTag saved = compartment.saveWithoutMetadata(helper.getLevel().registryAccess());
        CompartmentBlockEntity reloaded = new CompartmentBlockEntity(helper.absolutePos(relative), state);
        reloaded.setLevel(helper.getLevel());
        reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());

        helper.assertTrue(reloaded.capacity() == 400, "Diamond compartment capacity changed");
        helper.assertTrue(reloaded.backingInventory().getStackInSlot(399).getCount() == 32,
                "Last legal compartment slot did not persist");
        helper.assertTrue("Rare".equals(reloaded.tabName(7)), "Tab name did not persist");
        helper.assertTrue(reloaded.tabIcon(7).is(Items.EMERALD), "Tab icon did not persist");
        helper.assertTrue(reloaded.comparatorOutput() > 0, "Stored content produced no comparator signal");
        helper.succeed();
    }

    @GameTest(templateNamespace = "forestry", template = "empty", timeoutTicks = 200)
    public static void fieldKitAnalysesAfterLegacyDelayAndConsumesExactlyOnePaper(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        ItemStack kit = new ItemStack(CoreContent.FIELD_KIT.get());
        kit.setDamageValue(FieldKitItem.CAPACITY - 1);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, kit);
        FieldKitMenu menu = new FieldKitMenu(71, player.getInventory(), true);
        player.containerMenu = menu;

        ItemStack specimen = FlowerSpeciesType.get().getDefaultSpecies().createIndividual()
                .createStack(FlowerLifeStage.FLOWER);
        helper.assertTrue(menu.slots.size() == 38, "Field Kit did not expose 2 kit and 36 player slots");
        helper.assertTrue(menu.getSlot(FieldKitMenu.INDIVIDUAL_SLOT).x == 70
                        && menu.getSlot(FieldKitMenu.INDIVIDUAL_SLOT).y == 58,
                "Field Kit individual slot moved from its source coordinates");
        helper.assertTrue(menu.getSlot(FieldKitMenu.PAPER_SLOT).x == 208
                        && menu.getSlot(FieldKitMenu.PAPER_SLOT).y == 8,
                "Field Kit paper slot moved from its source coordinates");
        helper.assertTrue(menu.getSlot(FieldKitMenu.INDIVIDUAL_SLOT).mayPlace(specimen)
                        && !menu.getSlot(FieldKitMenu.INDIVIDUAL_SLOT).mayPlace(new ItemStack(Items.STONE))
                        && menu.getSlot(FieldKitMenu.INDIVIDUAL_SLOT).getMaxStackSize() == 1,
                "Field Kit individual validator or source stack limit changed");
        helper.assertTrue(menu.getSlot(FieldKitMenu.PAPER_SLOT).mayPlace(new ItemStack(Items.PAPER))
                        && !menu.getSlot(FieldKitMenu.PAPER_SLOT).mayPlace(specimen)
                        && menu.getSlot(FieldKitMenu.PAPER_SLOT).getMaxStackSize() == FieldKitItem.CAPACITY,
                "Field Kit paper validator or source capacity changed");
        menu.getSlot(FieldKitMenu.INDIVIDUAL_SLOT).set(specimen);

        for (int tick = 0; tick < FieldKitMenu.ANALYSIS_TICKS - 1; tick++) menu.broadcastChanges();
        ItemStack pending = menu.getSlot(FieldKitMenu.INDIVIDUAL_SLOT).getItem();
        helper.assertTrue(!GeneticOrganismAdapters.forStack(pending).read(pending).isAnalyzed(),
                "Field Kit completed before the source 100-tick delay");
        helper.assertTrue(menu.analysisProgress() == FieldKitMenu.ANALYSIS_TICKS - 1,
                "Field Kit did not preserve analysis progress");
        helper.assertTrue(menu.paperRemaining() == 1 && kit.getDamageValue() == FieldKitItem.CAPACITY - 1,
                "Field Kit consumed paper before analysis completed");

        menu.broadcastChanges();
        ItemStack analysed = menu.getSlot(FieldKitMenu.INDIVIDUAL_SLOT).getItem();
        helper.assertTrue(GeneticOrganismAdapters.forStack(analysed).read(analysed).isAnalyzed(),
                "Field Kit did not analyse the specimen at 100 ticks");
        helper.assertTrue(menu.paperRemaining() == 0 && kit.getDamageValue() == FieldKitItem.CAPACITY,
                "Field Kit did not consume exactly one stored paper");
        helper.assertTrue(menu.analysisProgress() == 0, "Completed Field Kit analysis retained stale progress");

        ItemStack analysedSnapshot = analysed.copy();
        menu.removed(player);
        helper.assertTrue(inventoryContains(player.getInventory(), analysedSnapshot),
                "Closing the Field Kit deleted its analysed specimen");
        helper.succeed();
    }

    private static boolean inventoryContains(net.minecraft.world.entity.player.Inventory inventory, ItemStack expected) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, expected)) return true;
        }
        return false;
    }

    private static CompartmentBlockEntity requireCompartment(GameTestHelper helper, BlockPos relative) {
        if (helper.getLevel().getBlockEntity(helper.absolutePos(relative)) instanceof CompartmentBlockEntity compartment) {
            return compartment;
        }
        helper.fail("Compartment block entity was not created");
        throw new IllegalStateException("Unreachable");
    }
}
