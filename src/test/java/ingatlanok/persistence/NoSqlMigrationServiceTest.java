package ingatlanok.persistence;

import com.google.gson.Gson;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoSqlMigrationServiceTest {

    @TempDir
    Path tempDir;

    private NitriteDatabaseManager dbManager;
    private NoSqlMigrationService migrationService;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("migration_test.db");
        dbManager = new NitriteDatabaseManager(dbPath);
        migrationService = new NoSqlMigrationService(dbManager, gson);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Városszintű JSON és config fájlok sikeres migrációja a NoSQL tárolóba")
    void testMigrateCityFilesAndConfig() throws IOException {
        Path citiesDir = tempDir.resolve("cities");
        Files.createDirectories(citiesDir);

        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/m1", "Lakás 1", "45 M Ft", "Debrecen", "Debrecen", "", "2026-10-14 10:00:00", false);
        Files.writeString(citiesDir.resolve("debrecen.json"), gson.toJson(List.of(item1)), StandardCharsets.UTF_8);

        SearchCriteria customCriteria = new SearchCriteria();
        customCriteria.setCity("Szeged");
        customCriteria.setMinPriceMillion(50);
        Path configFile = tempDir.resolve("last_config.json");
        Files.writeString(configFile, gson.toJson(customCriteria), StandardCharsets.UTF_8);

        boolean migrated = migrationService.migrateIfEmpty(citiesDir, null, configFile);
        assertTrue(migrated);

        NitriteListingStorage listingStorage = new NitriteListingStorage(dbManager);
        assertEquals(1, listingStorage.getTotalCount());
        assertTrue(listingStorage.containsUrl("https://dh.hu/m1"));

        NitriteConfigStorage configStorage = new NitriteConfigStorage(dbManager);
        SearchCriteria loaded = configStorage.getLastCriteria();
        assertEquals("Szeged", loaded.getCity());
        assertEquals(50, loaded.getMinPriceMillion());

        // Második futáskor már nem csinál semmit (idempotens)
        boolean secondRun = migrationService.migrateIfEmpty(citiesDir, null, configFile);
        assertFalse(secondRun);
    }
}
