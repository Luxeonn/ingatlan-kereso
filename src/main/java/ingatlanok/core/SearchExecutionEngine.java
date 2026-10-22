package ingatlanok.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;

public class SearchExecutionEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(SearchExecutionEngine.class);

  private final PortalEngineRegistry portalRegistry;
  private final ListingStorage listingStorage;
  private final ConfigStorage configStorage;
  private final PropertyFilterEngine filterEngine;
  private final DeduplicationEngine dedupEngine;
  private final MarketAnalyticsEngine analyticsEngine;
  private final ParallelPortalScrapeExecutor scrapeExecutor;
  private final SearchResultAggregator resultAggregator;

  public SearchExecutionEngine(
      PortalEngineRegistry portalRegistry,
      ListingStorage listingStorage,
      ConfigStorage configStorage,
      PropertyFilterEngine filterEngine,
      DeduplicationEngine dedupEngine,
      MarketAnalyticsEngine analyticsEngine) {
    this(
        portalRegistry,
        listingStorage,
        configStorage,
        filterEngine,
        dedupEngine,
        analyticsEngine,
        new ParallelPortalScrapeExecutor(),
        new SearchResultAggregator());
  }

  public SearchExecutionEngine(
      PortalEngineRegistry portalRegistry,
      ListingStorage listingStorage,
      ConfigStorage configStorage,
      PropertyFilterEngine filterEngine,
      DeduplicationEngine dedupEngine,
      MarketAnalyticsEngine analyticsEngine,
      ParallelPortalScrapeExecutor scrapeExecutor,
      SearchResultAggregator resultAggregator) {
    this.portalRegistry = java.util.Objects.requireNonNull(portalRegistry);
    this.listingStorage = java.util.Objects.requireNonNull(listingStorage);
    this.configStorage = configStorage;
    this.filterEngine = java.util.Objects.requireNonNull(filterEngine);
    this.dedupEngine = java.util.Objects.requireNonNull(dedupEngine);
    this.analyticsEngine = java.util.Objects.requireNonNull(analyticsEngine);
    this.scrapeExecutor = java.util.Objects.requireNonNull(scrapeExecutor);
    this.resultAggregator = java.util.Objects.requireNonNull(resultAggregator);
  }

  public SearchResult executeSearch(SearchCriteria criteria) {
    if (criteria == null) {
      criteria = new SearchCriteria();
    }

    List<String> citiesToSearch = criteria.getCities();
    if (citiesToSearch.isEmpty()) {
      citiesToSearch = List.of("Debrecen");
    }

    LOGGER.info("==================================================");
    LOGGER.info(
        "Keresés indítása: Városok={}, Jelleg={}, Ársáv={}-{} M Ft, Alapterület={}-{} m²",
        citiesToSearch,
        criteria.getPropertyType(),
        criteria.getMinPriceMillion(),
        criteria.getMaxPriceMillion(),
        criteria.getMinArea(),
        criteria.getMaxArea());
    LOGGER.info("Aktív portálok: {}", criteria.getPortals());
    LOGGER.info("==================================================");

    List<IngatlanItem> allAggregatedItems = new ArrayList<>();
    Map<String, Integer> portalCounts = new LinkedHashMap<>();
    Map<String, Integer> cityCounts = new LinkedHashMap<>();
    List<String> newCitiesInitialized = new ArrayList<>();
    List<PortalScrapeResult> allScrapeResults = new ArrayList<>();

    for (PortalEngine engine : portalRegistry.getAllEngines()) {
      portalCounts.put(engine.getName(), 0);
    }

    int cityIndex = 0;
    for (String currentCity : citiesToSearch) {
      cityIndex++;
      if (cityIndex > 1) {
        try {
          Thread.sleep(400);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
      }

      boolean isFirstSearch = !listingStorage.hasCityDatabase(currentCity);
      if (isFirstSearch) {
        newCitiesInitialized.add(currentCity);
        LOGGER.info(
            "[Új Város] '{}' még nem szerepel az adatbázisban. Alapállapot snapshot rögzítése...",
            currentCity);
      } else {
        LOGGER.info("[Ismert Város] '{}' adatbázisa létezik. Differenciál-keresés...", currentCity);
      }

      SearchCriteria cityCriteria = criteria.cloneForCity(currentCity);
      List<PortalEngine> activeEngines = portalRegistry.getActiveEngines(cityCriteria);

      ParallelPortalScrapeExecutor.CityScrapeOutput scrapeOutput =
          scrapeExecutor.execute(activeEngines, cityCriteria, currentCity);
      allScrapeResults.addAll(scrapeOutput.results());
      scrapeOutput.portalCounts().forEach((k, v) -> portalCounts.merge(k, v, Integer::sum));

      PropertyFilterEngine.FilterResult relevanceResult =
          filterEngine.filterRelevance(scrapeOutput.items(), cityCriteria, currentCity);
      List<IngatlanItem> relevantCityItems = relevanceResult.matchingItems();

      dedupEngine.applyNewStatus(currentCity, relevantCityItems, isFirstSearch);
      List<IngatlanItem> uniqueCityItems = dedupEngine.deduplicateByUrl(relevantCityItems);
      listingStorage.saveCityItems(currentCity, uniqueCityItems);
      enrichWithPersistedState(uniqueCityItems);

      PropertyFilterEngine.FilterResult viewResult =
          filterEngine.filterView(uniqueCityItems, cityCriteria);
      List<IngatlanItem> visibleCityItems = viewResult.matchingItems();

      int cityMatchingCount = visibleCityItems.size();
      int cityTotalFiltered = scrapeOutput.items().size() - cityMatchingCount;
      LOGGER.info(
          "  └─ [{}] Összesítés: {} letöltve -> {} megfelelő találat (Kiszűrve: {} db | Ár: {},"
              + " Méret: {}, Megtekintett: {})",
          currentCity,
          scrapeOutput.items().size(),
          cityMatchingCount,
          cityTotalFiltered,
          relevanceResult.filteredByPriceCount(),
          relevanceResult.filteredByAreaCount(),
          viewResult.filteredByViewedCount());

      cityCounts.put(currentCity, visibleCityItems.size());
      allAggregatedItems.addAll(visibleCityItems);
    }

    List<IngatlanItem> uniqueAggregatedList = dedupEngine.deduplicateByUrl(allAggregatedItems);
    SearchResult result =
        resultAggregator.aggregate(
            uniqueAggregatedList,
            criteria,
            configStorage,
            portalCounts,
            cityCounts,
            newCitiesInitialized);
    result.getPortalResults().addAll(allScrapeResults);

    LOGGER.info("==================================================");
    LOGGER.info("Keresés befejeződött: {}", result.getExecutedAt());
    LOGGER.info("Összes találat: {} | Új: {}", result.getTotalFound(), result.getNewCount());
    LOGGER.info("Városok szerint: {}", result.getCityCounts());
    if (!result.getNewCitiesInitialized().isEmpty()) {
      LOGGER.info("Új város(ok) baseline mentve: {}", result.getNewCitiesInitialized());
    }
    LOGGER.info("==================================================");

    return result;
  }

  private void enrichWithPersistedState(List<IngatlanItem> items) {
    if (items == null || items.isEmpty()) {
      return;
    }
    List<String> urls =
        items.stream()
            .map(IngatlanItem::getUrl)
            .filter(u -> u != null && !u.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    if (urls.isEmpty()) {
      return;
    }
    Map<String, IngatlanItem> persistedByUrl = listingStorage.findByUrls(urls);
    if (persistedByUrl == null || persistedByUrl.isEmpty()) {
      return;
    }
    for (IngatlanItem item : items) {
      if (item == null || item.getUrl() == null) {
        continue;
      }
      IngatlanItem persisted = persistedByUrl.get(item.getUrl().trim());
      if (persisted == null) {
        continue;
      }
      item.setViewed(persisted.isViewed());
      item.setViewedAt(persisted.getViewedAt());
      item.setPriceDelta(persisted.getPriceDelta());
      item.setPriceChangePercent(persisted.getPriceChangePercent());
      item.setLastPriceChangeAt(persisted.getLastPriceChangeAt());
      if (persisted.getStatus() != null && !persisted.getStatus().isBlank()) {
        item.setStatus(persisted.getStatus());
      }
      item.setNewConstruction(item.isNewConstruction() || persisted.isNewConstruction());
    }
  }

  public PortalEngineRegistry getPortalRegistry() {
    return portalRegistry;
  }

  public ListingStorage getListingStorage() {
    return listingStorage;
  }

  public ConfigStorage getConfigStorage() {
    return configStorage;
  }

  public PropertyFilterEngine getFilterEngine() {
    return filterEngine;
  }

  public DeduplicationEngine getDedupEngine() {
    return dedupEngine;
  }

  public MarketAnalyticsEngine getAnalyticsEngine() {
    return analyticsEngine;
  }
}
