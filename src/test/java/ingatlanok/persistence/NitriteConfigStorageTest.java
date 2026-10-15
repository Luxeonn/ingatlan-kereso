package ingatlanok.persistence;

import ingatlanok.model.SearchCriteria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NitriteConfigStorageTest {

    @TempDir
    Path tempDir;

    private NitriteDatabaseManager dbManager;
    private NitriteConfigStorage configStorage;

    @BeforeEach
    void setUp() {
        Path dbFile = tempDir.resolve("test_config.db");
        dbManager = new NitriteDatabaseManager(dbFile);
        configStorage = new NitriteConfigStorage(dbManager);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Alapértelmezett és elmentett SearchCriteria helyes betöltése NoSQL-ből")
    void testGetAndSaveLastCriteria() {
        SearchCriteria defaultCriteria = configStorage.getLastCriteria();
        assertNotNull(defaultCriteria);
        assertEquals("Debrecen", defaultCriteria.getCity());

        SearchCriteria customCriteria = new SearchCriteria();
        customCriteria.setCity("Budapest");
        customCriteria.setCities(List.of("Budapest", "Debrecen"));
        customCriteria.setMinPriceMillion(60);
        customCriteria.setMaxPriceMillion(110);
        customCriteria.setMinArea(75);
        customCriteria.setExcludeViewed(true);

        configStorage.saveLastCriteria(customCriteria);

        SearchCriteria loaded = configStorage.getLastCriteria();
        assertNotNull(loaded);
        assertEquals("Budapest", loaded.getCity());
        assertEquals(2, loaded.getCities().size());
        assertEquals(60, loaded.getMinPriceMillion());
        assertEquals(110, loaded.getMaxPriceMillion());
        assertEquals(75, loaded.getMinArea());
        assertTrue(loaded.isExcludeViewed());
    }
}
