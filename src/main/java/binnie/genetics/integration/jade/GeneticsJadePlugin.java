package binnie.genetics.integration.jade;

import binnie.core.machine.MachineBlock;
import binnie.core.machine.MachineBlockEntity;
import binnie.core.machine.MachineProcessHandler;
import binnie.core.machine.MachineProcessRegistry;
import binnie.core.registry.BinnieDataComponents;
import binnie.genetics.data.LaboratoryChargeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * Server-authoritative diagnostic information for every Binnie Genetics machine.
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
            if (!(accessor.getBlockEntity() instanceof MachineBlockEntity machine)
                    || !machine.spec().id().getNamespace().equals("binniegenetics")) {
                return;
            }

            data.putBoolean(DATA_PREFIX + "present", true);
            ResourceLocation process = machine.activeProcess();
            data.putString(DATA_PREFIX + "process", process == null ? "" : process.getPath());
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

            String process = data.getString(DATA_PREFIX + "process");
            tooltip.add(Component.translatable("jade.binniegenetics.machine_state"));
            tooltip.add(Component.translatable("jade.binniegenetics.state", Component.translatable(
                    "jade.binniegenetics." + data.getString(DATA_PREFIX + "state"), process)));

            int progress = data.getInt(DATA_PREFIX + "progress");
            int maxProgress = data.getInt(DATA_PREFIX + "max_progress");
            if (maxProgress > 0) {
                tooltip.add(Component.translatable("jade.binniegenetics.progress", progress, maxProgress,
                        Math.min(100, progress * 100 / maxProgress)));
            }
            tooltip.add(Component.translatable("jade.binniegenetics.energy", data.getInt(DATA_PREFIX + "energy"),
                    data.getInt(DATA_PREFIX + "max_energy")));
            appendTank(tooltip, data, "primary", "jade.binniegenetics.primary");
            appendTank(tooltip, data, "secondary", "jade.binniegenetics.secondary");
            appendTank(tooltip, data, "output", "jade.binniegenetics.output");
            if (data.contains(DATA_PREFIX + "reagent_charge")) {
                tooltip.add(Component.translatable("jade.binniegenetics.reagent_charge", data.getInt(DATA_PREFIX + "reagent_charge"),
                        data.getInt(DATA_PREFIX + "reagent_capacity"), data.getInt(DATA_PREFIX + "reagent_needed")));
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
            data.putString(DATA_PREFIX + id + "_id", fluid.isEmpty() ? "" : BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
            data.putInt(DATA_PREFIX + id + "_amount", tank.getFluidAmount());
            data.putInt(DATA_PREFIX + id + "_capacity", tank.getCapacity());
        }

        private static void appendTank(ITooltip tooltip, CompoundTag data, String id, String labelKey) {
            int amount = data.getInt(DATA_PREFIX + id + "_amount");
            int capacity = data.getInt(DATA_PREFIX + id + "_capacity");
            String fluid = data.getString(DATA_PREFIX + id + "_fluid");
            if (fluid.isEmpty()) {
                tooltip.add(Component.translatable("jade.binniegenetics.empty_tank", Component.translatable(labelKey), amount, capacity));
            } else {
                tooltip.add(Component.translatable("jade.binniegenetics.tank", Component.translatable(labelKey), fluid, amount, capacity));
                tooltip.add(Component.translatable("jade.binniegenetics.fluid_id", data.getString(DATA_PREFIX + id + "_id")));
            }
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
