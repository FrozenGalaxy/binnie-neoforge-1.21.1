package binnie.genetics.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class KnowledgeUiAssetTest {
    @Test
    void originalAnalystAndGeneBankAssetsArePackaged() {
        Path resources = Path.of("src/main/resources");
        List<String> assets = List.of(
                "assets/binniecore/textures/gui/guianalyst.png",
                "assets/binniegenetics/textures/item/dna.0.png",
                "assets/binniegenetics/textures/item/dna.1.png",
                "assets/binniegenetics/textures/gui/gene_dna.png",
                "assets/binniegenetics/textures/item/analyst.png",
                "assets/binniegenetics/textures/item/database.png",
                "assets/binniegenetics/textures/item/master_database.png",
                "assets/binniegenetics/textures/item/master_registry.png",
                "assets/binniegenetics/models/item/master_registry.json",
                "assets/binnietrees/textures/item/arborist_database.png",
                "assets/binnietrees/textures/item/master_arborist_database.png",
                "assets/binnietrees/textures/item/moth_database.png",
                "assets/binnietrees/textures/item/master_moth_database.png",
                "assets/binniebotany/textures/item/botanist_database.png",
                "assets/binniebotany/textures/item/botanist_database_master.png",
                "assets/binniebees/textures/item/bee_database.png",
                "assets/binniebees/textures/item/master_bee_database.png"
        );
        for (String asset : assets) assertTrue(Files.isRegularFile(resources.resolve(asset)), asset);
    }


    @Test
    void geneBankItemsUseTheirOriginalNames() throws Exception {
        JsonObject lang = JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/assets/binniegenetics/lang/en_us.json"))).getAsJsonObject();
        assertTrue("Gene Database".equals(lang.get("item.binniegenetics.database").getAsString()));
        assertTrue("Master Gene Database".equals(lang.get("item.binniegenetics.master_database").getAsString()));
        assertTrue("Gene Bank".equals(lang.get("screen.binniegenetics.database.gene_bank").getAsString()));
        assertTrue(lang.has("screen.binniegenetics.knowledge.no_description"));
        assertTrue("Hold Shift for more info".equals(
                lang.get("screen.binniegenetics.database.hold_shift").getAsString()));
    }

    @Test
    void geneBankUsesTheExactOriginalDnaSprite() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of("src/main/resources/assets/binniegenetics/textures/gui/gene_dna.png"));
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertTrue(sha256.equals("0b622629c79d2575cde52f4744f0992cf272bcb30103d3525b23f53940f6286f"), sha256);
    }

    @Test
    void masterGeneDatabaseUsesTheExactOriginalItemTexture() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of("src/main/resources/assets/binniegenetics/textures/item/master_database.png"));
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertTrue(sha256.equals("6cd402d8321e0b24dd1dbe916bd507b7445986f5ca74799a91e833256d68b8dc"), sha256);
    }

    @Test
    void masterRegistryUsesTheExactOriginalItemTexture() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of("src/main/resources/assets/binniegenetics/textures/item/master_registry.png"));
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertTrue(sha256.equals("469948a7d2138cb6acb4845584d1eb7ba986a5bd608071404805a6436dc3e71f"), sha256);
    }

    @Test
    void specialistDatabasesUseTheirExactOriginalItemTextures() throws Exception {
        assertOriginal("src/main/resources/assets/binniebees/textures/item/bee_database.png",
                "../reference/Binnie-1.12/extrabees/src/main/resources/assets/extrabees/textures/items/apiarist_database.png");
        assertOriginal("src/main/resources/assets/binniebees/textures/item/master_bee_database.png",
                "../reference/Binnie-1.12/extrabees/src/main/resources/assets/extrabees/textures/items/master_apiarist_database.png");
        assertOriginal("src/main/resources/assets/binnietrees/textures/item/moth_database.png",
                "../reference/Binnie-1.12/extratrees/src/main/resources/assets/extratrees/textures/items/lepi_database.png");
        assertOriginal("src/main/resources/assets/binnietrees/textures/item/master_moth_database.png",
                "../reference/Binnie-1.12/extratrees/src/main/resources/assets/extratrees/textures/items/master_lepi_database.png");
    }

    private static void assertOriginal(String modern, String legacy) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String modernHash = HexFormat.of().formatHex(digest.digest(Files.readAllBytes(Path.of(modern))));
        String legacyHash = HexFormat.of().formatHex(digest.digest(Files.readAllBytes(Path.of(legacy))));
        assertTrue(modernHash.equals(legacyHash), modern + " != " + legacy);
    }
}
