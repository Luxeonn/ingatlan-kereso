package ingatlanok.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Builder;
import lombok.Getter;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;

@Getter
public class SearchExecutionEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(SearchExecutionEngine.class);

  private final PortalEngineRegistry portalRegistry;
  private final ListingStorage listingStorage;
  private final ConfigStorage configStorage;
  private final PropertyFilterEngine filterEngine;
  private final DeduplicationEngine dedupEngine;
  private final MarketAnalyticsEngine analyticsEngine;
  private final DataQualityEngine qualityEngine;
  private final ParallelPortalScrapeExecutor scrapeExecutor;
  private final SearchResultAggregator resultAggregator;
  private final SearchPersistenceSynchronizer persistenceSynchronizer;

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
        null,
        null,
        null);
  }

  public SearchExecutionEngine(
      PortalEngineRegistry portalRegistry,
      ListingStorage listingStorage,
      ConfigStorage configStorage,
      PropertyFilterEngine filterEngine,
      DeduplicationEngine dedupEngine,
      MarketAnalyticsEngine analyticsEngine,
      DataQualityEngine qualityEngine) {
    this(
        portalRegistry,
        listingStorage,
        configStorage,
        filterEngine,
        dedupEngine,
        analyticsEngine,
        qualityEngine,
        null,
        null);
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
    this(
        portalRegistry,
        listingStorage,
        configStorage,
        filterEngine,
        dedupEngine,
        analyticsEngine,
        null,
        scrapeExecutor,
        resultAggregator);
  }

  @Builder
  public SearchExecutionEngine(
      PortalEngineRegistry portalRegistry,
      ListingStorage listingStorage,
      ConfigStorage configStorage,
      PropertyFilterEngine filterEngine,
      DeduplicationEngine dedupEngine,
      MarketAnalyticsEngine analyticsEngine,
      DataQualityEngine qualityEngine,
      ParallelPortalScrapeExecutor scrapeExecutor,
      SearchResultAggregator resultAggregator) {
    this.portalRegistry = Objects.requireNonNull(portalRegistry);
    this.listingStorage = Objects.requireNonNull(listingStorage);
    this.configStorage = configStorage;
    this.filterEngine = Objects.requireNonNull(filterEngine);
    this.dedupEngine = Objects.requireNonNull(dedupEngine);
    this.analyticsEngine = Objects.requireNonNull(analyticsEngine);
    this.qualityEngine = qualityEngine != null ? qualityEngine : new DataQualityEngine();
    this.scrapeExecutor =
        scrapeExecutor != null ? scrapeExecutor : new ParallelPortalScrapeExecutor();
    this.resultAggregator =
        resultAggregator != null ? resultAggregator : new SearchResultAggregator();
    this.persistenceSynchronizer =
        new SearchPersistenceSynchronizer(this.portalRegistry, this.listingStorage);
  }

  public SearchResult executeSearch(SearchCriteria criteria) {
    SearchCriteria effectiveCriteria = criteria != null ? criteria : new SearchCriteria();

    List<String> citiesToSearch = effectiveCriteria.getCities();
    if (citiesToSearch.isEmpty()) {
      citiesToSearch = List.of("Debrecen");
    }

    logSearchStart(citiesToSearch, effectiveCriteria);

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
      pauseBetweenCities(cityIndex);

      boolean isFirstSearch = !listingStorage.hasCityDatabase(currentCity);
      if (isFirstSearch) {
        newCitiesInitialized.add(currentCity);
        LOGGER.info(
            "[Új Város] '{}' még nem szerepel az adatbázisban. Alapállapot snapshot rögzítése...",
            currentCity);
      } else {
        LOGGER.info("[Ismert Város] '{}' adatbázisa létezik. Differenciál-keresés...", currentCity);
      }

      List<IngatlanItem> visibleCityItems =
          processCity(currentCity, effectiveCriteria, portalCounts, allScrapeResults, isFirstSearch);
      cityCounts.put(currentCity, visibleCityItems.size());
      allAggregatedItems.addAll(visibleCityItems);
    }

    List<IngatlanItem> uniqueAggregatedList = dedupEngine.deduplicateMultiSignal(allAggregatedItems);
    qualityEngine.evaluateAll(uniqueAggregatedList);
    logQualityStats(allAggregatedItems.size(), uniqueAggregatedList);

    persistenceSynchronizer.recalculateRelevantPortalCounts(portalCounts, uniqueAggregatedList);
    List<PortalScrapeResult> aggregatedPortalResults =
        persistenceSynchronizer.aggregatePortalResultsByPortal(
            allScrapeResults, uniqueAggregatedList);
    SearchResult result =
        resultAggregator.aggregate(
            uniqueAggregatedList,
            effectiveCriteria,
            configStorage,
            portalCounts,
            cityCounts,
            newCitiesInitialized);
    result.getPortalResults().addAll(aggregatedPortalResults);
    applyMultiCityExecutionStatus(result, allScrapeResults);

    logSearchEnd(result);
    return result;
  }

  private void applyMultiCityExecutionStatus(
      SearchResult result, List<PortalScrapeResult> allScrapeResults) {
    boolean hasFailure = false;
    boolean hasSuccess = false;
    for (PortalScrapeResult pr : allScrapeResults) {
      if (pr == null) {
        continue;
      }
      if (pr.getStatus() == ingatlanok.model.PortalScrapeStatus.FAILED
          || pr.getStatus() == ingatlanok.model.PortalScrapeStatus.BOT_PROTECTED) {
        hasFailure = true;
      } else {
        hasSuccess = true;
      }
    }
    if (hasFailure && hasSuccess) {
      result.setExecutionStatus(ingatlanok.model.SearchExecutionStatus.PARTIAL_SUCCESS);
    }
  }

  private void logSearchStart(List<String> citiesToSearch, SearchCriteria criteria) {
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
  }

  private void pauseBetweenCities(int cityIndex) {
    if (cityIndex > 1) {
      try {
        Thread.sleep(400);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private List<IngatlanItem> processCity(
      String currentCity,
      SearchCriteria criteria,
      Map<String, Integer> portalCounts,
      List<PortalScrapeResult> allScrapeResults,
      boolean isFirstSearch) {
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
    LOGGER.info(
        "  ├─ [{}] Adattisztítás: {} letöltve -> {} releváns (ár: {}, méret: {}, város: {}) -> {}"
            + " egyedi URL",
        currentCity,
        scrapeOutput.items().size(),
        relevantCityItems.size(),
        relevanceResult.filteredByPriceCount(),
        relevanceResult.filteredByAreaCount(),
        relevanceResult.filteredByCityCount(),
        uniqueCityItems.size());
    listingStorage.saveCityItems(currentCity, uniqueCityItems);
    persistenceSynchronizer.enrichWithPersistedState(uniqueCityItems);
    qualityEngine.evaluateAll(uniqueCityItems);

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

    return visibleCityItems;
  }

  private void logQualityStats(int totalItems, List<IngatlanItem> uniqueAggregatedList) {
    long lowQuality =
        uniqueAggregatedList.stream()
            .filter(
                i ->
                    i.getQualityScore() != null
                        && i.getQualityScore() < DataQualityEngine.PASSING_THRESHOLD)
            .count();
    LOGGER.info(
        "Deduplikáció és minőségellenőrzés: {} -> {} egyedi ingatlan ({} összevonva), {} db {} pont"
            + " alatti minőségű",
        totalItems,
        uniqueAggregatedList.size(),
        totalItems - uniqueAggregatedList.size(),
        lowQuality,
        DataQualityEngine.PASSING_THRESHOLD);
  }

  private void logSearchEnd(SearchResult result) {
    LOGGER.info("==================================================");
    LOGGER.info("Keresés befejeződött: {}", result.getExecutedAt());
    LOGGER.info("Összes találat: {} | Új: {}", result.getTotalFound(), result.getNewCount());
    LOGGER.info("Városok szerint: {}", result.getCityCounts());
    LOGGER.info("Portálok szerint: {}", result.getPortalCounts());
    if (!result.getNewCitiesInitialized().isEmpty()) {
      LOGGER.info("Új város(ok) baseline mentve: {}", result.getNewCitiesInitialized());
    }
    LOGGER.info("==================================================");
  }
}
