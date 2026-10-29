package ingatlanok.presentation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ingatlanok.core.MarketAnalyticsEngine;
import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;

public class DashboardPresentationService {


  private final SearchExecutionEngine searchEngine;
  private final MarketAnalyticsEngine analyticsEngine;

  public DashboardPresentationService(
      SearchExecutionEngine searchEngine, MarketAnalyticsEngine analyticsEngine) {
    this.searchEngine = java.util.Objects.requireNonNull(searchEngine);
    this.analyticsEngine = java.util.Objects.requireNonNull(analyticsEngine);
  }

  public DashboardResponseDto executeSearch(SearchCriteria criteria) {
    SearchResult searchResult = searchEngine.executeSearch(criteria);
    MarketStatsDto marketStats = analyticsEngine.calculateMarketStats(searchResult.getItems());
    List<PortalStatusDto> portalStatuses = buildPortalStatuses(searchResult);
    searchResult.setPortalStatuses(portalStatuses);

    return PresentationMapper.INSTANCE.toDashboardResponseDto(
        searchResult, marketStats, portalStatuses);
  }

  public MarketStatsDto calculateMarketStats(List<IngatlanItem> items) {
    return analyticsEngine.calculateMarketStats(items);
  }

  private List<PortalStatusDto> buildPortalStatuses(SearchResult searchResult) {
    List<PortalStatusDto> list = new ArrayList<>();
    if (searchResult == null || searchResult.getPortalResults() == null) {
      return list;
    }

    Map<String, PortalStatusDto> byPortalId = new LinkedHashMap<>();
    for (PortalScrapeResult pr : searchResult.getPortalResults()) {
      if (pr == null) {
        continue;
      }
      String key =
          pr.getPortalId() != null && !pr.getPortalId().isBlank()
              ? pr.getPortalId().trim().toLowerCase(Locale.ROOT)
              : (pr.getPortalName() != null
                  ? pr.getPortalName().trim().toLowerCase(Locale.ROOT)
                  : "unknown");
      int itemCount = pr.getItems() != null ? pr.getItems().size() : 0;
      String statusName = pr.getStatus() != null ? pr.getStatus().name() : "UNKNOWN";

      PortalStatusDto existing = byPortalId.get(key);
      if (existing == null) {
        byPortalId.put(key, PresentationMapper.INSTANCE.toPortalStatusDto(pr));
      } else {
        int foundCount = existing.getFoundCount() + itemCount;
        String fallbackUrl = existing.getFallbackUrl();
        if (fallbackUrl == null && pr.getFallbackUrl() != null) {
          fallbackUrl = pr.getFallbackUrl();
        }
        String mergedStatus =
            "SUCCESS".equals(statusName) || foundCount > 0 ? "SUCCESS" : existing.getStatus();
        byPortalId.put(
            key,
            existing.toBuilder()
                .foundCount(foundCount)
                .latencyMs(existing.getLatencyMs() + Math.max(0L, pr.getExecutionTimeMs()))
                .fallbackUrl(fallbackUrl)
                .status(mergedStatus)
                .build());
      }
    }

    list.addAll(byPortalId.values());
    return list;
  }
}
