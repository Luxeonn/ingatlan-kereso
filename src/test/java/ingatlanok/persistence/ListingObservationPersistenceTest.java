package ingatlanok.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingObservation;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.repository.ListingObservationRepository;
import ingatlanok.persistence.repository.PropertyRepository;

@SpringBootTest
@DisplayName("ListingObservation Persistence and Lifecycle Integration Tests")
class ListingObservationPersistenceTest {

  @Autowired private JpaIngatlanStorage storage;
  @Autowired private ListingObservationRepository observationRepo;
  @Autowired private PropertyRepository propertyRepo;

  @BeforeEach
  void setUp() {
    storage.clearHistory();
  }

  private IngatlanItem createItem(String id, String url, String portal, String city, long priceHuf) {
    return createItem(id, url, portal, city, priceHuf, 50.0, "ACTIVE");
  }

  private IngatlanItem createItem(
      String id, String url, String portal, String city, Long priceHuf, Double areaSqm, String status) {
    IngatlanItem item = new IngatlanItem();
    item.setId(id);
    item.setUrl(url);
    item.setPortal(portal);
    item.setCity(city);
    item.setTitle("Teszt " + portal + " " + city);
    item.setStatus(status);
    if (priceHuf != null) {
      double million = priceHuf / 1_000_000.0;
      item.setPrice(million + " M Ft");
      item.setPriceInfo(new PriceInfo(item.getPrice(), priceHuf, million));
    }
    if (areaSqm != null && areaSqm > 0) {
      long perM2 = priceHuf != null ? (long) (priceHuf / areaSqm) : 0L;
      item.setDetailsInfo(new PropertyDetails(areaSqm + " m2, 2 szoba", areaSqm, 2.0, perM2));
    }
    item.setPropertyType(PropertyType.LAKAS);
    item.setDiscoveredAt("2026-10-24 10:00:00");
    item.setNew(true);
    return item;
  }

  @Test
  @DisplayName("Initial save records NEW observation and populates firstSeenAt and lastSeenAt")
  void testInitialSaveRecordsObservation() {
    String url = "https://dh.hu/ingatlan/test-obs-1";
    IngatlanItem item = createItem("DH-1001", url, "dh.hu", "Debrecen", 45_000_000L);

    storage.saveCityItems("Debrecen", List.of(item));

    List<ListingObservation> observations = storage.getListingObservations(url);
    assertEquals(1, observations.size());

    ListingObservation obs = observations.get(0);
    assertEquals(45_000_000L, obs.getPriceHuf());
    assertEquals(900_000L, obs.getPricePerM2());
    assertEquals(ListingObservation.StatusEvent.NEW, obs.getStatusEvent());
    assertEquals(0L, obs.getPriceDelta());

    PropertyEntity entity = propertyRepo.findByUrl(url).orElse(null);
    assertNotNull(entity);
    assertNotNull(entity.getFirstSeenAt());
    assertNotNull(entity.getLastSeenAt());
    assertEquals(entity.getFirstSeenAt(), entity.getDiscoveredAt());
  }

  @Test
  @DisplayName("Subsequent saves track PRICE_CHANGED and UNCHANGED time-series observations")
  void testSubsequentSavesTrackTimeSeries() {
    String url = "https://oc.hu/ingatlan/test-obs-2";
    IngatlanItem item1 = createItem("OC-2002", url, "oc.hu", "Debrecen", 50_000_000L);
    storage.saveCityItems("Debrecen", List.of(item1));

    // Save 2: Price drop to 47M
    IngatlanItem item2 = createItem("OC-2002", url, "oc.hu", "Debrecen", 47_000_000L);
    storage.saveCityItems("Debrecen", List.of(item2));

    // Save 3: Unchanged price 47M
    IngatlanItem item3 = createItem("OC-2002", url, "oc.hu", "Debrecen", 47_000_000L);
    storage.saveCityItems("Debrecen", List.of(item3));

    List<ListingObservation> observations = storage.getListingObservations(url);
    assertEquals(3, observations.size());

    assertEquals(ListingObservation.StatusEvent.NEW, observations.get(0).getStatusEvent());
    assertEquals(50_000_000L, observations.get(0).getPriceHuf());

    assertEquals(ListingObservation.StatusEvent.PRICE_CHANGED, observations.get(1).getStatusEvent());
    assertEquals(47_000_000L, observations.get(1).getPriceHuf());
    assertEquals(-3_000_000L, observations.get(1).getPriceDelta());

    assertEquals(ListingObservation.StatusEvent.UNCHANGED, observations.get(2).getStatusEvent());
    assertEquals(47_000_000L, observations.get(2).getPriceHuf());

    PropertyEntity entity = propertyRepo.findByUrl(url).orElse(null);
    assertNotNull(entity);
    assertEquals("oc.hu_OC-2002", entity.getPropertyId());
    List<ListingObservation> byPropId = storage.getListingObservations("oc.hu_OC-2002");
    assertEquals(3, byPropId.size());
  }

