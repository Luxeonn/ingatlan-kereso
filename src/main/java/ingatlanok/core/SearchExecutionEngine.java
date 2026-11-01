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
  private final DataQualityEngine qualityEngine;
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
        new DataQualityEngine(),
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
      DataQualityEngine qualityEngine) {
    this(
        portalRegistry,
        listingStorage,
        configStorage,
        filterEngine,
        dedupEngine,
        analyticsEngine,
        qualityEngine,
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
    this(
        portalRegistry,
        listingStorage,
        configStorage,
        filterEngine,
        dedupEngine,
        analyticsEngine,
        new DataQualityEngine(),
        scrapeExecutor,
        resultAggregator);
  }

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
    this.portalRegistry = java.util.Objects.requireNonNull(portalRegistry);
    this.listingStorage = java.util.Objects.requireNonNull(listingStorage);
    this.configStorage = configStorage;
    this.filterEngine = java.util.Objects.requireNonNull(filterEngine);
    this.dedupEngine = java.util.Objects.requireNonNull(dedupEngine);
    this.analyticsEngine = java.util.Objects.requireNonNull(analyticsEngine);
    this.qualityEngine = qualityEngine != null ? qualityEngine : new DataQualityEngine();
    this.scrapeExecutor = java.util.Objects.requireNonNull(scrapeExecutor);
    this.resultAggregator = java.util.Objects.requireNonNull(resultAggregator);
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

    recalculateRelevantPortalCounts(portalCounts, uniqueAggregatedList);
    List<PortalScrapeResult> aggregatedPortalResults =
        aggregatePortalResultsByPortal(allScrapeResults, uniqueAggregatedList);
    SearchResult result =
        resultAggregator.aggregate(
            uniqueAggregatedList,
            effectiveCriteria,
            configStorage,
            portalCounts,
            cityCounts,
            newCitiesInitialized);
    result.getPortalResults().addAll(aggregatedPortalResults);

    logSearchEnd(result);
    return result;
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
    enrichWithPersistedState(uniqueCityItems);
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
      if (item != null && item.getUrl() != null) {
        applyPersistedFields(item, persistedByUrl.get(item.getUrl().trim()));
      }
    }
  }

  private void applyPersistedFields(IngatlanItem item, IngatlanItem persisted) {
    if (persisted == null) {
      return;
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

  private void recalculateRelevantPortalCounts(
      Map<String, Integer> portalCounts, List<IngatlanItem> relevantItems) {
    portalCounts.replaceAll((k, v) -> 0);
    if (relevantItems == null || relevantItems.isEmpty()) {
      return;
    }
    for (IngatlanItem item : relevantItems) {
      if (item == null) {
        continue;
      }
      String key = resolvePortalCountKey(item);
      portalCounts.merge(key, 1, Integer::sum);
    }
  }

  private String resolvePortalCountKey(IngatlanItem item) {
    String portalId = item.getSource() != null ? item.getSource().getPortalId() : null;
    if (portalId != null && !portalId.isBlank()) {
      java.util.Optional<PortalEngine> engineOpt = portalRegistry.getEngine(portalId);
      if (engineOpt.isPresent()) {
        return engineOpt.get().getName();
      }
    }
    String rawPortal =
        item.getPortal() != null && !item.getPortal().isBlank()
            ? item.getPortal().trim()
            : (item.getSource() != null && item.getSource().getPortalName() != null
                ? item.getSource().getPortalName().trim()
                : "Ismeretlen");
    for (PortalEngine engine : portalRegistry.getAllEngines()) {
      if (engine.getName().equalsIgnoreCase(rawPortal)
          || engine.getId().equalsIgnoreCase(rawPortal)) {
        return engine.getName();
      }
    }
    return rawPortal;
  }

  private boolean matchesPortal(IngatlanItem item, String portalId, String portalName) {
    if (item == null) {
      return false;
    }
    return matchesPortalId(item, portalId)
        || matchesPortalName(item, portalName)
        || matchesPortalIdToName(item, portalId)
        || matchesResolvedKey(item, portalName);
  }

  private boolean matchesPortalId(IngatlanItem item, String portalId) {
    String itemPortalId = item.getSource() != null ? item.getSource().getPortalId() : null;
    return portalId != null && itemPortalId != null && portalId.equalsIgnoreCase(itemPortalId);
  }

  private boolean matchesPortalName(IngatlanItem item, String portalName) {
    String itemPortalName =
        item.getSource() != null && item.getSource().getPortalName() != null
            ? item.getSource().getPortalName()
            : item.getPortal();
    return portalName != null && itemPortalName != null && portalName.equalsIgnoreCase(itemPortalName);
  }

  private boolean matchesPortalIdToName(IngatlanItem item, String portalId) {
    String itemPortalName =
        item.getSource() != null && item.getSource().getPortalName() != null
            ? item.getSource().getPortalName()
            : item.getPortal();
    return portalId != null && itemPortalName != null && portalId.equalsIgnoreCase(itemPortalName);
  }

  private boolean matchesResolvedKey(IngatlanItem item, String portalName) {
    String resolvedKey = resolvePortalCountKey(item);
    return portalName != null && portalName.equalsIgnoreCase(resolvedKey);
  }

  private List<PortalScrapeResult> aggregatePortalResultsByPortal(
      List<PortalScrapeResult> allScrapeResults, List<IngatlanItem> relevantItems) {
    if (allScrapeResults == null || allScrapeResults.isEmpty()) {
      return List.of();
    }
    Map<String, List<PortalScrapeResult>> grouped = new LinkedHashMap<>();
    for (PortalScrapeResult pr : allScrapeResults) {
      if (pr != null) {
        String key = resolveGroupKey(pr);
        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(pr);
      }
    }

    List<PortalScrapeResult> aggregated = new ArrayList<>(grouped.size());
    for (List<PortalScrapeResult> group : grouped.values()) {
      aggregated.add(buildAggregatedResult(group, relevantItems));
    }
    return aggregated;
  }

  private String resolveGroupKey(PortalScrapeResult pr) {
    if (pr.getPortalId() != null && !pr.getPortalId().isBlank()) {
      return pr.getPortalId().trim().toLowerCase(java.util.Locale.ROOT);
    }
    if (pr.getPortalName() != null) {
      return pr.getPortalName().trim().toLowerCase(java.util.Locale.ROOT);
    }
    return "unknown";
  }

  private PortalScrapeResult buildAggregatedResult(
      List<PortalScrapeResult> group, List<IngatlanItem> relevantItems) {
    PortalScrapeResult first = group.get(0);
    String portalId = first.getPortalId();
    String portalName = first.getPortalName();
    List<IngatlanItem> portalRelevantItems =
        relevantItems == null
            ? List.of()
            : relevantItems.stream()
                .filter(item -> matchesPortal(item, portalId, portalName))
                .toList();

    GroupScrapeMetrics metrics = evaluateGroupMetrics(group);
    ScrapeStatusDetails statusDetails = resolveStatusDetails(portalRelevantItems, metrics, first);

    return PortalScrapeResult.builder()
        .portalId(portalId)
        .portalName(portalName)
        .status(statusDetails.status())
        .items(new ArrayList<>(portalRelevantItems))
        .fallbackUrl(metrics.fallbackUrl())
        .message(statusDetails.message())
        .executionTimeMs(metrics.totalExecutionMs())
        .httpStatusCode(statusDetails.httpStatusCode())
        .technicalDetails(statusDetails.technicalDetails())
        .build();
  }

  private GroupScrapeMetrics evaluateGroupMetrics(List<PortalScrapeResult> group) {
    GroupMetricsAccumulator acc = new GroupMetricsAccumulator();
    for (PortalScrapeResult pr : group) {
      acc.accumulate(pr);
    }
    return acc.toMetrics();
  }

  private ScrapeStatusDetails resolveStatusDetails(
      List<IngatlanItem> items, GroupScrapeMetrics metrics, PortalScrapeResult first) {
    if (!items.isEmpty()) {
      return new ScrapeStatusDetails(
          ingatlanok.model.PortalScrapeStatus.SUCCESS,
          "Sikeres letöltés (" + items.size() + " találat)",
          200,
          "Sikeres HTTP 200 válasz");
    }
    if (metrics.hasFailed()) {
      return resolveFailedDetails(metrics);
    }
    if (metrics.hasBotProtected()) {
      return resolveBotProtectedDetails(metrics);
    }
    if (metrics.hasSpaFallback()) {
      return resolveSpaFallbackDetails(metrics);
    }
    return resolveEmptyStatusDetails(metrics, first);
  }

  private ScrapeStatusDetails resolveFailedDetails(GroupScrapeMetrics metrics) {
    return new ScrapeStatusDetails(
        ingatlanok.model.PortalScrapeStatus.FAILED,
        metrics.errorMessage() != null ? metrics.errorMessage() : "Hiba a letöltés során",
        metrics.httpStatusCode() != null ? metrics.httpStatusCode() : 500,
        metrics.technicalDetails() != null ? metrics.technicalDetails() : metrics.errorMessage());
  }

  private ScrapeStatusDetails resolveBotProtectedDetails(GroupScrapeMetrics metrics) {
    return new ScrapeStatusDetails(
        ingatlanok.model.PortalScrapeStatus.BOT_PROTECTED,
        metrics.errorMessage() != null
            ? metrics.errorMessage()
            : "Cloudflare WAF bot-védelem aktív; közvetlen keresőlink biztosítva",
        metrics.httpStatusCode() != null ? metrics.httpStatusCode() : 403,
        metrics.technicalDetails() != null
            ? metrics.technicalDetails()
            : "HTTP 403 Forbidden / Cloudflare Turnstile JavaScript challenge detektálva");
  }

  private ScrapeStatusDetails resolveSpaFallbackDetails(GroupScrapeMetrics metrics) {
    return new ScrapeStatusDetails(
        ingatlanok.model.PortalScrapeStatus.SPA_FALLBACK,
        metrics.errorMessage() != null
            ? metrics.errorMessage()
            : "Kliensoldali SPA alkalmazás; közvetlen portállink biztosítva",
        metrics.httpStatusCode() != null ? metrics.httpStatusCode() : 200,
        metrics.technicalDetails() != null
            ? metrics.technicalDetails()
            : "Dinamikus kliensoldali JavaScript renderelés szükséges");
  }

  private static class GroupMetricsAccumulator {
    private long totalExecutionMs = 0L;
    private String fallbackUrl = null;
    private String errorMessage = null;
    private Integer httpStatusCode = null;
    private String technicalDetails = null;
    private boolean hasFailed = false;
    private boolean hasBotProtected = false;
    private boolean hasSpaFallback = false;
    private boolean hasSuccess = false;

    void accumulate(PortalScrapeResult pr) {
      totalExecutionMs += Math.max(0L, pr.getExecutionTimeMs());
      if (fallbackUrl == null && pr.getFallbackUrl() != null && !pr.getFallbackUrl().isBlank()) {
        fallbackUrl = pr.getFallbackUrl();
      }
      updateStatusFlags(pr);
    }

    private void updateStatusFlags(PortalScrapeResult pr) {
      ingatlanok.model.PortalScrapeStatus st = pr.getStatus();
      if (st == ingatlanok.model.PortalScrapeStatus.FAILED) {
        hasFailed = true;
        captureError(pr);
      } else if (st == ingatlanok.model.PortalScrapeStatus.BOT_PROTECTED) {
        hasBotProtected = true;
        if (!hasFailed) {
          captureError(pr);
        }
      } else if (st == ingatlanok.model.PortalScrapeStatus.SPA_FALLBACK) {
        hasSpaFallback = true;
        if (!hasFailed && !hasBotProtected) {
          captureError(pr);
        }
      } else if (st == ingatlanok.model.PortalScrapeStatus.SUCCESS) {
        hasSuccess = true;
      }
    }

    private void captureError(PortalScrapeResult pr) {
      if (errorMessage == null) {
        errorMessage = pr.getMessage();
        httpStatusCode = pr.getHttpStatusCode();
        technicalDetails = pr.getTechnicalDetails();
      }
    }

    GroupScrapeMetrics toMetrics() {
      return new GroupScrapeMetrics(
          totalExecutionMs,
          fallbackUrl,
          errorMessage,
          httpStatusCode,
          technicalDetails,
          hasFailed,
          hasBotProtected,
          hasSpaFallback,
          hasSuccess);
    }
  }

  private ScrapeStatusDetails resolveEmptyStatusDetails(
      GroupScrapeMetrics metrics, PortalScrapeResult first) {
    String msg =
        metrics.hasSuccess()
            ? "0 találat a megadott szűrőkre"
            : (first.getMessage() != null
                ? first.getMessage()
                : "Nincs találat a megadott feltételekkel");
    int code = first.getHttpStatusCode() != null ? first.getHttpStatusCode() : 200;
    String tech =
        first.getTechnicalDetails() != null
            ? first.getTechnicalDetails()
            : "A lekérdezés 0 találatot eredményezett a keresési kritériumok alapján";
    return new ScrapeStatusDetails(ingatlanok.model.PortalScrapeStatus.EMPTY, msg, code, tech);
  }

  private record GroupScrapeMetrics(
      long totalExecutionMs,
      String fallbackUrl,
      String errorMessage,
      Integer httpStatusCode,
      String technicalDetails,
      boolean hasFailed,
      boolean hasBotProtected,
      boolean hasSpaFallback,
      boolean hasSuccess) {}

  private record ScrapeStatusDetails(
      ingatlanok.model.PortalScrapeStatus status,
      String message,
      Integer httpStatusCode,
      String technicalDetails) {}

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
