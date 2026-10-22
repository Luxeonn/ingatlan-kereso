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
    recalculateRelevantPortalCounts(portalCounts, uniqueAggregatedList);
    List<PortalScrapeResult> aggregatedPortalResults =
        aggregatePortalResultsByPortal(allScrapeResults, uniqueAggregatedList);
    SearchResult result =
        resultAggregator.aggregate(
            uniqueAggregatedList,
            criteria,
            configStorage,
            portalCounts,
            cityCounts,
            newCitiesInitialized);
    result.getPortalResults().addAll(aggregatedPortalResults);

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
    String itemPortalId = item.getSource() != null ? item.getSource().getPortalId() : null;
    if (portalId != null && itemPortalId != null && portalId.equalsIgnoreCase(itemPortalId)) {
      return true;
    }
    String itemPortalName =
        item.getSource() != null && item.getSource().getPortalName() != null
            ? item.getSource().getPortalName()
            : item.getPortal();
    if (portalName != null
        && itemPortalName != null
        && portalName.equalsIgnoreCase(itemPortalName)) {
      return true;
    }
    if (portalId != null && itemPortalName != null && portalId.equalsIgnoreCase(itemPortalName)) {
      return true;
    }
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
      if (pr == null) {
        continue;
      }
      String key =
          pr.getPortalId() != null && !pr.getPortalId().isBlank()
              ? pr.getPortalId().trim().toLowerCase(java.util.Locale.ROOT)
              : (pr.getPortalName() != null
                  ? pr.getPortalName().trim().toLowerCase(java.util.Locale.ROOT)
                  : "unknown");
      grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(pr);
    }

    List<PortalScrapeResult> aggregated = new ArrayList<>(grouped.size());
    for (List<PortalScrapeResult> group : grouped.values()) {
      PortalScrapeResult first = group.get(0);
      String portalId = first.getPortalId();
      String portalName = first.getPortalName();
      List<IngatlanItem> portalRelevantItems =
          relevantItems == null
              ? List.of()
              : relevantItems.stream()
                  .filter(item -> matchesPortal(item, portalId, portalName))
                  .toList();

      long totalExecutionMs = 0L;
      String fallbackUrl = null;
      String errorMessage = null;
      Integer httpStatusCode = null;
      String technicalDetails = null;
      boolean hasFailed = false;
      boolean hasBotProtected = false;
      boolean hasSpaFallback = false;
      boolean hasSuccess = false;

      for (PortalScrapeResult pr : group) {
        totalExecutionMs += Math.max(0L, pr.getExecutionTimeMs());
        if (fallbackUrl == null && pr.getFallbackUrl() != null && !pr.getFallbackUrl().isBlank()) {
          fallbackUrl = pr.getFallbackUrl();
        }
        ingatlanok.model.PortalScrapeStatus st = pr.getStatus();
        if (st == ingatlanok.model.PortalScrapeStatus.FAILED) {
          hasFailed = true;
          if (errorMessage == null) {
            errorMessage = pr.getMessage();
            httpStatusCode = pr.getHttpStatusCode();
            technicalDetails = pr.getTechnicalDetails();
          }
        } else if (st == ingatlanok.model.PortalScrapeStatus.BOT_PROTECTED) {
          hasBotProtected = true;
          if (!hasFailed && errorMessage == null) {
            errorMessage = pr.getMessage();
            httpStatusCode = pr.getHttpStatusCode();
            technicalDetails = pr.getTechnicalDetails();
          }
        } else if (st == ingatlanok.model.PortalScrapeStatus.SPA_FALLBACK) {
          hasSpaFallback = true;
          if (!hasFailed && !hasBotProtected && errorMessage == null) {
            errorMessage = pr.getMessage();
            httpStatusCode = pr.getHttpStatusCode();
            technicalDetails = pr.getTechnicalDetails();
          }
        } else if (st == ingatlanok.model.PortalScrapeStatus.SUCCESS) {
          hasSuccess = true;
        }
      }

      ingatlanok.model.PortalScrapeStatus finalStatus;
      String finalMessage;
      Integer finalHttpStatus;
      String finalTechDetails;

      if (!portalRelevantItems.isEmpty()) {
        finalStatus = ingatlanok.model.PortalScrapeStatus.SUCCESS;
        finalMessage = "Sikeres letöltés (" + portalRelevantItems.size() + " találat)";
        finalHttpStatus = 200;
        finalTechDetails = "Sikeres HTTP 200 válasz";
      } else if (hasFailed) {
        finalStatus = ingatlanok.model.PortalScrapeStatus.FAILED;
        finalMessage = errorMessage != null ? errorMessage : "Hiba a letöltés során";
        finalHttpStatus = httpStatusCode != null ? httpStatusCode : 500;
        finalTechDetails = technicalDetails != null ? technicalDetails : finalMessage;
      } else if (hasBotProtected) {
        finalStatus = ingatlanok.model.PortalScrapeStatus.BOT_PROTECTED;
        finalMessage =
            errorMessage != null
                ? errorMessage
                : "Cloudflare WAF bot-védelem aktív; közvetlen keresőlink biztosítva";
        finalHttpStatus = httpStatusCode != null ? httpStatusCode : 403;
        finalTechDetails =
            technicalDetails != null
                ? technicalDetails
                : "HTTP 403 Forbidden / Cloudflare Turnstile JavaScript challenge detektálva";
      } else if (hasSpaFallback) {
        finalStatus = ingatlanok.model.PortalScrapeStatus.SPA_FALLBACK;
        finalMessage =
            errorMessage != null
                ? errorMessage
                : "Kliensoldali SPA alkalmazás; közvetlen portállink biztosítva";
        finalHttpStatus = httpStatusCode != null ? httpStatusCode : 200;
        finalTechDetails =
            technicalDetails != null
                ? technicalDetails
                : "Dinamikus kliensoldali JavaScript renderelés szükséges";
      } else {
        finalStatus = ingatlanok.model.PortalScrapeStatus.EMPTY;
        finalMessage =
            hasSuccess
                ? "0 találat a megadott szűrőkre"
                : (first.getMessage() != null
                    ? first.getMessage()
                    : "Nincs találat a megadott feltételekkel");
        finalHttpStatus = first.getHttpStatusCode() != null ? first.getHttpStatusCode() : 200;
        finalTechDetails =
            first.getTechnicalDetails() != null
                ? first.getTechnicalDetails()
                : "A lekérdezés 0 találatot eredményezett a keresési kritériumok alapján";
      }

      aggregated.add(
          new PortalScrapeResult(
              portalId,
              portalName,
              finalStatus,
              new ArrayList<>(portalRelevantItems),
              fallbackUrl,
              finalMessage,
              totalExecutionMs,
              finalHttpStatus,
              finalTechDetails));
    }
    return aggregated;
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
