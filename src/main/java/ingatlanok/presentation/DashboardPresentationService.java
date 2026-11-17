package ingatlanok.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ingatlanok.core.IngatlanComHtmlImportService;
import ingatlanok.core.MarketAnalyticsEngine;
import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;

@Service
public class DashboardPresentationService {

  private final SearchExecutionEngine searchEngine;
  private final MarketAnalyticsEngine analyticsEngine;
  private final IngatlanComHtmlImportService htmlImportService;

  public DashboardPresentationService(
      SearchExecutionEngine searchEngine,
      MarketAnalyticsEngine analyticsEngine,
      @Autowired(required = false) IngatlanComHtmlImportService htmlImportService) {
    this.searchEngine = Objects.requireNonNull(searchEngine);
    this.analyticsEngine = Objects.requireNonNull(analyticsEngine);
    this.htmlImportService = htmlImportService;
  }

  public IngatlanComHtmlImportResponseDto importIngatlanComHtml(
      IngatlanComHtmlImportRequestDto request) {
    if (htmlImportService == null) {
      throw new IllegalStateException("HTML import szolgáltatás nem érhető el");
    }
    String html = request != null ? request.html() : null;
    String sourceUrl = request != null ? request.sourceUrl() : null;
    String city = request != null ? request.city() : null;
    var summary =
        htmlImportService.importHtml(
            html, sourceUrl, city, request != null ? request.propertyType() : null);
    return new IngatlanComHtmlImportResponseDto(
        summary.parsedCount(),
        summary.savedCount(),
        summary.newlyAddedCount(),
        summary.duplicateCount(),
        summary.city(),
        summary.items());
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
