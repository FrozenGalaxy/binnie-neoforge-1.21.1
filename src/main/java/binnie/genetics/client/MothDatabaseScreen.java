package binnie.genetics.client;

import binnie.genetics.menu.GeneticKnowledgeMenu;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Moth Database and Master Moth Database. */
public final class MothDatabaseScreen extends AbstractSpeciesDatabaseScreen {
    public MothDatabaseScreen(GeneticKnowledgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ForestrySpeciesTypes.BUTTERFLY);
    }
}
