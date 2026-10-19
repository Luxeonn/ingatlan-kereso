package ingatlanok.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;

@SpringBootTest
@DisplayName("Milestone 2 — Persistence Concurrency & Isolation Stress Tests (Challenger 2)")
public class PersistenceConcurrencyStressTest {

  @Autowired private JpaIngatlanStorage jpaStorage;

  @BeforeEach
  void setUp() {
    jpaStorage.clearHistory();
  }

  private IngatlanItem createItem(
      String id,
      String url,
      String portal,
      String city,
      String priceRaw,
      long priceAmount,
      double priceMillions,
      String details,
      double areaSqm,
      double rooms,
      String discoveredAt) {
    IngatlanItem item = new IngatlanItem();
    item.setId(id);
    item.setUrl(url);
    item.setPortal(portal);
    item.setCity(city);
    item.setTitle("Stress Listing " + id);
    item.setPrice(priceRaw);
    item.setPriceInfo(new PriceInfo(priceRaw, priceAmount, priceMillions));
    item.setDetails(details);
    item.setDetailsInfo(
        new PropertyDetails(details, areaSqm, rooms, (long) (priceAmount / areaSqm)));
    item.setDiscoveredAt(discoveredAt);
    item.setNew(true);
    item.setViewed(false);
    return item;
  }

  // =========================================================================
  // 1. Concurrent Read/Write Operations against JpaIngatlanStorage
  // =========================================================================

  @Test
  @DisplayName(
      "Challenge 1: Concurrent read/write stress on JpaIngatlanStorage with 16 parallel threads")
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
      executor.submit(
          () -> {
            try {
              startLatch.await(); // Thundering herd start
              for (int i = 0; i < operationsPerThread; i++) {
                String uniqueUrl = "https://jpa-stress.hu/" + threadId + "/" + i;
                IngatlanItem item =
                    createItem(
                        "jpa_" + threadId + "_" + i,
                        uniqueUrl,
                        (i % 2 == 0) ? "Duna House" : "Otthon Centrum",
                        (i % 3 == 0) ? "Budapest" : ((i % 3 == 1) ? "Debrecen" : "Szeged"),
                        "50 M Ft",
                        50_000_000L,
                        50.0,
                        "60 m2, 2 szoba",
                        60.0,
                        2.0,
                        "2026-03-01 10:00:00");

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
                List<ListingDto> filtered =
                    jpaStorage.findByCriteria(
                        new ListingFilterDto(
                            List.of(item.getCity()),
                            null,
                            40.0,
                            60.0,
                            null,
                            null,
                            null,
                            null,
                            false,
                            false,
                            "PRICE_ASC",
                            10,
                            0));
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
    assertTrue(
        errors.isEmpty(),
        "Encountered " + errors.size() + " errors during JPA concurrency stress: " + errors);
    assertEquals(threadCount * operationsPerThread, writeSuccesses.get());
    assertTrue(readSuccesses.get() > 0);
    assertEquals(threadCount * operationsPerThread, jpaStorage.getTotalCount());
  }
}
