package binnie.genetics.network;

import binnie.genetics.menu.GeneticKnowledgeMenu;
import binnie.genetics.menu.PunnettSquareMenu;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Genetics payload registration and strict server-side menu validation. */
public final class GeneticsNetwork {
    private GeneticsNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(
                SelectPunnettChromosomePayload.TYPE,
                SelectPunnettChromosomePayload.STREAM_CODEC,
                (payload, context) -> handleSelection(payload, (ServerPlayer) context.player())
        );
        registrar.playToServer(
                SelectDatabaseGenePayload.TYPE,
                SelectDatabaseGenePayload.STREAM_CODEC,
                (payload, context) -> handleDatabaseSelection(payload, (ServerPlayer) context.player())
        );
    }

    public static boolean handleSelection(SelectPunnettChromosomePayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof PunnettSquareMenu menu)) return false;
        if (menu.containerId != payload.containerId()) return false;
        return menu.selectChromosome(payload.chromosomeIndex());
    }

    public static boolean handleDatabaseSelection(SelectDatabaseGenePayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof GeneticKnowledgeMenu menu)) return false;
        if (menu.containerId != payload.containerId()) return false;
        return menu.selectGene(payload.geneIndex());
    }
}
