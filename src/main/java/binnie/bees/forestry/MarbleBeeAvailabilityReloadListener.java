package binnie.bees.forestry;

import forestry.apiculture.bees.genetics.ApicultureReloadHandler;
import forestry.apiculture.bees.genetics.BeeSpeciesDefinition;
import forestry.apiculture.bees.genetics.BeeSpeciesManager;
import forestry.core.engine.genetics.GeneticsReloadHandler;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Removes the marble-only bee line when the active datapacks provide no marble hive blocks. */
public final class MarbleBeeAvailabilityReloadListener extends ContextAwareReloadListener {
    private static final ResourceLocation MARBLE_TAG_ID = ResourceLocation.fromNamespaceAndPath("binniebees", "marble_hive_replaceables");
    private static final net.minecraft.tags.TagKey<Block> MARBLE_REPLACEABLES =
            net.minecraft.tags.TagKey.create(Registries.BLOCK, MARBLE_TAG_ID);
    private static final Set<ResourceLocation> MARBLE_LINE = Set.of(
            id("marble"), id("roman"), id("greek"), id("classical"));

    private final net.minecraft.world.item.crafting.RecipeManager recipeManager;

    private MarbleBeeAvailabilityReloadListener(net.minecraft.world.item.crafting.RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new MarbleBeeAvailabilityReloadListener(event.getServerResources().getRecipeManager()));
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return barrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
            boolean hasMarble = getRegistryLookup().lookupOrThrow(Registries.BLOCK)
                    .get(MARBLE_REPLACEABLES)
                    .map(HolderSet::size)
                    .orElse(0) > 0;

            Map<ResourceLocation, BeeSpeciesDefinition> loaded = BeeSpeciesManager.INSTANCE.getDefinitions();
            Map<ResourceLocation, BeeSpeciesDefinition> effective = loaded;
            if (!hasMarble) {
                effective = new LinkedHashMap<>(loaded);
                MARBLE_LINE.forEach(effective::remove);
                effective = Map.copyOf(effective);
            }

            BeeSpeciesManager.INSTANCE.setDefinitions(effective);
            ApicultureReloadHandler.rebuildSpecies(effective);
            GeneticsReloadHandler.rebuildMutations(recipeManager);
            if (!hasMarble) {
                forestry.Forestry.LOGGER.info("Skipped the Binnie's Bees marble lineage: no blocks are tagged {}",
                        MARBLE_TAG_ID);
            }
        }, gameExecutor);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("binniebees", path);
    }
}
