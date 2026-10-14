package ingatlanok.persistence;

import ingatlanok.model.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteConfigStorageTest {

    @TempDir
    Path tempDir;

    private SqliteDatabaseManager dbManager;
    private SqliteConfigStorage configStorage;

    @BeforeEach
    void setUp() {
        Path dbFile = tempDir.resolve("test_config.db");
        dbManager = new SqliteDatabaseManager(dbFile);
        configStorage = new SqliteConfigStorage(dbManager);
    }

    @Test
    @DisplayName("Alapértelmezett konfigurációt ad vissza, ha még nincs mentve")
    void testGetDefaultCriteria() {
        SearchCriteria criteria = configStorage.getLastCriteria();
        assertNotNull(criteria);
        assertEquals(70, criteria.getMinPriceMillion());
        assertEquals(120, criteria.getMaxPriceMillion());
    }

    @Test
    @DisplayName("Konfiguráció mentése és pontos visszatöltése SQLite-ból")
    void testSaveAndLoadCriteria() {
        SearchCriteria custom = new SearchCriteria();
        custom.setCities(List.of("Debrecen", "Nyíregyháza"));
        custom.setMinPriceMillion(35);
        custom.setMaxPriceMillion(75);
        custom.setMinArea(50);
        custom.setMaxArea(120);
        custom.setPropertyType("haz");
        custom.setPortals(List.of("dh", "oc"));

        configStorage.saveLastCriteria(custom);

        SearchCriteria loaded = configStorage.getLastCriteria();
        assertNotNull(loaded);
        assertEquals(List.of("Debrecen", "Nyíregyháza"), loaded.getCities());
        assertEquals(35, loaded.getMinPriceMillion());
        assertEquals(75, loaded.getMaxPriceMillion());
        assertEquals(50, loaded.getMinArea());
        assertEquals(120, loaded.getMaxArea());
        assertEquals("haz", loaded.getPropertyType());
        assertEquals(List.of("dh", "oc"), loaded.getPortals());
    }
}