  @Test
  @DisplayName("Observations are deleted when city history or global history is cleared")
  void testObservationsCleanupOnClearHistory() {
    String url1 = "https://jf.hu/ingatlan/test-obs-3";
    String url2 = "https://kb.hu/ingatlan/test-obs-4";

    storage.saveCityItems("Szeged", List.of(createItem("JF-3003", url1, "jofogas.hu", "Szeged", 30_000_000L)));
    storage.saveCityItems("Miskolc", List.of(createItem("KB-4004", url2, "koltozzbe.hu", "Miskolc", 25_000_000L)));

    assertFalse(storage.getListingObservations(url1).isEmpty());
    assertFalse(storage.getListingObservations(url2).isEmpty());

    storage.clearCityHistory("Szeged");
    assertTrue(storage.getListingObservations(url1).isEmpty());
    assertFalse(storage.getListingObservations(url2).isEmpty());

    storage.clearHistory();
    assertTrue(storage.getListingObservations(url2).isEmpty());
  }

  @Test
  @DisplayName("Stress Test: Fluctuating price sequence preserves strict ordering and accurate deltas")
  void testFluctuatingPriceSequenceObservations() {
    String url = "https://dh.hu/ingatlan/stress-fluct-1";
    String propId = "dh.hu_FLUCT-1";

    // Sequence: 100M -> 90M (-10%) -> 90M (0%) -> 95M (+5.56%) -> 70M (-26.32%) -> 70M (0%)
    long[] prices = {100_000_000L, 90_000_000L, 90_000_000L, 95_000_000L, 70_000_000L, 70_000_000L};
    for (long price : prices) {
      IngatlanItem item = createItem("FLUCT-1", url, "dh.hu", "Debrecen", price, 100.0, "ACTIVE");
      storage.saveCityItems("Debrecen", List.of(item));
    }

    List<ListingObservation> obs = storage.getListingObservations(url);
    assertEquals(6, obs.size(), "Should record exactly 6 observations for 6 saves");

    // Obs 0: Initial NEW
    assertEquals(ListingObservation.StatusEvent.NEW, obs.get(0).getStatusEvent());
    assertEquals(100_000_000L, obs.get(0).getPriceHuf());
    assertEquals(1_000_000L, obs.get(0).getPricePerM2());
    assertEquals(0L, obs.get(0).getPriceDelta());

    // Obs 1: Price drop to 90M
    assertEquals(ListingObservation.StatusEvent.PRICE_CHANGED, obs.get(1).getStatusEvent());
    assertEquals(90_000_000L, obs.get(1).getPriceHuf());
    assertEquals(900_000L, obs.get(1).getPricePerM2());
    assertEquals(-10_000_000L, obs.get(1).getPriceDelta());
    assertEquals(-10.0, obs.get(1).getChangePercent());

    // Obs 2: Unchanged 90M
    assertEquals(ListingObservation.StatusEvent.UNCHANGED, obs.get(2).getStatusEvent());
    assertEquals(90_000_000L, obs.get(2).getPriceHuf());

    // Obs 3: Price increase to 95M
    assertEquals(ListingObservation.StatusEvent.PRICE_CHANGED, obs.get(3).getStatusEvent());
    assertEquals(95_000_000L, obs.get(3).getPriceHuf());
    assertEquals(950_000L, obs.get(3).getPricePerM2());
    assertEquals(5_000_000L, obs.get(3).getPriceDelta());
    assertEquals(5.56, obs.get(3).getChangePercent());

    // Obs 4: Price drop to 70M
    assertEquals(ListingObservation.StatusEvent.PRICE_CHANGED, obs.get(4).getStatusEvent());
    assertEquals(70_000_000L, obs.get(4).getPriceHuf());
    assertEquals(700_000L, obs.get(4).getPricePerM2());
    assertEquals(-25_000_000L, obs.get(4).getPriceDelta());
    assertEquals(-26.32, obs.get(4).getChangePercent());

    // Obs 5: Unchanged 70M
    assertEquals(ListingObservation.StatusEvent.UNCHANGED, obs.get(5).getStatusEvent());
    assertEquals(70_000_000L, obs.get(5).getPriceHuf());

    // Query by property ID returns the same 6 observations
    assertEquals(6, storage.getListingObservations(propId).size());
  }

