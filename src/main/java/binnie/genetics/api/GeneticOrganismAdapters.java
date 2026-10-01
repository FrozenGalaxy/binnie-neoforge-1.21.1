package binnie.genetics.api;

import binnie.botany.genetics.FlowerSpeciesTypes;
import binnie.botany.api.genetics.FlowerLifeStage;
import binnie.core.forestry.ForestryModules;
import forestry.api.IForestryApi;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.api.lepidopterology.genetics.ButterflyLifeStage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registry of the four organism families supported by Binnie Genetics. */
public final class GeneticOrganismAdapters {
    private static final Map<ResourceLocation, GeneticOrganismAdapter> ADAPTERS = new LinkedHashMap<>();

    static {
        register(ForestrySpeciesTypes.BEE);
        register(ForestrySpeciesTypes.TREE);
        register(ForestrySpeciesTypes.BUTTERFLY);
        register(FlowerSpeciesTypes.FLOWER);
    }

    private GeneticOrganismAdapters() {}

    private static void register(ResourceLocation id) {
        ADAPTERS.put(id, new GeneticOrganismAdapter() {
            @Override public ResourceLocation id() { return id; }
            @Override public ISpeciesType<?, ?> speciesType() {
                return IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(id);
            }
            @Override
            public boolean isDnaManipulable(ItemStack stack) {
                if (!supports(stack)) return false;
                ILifeStage stage = IIndividualHandlerItem.getLifeStage(stack);
                if (stage == null) return false;
                if (id.equals(ForestrySpeciesTypes.BEE)) return stage == BeeLifeStage.LARVAE;
                if (id.equals(ForestrySpeciesTypes.TREE)) return stage == TreeLifeStage.POLLEN;
                if (id.equals(ForestrySpeciesTypes.BUTTERFLY)) return stage == ButterflyLifeStage.SERUM;
                if (id.equals(FlowerSpeciesTypes.FLOWER)) return stage == FlowerLifeStage.POLLEN;
                return false;
            }
        });
    }

    public static GeneticOrganismAdapter get(ResourceLocation id) {
        GeneticOrganismAdapter adapter = ADAPTERS.get(id);
        if (adapter == null) throw new IllegalArgumentException("Unsupported genetic organism type: " + id);
        return adapter;
    }

    public static Optional<GeneticOrganismAdapter> findForStack(ItemStack stack) {
        ISpeciesType<?, ?> type = IIndividualHandlerItem.getSpeciesType(stack);
        if (type == null || (type.id().equals(ForestrySpeciesTypes.BUTTERFLY) && !ForestryModules.butterfliesLoaded())) {
            return Optional.empty();
        }
        return Optional.ofNullable(ADAPTERS.get(type.id()));
    }

    public static boolean supports(ItemStack stack) {
        return findForStack(stack).isPresent();
    }

    public static GeneticOrganismAdapter forStack(ItemStack stack) {
        return findForStack(stack).orElseThrow(() ->
                new IllegalArgumentException("Stack does not contain a supported genetic individual"));
    }

    public static List<GeneticOrganismAdapter> all() {
        return ADAPTERS.values().stream()
                .filter(adapter -> !adapter.id().equals(ForestrySpeciesTypes.BUTTERFLY) || ForestryModules.butterfliesLoaded())
                .toList();
    }
}
