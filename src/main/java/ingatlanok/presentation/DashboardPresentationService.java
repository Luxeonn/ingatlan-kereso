package ingatlanok.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    this.searchEngine = Objects.requireNonNull(searchEngine);
    this.analyticsEngine = Objects.requireNonNull(analyticsEngine);
  }

  public DashboardResponseDto executeSearch(SearchCriteria criteria) {
    SearchResult searchResult = searchEngine.executeSearch(criteria);
    MarketStatsDto marketStats = calculateMarketStats(searchResult.getItems());
    List<PortalStatusDto> portalStatuses = buildPortalStatuses(searchResult);

    return PresentationMapper.INSTANCE.toDashboardResponseDto(
        searchResult, marketStats, portalStatuses);
  }

  public MarketStatsDto calculateMarketStats(List<IngatlanItem> items) {
    return PresentationMapper.INSTANCE.toMarketStatsDto(analyticsEngine.calculateMarketStats(items));
  }

  private List<PortalStatusDto> buildPortalStatuses(SearchResult searchResult) {
    if (searchResult == null || searchResult.getPortalResults() == null) {
      return new ArrayList<>();
    }

    List<PortalStatusDto> statuses = new ArrayList<>(searchResult.getPortalResults().size());
    for (PortalScrapeResult pr : searchResult.getPortalResults()) {
      if (pr != null) {
        statuses.add(PresentationMapper.INSTANCE.toPortalStatusDto(pr));
      }
    }
    return statuses;
  }
}