  @Test
  @DisplayName("Adversarial Lifecycle: NEW -> UNCHANGED -> INACTIVE -> RELISTED transitions")
  void testLifecycleStatusEventDeterminism() {
    String url = "https://oc.hu/ingatlan/stress-lifecycle-1";

    // 1. Initial discovery -> NEW
    storage.saveCityItems("Szeged", List.of(createItem("LC-1", url, "oc.hu", "Szeged", 50_000_000L, 50.0, "ACTIVE")));
    List<ListingObservation> obs1 = storage.getListingObservations(url);
    assertEquals(1, obs1.size());
    assertEquals(ListingObservation.StatusEvent.NEW, obs1.get(0).getStatusEvent());

    // 2. Unchanged update -> UNCHANGED
    storage.saveCityItems("Szeged", List.of(createItem("LC-1", url, "oc.hu", "Szeged", 50_000_000L, 50.0, "ACTIVE")));
    List<ListingObservation> obs2 = storage.getListingObservations(url);
    assertEquals(2, obs2.size());
    assertEquals(ListingObservation.StatusEvent.UNCHANGED, obs2.get(1).getStatusEvent());

    // 3. Delisted / Inactive -> UNCHANGED observation, property status becomes INACTIVE
    storage.saveCityItems(
        "Szeged", List.of(createItem("LC-1", url, "oc.hu", "Szeged", 50_000_000L, 50.0, "INACTIVE")));
    List<ListingObservation> obs3 = storage.getListingObservations(url);
    assertEquals(3, obs3.size());
    assertEquals(ListingObservation.StatusEvent.UNCHANGED, obs3.get(2).getStatusEvent());

    PropertyEntity propEntity = propertyRepo.findByUrl(url).orElseThrow();
    assertEquals("INACTIVE", propEntity.getStatus());

    // 4. Inactive updated while still inactive -> UNCHANGED
    storage.saveCityItems(
        "Szeged", List.of(createItem("LC-1", url, "oc.hu", "Szeged", 50_000_000L, 50.0, "INACTIVE")));
    List<ListingObservation> obs4 = storage.getListingObservations(url);
    assertEquals(4, obs4.size());
    assertEquals(ListingObservation.StatusEvent.UNCHANGED, obs4.get(3).getStatusEvent());

    // 5. Relisted as ACTIVE with same price -> RELISTED
    storage.saveCityItems("Szeged", List.of(createItem("LC-1", url, "oc.hu", "Szeged", 50_000_000L, 50.0, "ACTIVE")));
    List<ListingObservation> obs5 = storage.getListingObservations(url);
    assertEquals(5, obs5.size());
    assertEquals(ListingObservation.StatusEvent.RELISTED, obs5.get(4).getStatusEvent());

    PropertyEntity relistedProp = propertyRepo.findByUrl(url).orElseThrow();
    assertEquals("ACTIVE", relistedProp.getStatus());

    // 6. Archived -> UNCHANGED, then back to ACTIVE -> RELISTED
    storage.saveCityItems(
        "Szeged", List.of(createItem("LC-1", url, "oc.hu", "Szeged", 50_000_000L, 50.0, "ARCHIVED")));
    storage.saveCityItems("Szeged", List.of(createItem("LC-1", url, "oc.hu", "Szeged", 50_000_000L, 50.0, "ACTIVE")));
    List<ListingObservation> obs6 = storage.getListingObservations(url);
    assertEquals(7, obs6.size());
    assertEquals(ListingObservation.StatusEvent.UNCHANGED, obs6.get(5).getStatusEvent());
    assertEquals(ListingObservation.StatusEvent.RELISTED, obs6.get(6).getStatusEvent());
  }

  @Test
  @DisplayName("Edge Case: Null price and null details do not break observation recording")
  void testNullPriceAndDetailsObservations() {
    String url = "https://jf.hu/ingatlan/null-price-1";

    IngatlanItem item = createItem("NULL-1", url, "jofogas.hu", "Pécs", null, null, "ACTIVE");
    storage.saveCityItems("Pécs", List.of(item));

    List<ListingObservation> obs = storage.getListingObservations(url);
    assertEquals(1, obs.size());
    assertEquals(ListingObservation.StatusEvent.NEW, obs.get(0).getStatusEvent());
    assertNull(obs.get(0).getPriceHuf());
    assertNull(obs.get(0).getPricePerM2());

    // Subsequent save with null price -> UNCHANGED
    storage.saveCityItems("Pécs", List.of(item));
    List<ListingObservation> obs2 = storage.getListingObservations(url);
    assertEquals(2, obs2.size());
    assertEquals(ListingObservation.StatusEvent.UNCHANGED, obs2.get(1).getStatusEvent());
  }

