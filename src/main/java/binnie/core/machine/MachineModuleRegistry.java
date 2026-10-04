package binnie.core.machine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import binnie.core.registry.ModuleRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MachineModuleRegistry {
    private static final Set<String> NON_OCCLUDING_MACHINE_MODULES = Set.of(
            "binniebotany", "binniegenetics", "binnietrees");

    private final DeferredRegister.Blocks blocks;
    private final DeferredRegister.Items items;
    private final DeferredRegister<BlockEntityType<?>> blockEntities;
    private final Map<String, MachineSpec> specs = new LinkedHashMap<>();
    private final List<DeferredBlock<MachineBlock>> machineBlocks = new ArrayList<>();
    private final Map<String, DeferredBlock<MachineBlock>> blocksById = new LinkedHashMap<>();
    private final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> machineType;
    private final ModuleRegistry creativeRegistry;

    public MachineModuleRegistry(String modId, ModuleRegistry creativeRegistry, String... machineIds) {
        this.blocks = DeferredRegister.createBlocks(modId);
        this.items = DeferredRegister.createItems(modId);
        this.blockEntities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, modId);
        this.creativeRegistry = creativeRegistry;
        this.machineType = blockEntities.register("machine", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineBlockEntity(((MachineBlock) state.getBlock()).blockEntityType(), pos, state),
                machineBlocks.stream().map(DeferredBlock::get).toArray(Block[]::new)
        ).build(null));
        for (String id : machineIds) registerMachine(modId, id);
    }

    private void registerMachine(String modId, String id) {
        MachineSpec spec = MachineSpec.standard(modId, id);
        specs.put(id, spec);
        DeferredBlock<MachineBlock> block = blocks.register(id, () -> {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                    .strength(3.5F)
                    .requiresCorrectToolForDrops();
            if (NON_OCCLUDING_MACHINE_MODULES.contains(modId)) properties.noOcclusion();
            return new MachineBlock(properties, spec, machineType);
        });
        machineBlocks.add(block);
        blocksById.put(id, block);
        DeferredItem<BlockItem> item = items.registerItem(id, properties -> new BlockItem(block.get(), properties));
        creativeRegistry.include(item);
    }

    public Map<String, MachineSpec> specs() {
        return Map.copyOf(specs);
    }

    public DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> blockEntityType() {
        return machineType;
    }

    public DeferredBlock<MachineBlock> block(String id) {
        DeferredBlock<MachineBlock> block = blocksById.get(id);
        if (block == null) throw new IllegalArgumentException("Unknown machine " + id);
        return block;
    }

    public void register(IEventBus bus) {
        blocks.register(bus);
        items.register(bus);
        blockEntities.register(bus);
        bus.addListener(this::registerCapabilities);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, machineType.get(), (machine, side) -> machine.itemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, machineType.get(), (machine, side) -> machine.energy());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, machineType.get(), (machine, side) -> machine.fluidHandler(side));
    }
}
