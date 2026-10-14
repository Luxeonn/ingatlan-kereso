package ingatlanok.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
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

class SqliteMigrationServiceTest {

    @TempDir
    Path tempDir;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private Path dbFile;
    private Path citiesDir;
    private Path globalDbFile;
    private Path configJsonFile;
    private SqliteDatabaseManager dbManager;
    private SqliteMigrationService migrationService;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = tempDir.resolve("migrated.db");
        citiesDir = tempDir.resolve("cities");
        Files.createDirectories(citiesDir);
        globalDbFile = tempDir.resolve("ingatlanok_db.json");
        configJsonFile = tempDir.resolve("last_config.json");

        dbManager = new SqliteDatabaseManager(dbFile);
        migrationService = new SqliteMigrationService(dbManager, GSON);
    }

    @Test
    @DisplayName("Automatikus migráció JSON fájlokból az üres SQLite adatbázisba (Zero Data Loss)")
    void testMigrationFromLegacyJson() throws IOException {
        // Mock JSON városfájl létrehozása
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/m1", "Debreceni lakás", "50 M Ft", "Debrecen", "Debrecen", "55 m²", "2026-10-14 10:00:00", false);
        item1.setViewed(true);
        item1.setViewedAt("2026-10-14 11:00:00");

        Path debFile = citiesDir.resolve("debrecen.json");
        Files.writeString(debFile, GSON.toJson(List.of(item1)), StandardCharsets.UTF_8);

        // Mock konfiguráció JSON
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCities(List.of("Debrecen"));
        criteria.setMinPriceMillion(45);
        criteria.setMaxPriceMillion(95);
        Files.writeString(configJsonFile, GSON.toJson(criteria), StandardCharsets.UTF_8);

        // Migráció futtatása
        boolean result = migrationService.migrateIfEmpty(citiesDir, globalDbFile, configJsonFile);
        assertTrue(result, "A migrációnak sikeresnek kell lennie");

        // Ellenőrzés SQLite-ban
        SqliteListingStorage listingStorage = new SqliteListingStorage(dbManager);
        assertEquals(1, listingStorage.getTotalCount());
        List<IngatlanItem> items = listingStorage.getItemsByCity("Debrecen");
        assertEquals(1, items.size());
        assertEquals("https://dh.hu/m1", items.get(0).getUrl());
        assertTrue(items.get(0).isViewed(), "A megtekintett státusznak át kell jönnie a migráció során");
        assertEquals("2026-10-14 11:00:00", items.get(0).getViewedAt());

        SqliteConfigStorage configStorage = new SqliteConfigStorage(dbManager);
        SearchCriteria loadedCriteria = configStorage.getLastCriteria();
        assertEquals(45, loadedCriteria.getMinPriceMillion());
        assertEquals(95, loadedCriteria.getMaxPriceMillion());

        // Zero Data Loss ellenőrzése: a forrásfájlok továbbra is léteznek
        assertTrue(Files.exists(debFile), "Az eredeti JSON fájlnak meg kell maradnia");
        assertTrue(Files.exists(configJsonFile), "Az eredeti config JSON-nak meg kell maradnia");

        // Idempotencia ellenőrzése: második futtatáskor már nem csinál semmit
        boolean secondRun = migrationService.migrateIfEmpty(citiesDir, globalDbFile, configJsonFile);
        assertFalse(secondRun, "Ha már léteznek rekordok, az automatikus hirdetés-migráció nem fut újra feleslegesen");
    }
}
