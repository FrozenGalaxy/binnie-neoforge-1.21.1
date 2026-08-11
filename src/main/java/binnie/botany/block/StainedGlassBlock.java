package binnie.botany.block;

import binnie.botany.registry.BotanyWorldContent;
import binnie.core.catalog.LegacyCatalogs;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** The original Botany stained glass, with its flower-colour identity in block state. */
public final class StainedGlassBlock extends TransparentBlock {
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 79);

    public StainedGlassBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(COLOR);
    }

    /**
      * The 1.12 block dropped nothing normally, but Silk Touch recovered the
      * matching flower-colour metadata. The current state holds that identity and
      * the item component carries it back into inventories.
      */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null) return List.of();
        HolderLookup.RegistryLookup<Enchantment> enchantments = builder.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> silkTouch = enchantments.getOrThrow(Enchantments.SILK_TOUCH);
        ItemEnchantments itemEnchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return itemEnchantments.getLevel(silkTouch) > 0 ? List.of(stackFor(state)) : List.of();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, net.minecraft.core.BlockPos pos, Player player) {
        return stackFor(state);
    }

    private static ItemStack stackFor(BlockState state) {
        String color = LegacyCatalogs.values("binniebotany.flower_color").get(state.getValue(COLOR));
        return BotanyWorldContent.STAINED_GLASS_ITEM.get().createStack(color);
    }
}
