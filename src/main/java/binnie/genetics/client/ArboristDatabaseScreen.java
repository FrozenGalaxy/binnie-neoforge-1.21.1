package binnie.genetics.client;

import binnie.genetics.menu.GeneticKnowledgeMenu;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Arborist Database and Master Arborist Database. */
public final class ArboristDatabaseScreen extends AbstractSpeciesDatabaseScreen {
    public ArboristDatabaseScreen(GeneticKnowledgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ForestrySpeciesTypes.TREE);
    }
}
