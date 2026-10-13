package ingatlanok.presentation;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.scraper.PortalScrapeResult;
import ingatlanok.service.IngatlanSearchService;

import java.util.*;

public class DashboardPresentationService {

    private static final DashboardPresentationService INSTANCE = new DashboardPresentationService();

    private DashboardPresentationService() {
    }

    public static DashboardPresentationService getInstance() {
        return INSTANCE;
    }

    public DashboardResponseDto executeSearch(SearchCriteria criteria) {
        SearchResult searchResult = IngatlanSearchService.getInstance().executeSearch(criteria);
        MarketStatsDto marketStats = calculateMarketStats(searchResult.getItems());
        List<PortalStatusDto> portalStatuses = buildPortalStatuses(searchResult);
        searchResult.setPortalStatuses(portalStatuses);

        return new DashboardResponseDto(searchResult, marketStats, portalStatuses);
    }

    public MarketStatsDto calculateMarketStats(List<IngatlanItem> items) {
        MarketStatsDto stats = new MarketStatsDto();
        if (items == null || items.isEmpty()) {
            return stats;
        }

        List<Double> pricesMillion = new ArrayList<>();
        List<Long> pricesPerM2 = new ArrayList<>();
        Map<String, Integer> cityCounts = new LinkedHashMap<>();
        int newItemsCount = 0;

        for (IngatlanItem item : items) {
            if (item.isNew()) {
                newItemsCount++;
            }

            String city = item.getCity();
            if (city != null) {
                cityCounts.put(city, cityCounts.getOrDefault(city, 0) + 1);
            }

            if (item.getPriceInfo() != null && item.getPriceInfo().getAmountMillion() != null) {
                pricesMillion.add(item.getPriceInfo().getAmountMillion());
            }

            if (item.getDetailsInfo() != null && item.getDetailsInfo().getPricePerSquareMeter() != null) {
                pricesPerM2.add(item.getDetailsInfo().getPricePerSquareMeter());
            }
        }

        if (!pricesMillion.isEmpty()) {
            double sum = pricesMillion.stream().mapToDouble(Double::doubleValue).sum();
            double avg = sum / pricesMillion.size();
            stats.setAveragePriceMillion(Math.round(avg * 10.0) / 10.0);
            stats.setMinPriceMillion(pricesMillion.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
            stats.setMaxPriceMillion(pricesMillion.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
        }

        if (!pricesPerM2.isEmpty()) {
            long sumM2 = pricesPerM2.stream().mapToLong(Long::longValue).sum();
            stats.setAveragePricePerM2(sumM2 / pricesPerM2.size());
        }

        double newRate = Math.round(((double) newItemsCount / items.size() * 100.0) * 10.0) / 10.0;
        stats.setNewRatePercent(newRate);
        stats.setCityBreakdown(cityCounts);

        return stats;
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
