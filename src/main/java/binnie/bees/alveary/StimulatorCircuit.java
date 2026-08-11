package binnie.bees.alveary;

import binnie.bees.block.AlvearyPartType;
import binnie.bees.block.BinnieBeesAlvearyBlockEntity;
import forestry.api.core.circuits.ICircuit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Forestry circuit wrapper for one original stimulator program. */
public final class StimulatorCircuit implements ICircuit {
    private final StimulatorProgram program;

    public StimulatorCircuit(StimulatorProgram program) {
        this.program = program;
    }

    public StimulatorProgram program() { return program; }

    @Override public String getId() { return "binnie.circuit.stimulator." + program.id(); }
    @Override public String getTranslationKey() { return "binnie.circuit.stimulator." + program.id(); }
    @Override public boolean isCircuitable(Object tile) {
        return tile instanceof BinnieBeesAlvearyBlockEntity alveary && alveary.partType() == AlvearyPartType.STIMULATOR;
    }
    @Override public void onInsertion(int slot, Object tile) { }
    @Override public void onLoad(int slot, Object tile) { }
    @Override public void onRemoval(int slot, Object tile) { }
    @Override public void onTick(int slot, Object tile) { }
    @Override public void addTooltip(List<Component> list) {
        switch (program) {
            case LOW_VOLTAGE -> increase(list, "production", 50);
            case HIGH_VOLTAGE -> increase(list, "production", 150);
            case PLANT -> increase(list, "flowering", 50);
            case DEATH -> decrease(list, "lifespan", 20);
            case LIFE -> increase(list, "lifespan", 50);
            case MUTATION -> increase(list, "mutation", 50);
            case INHIBITOR -> {
                decrease(list, "territory", 60);
                decrease(list, "production", 10);
            }
            case TERRITORY -> increase(list, "territory", 50);
            case NETHER -> { }
        }
        // All nine original programs also increased genetic decay by 50%.
        increase(list, "genetic_decay", 50);
        if (program == StimulatorProgram.NETHER) {
            list.add(Component.translatable("tooltip.binniebees.stimulator.hellish").withStyle(ChatFormatting.GRAY));
        }
    }

    private static void increase(List<Component> list, String modifier, int amount) {
        list.add(Component.translatable("tooltip.binniebees.stimulator.increase",
                Component.translatable("tooltip.binniebees.stimulator.modifier." + modifier), amount)
                .withStyle(ChatFormatting.GRAY));
    }

    private static void decrease(List<Component> list, String modifier, int amount) {
        list.add(Component.translatable("tooltip.binniebees.stimulator.decrease",
                Component.translatable("tooltip.binniebees.stimulator.modifier." + modifier), amount)
                .withStyle(ChatFormatting.GRAY));
    }
}
