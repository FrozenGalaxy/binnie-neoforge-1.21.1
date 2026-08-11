package binnie.trees.client;

import binnie.trees.BinnieTrees;
import binnie.trees.registry.BinnieTreesFluids;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/** Binds the original per-liquid animated sprites to the fixed Binnie's Trees liquids. */
@EventBusSubscriber(modid = BinnieTrees.MOD_ID, value = Dist.CLIENT)
public final class BinnieTreesFluidClientEvents {
    private BinnieTreesFluidClientEvents() {}

    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        for (BinnieTreesFluids.BeverageFluid fluid : BinnieTreesFluids.beverages()) registerBeverage(event, fluid);
        register(event, "resin", BinnieTreesFluids.RESIN);
        register(event, "turpentine", BinnieTreesFluids.TURPENTINE);
    }

    /**
     * JEI supplies fluid tooltips without an associated item stack. Keep the
     * Binnie's Trees attribution consistent with JEI's normal blue/italic mod
     * attribution instead of leaving this one fluid-only line unformatted.
     */
    @SubscribeEvent
    public static void styleFluidTooltipAttribution(RenderTooltipEvent.GatherComponents event) {
        for (int index = 0; index < event.getTooltipElements().size(); index++) {
            Either<FormattedText, TooltipComponent> element = event.getTooltipElements().get(index);
            FormattedText text = element.left().orElse(null);
            if (text != null && "Binnies: Trees".equals(text.getString())) {
                event.getTooltipElements().set(index, Either.left(Component.literal(text.getString())
                        .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC)));
            }
        }
    }

    private static void registerBeverage(RegisterClientExtensionsEvent event, BinnieTreesFluids.BeverageFluid fluid) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, "block/fluid/beverage");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return texture; }
            @Override public ResourceLocation getFlowingTexture() { return texture; }
            @Override public int getTintColor() { return 0xFF000000 | fluid.tint(); }
        }, fluid.type());
    }

    private static void register(RegisterClientExtensionsEvent event, String id, BinnieTreesFluids.ProcessFluid fluid) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(BinnieTrees.MOD_ID, "block/fluid/" + id);
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return texture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return texture;
            }

            @Override
            public int getTintColor() {
                return 0xFF000000 | fluid.tint();
            }
        }, fluid.type());
    }
}
