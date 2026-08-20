package binnie.genetics.integration.jade;

import binnie.core.machine.MachineBlock;
import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineProcessHandler;
import binnie.core.machine.MachineProcessRegistry;
import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.data.LaboratoryChargeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Compact server-authoritative diagnostic information for every Binnie machine.
 * Jade is optional; this class is only discovered when Jade is installed.
 */
@WailaPlugin
public final class GeneticsJadePlugin implements IWailaPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("binniegenetics", "machine_state");
    private static final String DATA_PREFIX = "binnie_genetics_";
    private static final MachineStateProvider PROVIDER = new MachineStateProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PROVIDER, MachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PROVIDER, MachineBlock.class);
    }

    private static final class MachineStateProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof MachineBlockEntity machine)) {
                return;
            }

            data.putBoolean(DATA_PREFIX + "present", true);
            data.putString(DATA_PREFIX + "state", diagnosticState(accessor, machine));
            data.putInt(DATA_PREFIX + "progress", machine.progress());
            data.putInt(DATA_PREFIX + "max_progress", machine.maxProgress());
            data.putInt(DATA_PREFIX + "energy", machine.energy().getEnergyStored());
            data.putInt(DATA_PREFIX + "max_energy", machine.energy().getMaxEnergyStored());
            writeTank(data, "primary", machine.tank());
            writeTank(data, "secondary", machine.secondaryTank());
            writeTank(data, "output", machine.outputTank());
            writeReagent(data, machine);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.getBoolean(DATA_PREFIX + "present")) return;

            int progress = data.getInt(DATA_PREFIX + "progress");
            int maxProgress = data.getInt(DATA_PREFIX + "max_progress");
            String state = data.getString(DATA_PREFIX + "state");
            MutableComponent summary = Component.translatable("jade.binniegenetics.compact_" + state)
                    .withStyle(stateColor(state));
            if (maxProgress > 0) {
                summary.append(Component.translatable("jade.binniegenetics.compact_progress",
                        Math.min(100, progress * 100 / maxProgress)).withStyle(ChatFormatting.AQUA));
            }
            summary.append(Component.translatable("jade.binniegenetics.compact_energy",
                    compact(data.getInt(DATA_PREFIX + "energy")), compact(data.getInt(DATA_PREFIX + "max_energy")))
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(summary);

            appendTank(tooltip, data, "primary", "jade.binniegenetics.primary_short");
            appendTank(tooltip, data, "secondary", "jade.binniegenetics.secondary_short");
            appendTank(tooltip, data, "output", "jade.binniegenetics.output_short");
            if (data.contains(DATA_PREFIX + "reagent_charge")) {
                tooltip.add(Component.translatable("jade.binniegenetics.compact_reagent",
                        compact(data.getInt(DATA_PREFIX + "reagent_charge")),
                        compact(data.getInt(DATA_PREFIX + "reagent_capacity")),
                        compact(data.getInt(DATA_PREFIX + "reagent_needed"))).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }

        private static String diagnosticState(BlockAccessor accessor, MachineBlockEntity machine) {
            if (machine.activeProcess() == null) return "idle";
            MachineProcessHandler handler = MachineProcessRegistry.get(machine.spec().id());
            if (handler == null) return "active";
            try {
                return handler.createPlan(accessor.getLevel(), machine)
                        .map(plan -> handler.canProgress(accessor.getLevel(), machine, plan) ? "running" : "blocked")
                        .orElse("inputs_changed");
            } catch (RuntimeException ignored) {
                return "blocked";
            }
        }

        private static void writeTank(CompoundTag data, String id, FluidTank tank) {
            FluidStack fluid = tank.getFluid();
            data.putString(DATA_PREFIX + id + "_fluid", fluid.isEmpty() ? "" : fluid.getHoverName().getString());
            data.putInt(DATA_PREFIX + id + "_amount", tank.getFluidAmount());
            data.putInt(DATA_PREFIX + id + "_capacity", tank.getCapacity());
        }

        private static void appendTank(ITooltip tooltip, CompoundTag data, String id, String labelKey) {
            int amount = data.getInt(DATA_PREFIX + id + "_amount");
            int capacity = data.getInt(DATA_PREFIX + id + "_capacity");
            String fluid = data.getString(DATA_PREFIX + id + "_fluid");
            if (!fluid.isEmpty()) tooltip.add(Component.translatable("jade.binniegenetics.compact_tank",
                    Component.translatable(labelKey), fluid, compact(amount), compact(capacity))
                    .withStyle(id.equals("output") ? ChatFormatting.GREEN : ChatFormatting.BLUE));
        }

        private static ChatFormatting stateColor(String state) {
            return switch (state) {
                case "running" -> ChatFormatting.GREEN;
                case "blocked", "inputs_changed" -> ChatFormatting.RED;
                case "active" -> ChatFormatting.AQUA;
                default -> ChatFormatting.GRAY;
            };
        }

        private static String compact(int value) {
            if (value >= 1_000_000 && value % 1_000_000 == 0) return value / 1_000_000 + "M";
            if (value >= 1_000 && value % 1_000 == 0) return value / 1_000 + "k";
            return Integer.toString(value);
        }

        private static void writeReagent(CompoundTag data, MachineBlockEntity machine) {
            int needed = switch (machine.spec().id().getPath()) {
                case "isolator" -> 5_000;
                case "sequencer", "polymeriser" -> 4_000;
                case "analyser" -> 6_000;
                case "genepool" -> 2_500;
                default -> 0;
            };
            if (needed == 0) return;
            int slot = machine.spec().id().getPath().equals("polymeriser")
                    ? MachineBlockEntity.CATALYST_SLOT : MachineBlockEntity.REAGENT_SLOT;
            ItemStack chargedTool = machine.inventory().getStackInSlot(slot);
            LaboratoryChargeData charge = chargedTool.get(BinnieDataComponents.LABORATORY_CHARGE);
            data.putInt(DATA_PREFIX + "reagent_charge", chargedTool.isEmpty() ? 0
                    : charge == null ? LaboratoryChargeData.FULL : charge.milliCharge());
            data.putInt(DATA_PREFIX + "reagent_capacity", LaboratoryChargeData.FULL);
            data.putInt(DATA_PREFIX + "reagent_needed", needed);
        }
    }
}
