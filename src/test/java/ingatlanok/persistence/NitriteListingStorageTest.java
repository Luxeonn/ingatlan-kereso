package ingatlanok.persistence;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.StatusLogEntry;
import org.junit.jupiter.api.AfterEach;
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

class NitriteListingStorageTest {

    @TempDir
    Path tempDir;

    private NitriteDatabaseManager dbManager;
    private NitriteListingStorage storage;

    @BeforeEach
    void setUp() {
        Path dbFile = tempDir.resolve("test_nosql_listings.db");
        dbManager = new NitriteDatabaseManager(dbFile);
        storage = new NitriteListingStorage(dbManager);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Hirdetések mentése és lekérdezése városonként és globálisan NoSQL-ben")
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
        assertTrue(loaded.isViewed(), "A megtekintett flagnek meg kell maradnia");
        assertNotNull(loaded.getViewedAt());
    }

    @Test
    @DisplayName("Árváltozás és ártörténet helyes rögzítése (Price Drop & History)")
    void testPriceHistoryAndDiscountDetection() {
        IngatlanItem item = new IngatlanItem("1", "Duna House", "https://dh.hu/pricedrop", "Lakás", "60 M Ft", "Debrecen", "Debrecen", "70 m²", "2026-10-14 10:00:00", true);
        item.setPriceInfo(new PriceInfo("60 M Ft", 60_000_000L, 60.0));
        storage.saveCityItems("Debrecen", List.of(item));

        List<PriceHistoryEntry> history1 = storage.getPriceHistory("https://dh.hu/pricedrop");
        assertEquals(1, history1.size());
        assertEquals(60_000_000L, history1.get(0).priceAmount());

        // Árcsökkenés: 60M -> 54M (-6M, -10%)
        IngatlanItem itemDrop = new IngatlanItem("1", "Duna House", "https://dh.hu/pricedrop", "Lakás", "54 M Ft", "Debrecen", "Debrecen", "70 m²", "2026-10-14 12:00:00", false);
        itemDrop.setPriceInfo(new PriceInfo("54 M Ft", 54_000_000L, 54.0));
        storage.saveCityItems("Debrecen", List.of(itemDrop));

        List<PriceHistoryEntry> history2 = storage.getPriceHistory("https://dh.hu/pricedrop");
        assertEquals(2, history2.size());
        PriceHistoryEntry dropEntry = history2.get(1);
        assertEquals(54_000_000L, dropEntry.priceAmount());
        assertEquals(60_000_000L, dropEntry.previousPriceAmount());
        assertEquals(-6_000_000L, dropEntry.priceDelta());
        assertEquals(-10.0, dropEntry.changePercent());

        // Leárazott ingatlanok lekérdezése
        List<IngatlanItem> discounted = storage.getDiscountedProperties("Debrecen");
        assertEquals(1, discounted.size());
        assertEquals(-6_000_000L, discounted.get(0).getPriceDelta());

        // Státusznapló ellenőrzése
        List<StatusLogEntry> statusLog = storage.getStatusLog("https://dh.hu/pricedrop");
        assertTrue(statusLog.size() >= 2);
    }

    @Test
    @DisplayName("Keresési előzmények és adatok törlése városonként és globálisan")
    void testClearHistory() {
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/c1", "Lakás 1", "40 M Ft", "Debrecen", "Debrecen", "", "2026-10-14 10:00:00", true);
        IngatlanItem item2 = new IngatlanItem("2", "Otthon Centrum", "https://oc.hu/c2", "Lakás 2", "50 M Ft", "Budapest", "Budapest", "", "2026-10-14 10:00:00", true);
        storage.saveAll(List.of(item1, item2));

        assertEquals(2, storage.getTotalCount());
        assertTrue(storage.hasCityDatabase("Debrecen"));
        assertTrue(storage.hasCityDatabase("Budapest"));

        storage.clearCityHistory("Debrecen");
        assertFalse(storage.hasCityDatabase("Debrecen"));
        assertTrue(storage.hasCityDatabase("Budapest"));
        assertEquals(1, storage.getTotalCount());

        storage.clearHistory();
        assertEquals(0, storage.getTotalCount());
        assertFalse(storage.hasCityDatabase("Budapest"));
    }

    @Test
    @DisplayName("Portálok szerinti statisztika és ismert URL-ek halmaza")
    void testPortalsCountAndAllUrls() {
        IngatlanItem item1 = new IngatlanItem("1", "Duna House", "https://dh.hu/u1", "L1", "40 M Ft", "Debrecen", "Debrecen", "", "2026-10-14", true);
        IngatlanItem item2 = new IngatlanItem("2", "Duna House", "https://dh.hu/u2", "L2", "45 M Ft", "Debrecen", "Debrecen", "", "2026-10-14", true);
        IngatlanItem item3 = new IngatlanItem("3", "Otthon Centrum", "https://oc.hu/u3", "L3", "50 M Ft", "Budapest", "Budapest", "", "2026-10-14", true);
        storage.saveAll(List.of(item1, item2, item3));

        Map<String, Long> portalCounts = storage.countByPortal();
        assertEquals(2L, portalCounts.get("Duna House"));
        assertEquals(1L, portalCounts.get("Otthon Centrum"));

        Set<String> allUrls = storage.getAllUrls();
        assertEquals(3, allUrls.size());
        assertTrue(allUrls.contains("https://dh.hu/u1"));
        assertTrue(allUrls.contains("https://dh.hu/u2"));
        assertTrue(allUrls.contains("https://oc.hu/u3"));

        List<Map<String, Object>> tracked = storage.getTrackedCitiesInfo();
        assertEquals(2, tracked.size());
    }

    @Test
    @DisplayName("Párhuzamos konkurens mentés és olvasás szálbiztosságának ellenőrzése")
    void testConcurrentReadAndWrite() throws InterruptedException, ExecutionException {
        int threadCount = 8;
        int itemsPerThread = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                List<IngatlanItem> items = new ArrayList<>();
                for (int i = 0; i < itemsPerThread; i++) {
                    String url = "https://concurrent.hu/" + threadId + "/" + i;
                    IngatlanItem item = new IngatlanItem(
                            threadId + "_" + i,
                            "Duna House",
                            url,
                            "Cím " + threadId + "_" + i,
                            (30 + i) + " M Ft",
                            "Debrecen",
                            "Debrecen",
                            "50 m²",
                            "2026-10-14 10:00:00",
                            true
                    );
                    items.add(item);
                }
                storage.saveAll(items);

                // Konkurens olvasások a mentés közben
                storage.getTotalCount();
                storage.getItemsByCity("Debrecen");
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertEquals(threadCount * itemsPerThread, storage.getTotalCount());
    }
}
