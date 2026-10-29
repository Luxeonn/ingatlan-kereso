package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import ingatlanok.presentation.PortalStatusDto;

@Getter
@Setter
@NoArgsConstructor
public class SearchResult implements Serializable {

  private List<IngatlanItem> items = new ArrayList<>();
  private int newCount;
  private int totalFound;
  private String executedAt;
  private Map<String, Integer> portalCounts = new HashMap<>();
  private Map<String, Integer> cityCounts = new LinkedHashMap<>();
  private List<String> newCitiesInitialized = new ArrayList<>();
  private List<PortalScrapeResult> portalResults = new ArrayList<>();
  private List<PortalStatusDto> portalStatuses = new ArrayList<>();

  public void setPortalStatuses(List<PortalStatusDto> portalStatuses) {
    this.portalStatuses = portalStatuses != null ? portalStatuses : new ArrayList<>();
  }
}
