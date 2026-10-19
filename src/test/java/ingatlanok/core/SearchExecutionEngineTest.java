package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.scraper.PortalScrapeResult;

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

    PortalEngineRegistry registry = PortalEngineRegistry.getInstance();
    registry.register(goodEngine);
    registry.register(failingEngine);

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
                        && pr.getStatus() == ingatlanok.scraper.PortalScrapeStatus.SUCCESS);
    boolean hasFailure =
        result.getPortalResults().stream()
            .anyMatch(
                pr ->
                    "fail".equals(pr.getPortalId())
                        && pr.getStatus() == ingatlanok.scraper.PortalScrapeStatus.FAILED);

    assertTrue(hasSuccess, "A jó portálnak sikeresnek kell lennie");
    assertTrue(hasFailure, "A hibás portálnak hibaállapotot kell rögzítenie leállás nélkül");
  }
}
