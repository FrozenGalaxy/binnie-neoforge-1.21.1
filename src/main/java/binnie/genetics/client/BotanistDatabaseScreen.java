package binnie.genetics.client;

import binnie.botany.genetics.FlowerSpeciesTypes;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Botanist Database and Master Botanist Database. */
public final class BotanistDatabaseScreen extends AbstractSpeciesDatabaseScreen {
    public BotanistDatabaseScreen(GeneticKnowledgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, FlowerSpeciesTypes.FLOWER);
    }
}
