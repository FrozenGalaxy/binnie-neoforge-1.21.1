package binnie.genetics.client;

import binnie.genetics.menu.GeneticKnowledgeMenu;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Apiarist Database and Master Apiarist Database. */
public final class ApiaristDatabaseScreen extends AbstractSpeciesDatabaseScreen {
    public ApiaristDatabaseScreen(GeneticKnowledgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ForestrySpeciesTypes.BEE);
    }
}
