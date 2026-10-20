package ingatlanok.presentation;

import java.util.ArrayList;
import java.util.List;

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

    return new DashboardResponseDto(searchResult, marketStats, portalStatuses);
  }

  public MarketStatsDto calculateMarketStats(List<IngatlanItem> items) {
    return analyticsEngine.calculateMarketStats(items);
  }

  private List<PortalStatusDto> buildPortalStatuses(SearchResult searchResult) {
    List<PortalStatusDto> list = new ArrayList<>();
    if (searchResult == null || searchResult.getPortalResults() == null) {
      return list;
    }

    for (PortalScrapeResult pr : searchResult.getPortalResults()) {
      PortalStatusDto dto =
          new PortalStatusDto(
              pr.getPortalId(),
              pr.getPortalName(),
              pr.getStatus() != null ? pr.getStatus().name() : "UNKNOWN",
              pr.getItems() != null ? pr.getItems().size() : 0,
              pr.getExecutionTimeMs(),
              pr.getFallbackUrl(),
              pr.getMessage(),
              pr.getHttpStatusCode(),
              pr.getTechnicalDetails());
      list.add(dto);
    }

    return list;
  }
}
