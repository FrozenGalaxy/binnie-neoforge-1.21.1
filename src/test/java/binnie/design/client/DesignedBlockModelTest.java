package binnie.design.client;

import binnie.core.data.DesignData;
import binnie.design.api.DesignedBlockKind;
import binnie.design.block.DesignedBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DesignedBlockModelTest {
    private static final DesignData DATA = new DesignData("letter_a", "letter_a",
            ResourceLocation.withDefaultNamespace("oak_planks"),
            ResourceLocation.withDefaultNamespace("birch_planks"), 0, false);

    @Test
    void localDesignedFaceMapsToStoredOutwardFacingSide() {
        for (Direction facing : Direction.values()) {
            Vector3f transformed = DesignedBlockModel.transform(facing, DATA, false)
                    .transformDirection(new Vector3f(0, 1, 0));
            assertEquals(facing, Direction.getNearest(transformed.x, transformed.y, transformed.z), facing.getName());
        }
    }


    @Test
    void faceBakeryTransformKeepsEveryVertexInsideItsOwnBlock() {
        for (Direction facing : Direction.values()) {
            for (int rotation = 0; rotation < 4; rotation++) {
                for (boolean mirrored : new boolean[]{false, true}) {
                    DesignData data = new DesignData(DATA.design(), DATA.pattern(), DATA.primaryMaterial(),
                            DATA.secondaryMaterial(), rotation, mirrored, DATA.primaryVariant(), DATA.secondaryVariant());
                    Matrix4f transform = DesignedBlockModel.transform(facing, data, false);
                    for (int x = 0; x <= 1; x++) {
                        for (int y = 0; y <= 1; y++) {
                            for (int z = 0; z <= 1; z++) {
                // This is the matching centre-pivot convention used by FaceBakery.applyModelRotation.
                                Vector3f vertex = new Vector3f(x - 0.5F, y - 0.5F, z - 0.5F);
                                transform.transformPosition(vertex);
                                vertex.add(0.5F, 0.5F, 0.5F);
                                String context = facing + "/" + rotation + "/" + mirrored + ": " + vertex;
                                assertTrue(vertex.x >= -1.0E-5F && vertex.x <= 1.00001F, "x escaped block for " + context);
                                assertTrue(vertex.y >= -1.0E-5F && vertex.y <= 1.00001F, "y escaped block for " + context);
                                assertTrue(vertex.z >= -1.0E-5F && vertex.z <= 1.00001F, "z escaped block for " + context);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void placementStoresTheDesignedFaceTowardThePlayer() {
        for (Direction playerLook : Direction.values()) {
            assertEquals(playerLook.getOpposite(),
                    DesignedBlock.placementFacing(DesignedBlockKind.WOOD, Direction.UP, playerLook));
            assertEquals(playerLook.getOpposite(),
                    DesignedBlock.placementFacing(DesignedBlockKind.GLASS, Direction.UP, playerLook));
            assertEquals(playerLook.getOpposite(),
                    DesignedBlock.placementFacing(DesignedBlockKind.CERAMIC, Direction.UP, playerLook));
        }
        for (Direction clickedFace : Direction.values()) {
            assertEquals(clickedFace,
                    DesignedBlock.placementFacing(DesignedBlockKind.PANEL, clickedFace, Direction.NORTH));
        }
    }

    @Test
    void spriteFamiliesAreSeparatedByMaterialSystem() {
        assertEquals("binnietrees:design_sprite/wood/letter_a_primary",
                DesignedBlockModel.spriteModel(binnie.design.api.DesignedBlockKind.WOOD, "letter_a", 0).toString());
        assertEquals("binnietrees:design_sprite/glass/letter_a_secondary",
                DesignedBlockModel.spriteModel(binnie.design.api.DesignedBlockKind.GLASS, "letter_a", 1).toString());
        assertEquals("binniebotany:design_sprite/ceramic/letter_a_primary",
                DesignedBlockModel.spriteModel(binnie.design.api.DesignedBlockKind.CERAMIC, "letter_a", 0).toString());
    }
}
