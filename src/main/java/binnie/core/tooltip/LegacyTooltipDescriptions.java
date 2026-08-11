package binnie.core.tooltip;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
  * Source-language keys for the item descriptions that Binnie's 1.12 client
  * displayed on demand. Keeping the mapping here makes the current registry
  * IDs explicit instead of falling back to a generated description.
  */
public final class LegacyTooltipDescriptions {
    private static final Map<ResourceLocation, String> SHIFT_DESCRIPTIONS = Map.ofEntries(
            entry("binniecore", "field_kit", "tooltip.binniecore.field_kit.description"),
            entry("binniebotany", "tileworker", "tooltip.binniebotany.tileworker.description"),
            entry("binniebees", "alveary_frame", "tooltip.binniebees.alveary.frame.description"),
            entry("binniebees", "alveary_lighting", "tooltip.binniebees.alveary.lighting.description"),
            entry("binniebees", "alveary_mutator", "tooltip.binniebees.alveary.mutator.description"),
            entry("binniebees", "alveary_rain_shield", "tooltip.binniebees.alveary.rain_shield.description"),
            entry("binniebees", "alveary_stimulator", "tooltip.binniebees.alveary.stimulator.description"),
            entry("binniebees", "alveary_hatchery", "tooltip.binniebees.alveary.hatchery.description"),
            entry("binniebees", "alveary_transmission", "tooltip.binniebees.alveary.transmission.description"),
            entry("binnietrees", "brewery", "tooltip.binnietrees.brewery.description"),
            entry("binnietrees", "distillery", "tooltip.binnietrees.distillery.description"),
            entry("binnietrees", "press", "tooltip.binnietrees.press.description"),
            entry("binnietrees", "lumbermill", "tooltip.binnietrees.lumbermill.description"),
            entry("binnietrees", "woodworker", "tooltip.binnietrees.woodworker.description"),
            entry("binnietrees", "panelworker", "tooltip.binnietrees.panelworker.description"),
            entry("binnietrees", "glassworker", "tooltip.binnietrees.glassworker.description"),
            entry("binniegenetics", "isolator", "tooltip.binniegenetics.isolator.description"),
            entry("binniegenetics", "sequencer", "tooltip.binniegenetics.sequencer.description"),
            entry("binniegenetics", "polymeriser", "tooltip.binniegenetics.polymeriser.description"),
            entry("binniegenetics", "inoculator", "tooltip.binniegenetics.inoculator.description"),
            entry("binniegenetics", "genepool", "tooltip.binniegenetics.genepool.description"),
            entry("binniegenetics", "laboratory_machine", "tooltip.binniegenetics.laboratory_machine.description"),
            entry("binniegenetics", "analyser", "tooltip.binniegenetics.analyser.description"),
            entry("binniegenetics", "incubator", "tooltip.binniegenetics.incubator.description"),
            entry("binniegenetics", "acclimatiser", "tooltip.binniegenetics.acclimatiser.description"),
            entry("binniegenetics", "splicer", "tooltip.binniegenetics.splicer.description")
    );

    private static final Map<ResourceLocation, String> ALWAYS_VISIBLE_DESCRIPTIONS = Map.ofEntries(
            entry("binniebotany", "mortar", "tooltip.binniebotany.mortar.legacy"),
            entry("binniebees", "master_bee_database", "tooltip.binniebees.master_bee_database.legacy"),
            entry("binnietrees", "master_arborist_database", "tooltip.binnietrees.master_arborist_database.legacy"),
            entry("binnietrees", "master_moth_database", "tooltip.binnietrees.master_moth_database.legacy")
    );