  @Test
  @DisplayName("Cleanup Cascade: Selective city history cleanup and total clearHistory wipe")
  void testCascadeCleanupStress() {
    for (String city : List.of("Debrecen", "Szeged", "Miskolc")) {
      List<IngatlanItem> items = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        items.add(createItem(
            city + "-" + i,
            "https://dh.hu/ingatlan/" + city + "-" + i,
            "dh.hu",
            city,
            30_000_000L + i * 1_000_000L,
            50.0,
            "ACTIVE"));
      }
      storage.saveCityItems(city, items);
      // Update each item with new price to produce 2 observations per item
      for (IngatlanItem item : items) {
        item.setPriceInfo(new PriceInfo("28.0 M Ft", 28_000_000L, 28.0));
      }
      storage.saveCityItems(city, items);
    }

    assertEquals(30, observationRepo.count(), "Total initial observations should be 30 (3 cities x 5 items x 2)");
    assertEquals(15, propertyRepo.count(), "Total initial properties should be 15 (3 cities x 5 items)");

    // Clear Debrecen only
    storage.clearCityHistory("Debrecen");

    assertEquals(20, observationRepo.count(), "Debrecen observations (10) deleted, 20 remaining");
    assertEquals(10, propertyRepo.count(), "Debrecen properties (5) deleted, 10 remaining");

    // Verify Debrecen observations are gone, Szeged remains intact
    assertTrue(storage.getListingObservations("https://dh.hu/ingatlan/Debrecen-0").isEmpty());
    assertEquals(2, storage.getListingObservations("https://dh.hu/ingatlan/Szeged-0").size());

    // Non-existent or empty city cleanup must be safe no-op
    assertDoesNotThrow(() -> storage.clearCityHistory("NemLetezoVaros"));
    assertDoesNotThrow(() -> storage.clearCityHistory(null));
    assertDoesNotThrow(() -> storage.clearCityHistory("   "));
    assertEquals(20, observationRepo.count());

    // Global clearHistory wipes all
    storage.clearHistory();
    assertEquals(0, observationRepo.count());
    assertEquals(0, propertyRepo.count());
  }

  @Test
  @DisplayName("Observation Query Polymorphism: url, propertyId, canonicalId, and whitespace resilience")
  void testObservationQueryPolymorphism() {
    String url = "https://kb.hu/ingatlan/poly-101";
    IngatlanItem item = createItem("POLY-101", url, "koltozzbe.hu", "Győr", 40_000_000L, 60.0, "ACTIVE");
    storage.saveCityItems("Győr", List.of(item));

    PropertyEntity entity = propertyRepo.findByUrl(url).orElseThrow();
    entity.setCanonicalId("CANONICAL-POLY-UUID");
    propertyRepo.save(entity);

    IngatlanItem item2 = createItem("POLY-101", url, "koltozzbe.hu", "Győr", 38_000_000L, 60.0, "ACTIVE");
    storage.saveCityItems("Győr", List.of(item2));

    List<ListingObservation> byUrl = storage.getListingObservations(url);
    assertEquals(2, byUrl.size());

    List<ListingObservation> byPropId = storage.getListingObservations("koltozzbe.hu_POLY-101");
    assertEquals(2, byPropId.size());

    List<ListingObservation> byCanonicalId = storage.getListingObservations("CANONICAL-POLY-UUID");
    assertFalse(byCanonicalId.isEmpty());

    assertTrue(storage.getListingObservations(null).isEmpty());
    assertTrue(storage.getListingObservations("").isEmpty());
    assertTrue(storage.getListingObservations("   ").isEmpty());
  }

  @Test
  @DisplayName("Concurrency: Concurrent scraper workers saving distinct cities without observation loss")
  void testConcurrentMultiCityScraping() throws InterruptedException, ExecutionException {
    int threadCount = 6;
    int itemsPerThread = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    List<Callable<Boolean>> tasks = new ArrayList<>();

    for (int t = 0; t < threadCount; t++) {
      final int threadIdx = t;
      tasks.add(() -> {
        String city = "City-" + threadIdx;
        List<IngatlanItem> items = new ArrayList<>();
        for (int i = 0; i < itemsPerThread; i++) {
          String url = "https://dh.hu/ingatlan/concurrent-" + threadIdx + "-" + i;
          items.add(createItem(
              "CONC-" + threadIdx + "-" + i,
              url,
              "dh.hu",
              city,
              35_000_000L + (i * 500_000L),
              55.0,
              "ACTIVE"));
        }
        storage.saveCityItems(city, items);
        return true;
      });
    }

    List<Future<Boolean>> futures = executor.invokeAll(tasks);
    executor.shutdown();
    assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

    for (Future<Boolean> future : futures) {
      assertTrue(future.get(), "Thread task failed during execution");
    }

    int expectedTotal = threadCount * itemsPerThread;
    assertEquals(expectedTotal, propertyRepo.count(), "All concurrent properties should be saved");
    assertEquals(expectedTotal, observationRepo.count(), "All concurrent observations should be saved");
  }
}
