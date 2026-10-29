package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import ingatlanok.presentation.PortalStatusDto;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class SearchResult implements Serializable {

  @Builder.Default
  private List<IngatlanItem> items = new ArrayList<>();
  private int newCount;
  private int totalFound;
  private String executedAt;
  @Builder.Default
  private Map<String, Integer> portalCounts = new HashMap<>();
  @Builder.Default
  private Map<String, Integer> cityCounts = new LinkedHashMap<>();
  @Builder.Default
  private List<String> newCitiesInitialized = new ArrayList<>();
  @Builder.Default
  private List<PortalScrapeResult> portalResults = new ArrayList<>();
  @Builder.Default
  private List<PortalStatusDto> portalStatuses = new ArrayList<>();

  public void setPortalStatuses(List<PortalStatusDto> portalStatuses) {
    this.portalStatuses = portalStatuses != null ? portalStatuses : new ArrayList<>();
  }
}
