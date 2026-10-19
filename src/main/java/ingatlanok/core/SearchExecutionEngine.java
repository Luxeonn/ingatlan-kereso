package ingatlanok.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.scraper.PortalScrapeResult;

public class SearchExecutionEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(SearchExecutionEngine.class);
  private static final SearchExecutionEngine INSTANCE = new SearchExecutionEngine();

  private final PortalEngineRegistry portalRegistry;
  private final ListingStorage listingStorage;
  private final ConfigStorage configStorage;
  private final PropertyFilterEngine filterEngine;
  private final DeduplicationEngine dedupEngine;
  private final MarketAnalyticsEngine analyticsEngine;
  private final ParallelPortalScrapeExecutor scrapeExecutor;
  private final SearchResultAggregator resultAggregator;

  private SearchExecutionEngine() {
    this(
        PortalEngineRegistry.getInstance(),
        IngatlanRepository.getInstance(),
        IngatlanRepository.getInstance(),
        new PropertyFilterEngine(),
        new DeduplicationEngine(IngatlanRepository.getInstance()),
        new MarketAnalyticsEngine());
  }

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
    this.portalRegistry =
        portalRegistry != null ? portalRegistry : PortalEngineRegistry.getInstance();
    this.listingStorage =
        listingStorage != null ? listingStorage : IngatlanRepository.getInstance();
    this.configStorage = configStorage != null ? configStorage : IngatlanRepository.getInstance();
    this.filterEngine = filterEngine != null ? filterEngine : new PropertyFilterEngine();
    this.dedupEngine =
        dedupEngine != null ? dedupEngine : new DeduplicationEngine(this.listingStorage);
    this.analyticsEngine = analyticsEngine != null ? analyticsEngine : new MarketAnalyticsEngine();
    this.scrapeExecutor =
        scrapeExecutor != null ? scrapeExecutor : new ParallelPortalScrapeExecutor();
    this.resultAggregator =
        resultAggregator != null ? resultAggregator : new SearchResultAggregator();
  }

  public static SearchExecutionEngine getInstance() {
    return INSTANCE;
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

      PropertyFilterEngine.FilterResult filterResult =
          filterEngine.filter(scrapeOutput.items(), cityCriteria, currentCity);
      List<IngatlanItem> filteredCityItems = filterResult.matchingItems();

      dedupEngine.applyNewStatus(currentCity, filteredCityItems, isFirstSearch);
      List<IngatlanItem> uniqueCityItems = dedupEngine.deduplicateByUrl(filteredCityItems);
      listingStorage.saveCityItems(currentCity, uniqueCityItems);

      int cityMatchingCount = uniqueCityItems.size();
      int cityTotalFiltered = scrapeOutput.items().size() - cityMatchingCount;
      LOGGER.info(
          "  └─ [{}] Összesítés: {} letöltve -> {} megfelelő találat (Kiszűrve: {} db | Ár: {},"
              + " Méret: {}, Megtekintett: {})",
          currentCity,
          scrapeOutput.items().size(),
          cityMatchingCount,
          cityTotalFiltered,
          filterResult.filteredByPriceCount(),
          filterResult.filteredByAreaCount(),
          filterResult.filteredByViewedCount());

      cityCounts.put(currentCity, uniqueCityItems.size());
      allAggregatedItems.addAll(uniqueCityItems);
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