    private static final Map<ResourceLocation, String> MACHINE_INFO = Map.ofEntries(
            entry("binniebotany", "tileworker", "info.binniebotany.machine.tileworker"),
            entry("binnietrees", "brewery", "info.binnietrees.machine.brewery"),
            entry("binnietrees", "distillery", "info.binnietrees.machine.distillery"),
            entry("binnietrees", "press", "info.binnietrees.machine.press"),
            entry("binnietrees", "lumbermill", "info.binnietrees.machine.lumbermill"),
            entry("binnietrees", "woodworker", "info.binnietrees.machine.woodworker"),
            entry("binnietrees", "panelworker", "info.binnietrees.machine.panelworker"),
            entry("binnietrees", "glassworker", "info.binnietrees.machine.glassworker"),
            entry("binniegenetics", "isolator", "info.binniegenetics.machine.isolator"),
            entry("binniegenetics", "sequencer", "info.binniegenetics.machine.sequencer"),
            entry("binniegenetics", "polymeriser", "info.binniegenetics.machine.polymeriser"),
            entry("binniegenetics", "inoculator", "info.binniegenetics.machine.inoculator"),
            entry("binniegenetics", "genepool", "info.binniegenetics.machine.genepool"),
            entry("binniegenetics", "laboratory_machine", "info.binniegenetics.machine.laboratory_machine"),
            entry("binniegenetics", "analyser", "info.binniegenetics.machine.analyser"),
            entry("binniegenetics", "incubator", "info.binniegenetics.machine.incubator"),
            entry("binniegenetics", "acclimatiser", "info.binniegenetics.machine.acclimatiser"),
            entry("binniegenetics", "splicer", "info.binniegenetics.machine.splicer")
    );

    /** Short, distinct hover tips for the Forestry-style information ledgers. */
    private static final Map<ResourceLocation, String> MACHINE_TIPS = Map.ofEntries(
            entry("binniebotany", "tileworker", "tip.binniebotany.machine.tileworker"),
            entry("binnietrees", "brewery", "tip.binnietrees.machine.brewery"),
            entry("binnietrees", "distillery", "tip.binnietrees.machine.distillery"),
            entry("binnietrees", "press", "tip.binnietrees.machine.press"),
            entry("binnietrees", "lumbermill", "tip.binnietrees.machine.lumbermill"),
            entry("binnietrees", "woodworker", "tip.binnietrees.machine.woodworker"),
            entry("binnietrees", "panelworker", "tip.binnietrees.machine.panelworker"),
            entry("binnietrees", "glassworker", "tip.binnietrees.machine.glassworker"),
            entry("binniegenetics", "isolator", "tip.binniegenetics.machine.isolator"),
            entry("binniegenetics", "sequencer", "tip.binniegenetics.machine.sequencer"),
            entry("binniegenetics", "polymeriser", "tip.binniegenetics.machine.polymeriser"),
            entry("binniegenetics", "inoculator", "tip.binniegenetics.machine.inoculator"),
            entry("binniegenetics", "genepool", "tip.binniegenetics.machine.genepool"),
            entry("binniegenetics", "laboratory_machine", "tip.binniegenetics.machine.laboratory_machine"),
            entry("binniegenetics", "analyser", "tip.binniegenetics.machine.analyser"),
            entry("binniegenetics", "incubator", "tip.binniegenetics.machine.incubator"),
            entry("binniegenetics", "acclimatiser", "tip.binniegenetics.machine.acclimatiser"),
            entry("binniegenetics", "splicer", "tip.binniegenetics.machine.splicer")
    );

    private LegacyTooltipDescriptions() {
    }

    public static Optional<String> shiftDescription(ResourceLocation itemId) {
        return Optional.ofNullable(SHIFT_DESCRIPTIONS.get(itemId));
    }

    public static Optional<String> alwaysVisibleDescription(ResourceLocation itemId) {
        return Optional.ofNullable(ALWAYS_VISIBLE_DESCRIPTIONS.get(itemId));
    }

    public static Optional<String> machineInfo(ResourceLocation machineId) {
        return Optional.ofNullable(MACHINE_INFO.get(machineId));
    }

    public static Optional<String> machineTip(ResourceLocation machineId) {
        return Optional.ofNullable(MACHINE_TIPS.get(machineId));
    }

    public static String compartmentDescription(String tier) {
        return "tooltip.binniecore.compartment." + tier + ".description";
    }

    private static Map.Entry<ResourceLocation, String> entry(String namespace, String path, String key) {
        return Map.entry(ResourceLocation.fromNamespaceAndPath(namespace, path), key);
    }
}
