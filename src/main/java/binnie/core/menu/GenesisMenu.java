package binnie.core.menu;

import binnie.core.item.GenesisItem;
import binnie.core.registry.CoreRegistries;
import binnie.genetics.api.GeneticOrganismAdapter;
import binnie.genetics.api.GeneticOrganismAdapters;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.IChromosome;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-authoritative creative genome editor using ForestryCE's inline allele model. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class GenesisMenu extends AbstractContainerMenu {
    private final Player player;
    private final boolean mainHand;
    private final List<GeneticOrganismAdapter> adapters = GeneticOrganismAdapters.all();
    private final Map<IChromosome<?>, AllelePair<?>> selectedPairs = new IdentityHashMap<>();
    private int typeIndex;
    private int speciesIndex;
    private int stageIndex;
    private int chromosomeIndex;
    private boolean analyzed = true;

    public static GenesisMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new GenesisMenu(containerId, inventory, buffer.readBoolean());
    }

    public GenesisMenu(int containerId, Inventory inventory, boolean mainHand) {
        super(CoreRegistries.GENESIS_MENU.get(), containerId);
        this.player = inventory.player;
        this.mainHand = mainHand;
        resetGenome();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 129 + col * 18, 140 + row * 18));
        }
        for (int col = 0; col < 9; col++) {
            boolean locked = mainHand && col == inventory.selected;
            addSlot(new LockedHandSlot(inventory, col, 129 + col * 18, 198, locked));
        }
    }

    private ItemStack heldGenesis() { return mainHand ? player.getMainHandItem() : player.getOffhandItem(); }

    public GeneticOrganismAdapter adapter() {
        if (adapters.isEmpty()) throw new IllegalStateException("No genetic organism adapters are registered");
        return adapters.get(Math.clamp(typeIndex, 0, adapters.size() - 1));
    }
    public ISpeciesType<?, ?> speciesType() { return adapter().speciesType(); }
    public List<? extends ISpecies<?>> species() { return speciesType().getAllSpecies(); }
    public List<ILifeStage> stages() { return List.copyOf(speciesType().getLifeStages()); }
    public List<IChromosome<?>> chromosomes() { return adapter().chromosomes(); }
    public ISpecies<?> selectedSpecies() { return species().get(Math.clamp(speciesIndex, 0, Math.max(0, species().size() - 1))); }
    public ILifeStage selectedStage() { return stages().get(Math.clamp(stageIndex, 0, Math.max(0, stages().size() - 1))); }
    public IChromosome<?> selectedChromosome() { return chromosomes().get(Math.clamp(chromosomeIndex, 0, Math.max(0, chromosomes().size() - 1))); }
    public int typeIndex() { return typeIndex; }
    public int speciesIndex() { return speciesIndex; }
    public int stageIndex() { return stageIndex; }
    public int chromosomeIndex() { return chromosomeIndex; }
    public int typeCount() { return adapters.size(); }
    public boolean analyzed() { return analyzed; }

    public AllelePair<?> selectedPair() {
        IChromosome<?> chromosome = selectedChromosome();
        return selectedPairs.getOrDefault(chromosome, selectedSpecies().getDefaultGenome().getChromosomes().get(chromosome));
    }

    public AllelePair<?> pairFor(IChromosome<?> chromosome) {
        return selectedPairs.getOrDefault(chromosome,
                selectedSpecies().getDefaultGenome().getChromosomes().get(chromosome));
    }

    public ItemStack preview(ILifeStage stage) {
        try {
            ISpecies species = selectedSpecies();
            IGenome genome = species.getDefaultGenome().copyWithPairs(selectedPairs);
            IIndividual individual = species.createIndividual(genome);
            if (analyzed) individual.analyze();
            return individual.createStack(stage);
        } catch (RuntimeException exception) {
            return ItemStack.EMPTY;
        }
    }

    public List<Allele<?>> availableAlleles() {
        IChromosome chromosome = selectedChromosome();
        // The 1.12 Genesis used a TreeSet comparator: numeric alleles were ordered by their
        // actual magnitude, rather than by the order in which species happened to register.
        // Keying by value also mirrors that comparator's de-duplication of dominant/recessive
        // instances which represent the same selectable value.
        LinkedHashMap<Object, Allele<?>> values = new LinkedHashMap<>();
        addAllele(values, speciesType().getKaryotype().getDefaultAllele(chromosome));
        for (ISpecies<?> candidate : species()) {
            AllelePair<?> pair = candidate.getDefaultGenome().getAllelePair(chromosome);
            addAllele(values, pair.active());
            addAllele(values, pair.inactive());
        }
        addAllele(values, selectedPair().active());
        addAllele(values, selectedPair().inactive());
        ArrayList<Allele<?>> sorted = new ArrayList<>(values.values());
        sorted.sort(Comparator.comparing(Allele::value, GenesisMenu::compareAlleleValues));
        return List.copyOf(sorted);
    }

    private static void addAllele(Map<Object, Allele<?>> values, Allele<?> allele) {
        values.putIfAbsent(allele.value(), allele);
    }

    static int compareAlleleValues(Object first, Object second) {
        if (first == second) return 0;
        if (first instanceof Number a && second instanceof Number b)
            return Double.compare(a.doubleValue(), b.doubleValue());
        if (first instanceof net.minecraft.core.Vec3i a && second instanceof net.minecraft.core.Vec3i b) {
            int volume = Long.compare((long) a.getX() * a.getY() * a.getZ(), (long) b.getX() * b.getY() * b.getZ());
            if (volume != 0) return volume;
            int x = Integer.compare(a.getX(), b.getX());
            if (x != 0) return x;
            int y = Integer.compare(a.getY(), b.getY());
            return y != 0 ? y : Integer.compare(a.getZ(), b.getZ());
        }
        if (first instanceof Enum<?> a && second instanceof Enum<?> b && a.getDeclaringClass() == b.getDeclaringClass())
            return Integer.compare(a.ordinal(), b.ordinal());
        return String.valueOf(first).compareToIgnoreCase(String.valueOf(second));
    }

    public boolean apply(Action action, int delta) {
        if (!stillValid(player) || adapters.isEmpty()) return false;
        if (species().isEmpty() || stages().isEmpty() || chromosomes().isEmpty()) return false;
        if (delta == 0 && action != Action.CREATE) delta = 1;
        return switch (action) {
            case TYPE -> { typeIndex = Math.floorMod(typeIndex + delta, adapters.size()); speciesIndex = stageIndex = chromosomeIndex = 0; resetGenome(); yield true; }
            case SPECIES -> { speciesIndex = Math.floorMod(speciesIndex + delta, species().size()); chromosomeIndex = 0; resetGenome(); yield true; }
            case STAGE -> { stageIndex = Math.floorMod(stageIndex + delta, stages().size()); yield true; }
            case CHROMOSOME -> { chromosomeIndex = Math.floorMod(chromosomeIndex + delta, chromosomes().size()); yield true; }
            case ACTIVE -> cycleAllele(true, delta);
            case INACTIVE -> cycleAllele(false, delta);
            case RESET -> { resetGenome(); yield true; }
            case TOGGLE_ANALYZED -> { analyzed = !analyzed; yield true; }
            case CREATE -> createResult();
        };
    }

    /** Applies an absolute, bounds-checked GUI selection on both logical sides. */
    public boolean select(Action action, int index) {
        if (!stillValid(player) || adapters.isEmpty()) return false;
        return switch (action) {
            case TYPE -> {
                if (index < 0 || index >= adapters.size()) yield false;
                typeIndex = index;
                speciesIndex = stageIndex = chromosomeIndex = 0;
                resetGenome();
                yield true;
            }
            case CHROMOSOME -> {
                if (index < 0 || index >= chromosomes().size()) yield false;
                chromosomeIndex = index;
                yield true;
            }
            case ACTIVE, INACTIVE -> selectAllele(action == Action.ACTIVE, index);
            case STAGE -> {
                if (index < 0 || index >= stages().size()) yield false;
                stageIndex = index;
                yield true;
            }
            default -> false;
        };
    }

    private boolean selectAllele(boolean active, int index) {
        List<Allele<?>> values = availableAlleles();
        if (index < 0 || index >= values.size()) return false;
        IChromosome chromosome = selectedChromosome();
        AllelePair current = selectedPair();
        Allele replacement = values.get(index);
        selectedPairs.put(chromosome, active
                ? new AllelePair(replacement, current.inactive())
                : new AllelePair(current.active(), replacement));
        return true;
    }

    private boolean cycleAllele(boolean active, int delta) {
        IChromosome chromosome = selectedChromosome();
        AllelePair current = selectedPair();
        List<Allele<?>> values = availableAlleles();
        if (values.isEmpty()) return false;
        Allele currentAllele = active ? current.active() : current.inactive();
        int index = Math.max(0, values.indexOf(currentAllele));
        Allele replacement = values.get(Math.floorMod(index + delta, values.size()));
        selectedPairs.put(chromosome, active
                ? new AllelePair(replacement, current.inactive())
                : new AllelePair(current.active(), replacement));
        return true;
    }

    private void resetGenome() {
        selectedPairs.clear();
        if (adapters.isEmpty() || species().isEmpty()) return;
        selectedPairs.putAll(selectedSpecies().getDefaultGenome().getChromosomes());
    }

    private boolean createResult() {
        if (!getCarried().isEmpty()) return false;
        try {
            ISpecies species = selectedSpecies();
            IGenome genome = species.getDefaultGenome().copyWithPairs(selectedPairs);
            IIndividual individual = species.createIndividual(genome);
            if (analyzed) individual.analyze();
            ItemStack output = individual.createStack(selectedStage());
            if (output.isEmpty()) return false;
            setCarried(output);
            broadcastChanges();
            return true;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return heldGenesis().getItem() instanceof GenesisItem;
    }

    public enum Action { TYPE, SPECIES, STAGE, CHROMOSOME, ACTIVE, INACTIVE, RESET, CREATE, TOGGLE_ANALYZED }
}
