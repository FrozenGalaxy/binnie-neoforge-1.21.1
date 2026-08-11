package binnie.genetics.tracking;

import binnie.genetics.api.GeneticOrganismAdapters;
import binnie.genetics.data.AlleleValue;
import binnie.genetics.data.GeneticKnowledgeData;
import binnie.genetics.data.GeneticKnowledgeEntry;
import binnie.genetics.data.GeneticSpeciesDiscovery;
import com.mojang.serialization.DataResult;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.IChromosome;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative per-player discovery storage, persisted in the overworld. */
public final class GeneticKnowledgeSavedData extends SavedData {
    private static final String FILE_NAME = "binnie_genetics_knowledge";
    private static final String PLAYERS = "Players";
    private static final String PLAYER = "Player";
    private static final String KNOWLEDGE = "Knowledge";

    private final Map<UUID, GeneticKnowledgeData> players = new HashMap<>();

    public static GeneticKnowledgeSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is unavailable for Genetics knowledge storage");
        SavedData.Factory<GeneticKnowledgeSavedData> factory = new SavedData.Factory<>(
                GeneticKnowledgeSavedData::new,
                GeneticKnowledgeSavedData::load
        );
        return overworld.getDataStorage().computeIfAbsent(factory, FILE_NAME);
    }

    public GeneticKnowledgeData get(UUID player) {
        return players.getOrDefault(player, GeneticKnowledgeData.EMPTY);
    }

    public boolean discoverGene(UUID player, GeneticKnowledgeEntry gene) {
        GeneticKnowledgeData current = get(player);
        GeneticKnowledgeData next = current.withGene(gene);
        if (next.equals(current)) return false;
        players.put(player, next);
        setDirty();
        return true;
    }

    public boolean discoverSpecies(UUID player, GeneticSpeciesDiscovery species) {
        GeneticKnowledgeData current = get(player);
        GeneticKnowledgeData next = current.withSpecies(species);
        if (next.equals(current)) return false;
        players.put(player, next);
        setDirty();
        return true;
    }

    public boolean discoverIndividual(UUID player, IIndividual individual) {
        var adapter = GeneticOrganismAdapters.get(individual.getType().id());
        GeneticKnowledgeData current = get(player);
        GeneticKnowledgeData next = current
                .withSpecies(new GeneticSpeciesDiscovery(adapter.id(), individual.getSpecies().id()))
                .withSpecies(new GeneticSpeciesDiscovery(adapter.id(), individual.getInactiveSpecies().id()));
        for (IChromosome<?> chromosome : adapter.chromosomes()) {
            AllelePair<?> pair = individual.getGenome().getAllelePair(chromosome);
            next = next.withGene(new GeneticKnowledgeEntry(adapter.id(), chromosome.id(),
                    AlleleValue.fromJava(pair.active().value()), pair.active().dominant()));
            next = next.withGene(new GeneticKnowledgeEntry(adapter.id(), chromosome.id(),
                    AlleleValue.fromJava(pair.inactive().value()), pair.inactive().dominant()));
        }
        if (next.equals(current)) return false;
        players.put(player, next);
        setDirty();
        return true;
    }

    /** Union used by master database/registry items. */
    public GeneticKnowledgeData allKnown() {
        GeneticKnowledgeData result = GeneticKnowledgeData.EMPTY;
        for (GeneticKnowledgeData value : players.values()) result = result.merge(value);
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        players.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID(PLAYER, entry.getKey());
            Tag encoded = GeneticKnowledgeData.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue())
                    .getOrThrow(error -> new IllegalStateException("Could not encode Genetics knowledge: " + error));
            playerTag.put(KNOWLEDGE, encoded);
            list.add(playerTag);
        });
        tag.put(PLAYERS, list);
        return tag;
    }

    private static GeneticKnowledgeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GeneticKnowledgeSavedData data = new GeneticKnowledgeSavedData();
        ListTag list = tag.getList(PLAYERS, Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag playerTag = (CompoundTag) element;
            if (!playerTag.hasUUID(PLAYER) || !playerTag.contains(KNOWLEDGE)) continue;
            DataResult<GeneticKnowledgeData> decoded = GeneticKnowledgeData.CODEC.parse(NbtOps.INSTANCE, playerTag.get(KNOWLEDGE));
            decoded.resultOrPartial(message -> System.err.println("Skipping invalid Genetics knowledge entry: " + message))
                    .ifPresent(value -> data.players.put(playerTag.getUUID(PLAYER), value));
        }
        return data;
    }
}
