package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.InMemoryListingStorage;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;

class SearchExecutionEngineTest {

  @Test
  @DisplayName("Párhuzamos futtatás és 0-Crash garancia hibás portál esetén")
  void testExecuteSearchWithCrashingAndSucceedingPortals() {
    ListingStorage mockStorage =
        new ListingStorage() {
          private final Map<String, List<IngatlanItem>> stored = new HashMap<>();

          @Override
          public void saveCityItems(String city, Collection<IngatlanItem> items) {
            stored.put(city, new ArrayList<>(items));
          }

          @Override
          public void saveAll(Collection<IngatlanItem> items) {}

          @Override
          public List<IngatlanItem> getItemsByCity(String city) {
            return stored.getOrDefault(city, List.of());
          }

          @Override
          public List<IngatlanItem> getAll() {
            return List.of();
          }

          @Override
          public boolean hasCityDatabase(String city) {
            return true;
          }

          @Override
          public boolean isNewUrl(String city, String url) {
            return true;
          }

          @Override
          public boolean containsUrl(String url) {
            return false;
          }

          @Override
          public boolean markAsViewed(String url) {
            return false;
          }

          @Override
          public boolean isViewed(String url) {
            return false;
          }

          @Override
          public Set<String> getAllUrls() {
            return Set.of();
          }

          @Override
          public List<Map<String, Object>> getTrackedCitiesInfo() {
            return List.of();
          }

          @Override
          public void clearCityHistory(String city) {}

          @Override
          public void clearHistory() {}

          @Override
          public int getTotalCount() {
            return 0;
          }

          @Override
          public String getLastRunTime() {
            return null;
          }
        };

    ConfigStorage mockConfig =
        new ConfigStorage() {
          private SearchCriteria last;

          @Override
          public SearchCriteria getLastCriteria() {
            return last != null ? last : new SearchCriteria();
          }

          @Override
          public void saveLastCriteria(SearchCriteria criteria) {
            this.last = criteria;
          }
        };

    PortalEngine goodEngine =
        new PortalEngine() {
          @Override
          public PortalDescriptor getDescriptor() {
            return new PortalDescriptor(
                "good", "Good Portal", "#123456", "https://good.hu", true, List.of("lakas"), true);
          }

          @Override
          public PortalScrapeResult scrape(SearchCriteria criteria) {
            IngatlanItem item =
                new IngatlanItem(
                    "1",
                    PortalSource.of("good"),
                    "https://good.hu/1",
                    "Szép lakás",
                    new PriceInfo("50 M Ft", 50_000_000L, 50.0),
                    criteria.getCity(),
                    new PropertyDetails("60 m²", 60.0, 2.0, 833_333L),
                    "2026-10-14",
                    true);
            return PortalScrapeResult.success("good", "Good Portal", List.of(item), 100);
          }
        };

    PortalEngine failingEngine =
        new PortalEngine() {
          @Override
          public PortalDescriptor getDescriptor() {
            return new PortalDescriptor(
                "fail",
                "Failing Portal",
                "#654321",
                "https://fail.hu",
                true,
                List.of("lakas"),
                true);
          }

          @Override
          public PortalScrapeResult scrape(SearchCriteria criteria) {
            throw new RuntimeException("Simulated network timeout");
          }
        };

    PortalEngineRegistry registry = new PortalEngineRegistry(List.of(goodEngine, failingEngine));

    SearchExecutionEngine engine =
        new SearchExecutionEngine(
            registry,
            mockStorage,
            mockConfig,
            new PropertyFilterEngine(),
            new DeduplicationEngine(mockStorage),
            new MarketAnalyticsEngine());

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCities(List.of("Debrecen"));
    criteria.setPortals(List.of("good", "fail"));
    criteria.setMinPriceMillion(40);
    criteria.setMaxPriceMillion(70);
    criteria.setMinArea(null);
    criteria.setMaxArea(null);

    SearchResult result = engine.executeSearch(criteria);

    assertNotNull(result);
    assertEquals(1, result.getItems().size());
    assertEquals("https://good.hu/1", result.getItems().get(0).getUrl());
    assertEquals(2, result.getPortalResults().size());

    boolean hasSuccess =
        result.getPortalResults().stream()
            .anyMatch(
                pr ->
                    "good".equals(pr.getPortalId())
                        && pr.getStatus() == ingatlanok.model.PortalScrapeStatus.SUCCESS);
    boolean hasFailure =
        result.getPortalResults().stream()
            .anyMatch(
                pr ->
                    "fail".equals(pr.getPortalId())
                        && pr.getStatus() == ingatlanok.model.PortalScrapeStatus.FAILED);

    assertTrue(hasSuccess, "A jó portálnak sikeresnek kell lennie");
    assertTrue(hasFailure, "A hibás portálnak hibaállapotot kell rögzítenie leállás nélkül");
  }

