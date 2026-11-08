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
  private int filteredCount;
  private String executedAt;
  @Builder.Default
  private Map<String, Integer> portalCounts = new HashMap<>();
  @Builder.Default
  private Map<String, Integer> cityCounts = new LinkedHashMap<>();
  @Builder.Default
  private List<String> newCitiesInitialized = new ArrayList<>();
  @Builder.Default
  private List<PortalScrapeResult> portalResults = new ArrayList<>();
  private SearchExecutionStatus executionStatus;

  public SearchExecutionStatus getExecutionStatus() {
    if (executionStatus != null) {
      return executionStatus;
    }
    if (portalResults == null || portalResults.isEmpty()) {
      return SearchExecutionStatus.SUCCESS;
    }
    boolean hasSuccess = false;
    boolean hasFailure = false;
    for (PortalScrapeResult pr : portalResults) {
      if (pr == null) {
        continue;
      }
      if (pr.getStatus() == PortalScrapeStatus.FAILED
          || pr.getStatus() == PortalScrapeStatus.BOT_PROTECTED) {
        hasFailure = true;
      } else {
        hasSuccess = true;
      }
    }
    if (hasFailure && hasSuccess) {
      return SearchExecutionStatus.PARTIAL_SUCCESS;
    }
    if (hasFailure) {
      return SearchExecutionStatus.TOTAL_FAILURE;
    }
    return SearchExecutionStatus.SUCCESS;
  }
}
