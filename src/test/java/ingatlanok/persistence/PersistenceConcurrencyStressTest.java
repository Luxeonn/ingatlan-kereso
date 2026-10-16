package ingatlanok.persistence;

import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Milestone 2 — Persistence Concurrency & Isolation Stress Tests (Challenger 2)")
public class PersistenceConcurrencyStressTest {

    @Autowired
    private JpaIngatlanStorage jpaStorage;

    private static final Path DEFAULT_NOSQL_PATH = Path.of("data/ingatlanok_nosql.db");

    @BeforeEach
    void setUp() {
        jpaStorage.clearHistory();
    }

    @AfterEach
    void tearDown() {
        NitriteDatabaseManager.closeAll();
    }

    private IngatlanItem createItem(String id, String url, String portal, String city,
                                    String priceRaw, long priceAmount, double priceMillions,
                                    String details, double areaSqm, double rooms, String discoveredAt) {
        IngatlanItem item = new IngatlanItem();
        item.setId(id);
        item.setUrl(url);
        item.setPortal(portal);
        item.setCity(city);
        item.setTitle("Stress Listing " + id);
        item.setPrice(priceRaw);
        item.setPriceInfo(new PriceInfo(priceRaw, priceAmount, priceMillions));
        item.setDetails(details);
        item.setDetailsInfo(new PropertyDetails(details, areaSqm, rooms, (long) (priceAmount / areaSqm)));
        item.setDiscoveredAt(discoveredAt);
        item.setNew(true);
        item.setViewed(false);
        return item;
    }

    // =========================================================================
    // 1. Concurrent Read/Write Operations against JpaIngatlanStorage
    // =========================================================================

