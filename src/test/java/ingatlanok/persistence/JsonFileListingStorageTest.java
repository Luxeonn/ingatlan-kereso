package ingatlanok.persistence;

import ingatlanok.model.IngatlanItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileListingStorageTest {

    @TempDir
    Path tempDir;

    private Path citiesDir;
    private Path globalDbFile;
    private Path legacyFile;
    private JsonFileListingStorage storage;

    @BeforeEach
    void setUp() {
        citiesDir = tempDir.resolve("cities");
        globalDbFile = tempDir.resolve("db.json");
        legacyFile = tempDir.resolve("legacy.txt");
        storage = new JsonFileListingStorage(citiesDir, globalDbFile, legacyFile, null);
    }

    @Test
    @DisplayName("Mentés és lekérdezés működik városonként és globálisan")
    void testSaveAndGetItems() {
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/1", "Szép lakás", "50 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 12:00:00", true);
        IngatlanItem item2 = new IngatlanItem("2", "Otthon Centrum", "https://oc.hu/2", "Családi ház", "80 M Ft", "Budapest", "Budapest", "100 m²", "2026-10-14 12:05:00", true);

        storage.saveCityItems("Debrecen", List.of(item1));
        storage.saveCityItems("Budapest", List.of(item2));

        assertTrue(storage.hasCityDatabase("Debrecen"));
        assertTrue(storage.hasCityDatabase("Budapest"));
        assertFalse(storage.hasCityDatabase("Szeged"));

        List<IngatlanItem> debrecenItems = storage.getItemsByCity("Debrecen");
        assertEquals(1, debrecenItems.size());
        assertEquals("https://dh.hu/1", debrecenItems.get(0).getUrl());

        List<IngatlanItem> allItems = storage.getAll();
        assertEquals(2, allItems.size());
        assertEquals(2, storage.getTotalCount());
        assertTrue(storage.containsUrl("https://dh.hu/1"));
        assertTrue(storage.containsUrl("https://oc.hu/2"));
        assertNotNull(storage.getLastRunTime());
    }

    @Test
    @DisplayName("Duplikátum és új URL detektálás")
    void testNewUrlDetection() {
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/1", "Szép lakás", "50 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 12:00:00", true);
        storage.saveCityItems("Debrecen", List.of(item1));

        // Ismert városban a már elmentett URL nem új
        assertFalse(storage.isNewUrl("Debrecen", "https://dh.hu/1"));

        // Ismert városban a még nem látott URL új
        assertTrue(storage.isNewUrl("Debrecen", "https://dh.hu/new-item"));

        // Ismeretlen városban az isNewUrl false-ot ad vissza (baseline állapot)
        assertFalse(storage.isNewUrl("Miskolc", "https://dh.hu/miskolc-1"));
    }

    @Test
    @DisplayName("Megtekintett állapot mentése és lekérdezése")
    void testMarkAsViewed() {
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/view-test", "Lakás", "50 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 12:00:00", true);
        storage.saveCityItems("Debrecen", List.of(item1));

        assertFalse(storage.isViewed("https://dh.hu/view-test"));

        boolean marked = storage.markAsViewed("https://dh.hu/view-test");
        assertTrue(marked);
        assertTrue(storage.isViewed("https://dh.hu/view-test"));

        assertFalse(storage.markAsViewed("https://nemletezo.hu"));
        assertFalse(storage.isViewed("https://nemletezo.hu"));
    }

    @Test
    @DisplayName("Path traversal támadási kísérletek szigorú elutasítása")
    void testPathTraversalPrevention() {
        assertThrows(IllegalArgumentException.class, () -> storage.resolveAndValidateCityFile("../secret"));
        assertThrows(IllegalArgumentException.class, () -> storage.resolveAndValidateCityFile("../../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> storage.resolveAndValidateCityFile("city/name"));
        assertThrows(IllegalArgumentException.class, () -> storage.resolveAndValidateCityFile("city\\name"));
        assertThrows(IllegalArgumentException.class, () -> storage.resolveAndValidateCityFile("invalid;name"));
        assertThrows(IllegalArgumentException.class, () -> storage.resolveAndValidateCityFile(""));

        // Érvényes kulcs nem dob kivételt
        assertDoesNotThrow(() -> {
            Path p = storage.resolveAndValidateCityFile("hajdu-szoboszlo_1");
            assertTrue(p.startsWith(citiesDir));
        });
    }

    @Test
    @DisplayName("Atomi mentés: ideiglenes .tmp fájlok nem maradnak vissza")
    void testAtomicWriteNoLeftoverTmp() throws IOException {
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/tmp-test", "Lakás", "50 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 12:00:00", true);
        storage.saveCityItems("Debrecen", List.of(item1));

        Path debrecenJson = citiesDir.resolve("debrecen.json");
        Path debrecenTmp = citiesDir.resolve("debrecen.json.tmp");
        assertTrue(Files.exists(debrecenJson));
        assertFalse(Files.exists(debrecenTmp));
    }

    @Test
    @DisplayName("Előzmények törlése városonként és globálisan")
    void testClearHistory() {
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/1", "Lakás", "50 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 12:00:00", true);
        IngatlanItem item2 = new IngatlanItem("2", "Otthon Centrum", "https://oc.hu/2", "Ház", "80 M Ft", "Budapest", "Budapest", "100 m²", "2026-10-14 12:05:00", true);
        storage.saveCityItems("Debrecen", List.of(item1));
        storage.saveCityItems("Budapest", List.of(item2));

        assertEquals(2, storage.getTotalCount());

        storage.clearCityHistory("Debrecen");
        assertEquals(1, storage.getTotalCount());
        assertFalse(storage.hasCityDatabase("Debrecen"));
        assertTrue(storage.hasCityDatabase("Budapest"));

        storage.clearHistory();
        assertEquals(0, storage.getTotalCount());
        assertFalse(storage.hasCityDatabase("Budapest"));
    }

    @Test
    @DisplayName("Konkurens szálbiztos írás és olvasás")
    void testConcurrentReadWrite() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Callable<Boolean>> tasks = new java.util.ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            tasks.add(() -> {
                String url = "https://portal.hu/item-" + index;
                IngatlanItem item = new IngatlanItem(String.valueOf(index), "Duna House", url, "Lakás " + index, "50 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 12:00:00", true);
                storage.saveCityItems("Debrecen", List.of(item));
                assertTrue(storage.containsUrl(url));
                storage.isViewed(url);
                return true;
            });
        }

        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        for (Future<Boolean> f : futures) {
            assertTrue(f.get());
        }
        executor.shutdown();

        assertEquals(threadCount, storage.getItemsByCity("Debrecen").size());
    }
}
