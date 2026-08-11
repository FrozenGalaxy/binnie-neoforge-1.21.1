package binnie.botany.client;

import binnie.botany.client.binniegenetics.FlowerAnalyzerPlugin;
import binnie.botany.genetics.FlowerSpeciesTypes;
import binnie.trees.forestry.LegacyButterflyCatalog;
import binnie.trees.forestry.LegacyTreeCatalog;
import forestry.api.client.plugin.IClientRegistration;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.function.Consumer;

/** Client-side ForestryCE registrations for Binnie's custom organisms and 1.12 models. */
public final class BinnieForestryClientRegistration implements Consumer<IClientRegistration> {
    @Override
    public void accept(IClientRegistration registration) {
        registration.setAnalyzerPlugin(FlowerSpeciesTypes.FLOWER, new FlowerAnalyzerPlugin());
        for (LegacyTreeCatalog.TreeBinding tree : LegacyTreeCatalog.load().species()) {
            String model = tree.saplingType().toLowerCase(Locale.ROOT);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("binnietrees", tree.id());
            registration.setSaplingModel(id,
                    ResourceLocation.fromNamespaceAndPath("binnietrees", "block/saplings/tree_" + model),
                    ResourceLocation.fromNamespaceAndPath("binnietrees", "item/saplings/tree_" + model));
        }
        for (LegacyButterflyCatalog.Species species : LegacyButterflyCatalog.SPECIES) {
            registration.setButterflySprites(species.resourceId(),
                    ResourceLocation.fromNamespaceAndPath("binnietrees", "item/butterflies/" + species.id()),
                    ResourceLocation.fromNamespaceAndPath("binnietrees", "textures/entity/butterflies/" + species.id() + ".png"));
        }
    }
}
