package ingatlanok.persistence;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class SqliteListingStorageTest {

    @TempDir
    Path tempDir;

    private SqliteDatabaseManager dbManager;
    private SqliteListingStorage storage;

    @BeforeEach
    void setUp() {
        Path dbFile = tempDir.resolve("test_listings.db");
        dbManager = new SqliteDatabaseManager(dbFile);
        storage = new SqliteListingStorage(dbManager);
    }

    @Test
    @DisplayName("Hirdetések mentése és lekérdezése városonként és globálisan")
    void testSaveAndGetItems() {
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/1", "Szép lakás Debrecenben", "55 M Ft", "Debrecen", "Debrecen", "60 m² 2 szoba", "2026-10-14 10:00:00", true);
        item1.setPriceInfo(new PriceInfo("55 M Ft", 55_000_000L, 55.0));
        item1.setDetailsInfo(new PropertyDetails("60 m² 2 szoba", 60.0, 2.0, 916_666L));
        item1.setImageUrl("https://dh.hu/img1.jpg");

        IngatlanItem item2 = new IngatlanItem("2", "Otthon Centrum", "https://oc.hu/2", "Családi ház", "85 M Ft", "Budapest", "Budapest", "120 m² 4 szoba", "2026-10-14 11:00:00", true);

        storage.saveCityItems("Debrecen", List.of(item1));
        storage.saveCityItems("Budapest", List.of(item2));

        assertTrue(storage.hasCityDatabase("Debrecen"));
        assertTrue(storage.hasCityDatabase("Budapest"));
        assertFalse(storage.hasCityDatabase("Szeged"));

        List<IngatlanItem> debrecenItems = storage.getItemsByCity("Debrecen");
        assertEquals(1, debrecenItems.size());
        IngatlanItem loaded1 = debrecenItems.get(0);
        assertEquals("https://dh.hu/1", loaded1.getUrl());
        assertEquals("Szép lakás Debrecenben", loaded1.getTitle());
        assertEquals("55 M Ft", loaded1.getPrice());
        assertNotNull(loaded1.getPriceInfo());
        assertEquals(55_000_000L, loaded1.getPriceInfo().getAmountHuf());
        assertEquals(55.0, loaded1.getPriceInfo().getAmountMillion());
        assertNotNull(loaded1.getDetailsInfo());
        assertEquals(60.0, loaded1.getDetailsInfo().getAreaSquareMeters());
        assertEquals(2.0, loaded1.getDetailsInfo().getRoomCount());
        assertEquals("https://dh.hu/img1.jpg", loaded1.getImageUrl());

        List<IngatlanItem> all = storage.getAll();
        assertEquals(2, all.size());
        assertEquals(2, storage.getTotalCount());
        assertTrue(storage.containsUrl("https://dh.hu/1"));
        assertTrue(storage.containsUrl("https://oc.hu/2"));
        assertNotNull(storage.getLastRunTime());
    }

    @Test
    @DisplayName("Duplikált URL frissíti a rekordot de megőrzi a megtekintett státuszt")
    void testDuplicateUrlUpdatePreservesViewed() {
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/unique1", "Eredeti cím", "50 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 10:00:00", true);
        storage.saveCityItems("Debrecen", List.of(item1));

        assertFalse(storage.isViewed("https://dh.hu/unique1"));
        assertTrue(storage.markAsViewed("https://dh.hu/unique1"));
        assertTrue(storage.isViewed("https://dh.hu/unique1"));

        // Újramentés frissített adatokkal
        IngatlanItem item1Updated = new IngatlanItem("1", "Duna House", "https://dh.hu/unique1", "Frissített cím", "48 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 12:00:00", false);
        storage.saveCityItems("Debrecen", List.of(item1Updated));

        List<IngatlanItem> items = storage.getItemsByCity("Debrecen");
        assertEquals(1, items.size());
        IngatlanItem loaded = items.get(0);
        assertEquals("Frissített cím", loaded.getTitle());
        assertEquals("48 M Ft", loaded.getPrice());
        assertTrue(loaded.isViewed(), "A megtekintett állapotnak meg kell maradnia újramentés után is!");
    }

    @Test
    @DisplayName("isNewUrl működése és getAllUrls ellenőrzése")
    void testIsNewUrlAndGetAllUrls() {
        String baselineUrl = "https://dh.hu/baseline-1";
        // Ismeretlen városban az isNewUrl false-ot ad vissza (baseline állapot)
        assertFalse(storage.isNewUrl("Debrecen", baselineUrl));
        assertFalse(storage.containsUrl(baselineUrl));

        IngatlanItem baselineItem = new IngatlanItem("10", "Duna House", baselineUrl, "Alap ház", "60 M Ft", "Debrecen", "Debrecen", "70 m²", "2026-10-14 10:00:00", false);
        storage.saveCityItems("Debrecen", List.of(baselineItem));

        // Mentés után a már elmentett URL nem új
        assertFalse(storage.isNewUrl("Debrecen", baselineUrl));
        assertTrue(storage.containsUrl(baselineUrl));

        // Mentés után egy új URL ismert városban újnak számít
        String newUrl = "https://dh.hu/brand-new";
        assertTrue(storage.isNewUrl("Debrecen", newUrl));
        assertFalse(storage.containsUrl(newUrl));

        Set<String> allUrls = storage.getAllUrls();
        assertTrue(allUrls.contains(baselineUrl));
        assertEquals(1, allUrls.size());
    }

    @Test
    @DisplayName("Tracked cities statisztika és előzmény törlése városonként és globálisan")
    void testTrackedCitiesAndClear() {
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/1", "Debrecen 1", "50 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 10:00:00", true);
        IngatlanItem item2 = new IngatlanItem("2", "Duna House", "https://dh.hu/2", "Debrecen 2", "55 M Ft", "Debrecen", "Debrecen", "55 m²", "2026-10-14 10:05:00", true);
        IngatlanItem item3 = new IngatlanItem("3", "Otthon Centrum", "https://oc.hu/3", "Szeged 1", "40 M Ft", "Szeged", "Szeged", "45 m²", "2026-10-14 10:10:00", true);

        storage.saveAll(List.of(item1, item2, item3));

        List<Map<String, Object>> tracked = storage.getTrackedCitiesInfo();
        assertEquals(2, tracked.size());

        // Város szerinti törlés
        storage.clearCityHistory("Szeged");
        assertEquals(2, storage.getTotalCount());
        assertFalse(storage.hasCityDatabase("Szeged"));
        assertTrue(storage.hasCityDatabase("Debrecen"));

        // Teljes törlés
        storage.clearHistory();
        assertEquals(0, storage.getTotalCount());
        assertFalse(storage.hasCityDatabase("Debrecen"));
        assertTrue(storage.getAll().isEmpty());
    }

    @Test
    @DisplayName("Keresési futás sikeresen rögzíthető a search_runs táblába")
    void testRecordSearchRun() {
        assertDoesNotThrow(() -> {
            storage.recordSearchRun("2026-10-14 15:00:00", 42, 5, "{\"city\":\"Debrecen\"}", "{\"Duna House\":20}");
        });
    }

    @Test
    @DisplayName("Párhuzamos szálak konkurenciabiztos írása és olvasása")
    void testConcurrentWritesAndReads() throws InterruptedException, ExecutionException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int id = i;
            tasks.add(() -> {
                IngatlanItem item = new IngatlanItem("c-" + id, "Portal", "https://portal.hu/" + id, "Lakás " + id, (30 + id) + " M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 10:00:00", true);
                storage.saveCityItems("Debrecen", List.of(item));
                storage.containsUrl("https://portal.hu/" + id);
                storage.markAsViewed("https://portal.hu/" + id);
                storage.getItemsByCity("Debrecen");
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertEquals(threads, storage.getTotalCount());
    }

    @Test
    @DisplayName("3NF relációs integritás, idegen kulcsok és ON DELETE CASCADE működése")
    void testRelationalIntegrityAndCascadeDelete() throws java.sql.SQLException {
        IngatlanItem item = new IngatlanItem("100", "Duna House", "https://dh.hu/rel-test", "3NF Lakás", "65 M Ft", "Debrecen", "Debrecen", "75 m² 3 szoba", "2026-10-14 12:00:00", true);
        item.setPriceInfo(new PriceInfo("65 M Ft", 65_000_000L, 65.0));
        item.setDetailsInfo(new PropertyDetails("75 m² 3 szoba", 75.0, 3.0, 866_666L));
        item.setImageUrl("https://dh.hu/photo.jpg");

        storage.saveCityItems("Debrecen", List.of(item));

        try (java.sql.Connection conn = dbManager.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {

            // 1. Portál ellenőrzése
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT id, name FROM portals WHERE id = 'dh';")) {
                assertTrue(rs.next());
                assertEquals("Duna House", rs.getString("name"));
            }

            // 2. Település ellenőrzése
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT id, name, normalized_slug FROM locations WHERE normalized_slug = 'debrecen';")) {
                assertTrue(rs.next());
                assertEquals("Debrecen", rs.getString("name"));
            }

            // 3. Properties rekord és idegen kulcsok
            String propertyId;
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT id, portal_id, location_id FROM properties WHERE url = 'https://dh.hu/rel-test';")) {
                assertTrue(rs.next());
                propertyId = rs.getString("id");
                assertEquals("dh", rs.getString("portal_id"));
                assertEquals("debrecen", rs.getString("location_id"));
            }

            // 4. Attribútumok és képek relációi
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT price_amount, area_sqm, rooms FROM property_attributes WHERE property_id = '" + propertyId + "';")) {
                assertTrue(rs.next());
                assertEquals(65_000_000L, rs.getLong("price_amount"));
                assertEquals(75.0, rs.getDouble("area_sqm"));
                assertEquals(3.0, rs.getDouble("rooms"));
            }

            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT image_url, is_primary FROM property_images WHERE property_id = '" + propertyId + "';")) {
                assertTrue(rs.next());
                assertEquals("https://dh.hu/photo.jpg", rs.getString("image_url"));
                assertTrue(rs.getBoolean("is_primary"));
            }

            // 5. ON DELETE CASCADE ellenőrzése: property törlésekor az attribútumok és képek is törlődnek
            stmt.executeUpdate("DELETE FROM properties WHERE id = '" + propertyId + "';");

            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM property_attributes WHERE property_id = '" + propertyId + "';")) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "property_attributes rekordnak kaszkádolva törlődnie kellett!");
            }

            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM property_images WHERE property_id = '" + propertyId + "';")) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "property_images rekordnak kaszkádolva törlődnie kellett!");
            }
        }
    }

    @Test
    @DisplayName("Portálonkénti aggregált statisztika 3NF relációkból")
    void testCountByPortal3NF() {
        IngatlanItem item1 = new IngatlanItem("p1", "Duna House", "https://dh.hu/p1", "DH 1", "50 M Ft", "Debrecen", "Debrecen", "50 m²", "2026-10-14 10:00:00", true);
        IngatlanItem item2 = new IngatlanItem("p2", "Duna House", "https://dh.hu/p2", "DH 2", "55 M Ft", "Debrecen", "Debrecen", "55 m²", "2026-10-14 10:05:00", true);
        IngatlanItem item3 = new IngatlanItem("p3", "Otthon Centrum", "https://oc.hu/p3", "OC 1", "40 M Ft", "Budapest", "Budapest", "45 m²", "2026-10-14 10:10:00", true);

        storage.saveAll(List.of(item1, item2, item3));

        Map<String, Long> counts = storage.countByPortal();
        assertNotNull(counts);
        assertEquals(2L, counts.get("duna house"));
        assertEquals(1L, counts.get("otthon centrum"));
    }
}