    @Test
    @DisplayName("Challenge 1: Concurrent read/write stress on JpaIngatlanStorage with 16 parallel threads")
    void testConcurrentReadWriteJpaStorage() throws Exception {
        int threadCount = 16;
        int operationsPerThread = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger writeSuccesses = new AtomicInteger(0);
        AtomicInteger readSuccesses = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Thundering herd start
                    for (int i = 0; i < operationsPerThread; i++) {
                        String uniqueUrl = "https://jpa-stress.hu/" + threadId + "/" + i;
                        IngatlanItem item = createItem(
                                "jpa_" + threadId + "_" + i,
                                uniqueUrl,
                                (i % 2 == 0) ? "Duna House" : "Otthon Centrum",
                                (i % 3 == 0) ? "Budapest" : ((i % 3 == 1) ? "Debrecen" : "Szeged"),
                                "50 M Ft", 50_000_000L, 50.0,
                                "60 m2, 2 szoba", 60.0, 2.0,
                                "2026-03-01 10:00:00"
                        );

                        // Concurrent write
                        jpaStorage.saveAll(List.of(item));
                        writeSuccesses.incrementAndGet();

                        // Concurrent read
                        List<IngatlanItem> items = jpaStorage.getItemsByCity(item.getCity());
                        assertNotNull(items);
                        assertTrue(jpaStorage.containsUrl(uniqueUrl));
                        readSuccesses.incrementAndGet();

                        // Concurrent update: mark viewed
                        if (i % 5 == 0) {
                            jpaStorage.markAsViewed(uniqueUrl);
                            assertTrue(jpaStorage.isViewed(uniqueUrl));
                        }

                        // Concurrent filter query
                        List<ListingDto> filtered = jpaStorage.findByCriteria(
                                new ListingFilterDto(List.of(item.getCity()), null, 40.0, 60.0, null, null, null, null, false, false, "PRICE_ASC", 10, 0)
                        );
                        assertNotNull(filtered);
                    }
                } catch (Throwable ex) {
                    errors.add(ex);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "JPA concurrency stress test timed out after 30 seconds");
        assertTrue(errors.isEmpty(), "Encountered " + errors.size() + " errors during JPA concurrency stress: " + errors);
        assertEquals(threadCount * operationsPerThread, writeSuccesses.get());
        assertTrue(readSuccesses.get() > 0);
        assertEquals(threadCount * operationsPerThread, jpaStorage.getTotalCount());
    }

    // =========================================================================
    // 2. Concurrent Read/Write Operations against NitriteListingStorage
    // =========================================================================

    @Test
    @DisplayName("Challenge 2: Concurrent read/write stress on NitriteListingStorage with 12 parallel threads")
    void testConcurrentReadWriteNitriteStorage() throws Exception {
        Path tempDbPath = Files.createTempFile("nitrite_stress_", ".db");
        Files.deleteIfExists(tempDbPath); // Nitrite will create it

        NitriteDatabaseManager dbManager = NitriteDatabaseManager.getInstance(tempDbPath);
        NitriteListingStorage nitriteStorage = new NitriteListingStorage(dbManager);

        int threadCount = 12;
        int operationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger writeCount = new AtomicInteger(0);

        try {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < operationsPerThread; i++) {
                            String url = "https://nitrite-stress.hu/" + threadId + "/" + i;
                            IngatlanItem item = createItem(
                                    "no2_" + threadId + "_" + i,
                                    url,
                                    "Költözzbe",
                                    "Pécs",
                                    "45 M Ft", 45_000_000L, 45.0,
                                    "55 m2, 2 szoba", 55.0, 2.0,
                                    "2026-03-01 12:00:00"
                            );

                            // Write under write-lock
                            nitriteStorage.saveAll(List.of(item));
                            writeCount.incrementAndGet();

                            // Read under read-lock
                            assertTrue(nitriteStorage.containsUrl(url));
                            assertFalse(nitriteStorage.isNewUrl("Pécs", url));

                            // Query price history
                            List<PriceHistoryEntry> ph = nitriteStorage.getPriceHistory(url);
                            assertNotNull(ph);

                            if (i % 4 == 0) {
                                assertTrue(nitriteStorage.markAsViewed(url));
                                assertTrue(nitriteStorage.isViewed(url));
                            }
                        }
                    } catch (Throwable ex) {
                        errors.add(ex);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(completed, "Nitrite concurrency stress test timed out");
            assertTrue(errors.isEmpty(), "Nitrite concurrency stress encountered errors: " + errors);
            assertEquals(threadCount * operationsPerThread, writeCount.get());
            assertEquals(threadCount * operationsPerThread, nitriteStorage.getTotalCount());
        } finally {
            NitriteDatabaseManager.closeAll();
            Files.deleteIfExists(tempDbPath);
        }
    }

    // =========================================================================
    // 3. NoSQL DB File Lock & Creation Isolation during Spring Data JPA operations
    // =========================================================================

    @Test
    @DisplayName("Challenge 3: Empirically verify data/ingatlanok_nosql.db is NEVER created or locked by Spring JPA")
    void testNoSqlDbFileNotCreatedOrLockedByJpaOperations() throws IOException {
        // Step 1: Ensure no lingering file
        NitriteDatabaseManager.closeAll();
        if (Files.exists(DEFAULT_NOSQL_PATH)) {
            try {
                Files.delete(DEFAULT_NOSQL_PATH);
            } catch (Exception ignored) {
                // If locked, fail immediately
                fail("data/ingatlanok_nosql.db is currently locked before JPA test execution!");
            }
        }
        assertFalse(Files.exists(DEFAULT_NOSQL_PATH), "Pre-condition failed: nosql db should not exist");

        // Step 2: Perform extensive Spring Data JPA operations
        IngatlanItem item1 = createItem("jpa_iso_1", "https://iso.hu/1", "Duna House", "Budapest",
                "60 M Ft", 60_000_000L, 60.0, "60 m2", 60.0, 2.0, "2026-03-01 10:00:00");
        IngatlanItem item2 = createItem("jpa_iso_2", "https://iso.hu/2", "Otthon Centrum", "Debrecen",
                "80 M Ft", 80_000_000L, 80.0, "90 m2", 90.0, 3.0, "2026-03-01 11:00:00");

        jpaStorage.saveAll(List.of(item1, item2));
        assertEquals(2, jpaStorage.getAll().size());
        assertTrue(jpaStorage.containsUrl("https://iso.hu/1"));
        jpaStorage.markAsViewed("https://iso.hu/1");
        assertTrue(jpaStorage.isViewed("https://iso.hu/1"));
        assertEquals(1, jpaStorage.getItemsByCity("Budapest").size());

        SearchCriteria sc = new SearchCriteria();
        sc.setCity("Debrecen");
        jpaStorage.saveLastCriteria(sc);
        assertEquals("Debrecen", jpaStorage.getLastCriteria().getCity());

        // Step 3: Empirically verify that data/ingatlanok_nosql.db was NEVER created
        assertFalse(Files.exists(DEFAULT_NOSQL_PATH),
                "VIOLATION: data/ingatlanok_nosql.db was created during pure Spring Data JPA operations!");

        // Step 4: Verify IngatlanRepository singleton lazy loading does not initialize Nitrite until requested
        IngatlanRepository repo = IngatlanRepository.getInstance();
        assertNotNull(repo);
        // Ensure that simply referencing IngatlanRepository does not create the NoSQL file
        assertFalse(Files.exists(DEFAULT_NOSQL_PATH),
                "VIOLATION: IngatlanRepository.getInstance() eagerly triggered Nitrite database creation!");
    }

    // =========================================================================
    // 4. Consecutive Test Runs and MVStore File Lock Isolation
    // =========================================================================

    @Test
    @DisplayName("Challenge 4: Multiple consecutive open/close cycles do NOT fail with MVStoreException file lock")
    void testConsecutiveNitriteOpenCloseCycles() throws IOException {
        Path tempDbPath = Files.createTempFile("nitrite_lifecycle_", ".db");
        Files.deleteIfExists(tempDbPath);

        int cycles = 5;
        for (int cycle = 0; cycle < cycles; cycle++) {
            NitriteDatabaseManager dbManager = NitriteDatabaseManager.getInstance(tempDbPath);
            assertFalse(dbManager.isClosed(), "DB manager should be open on cycle " + cycle);

            NitriteListingStorage storage = new NitriteListingStorage(dbManager);
            String url = "https://cycle.hu/" + cycle;
            IngatlanItem item = createItem("cyc_" + cycle, url, "Portal", "Város", "30 M Ft", 30_000_000L, 30.0, "40 m2", 40.0, 1.0, "2026-03-01");
            storage.saveAll(List.of(item));
            assertTrue(storage.containsUrl(url));

            // Explicitly close
            NitriteDatabaseManager.closeAll();
            assertTrue(dbManager.isClosed(), "DB manager should be marked closed on cycle " + cycle);

            // Verify file can be read or manipulated immediately by OS without file lock error
            File file = tempDbPath.toFile();
            if (file.exists()) {
                assertTrue(file.canWrite(), "File lock held by MVStore on cycle " + cycle);
            }
        }

        Files.deleteIfExists(tempDbPath);
    }
}
