package binnie.botany.client;

import binnie.botany.BinnieBotany;
import binnie.botany.api.genetics.IFlower;
import binnie.botany.block.FlowerBlockEntity;
import binnie.botany.block.StainedGlassBlock;
import binnie.botany.genetics.FlowerChromosomes;
import binnie.botany.genetics.FlowerColor;
import binnie.botany.genetics.FlowerSpeciesType;
import binnie.botany.registry.BotanyContent;
import binnie.botany.registry.BotanyWorldContent;
import binnie.botany.registry.BotanyMachines;
import forestry.core.engine.genetics.ItemGE;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Genetic item and planted-flower tinting without loading client classes on a server. */
@EventBusSubscriber(modid = BinnieBotany.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("deprecation") // 1.21.1 runtime layers for component-rendered blocks.
public class BotanyClientEvents {
    private BotanyClientEvents() {}

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (!(ItemGE.getIndividual(stack) instanceof IFlower flower)) return 0xFFFFFFFF;
            return argb(color(flower, tintIndex));
        }, BotanyContent.FLOWER.get(), BotanyContent.POLLEN.get(), BotanyContent.SEED.get());
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFFFF;
            String value = BotanyContent.PIGMENT.get().value(stack);
            return argb(FlowerSpeciesType.get().getColorSafe(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, value)).rgb());
        }, BotanyContent.PIGMENT.get());
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFFFF;
            int index = binnie.botany.item.StainedGlassItem.colorIndex(stack);
            String color = binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color").get(index);
            return argb(FlowerSpeciesType.get().getColorSafe(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, color)).rgb());
        }, BotanyWorldContent.STAINED_GLASS_ITEM.get());
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFFFF;
            String color = binnie.botany.item.CeramicItem.color(stack);
            return argb(FlowerSpeciesType.get().getColorSafe(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, color)).rgb());
        }, BotanyWorldContent.CERAMIC_ITEM.get());
        event.register((stack, tintIndex) -> {
            if (tintIndex == 1) return argb(FlowerSpeciesType.get().getColorSafe(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, binnie.botany.item.CeramicBrickItem.first(stack))).rgb());
            if (tintIndex == 2) return argb(FlowerSpeciesType.get().getColorSafe(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, binnie.botany.item.CeramicBrickItem.second(stack))).rgb());
            return 0xFFFFFFFF;
        }, BotanyWorldContent.CERAMIC_BRICK_ITEM.get());
        event.register((stack, tintIndex) -> {
            var tube = binnie.botany.farming.BotanyTubeVariants.physical(BotanyContent.INSULATED_TUBE.get().value(stack));
            return switch (tintIndex) {
                case 0 -> 0xFFFFFFFF;
                case 1 -> argb(tube.materialColor());
                case 2 -> argb(tube.insulationColor());
                default -> 0xFFFFFFFF;
            };
        }, BotanyContent.INSULATED_TUBE.get());
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFFFF;
            String value = BotanyContent.CLAY.get().value(stack);
            return argb(FlowerSpeciesType.get().getColorSafe(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, value)).rgb());
        }, BotanyContent.CLAY.get());
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof FlowerBlockEntity blockEntity)) return 0xFFFFFFFF;
            IFlower flower = blockEntity.getFlower();
            return flower == null ? 0xFFFFFF : color(flower, tintIndex);
        }, BotanyWorldContent.FLOWER_BLOCK.get());
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFFFF;
            String color = binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color").get(state.getValue(StainedGlassBlock.COLOR));
            return FlowerSpeciesType.get().getColorSafe(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, color)).rgb();
        }, BotanyWorldContent.STAINED_GLASS.get());
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFFFF;
            String color = binnie.core.catalog.LegacyCatalogs.values("binniebotany.flower_color").get(state.getValue(binnie.botany.block.CeramicBlock.COLOR));
            return FlowerSpeciesType.get().getColorSafe(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, color)).rgb();
        }, BotanyWorldContent.CERAMIC.get());
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof binnie.botany.block.CeramicBrickBlockEntity be)) return 0xFFFFFFFF;
            String color = tintIndex == 1 ? be.first() : tintIndex == 2 ? be.second() : "white";
            return tintIndex == 1 || tintIndex == 2 ? FlowerSpeciesType.get().getColorSafe(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, color)).rgb() : 0xFFFFFFFF;
        }, BotanyWorldContent.CERAMIC_BRICK.get());
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(BotanyWorldContent.CERAMIC_BRICK_ITEM.get(),
                    ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, "ceramic_brick_type"),
                    (stack, level, entity, seed) -> binnie.botany.item.CeramicBrickItem.type(stack).ordinal());
            ResourceLocation soilVariant = ResourceLocation.fromNamespaceAndPath(BinnieBotany.MOD_ID, "soil_variant");
            for (var item : java.util.List.of(BotanyWorldContent.SOIL_ITEM.get(),
                    BotanyWorldContent.LOAM_ITEM.get(), BotanyWorldContent.FLOWERBED_ITEM.get())) {
                ItemProperties.register(item, soilVariant,
                        (stack, level, entity, seed) -> binnie.botany.item.SoilBlockItem.modelVariant(stack));
            }
            ItemBlockRenderTypes.setRenderLayer(BotanyWorldContent.STAINED_GLASS.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(BotanyWorldContent.FLOWER_BLOCK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(BotanyWorldContent.GARDEN_PLANT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(BotanyWorldContent.CERAMIC_BRICK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(BotanyMachines.REGISTRY.block("tileworker").get(), RenderType.cutout());
        });
    }

    private static int argb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    private static int color(IFlower flower, int tintIndex) {
        var chromosome = tintIndex == 0 ? FlowerChromosomes.STEM : tintIndex == 1 ? FlowerChromosomes.PRIMARY_COLOR : FlowerChromosomes.SECONDARY_COLOR;
        FlowerColor color = FlowerSpeciesType.get().getColor(flower.getGenome().getActiveValue(chromosome));
        return flower.isWilted() ? color.wiltedRgb() : color.rgb();
    }
}
