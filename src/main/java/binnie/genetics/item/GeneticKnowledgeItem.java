package binnie.genetics.item;

import binnie.core.registry.BinnieDataComponents;
import binnie.core.forestry.ForestryModules;
import binnie.genetics.data.GeneticKnowledgeData;
import binnie.genetics.data.GeneticSpeciesDiscovery;
import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.genetics.tracking.GeneticKnowledgeCatalog;
import binnie.genetics.tracking.GeneticKnowledgeSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Portable snapshot item backed by the server-authoritative per-player discovery tracker. */
public final class GeneticKnowledgeItem extends Item {
    private final GeneticKnowledgeMenu.Mode mode;
    private final GeneticKnowledgeMenu.InterfaceType interfaceType;
    private final net.minecraft.resources.ResourceLocation speciesTypeFilter;

    public GeneticKnowledgeItem(Properties properties, GeneticKnowledgeMenu.Mode mode) {
        this(properties, mode, defaultInterface(mode), null);
    }

    public GeneticKnowledgeItem(Properties properties, GeneticKnowledgeMenu.Mode mode,
                                net.minecraft.resources.ResourceLocation speciesTypeFilter) {
        this(properties, mode, GeneticKnowledgeMenu.InterfaceType.SPECIES_DATABASE, speciesTypeFilter);
    }

    public GeneticKnowledgeItem(Properties properties, GeneticKnowledgeMenu.Mode mode,
                                GeneticKnowledgeMenu.InterfaceType interfaceType,
                                net.minecraft.resources.ResourceLocation speciesTypeFilter) {
        super(properties.stacksTo(1));
        this.mode = mode;
        this.interfaceType = interfaceType;
        this.speciesTypeFilter = speciesTypeFilter;
    }

    private static GeneticKnowledgeMenu.InterfaceType defaultInterface(GeneticKnowledgeMenu.Mode mode) {
        if (mode == GeneticKnowledgeMenu.Mode.ANALYST) return GeneticKnowledgeMenu.InterfaceType.ANALYST;
        if (mode.isRegistry()) return GeneticKnowledgeMenu.InterfaceType.REGISTRY;
        return GeneticKnowledgeMenu.InterfaceType.GENE_DATABASE;
    }

    public GeneticKnowledgeMenu.Mode mode() {
        return mode;
    }

    public GeneticKnowledgeMenu.InterfaceType interfaceType() {
        return interfaceType;
    }

    @javax.annotation.Nullable
    public net.minecraft.resources.ResourceLocation speciesTypeFilter() {
        return speciesTypeFilter;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, stack, hand == InteractionHand.MAIN_HAND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Opens this database from the selected hand. */
    public void open(ServerPlayer serverPlayer, ItemStack stack, boolean mainHand) {
        open(serverPlayer, stack, mainHand, true);
    }

    /** Opens this database from a mounted Lab Stand stack. */
    public void openFromLabStand(ServerPlayer serverPlayer, ItemStack stack) {
        open(serverPlayer, stack, false, false);
    }

    private void open(ServerPlayer serverPlayer, ItemStack stack, boolean mainHand, boolean requiresHeldItem) {
        if (forestry.api.core.genetics.ForestrySpeciesTypes.BUTTERFLY.equals(speciesTypeFilter)
                && !ForestryModules.butterfliesLoaded()) {
            serverPlayer.displayClientMessage(Component.translatable("message.binniegenetics.butterflies_unavailable"), true);
            return;
        }
        GeneticKnowledgeSavedData tracker = GeneticKnowledgeSavedData.get(serverPlayer.serverLevel());
        GeneticKnowledgeData known;
        if (mode.exposesCompleteGeneCatalog(interfaceType)) {
            known = GeneticKnowledgeCatalog.masterGeneDatabase();
        } else if (mode.master()) {
            known = GeneticKnowledgeCatalog.registeredDefaults().merge(tracker.allKnown());
        } else {
            known = tracker.get(serverPlayer.getUUID());
        }
        if (speciesTypeFilter != null) {
            var breedingTracker = GeneticOrganismAdapters.get(speciesTypeFilter).speciesType()
                    .getBreedingTracker(serverPlayer.serverLevel(), serverPlayer.getGameProfile());
            for (var species : breedingTracker.getDiscoveredSpecies()) {
                tracker.discoverSpecies(serverPlayer.getUUID(), new GeneticSpeciesDiscovery(speciesTypeFilter, species));
            }
            known = known.merge(tracker.get(serverPlayer.getUUID()));
        }
        GeneticKnowledgeData snapshot = known.filterSpeciesType(speciesTypeFilter);
        stack.set(BinnieDataComponents.GENETIC_KNOWLEDGE.get(), snapshot);
        serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, menuPlayer) -> new GeneticKnowledgeMenu(id, inventory, snapshot, mode, interfaceType, speciesTypeFilter, mainHand, requiresHeldItem),
                stack.getHoverName()), buffer -> {
            buffer.writeEnum(mode);
            buffer.writeEnum(interfaceType);
            buffer.writeBoolean(speciesTypeFilter != null);
            if (speciesTypeFilter != null) buffer.writeResourceLocation(speciesTypeFilter);
            buffer.writeBoolean(mainHand);
            buffer.writeBoolean(requiresHeldItem);
            GeneticKnowledgeData.STREAM_CODEC.encode(buffer, snapshot);
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GeneticKnowledgeData data = stack.getOrDefault(BinnieDataComponents.GENETIC_KNOWLEDGE.get(), GeneticKnowledgeData.EMPTY);
        tooltip.add(Component.translatable("tooltip.binniegenetics.knowledge.species", data.species().size()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.binniegenetics.knowledge.genes", data.genes().size()).withStyle(ChatFormatting.GRAY));
        if (mode.master()) tooltip.add(Component.translatable("tooltip.binniegenetics.knowledge.master").withStyle(ChatFormatting.GOLD));
    }
}
