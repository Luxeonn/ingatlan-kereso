package ingatlanok.presentation;

import ingatlanok.core.MarketAnalyticsEngine;
import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.scraper.PortalScrapeResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Prezentációs Facade a Core keresőmotor és analitika felé.
 */
public class DashboardPresentationService {

    private static final DashboardPresentationService INSTANCE = new DashboardPresentationService();

    private final SearchExecutionEngine searchEngine;
    private final MarketAnalyticsEngine analyticsEngine;

    private DashboardPresentationService() {
        this(SearchExecutionEngine.getInstance(), new MarketAnalyticsEngine());
    }

    public DashboardPresentationService(SearchExecutionEngine searchEngine, MarketAnalyticsEngine analyticsEngine) {
        this.searchEngine = searchEngine != null ? searchEngine : SearchExecutionEngine.getInstance();
        this.analyticsEngine = analyticsEngine != null ? analyticsEngine : new MarketAnalyticsEngine();
    }

    public static DashboardPresentationService getInstance() {
        return INSTANCE;
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
            PortalStatusDto dto = new PortalStatusDto(
                    pr.getPortalId(),
                    pr.getPortalName(),
                    pr.getStatus() != null ? pr.getStatus().name() : "UNKNOWN",
                    pr.getItems() != null ? pr.getItems().size() : 0,
                    pr.getExecutionTimeMs(),
                    pr.getFallbackUrl(),
                    pr.getMessage(),
                    pr.getHttpStatusCode(),
                    pr.getTechnicalDetails()
            );
            list.add(dto);
        }

        return list;
    }
}
