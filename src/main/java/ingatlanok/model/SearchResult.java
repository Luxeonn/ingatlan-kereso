package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ingatlanok.presentation.PortalStatusDto;
import ingatlanok.model.PortalScrapeResult;

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

  public SearchResult() {}

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

  public List<PortalScrapeResult> getPortalResults() {
    return portalResults;
  }

  public void setPortalResults(List<PortalScrapeResult> portalResults) {
    this.portalResults = portalResults;
  }

  public List<PortalStatusDto> getPortalStatuses() {
    return portalStatuses;
  }

  public void setPortalStatuses(List<PortalStatusDto> portalStatuses) {
    this.portalStatuses = portalStatuses != null ? portalStatuses : new ArrayList<>();
  }
}