  @Test
  @DisplayName("onlyDiscounted szűrés és ár-delta dúsítás mentés után")
  void testOnlyDiscountedEnrichmentAfterPersistence() {
    AtomicReference<Long> currentPriceHuf = new AtomicReference<>(50_000_000L);
    AtomicInteger findByUrlsCallCount = new AtomicInteger(0);

    InMemoryListingStorage baseStorage =
        new InMemoryListingStorage() {
          @Override
          public Map<String, IngatlanItem> findByUrls(Collection<String> urls) {
            findByUrlsCallCount.incrementAndGet();
            return super.findByUrls(urls);
          }
        };

    PortalEngine stubEngine =
        new PortalEngine() {
          @Override
          public PortalDescriptor getDescriptor() {
            return new PortalDescriptor(
                "stub", "Stub Portal", "#112233", "https://stub.hu", true, List.of("lakas"), true);
          }

          @Override
          public PortalScrapeResult scrape(SearchCriteria criteria) {
            long huf = currentPriceHuf.get();
            double million = huf / 1_000_000.0;
            IngatlanItem item =
                new IngatlanItem(
                    "s1",
                    PortalSource.of("stub"),
                    "https://stub.hu/ingatlan/1",
                    "Debreceni lakás",
                    new PriceInfo(million + " M Ft", huf, million),
                    "Debrecen",
                    new PropertyDetails("60 m²", 60.0, 2.0, huf / 60),
                    "2026-10-20 10:00:00",
                    false);
            return PortalScrapeResult.success("stub", "Stub Portal", List.of(item), 10);
          }
        };

    SearchExecutionEngine engine =
        new SearchExecutionEngine(
            new PortalEngineRegistry(List.of(stubEngine)),
            baseStorage,
            baseStorage,
            new PropertyFilterEngine(),
            new DeduplicationEngine(baseStorage),
            new MarketAnalyticsEngine());

    SearchCriteria firstRun = new SearchCriteria();
    firstRun.setCities(List.of("Debrecen"));
    firstRun.setPortals(List.of("stub"));
    firstRun.setMinPriceMillion(30);
    firstRun.setMaxPriceMillion(70);
    firstRun.setMinArea(40);
    firstRun.setMaxArea(100);

    SearchResult result1 = engine.executeSearch(firstRun);
    assertEquals(1, result1.getItems().size());
    assertEquals(0L, result1.getItems().get(0).getPriceDelta());
    assertEquals(1, findByUrlsCallCount.get());

    currentPriceHuf.set(45_000_000L);
    SearchCriteria secondRun = firstRun.cloneForCity("Debrecen");
    secondRun.setOnlyDiscounted(true);

    SearchResult result2 = engine.executeSearch(secondRun);
    assertEquals(1, result2.getItems().size());
    IngatlanItem discountedItem = result2.getItems().get(0);
    assertEquals(-5_000_000L, discountedItem.getPriceDelta());
    assertEquals(-10.0, discountedItem.getPriceChangePercent());
    assertEquals("PRICE_DROP", discountedItem.getStatus());
    assertTrue(discountedItem.isDiscounted());
    assertEquals(2, findByUrlsCallCount.get());
  }

  @Test
  @DisplayName("Korábban megtekintett hirdetés újrakeresés után viewed=true és excludeViewed kiszűri")
  void testViewedEnrichmentAndExcludeViewedFilter() {
    InMemoryListingStorage storage = new InMemoryListingStorage();

    PortalEngine stubEngine =
        new PortalEngine() {
          @Override
          public PortalDescriptor getDescriptor() {
            return new PortalDescriptor(
                "stub", "Stub Portal", "#112233", "https://stub.hu", true, List.of("lakas"), true);
          }

          @Override
          public PortalScrapeResult scrape(SearchCriteria criteria) {
            IngatlanItem item1 =
                new IngatlanItem(
                    "s1",
                    PortalSource.of("stub"),
                    "https://stub.hu/ingatlan/viewed",
                    "Megtekintett lakás",
                    new PriceInfo("50 M Ft", 50_000_000L, 50.0),
                    "Debrecen",
                    new PropertyDetails("60 m²", 60.0, 2.0, 833_333L),
                    "2026-10-20 10:00:00",
                    false);
            IngatlanItem item2 =
                new IngatlanItem(
                    "s2",
                    PortalSource.of("stub"),
                    "https://stub.hu/ingatlan/unviewed",
                    "Nem megtekintett lakás",
                    new PriceInfo("55 M Ft", 55_000_000L, 55.0),
                    "Debrecen",
                    new PropertyDetails("65 m²", 65.0, 2.0, 846_153L),
                    "2026-10-20 10:00:00",
                    false);
            return PortalScrapeResult.success("stub", "Stub Portal", List.of(item1, item2), 10);
          }
        };

    SearchExecutionEngine engine =
        new SearchExecutionEngine(
            new PortalEngineRegistry(List.of(stubEngine)),
            storage,
            storage,
            new PropertyFilterEngine(),
            new DeduplicationEngine(storage),
            new MarketAnalyticsEngine());

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCities(List.of("Debrecen"));
    criteria.setPortals(List.of("stub"));
    criteria.setMinPriceMillion(40);
    criteria.setMaxPriceMillion(70);
    criteria.setMinArea(40);
    criteria.setMaxArea(100);

    SearchResult r1 = engine.executeSearch(criteria);
    assertEquals(2, r1.getItems().size());
    assertTrue(storage.markAsViewed("https://stub.hu/ingatlan/viewed"));

    // Újrakeresés excludeViewed=false mellett: viewed állapot visszatér a találaton
    SearchResult r2 = engine.executeSearch(criteria);
    assertEquals(2, r2.getItems().size());
    IngatlanItem viewedInR2 =
        r2.getItems().stream()
            .filter(i -> "https://stub.hu/ingatlan/viewed".equals(i.getUrl()))
            .findFirst()
            .orElseThrow();
    assertTrue(viewedInR2.isViewed());

    // Újrakeresés excludeViewed=true mellett: a megtekintett elem kimarad
    criteria.setExcludeViewed(true);
    SearchResult r3 = engine.executeSearch(criteria);
    assertEquals(1, r3.getItems().size());
    assertEquals("https://stub.hu/ingatlan/unviewed", r3.getItems().get(0).getUrl());
    assertFalse(r3.getItems().get(0).isViewed());
  }

