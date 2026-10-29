package ingatlanok.presentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchResult;

@Getter
@Setter
@NoArgsConstructor
public class DashboardResponseDto implements Serializable {

  private List<IngatlanItem> items = new ArrayList<>();
  private int newCount;
  private int totalFound;
  private String executedAt;
  private Map<String, Integer> portalCounts = new LinkedHashMap<>();
  private Map<String, Integer> cityCounts = new LinkedHashMap<>();
  private List<String> newCitiesInitialized = new ArrayList<>();

  private MarketStatsDto marketStats;
  private List<PortalStatusDto> portalStatuses = new ArrayList<>();

  public DashboardResponseDto(
      SearchResult searchResult, MarketStatsDto marketStats, List<PortalStatusDto> portalStatuses) {
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
}
