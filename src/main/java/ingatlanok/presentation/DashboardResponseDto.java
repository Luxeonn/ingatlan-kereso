package ingatlanok.presentation;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchResult;

import java.io.Serializable;
import java.util.*;

/**
 * Prezentációs aggregált válaszmodell a Web Dashboard és API kliensek számára.
 * Megőrzi a teljes kompatibilitást a korábbi SearchResult mezőivel, miközben
 * kiegészül piaci statisztikákkal és portál státuszkártyákkal.
 */
public class DashboardResponseDto implements Serializable {

    private List<IngatlanItem> items = new ArrayList<>();
    private int newCount;
    private int totalFound;
    private String executedAt;
    private Map<String, Integer> portalCounts = new LinkedHashMap<>();
    private Map<String, Integer> cityCounts = new LinkedHashMap<>();
    private List<String> newCitiesInitialized = new ArrayList<>();

    // Új prezentációs és statisztikai mezők
    private MarketStatsDto marketStats;
    private List<PortalStatusDto> portalStatuses = new ArrayList<>();

    public DashboardResponseDto() {
    }

    public DashboardResponseDto(SearchResult searchResult, MarketStatsDto marketStats, List<PortalStatusDto> portalStatuses) {
        if (searchResult != null) {
            this.items = searchResult.getItems();
            this.newCount = searchResult.getNewCount();
            this.totalFound = searchResult.getTotalFound();
            this.executedAt = searchResult.getExecutedAt();
            this.portalCounts = searchResult.getPortalCounts();
            this.cityCounts = searchResult.getCityCounts();
            this.newCitiesInitialized = searchResult.getNewCitiesInitialized();
        }
        this.marketStats = marketStats;
        this.portalStatuses = portalStatuses != null ? portalStatuses : new ArrayList<>();
    }

    public List<IngatlanItem> getItems() {
        return items;
    }

    public void setItems(List<IngatlanItem> items) {
        this.items = items;
    }

    public int getNewCount() {
        return newCount;
    }

    public void setNewCount(int newCount) {
        this.newCount = newCount;
    }

    public int getTotalFound() {
        return totalFound;
    }

    public void setTotalFound(int totalFound) {
        this.totalFound = totalFound;
    }

    public String getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(String executedAt) {
        this.executedAt = executedAt;
    }

    public Map<String, Integer> getPortalCounts() {
        return portalCounts;
    }

    public void setPortalCounts(Map<String, Integer> portalCounts) {
        this.portalCounts = portalCounts;
    }

    public Map<String, Integer> getCityCounts() {
        return cityCounts;
    }

    public void setCityCounts(Map<String, Integer> cityCounts) {
        this.cityCounts = cityCounts;
    }

    public List<String> getNewCitiesInitialized() {
        return newCitiesInitialized;
    }

    public void setNewCitiesInitialized(List<String> newCitiesInitialized) {
        this.newCitiesInitialized = newCitiesInitialized;
    }

    public MarketStatsDto getMarketStats() {
        return marketStats;
    }

    public void setMarketStats(MarketStatsDto marketStats) {
        this.marketStats = marketStats;
    }

    public List<PortalStatusDto> getPortalStatuses() {
        return portalStatuses;
    }

    public void setPortalStatuses(List<PortalStatusDto> portalStatuses) {
        this.portalStatuses = portalStatuses;
    }
}