  @Test
  @DisplayName("onlyNewConstruction nézetszűrő nem rontja el a város baseline-t és az isNew jelölést")
  void testViewFilterDoesNotCorruptCityBaseline() {
    InMemoryListingStorage storage = new InMemoryListingStorage();

    PortalEngine stubEngine =
        new PortalEngine() {
          @Override
          public PortalDescriptor getDescriptor() {
            return new PortalDescriptor(
                "stub", "Stub Portal", "#112233", "https://stub.hu", true, List.of("lakas"), true);
          }

          @Override
          public PortalScrapeResult scrape(SearchCriteria criteria) {
            IngatlanItem newBuild =
                new IngatlanItem(
                    "a",
                    PortalSource.of("stub"),
                    "https://stub.hu/ingatlan/new-build",
                    "Új építésű lakás",
                    new PriceInfo("60 M Ft", 60_000_000L, 60.0),
                    "Debrecen",
                    new PropertyDetails("60 m²", 60.0, 2.0, 1_000_000L),
                    "2026-10-20 10:00:00",
                    false);
            newBuild.setNewConstruction(true);

            IngatlanItem usedHome =
                new IngatlanItem(
                    "b",
                    PortalSource.of("stub"),
                    "https://stub.hu/ingatlan/used-home",
                    "Használt lakás",
                    new PriceInfo("50 M Ft", 50_000_000L, 50.0),
                    "Debrecen",
                    new PropertyDetails("60 m²", 60.0, 2.0, 833_333L),
                    "2026-10-20 10:00:00",
                    false);
            usedHome.setNewConstruction(false);

            return PortalScrapeResult.success(
                "stub", "Stub Portal", List.of(newBuild, usedHome), 10);
          }
        };

    SearchExecutionEngine engine =
        new SearchExecutionEngine(
            new PortalEngineRegistry(List.of(stubEngine)),
            storage,
            storage,
            new PropertyFilterEngine(),
            new DeduplicationEngine(storage),
            new MarketAnalyticsEngine());

    SearchCriteria firstRun = new SearchCriteria();
    firstRun.setCities(List.of("Debrecen"));
    firstRun.setPortals(List.of("stub"));
    firstRun.setMinPriceMillion(40);
    firstRun.setMaxPriceMillion(70);
    firstRun.setMinArea(40);
    firstRun.setMaxArea(100);
    firstRun.setOnlyNewConstruction(true);

    SearchResult r1 = engine.executeSearch(firstRun);
    assertEquals(1, r1.getItems().size());
    assertEquals("https://stub.hu/ingatlan/new-build", r1.getItems().get(0).getUrl());
    // A használt lakásnak is be kellett kerülnie a baseline adatbázisba!
    assertTrue(storage.containsUrl("https://stub.hu/ingatlan/used-home"));

    SearchCriteria secondRun = firstRun.cloneForCity("Debrecen");
    secondRun.setOnlyNewConstruction(false);
    SearchResult r2 = engine.executeSearch(secondRun);
    assertEquals(2, r2.getItems().size());
    IngatlanItem usedInR2 =
        r2.getItems().stream()
            .filter(i -> "https://stub.hu/ingatlan/used-home".equals(i.getUrl()))
            .findFirst()
            .orElseThrow();
    assertFalse(usedInR2.isNew(), "A meglévő nem-újépítésű hirdetés nem lehet hamisan ÚJ");
  }
}
