package binnie.trees.registry;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.arboriculture.wood.IWoodTyped;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.Objects;

/** Forestry-identifiable control blocks for Binnie's Trees wood families. */
final class BinnieTreeWoodControlBlocks {
    private BinnieTreeWoodControlBlocks() {
    }

    static final class Door extends DoorBlock implements IWoodTyped {
        private final IWoodType woodType;

        Door(IWoodType woodType, BlockBehaviour.Properties properties) {
            super(BlockSetType.OAK, properties);
            this.woodType = Objects.requireNonNull(woodType, "woodType");
        }

        @Override
        public WoodBlockKind getBlockKind() {
            return WoodBlockKind.DOOR;
        }

        @Override
        public boolean isFireproof() {
            return false;
        }

        @Override
        public IWoodType getWoodType() {
            return this.woodType;
        }
    }

    static final class Trapdoor extends TrapDoorBlock implements IWoodTyped {
        private final IWoodType woodType;

        Trapdoor(IWoodType woodType, BlockBehaviour.Properties properties) {
            super(BlockSetType.OAK, properties);
            this.woodType = Objects.requireNonNull(woodType, "woodType");
        }

        @Override
        public WoodBlockKind getBlockKind() {
            return WoodBlockKind.TRAPDOOR;
        }

        @Override
        public boolean isFireproof() {
            return false;
        }

        @Override
        public IWoodType getWoodType() {
            return this.woodType;
        }
    }

    static final class PressurePlate extends PressurePlateBlock implements IWoodTyped {
        private final IWoodType woodType;

        PressurePlate(IWoodType woodType, BlockBehaviour.Properties properties) {
            super(BlockSetType.OAK, properties);
            this.woodType = Objects.requireNonNull(woodType, "woodType");
        }

        @Override
        public WoodBlockKind getBlockKind() {
            return WoodBlockKind.PRESSURE_PLATE;
        }

        @Override
        public boolean isFireproof() {
            return false;
        }

        @Override
        public IWoodType getWoodType() {
            return this.woodType;
        }
    }

    static final class Button extends ButtonBlock implements IWoodTyped {
        private final IWoodType woodType;

        Button(IWoodType woodType, BlockBehaviour.Properties properties) {
            super(BlockSetType.OAK, 30, properties);
            this.woodType = Objects.requireNonNull(woodType, "woodType");
        }

        @Override
        public WoodBlockKind getBlockKind() {
            return WoodBlockKind.BUTTON;
        }

        @Override
        public boolean isFireproof() {
            return false;
        }

        @Override
        public IWoodType getWoodType() {
            return this.woodType;
        }
    }
}
