package ingatlanok.persistence;

import ingatlanok.model.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonConfigStorageTest {

    @TempDir
    Path tempDir;

    private Path configFile;
    private JsonConfigStorage storage;

    @BeforeEach
    void setUp() {
        configFile = tempDir.resolve("last_config.json");
        storage = new JsonConfigStorage(configFile);
    }

    @Test
    @DisplayName("Ha a fájl nem létezik, alapértelmezett konfigurációt ad vissza")
    void testGetDefaultWhenNotExists() {
        SearchCriteria criteria = storage.getLastCriteria();
        assertNotNull(criteria);
        assertEquals(List.of("Debrecen"), criteria.getCities());
    }

    @Test
    @DisplayName("Mentés és visszatöltés pontosan megegyezik")
    void testSaveAndLoadCriteria() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCities(List.of("Debrecen", "Budapest"));
        criteria.setPropertyType("haz");
        criteria.setMinPriceMillion(45);
        criteria.setMaxPriceMillion(95);
        criteria.setMinArea(80);
        criteria.setMaxArea(160);
        criteria.setAutoOpenBrowser(true);

        storage.saveLastCriteria(criteria);

        assertTrue(Files.exists(configFile));
        assertFalse(Files.exists(configFile.resolveSibling("last_config.json.tmp")));

        SearchCriteria loaded = storage.getLastCriteria();
        assertNotNull(loaded);
        assertEquals(List.of("Debrecen", "Budapest"), loaded.getCities());
        assertEquals("haz", loaded.getPropertyType());
        assertEquals(45, loaded.getMinPriceMillion());
        assertEquals(95, loaded.getMaxPriceMillion());
        assertEquals(80, loaded.getMinArea());
        assertEquals(160, loaded.getMaxArea());
        assertTrue(loaded.isAutoOpenBrowser());
    }

    @Test
    @DisplayName("Null mentési kérés nem okoz hibát")
    void testSaveNullGraceful() {
        assertDoesNotThrow(() -> storage.saveLastCriteria(null));
    }
}
