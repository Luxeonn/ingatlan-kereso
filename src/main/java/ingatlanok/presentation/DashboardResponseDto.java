package ingatlanok.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchExecutionStatus;

/**
 * Top-level presentation aggregation response for search execution (#401 / JAVA-007).
 */
@Builder
public record DashboardResponseDto(
    List<IngatlanItem> items,
    int newCount,
    int totalFound,
    int filteredCount,
    String executedAt,
    SearchExecutionStatus executionStatus,
    Map<String, Integer> portalCounts,
    Map<String, Integer> cityCounts,
    List<String> newCitiesInitialized,
    MarketStatsDto marketStats,
    List<PortalStatusDto> portalStatuses) {

  public DashboardResponseDto {
    items = items != null ? Collections.unmodifiableList(new ArrayList<>(items)) : List.of();
    portalCounts =
        portalCounts != null
            ? Collections.unmodifiableMap(new LinkedHashMap<>(portalCounts))
            : Map.of();
    cityCounts =
        cityCounts != null
            ? Collections.unmodifiableMap(new LinkedHashMap<>(cityCounts))
            : Map.of();
    newCitiesInitialized =
        newCitiesInitialized != null
            ? Collections.unmodifiableList(new ArrayList<>(newCitiesInitialized))
            : List.of();
    portalStatuses =
        portalStatuses != null
            ? Collections.unmodifiableList(new ArrayList<>(portalStatuses))
            : List.of();
  }

  public DashboardResponseDto() {
    this(
        List.of(),
        0,
        0,
        0,
        null,
        null,
        Map.of(),
        Map.of(),
        List.of(),
        null,
        List.of());
  }
}
