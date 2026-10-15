package ingatlanok.persistence;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NitriteDatabaseManagerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("NitriteDatabaseManager sikeresen inicializálja az MVStore-t és a kollekciókat")
    void testInitializationAndCollections() {
        Path dbPath = tempDir.resolve("test_mgr.db");
        try (NitriteDatabaseManager mgr = new NitriteDatabaseManager(dbPath)) {
            assertNotNull(mgr.getNitrite());
            assertFalse(mgr.isClosed());
            assertEquals(dbPath, mgr.getDatabasePath());
            assertNotNull(mgr.getRwLock());

            NitriteCollection propCol = mgr.getPropertiesCollection();
            assertNotNull(propCol);
            assertEquals(NitriteDatabaseManager.COLLECTION_PROPERTIES, propCol.getName());

            NitriteCollection phCol = mgr.getPriceHistoryCollection();
            assertNotNull(phCol);
            assertEquals(NitriteDatabaseManager.COLLECTION_PRICE_HISTORY, phCol.getName());

            NitriteCollection slCol = mgr.getStatusLogCollection();
            assertNotNull(slCol);
            assertEquals(NitriteDatabaseManager.COLLECTION_STATUS_LOG, slCol.getName());

            NitriteCollection cfgCol = mgr.getConfigCollection();
            assertNotNull(cfgCol);
            assertEquals(NitriteDatabaseManager.COLLECTION_CONFIG, cfgCol.getName());

            NitriteCollection runsCol = mgr.getSearchRunsCollection();
            assertNotNull(runsCol);
            assertEquals(NitriteDatabaseManager.COLLECTION_SEARCH_RUNS, runsCol.getName());

            // Dokumentum beszúrás és ellenőrzés
            propCol.insert(Document.createDocument("url", "https://test.hu/1").put("city", "Debrecen"));
            assertEquals(1, propCol.size());
        }
    }

    @Test
    @DisplayName("Alapértelmezett konstruktor és bezárás viselkedése")
    void testDefaultConstructorAndClose() {
        Path dbPath = tempDir.resolve("test_close.db");
        NitriteDatabaseManager mgr = new NitriteDatabaseManager(dbPath);
        assertFalse(mgr.isClosed());
        mgr.close();
        assertTrue(mgr.isClosed());

        // Többszöri close() hívás sem dob kivételt
        assertDoesNotThrow(mgr::close);
    }
}
