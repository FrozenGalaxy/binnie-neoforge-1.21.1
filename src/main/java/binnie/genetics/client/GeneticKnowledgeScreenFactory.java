package binnie.genetics.client;

import binnie.botany.genetics.FlowerSpeciesTypes;
import binnie.genetics.menu.GeneticKnowledgeMenu;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Routes the shared knowledge menu to the concrete original Binnie screen class. */
public class GeneticKnowledgeScreenFactory {
    private GeneticKnowledgeScreenFactory() { }

    public static AbstractContainerScreen<GeneticKnowledgeMenu> create(GeneticKnowledgeMenu menu,
                                                                       Inventory inventory,
                                                                       Component title) {
        return switch (menu.interfaceType()) {
            case ANALYST -> new AnalystScreen(menu, inventory, title);
            case REGISTRY -> new RegistryScreen(menu, inventory, title);
            case GENE_DATABASE -> new GeneDatabaseScreen(menu, inventory, title);
            case ARBORIST_DATABASE -> new ArboristDatabaseScreen(menu, inventory, title);
            case SPECIES_DATABASE -> specialist(menu, inventory, title);
        };
    }

    private static AbstractContainerScreen<GeneticKnowledgeMenu> specialist(GeneticKnowledgeMenu menu,
                                                                            Inventory inventory,
                                                                            Component title) {
        ResourceLocation family = menu.speciesTypeFilter();
        if (ForestrySpeciesTypes.BEE.equals(family)) return new ApiaristDatabaseScreen(menu, inventory, title);
        if (ForestrySpeciesTypes.BUTTERFLY.equals(family)) return new MothDatabaseScreen(menu, inventory, title);
        if (FlowerSpeciesTypes.FLOWER.equals(family)) return new BotanistDatabaseScreen(menu, inventory, title);
        return new ArboristDatabaseScreen(menu, inventory, title);
    }
}
