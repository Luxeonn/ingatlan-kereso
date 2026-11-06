package ingatlanok.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;

/**
 * Synchronizes scraped listing items with persisted storage state (viewed flags, price deltas,
 * status history) and reconciles per-portal scrape counts and diagnostic status cards.
 */
public class SearchPersistenceSynchronizer {

  private final PortalEngineRegistry portalRegistry;
  private final ListingStorage listingStorage;

  public SearchPersistenceSynchronizer(
      PortalEngineRegistry portalRegistry, ListingStorage listingStorage) {
    this.portalRegistry = Objects.requireNonNull(portalRegistry);
    this.listingStorage = Objects.requireNonNull(listingStorage);
  }

  public void enrichWithPersistedState(List<IngatlanItem> items) {
    if (items == null || items.isEmpty()) {
      return;
    }
    List<String> urls =
        items.stream()
            .map(IngatlanItem::getUrl)
            .filter(u -> u != null && !u.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    if (urls.isEmpty()) {
      return;
    }
    Map<String, IngatlanItem> persistedByUrl = listingStorage.findByUrls(urls);
    if (persistedByUrl == null || persistedByUrl.isEmpty()) {
      return;
    }
    for (IngatlanItem item : items) {
      if (item != null && item.getUrl() != null) {
        applyPersistedFields(item, persistedByUrl.get(item.getUrl().trim()));
      }
    }
  }

  private void applyPersistedFields(IngatlanItem item, IngatlanItem persisted) {
    if (persisted == null) {
      return;
    }
    item.setViewed(persisted.isViewed());
    item.setViewedAt(persisted.getViewedAt());
    item.setHidden(persisted.isHidden());
    item.setHiddenAt(persisted.getHiddenAt());
    item.setPriceDelta(persisted.getPriceDelta());
    item.setPriceChangePercent(persisted.getPriceChangePercent());
    item.setLastPriceChangeAt(persisted.getLastPriceChangeAt());
    if (persisted.getStatus() != null && !persisted.getStatus().isBlank()) {
      item.setStatus(persisted.getStatus());
    }
    item.setNewConstruction(item.isNewConstruction() || persisted.isNewConstruction());
  }

  public void recalculateRelevantPortalCounts(
      Map<String, Integer> portalCounts, List<IngatlanItem> relevantItems) {
    portalCounts.replaceAll((k, v) -> 0);
    if (relevantItems == null || relevantItems.isEmpty()) {
      return;
    }
    for (IngatlanItem item : relevantItems) {
      if (item == null) {
        continue;
      }
      String key = resolvePortalCountKey(item);
      portalCounts.merge(key, 1, Integer::sum);
    }
  }

  public String resolvePortalCountKey(IngatlanItem item) {
    String portalId = item.getSource() != null ? item.getSource().getPortalId() : null;
    if (portalId != null && !portalId.isBlank()) {
      Optional<PortalEngine> engineOpt = portalRegistry.getEngine(portalId);
      if (engineOpt.isPresent()) {
        return engineOpt.get().getName();
      }
    }
    String rawPortal =
        item.getPortal() != null && !item.getPortal().isBlank()
            ? item.getPortal().trim()
            : (item.getSource() != null && item.getSource().getPortalName() != null
                ? item.getSource().getPortalName().trim()
                : "Ismeretlen");
    for (PortalEngine engine : portalRegistry.getAllEngines()) {
      if (engine.getName().equalsIgnoreCase(rawPortal)
          || engine.getId().equalsIgnoreCase(rawPortal)) {
        return engine.getName();
      }
    }
    return rawPortal;
  }

  public List<PortalScrapeResult> aggregatePortalResultsByPortal(
      List<PortalScrapeResult> allScrapeResults, List<IngatlanItem> relevantItems) {
    if (allScrapeResults == null || allScrapeResults.isEmpty()) {
      return List.of();
    }
    Map<String, List<PortalScrapeResult>> grouped = new LinkedHashMap<>();
    for (PortalScrapeResult pr : allScrapeResults) {
      if (pr != null) {
        String key = resolveGroupKey(pr);
        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(pr);
      }
    }

    List<PortalScrapeResult> aggregated = new ArrayList<>(grouped.size());
    for (List<PortalScrapeResult> group : grouped.values()) {
      aggregated.add(buildAggregatedResult(group, relevantItems));
    }
    return aggregated;
  }

  private boolean matchesPortal(IngatlanItem item, String portalId, String portalName) {
    if (item == null) {
      return false;
    }
    return matchesPortalId(item, portalId)
        || matchesPortalName(item, portalName)
        || matchesPortalIdToName(item, portalId)
        || matchesResolvedKey(item, portalName);
  }

  private boolean matchesPortalId(IngatlanItem item, String portalId) {
    String itemPortalId = item.getSource() != null ? item.getSource().getPortalId() : null;
    return portalId != null && itemPortalId != null && portalId.equalsIgnoreCase(itemPortalId);
  }

  private boolean matchesPortalName(IngatlanItem item, String portalName) {
    String itemPortalName =
        item.getSource() != null && item.getSource().getPortalName() != null
            ? item.getSource().getPortalName()
            : item.getPortal();
    return portalName != null
        && itemPortalName != null
        && portalName.equalsIgnoreCase(itemPortalName);
  }

  private boolean matchesPortalIdToName(IngatlanItem item, String portalId) {
    String itemPortalName =
        item.getSource() != null && item.getSource().getPortalName() != null
            ? item.getSource().getPortalName()
            : item.getPortal();
    return portalId != null && itemPortalName != null && portalId.equalsIgnoreCase(itemPortalName);
  }

  private boolean matchesResolvedKey(IngatlanItem item, String portalName) {
    String resolvedKey = resolvePortalCountKey(item);
    return portalName != null && portalName.equalsIgnoreCase(resolvedKey);
  }

  private String resolveGroupKey(PortalScrapeResult pr) {
    if (pr.getPortalId() != null && !pr.getPortalId().isBlank()) {
      return pr.getPortalId().trim().toLowerCase(Locale.ROOT);
    }
    if (pr.getPortalName() != null) {
      return pr.getPortalName().trim().toLowerCase(Locale.ROOT);
    }
    return "unknown";
  }

  private PortalScrapeResult buildAggregatedResult(
      List<PortalScrapeResult> group, List<IngatlanItem> relevantItems) {
    PortalScrapeResult first = group.get(0);
    String portalId = first.getPortalId();
    String portalName = first.getPortalName();
    List<IngatlanItem> portalRelevantItems =
        relevantItems == null
            ? List.of()
            : relevantItems.stream()
                .filter(item -> matchesPortal(item, portalId, portalName))
                .toList();

    GroupScrapeMetrics metrics = evaluateGroupMetrics(group);
    ScrapeStatusDetails statusDetails = resolveStatusDetails(portalRelevantItems, metrics, first);

    return PortalScrapeResult.builder()
        .portalId(portalId)
        .portalName(portalName)
        .status(statusDetails.status())
        .items(new ArrayList<>(portalRelevantItems))
        .fallbackUrl(metrics.fallbackUrl())
        .message(statusDetails.message())
        .executionTimeMs(metrics.totalExecutionMs())
        .httpStatusCode(statusDetails.httpStatusCode())
        .technicalDetails(statusDetails.technicalDetails())
        .build();
  }

  private GroupScrapeMetrics evaluateGroupMetrics(List<PortalScrapeResult> group) {
    GroupMetricsAccumulator acc = new GroupMetricsAccumulator();
    for (PortalScrapeResult pr : group) {
      acc.accumulate(pr);
    }
    return acc.toMetrics();
  }

  private ScrapeStatusDetails resolveStatusDetails(
      List<IngatlanItem> items, GroupScrapeMetrics metrics, PortalScrapeResult first) {
    if (!items.isEmpty()) {
      return new ScrapeStatusDetails(
          PortalScrapeStatus.SUCCESS,
          "Sikeres letöltés (" + items.size() + " találat)",
          200,
          "Sikeres HTTP 200 válasz");
    }
    if (metrics.hasFailed()) {
      return resolveFailedDetails(metrics);
    }
    if (metrics.hasBotProtected()) {
      return resolveBotProtectedDetails(metrics);
    }
    if (metrics.hasSpaFallback()) {
      return resolveSpaFallbackDetails(metrics);
    }
    return resolveEmptyStatusDetails(metrics, first);
  }

  private ScrapeStatusDetails resolveFailedDetails(GroupScrapeMetrics metrics) {
    return new ScrapeStatusDetails(
        PortalScrapeStatus.FAILED,
        metrics.errorMessage() != null ? metrics.errorMessage() : "Hiba a letöltés során",
        metrics.httpStatusCode() != null ? metrics.httpStatusCode() : 500,
        metrics.technicalDetails() != null ? metrics.technicalDetails() : metrics.errorMessage());
  }

  private ScrapeStatusDetails resolveBotProtectedDetails(GroupScrapeMetrics metrics) {
    return new ScrapeStatusDetails(
        PortalScrapeStatus.BOT_PROTECTED,
        metrics.errorMessage() != null
            ? metrics.errorMessage()
            : "Cloudflare WAF bot-védelem aktív; közvetlen keresőlink biztosítva",
        metrics.httpStatusCode() != null ? metrics.httpStatusCode() : 403,
        metrics.technicalDetails() != null
            ? metrics.technicalDetails()
            : "HTTP 403 Forbidden / Cloudflare Turnstile JavaScript challenge detektálva");
  }

  private ScrapeStatusDetails resolveSpaFallbackDetails(GroupScrapeMetrics metrics) {
    return new ScrapeStatusDetails(
        PortalScrapeStatus.SPA_FALLBACK,
        metrics.errorMessage() != null
            ? metrics.errorMessage()
            : "Kliensoldali SPA alkalmazás; közvetlen portállink biztosítva",
        metrics.httpStatusCode() != null ? metrics.httpStatusCode() : 200,
        metrics.technicalDetails() != null
            ? metrics.technicalDetails()
            : "Dinamikus kliensoldali JavaScript renderelés szükséges");
  }

  private ScrapeStatusDetails resolveEmptyStatusDetails(
      GroupScrapeMetrics metrics, PortalScrapeResult first) {
    String msg =
        metrics.hasSuccess()
            ? "0 találat a megadott szűrőkre"
            : (first.getMessage() != null
                ? first.getMessage()
                : "Nincs találat a megadott feltételekkel");
    int code = first.getHttpStatusCode() != null ? first.getHttpStatusCode() : 200;
    String tech =
        first.getTechnicalDetails() != null
            ? first.getTechnicalDetails()
            : "A lekérdezés 0 találatot eredményezett a keresési kritériumok alapján";
    return new ScrapeStatusDetails(PortalScrapeStatus.EMPTY, msg, code, tech);
  }

  private static class GroupMetricsAccumulator {
    private long totalExecutionMs = 0L;
    private String fallbackUrl = null;
    private String errorMessage = null;
    private Integer httpStatusCode = null;
    private String technicalDetails = null;
    private boolean hasFailed = false;
    private boolean hasBotProtected = false;
    private boolean hasSpaFallback = false;
    private boolean hasSuccess = false;

    void accumulate(PortalScrapeResult pr) {
      totalExecutionMs += Math.max(0L, pr.getExecutionTimeMs());
      if (fallbackUrl == null && pr.getFallbackUrl() != null && !pr.getFallbackUrl().isBlank()) {
        fallbackUrl = pr.getFallbackUrl();
      }
      updateStatusFlags(pr);
    }

    private void updateStatusFlags(PortalScrapeResult pr) {
      PortalScrapeStatus st = pr.getStatus();
      if (st == PortalScrapeStatus.FAILED) {
        hasFailed = true;
        captureError(pr);
      } else if (st == PortalScrapeStatus.BOT_PROTECTED) {
        hasBotProtected = true;
        if (!hasFailed) {
          captureError(pr);
        }
      } else if (st == PortalScrapeStatus.SPA_FALLBACK) {
        hasSpaFallback = true;
        if (!hasFailed && !hasBotProtected) {
          captureError(pr);
        }
      } else if (st == PortalScrapeStatus.SUCCESS) {
        hasSuccess = true;
      }
    }

    private void captureError(PortalScrapeResult pr) {
      if (errorMessage == null) {
        errorMessage = pr.getMessage();
        httpStatusCode = pr.getHttpStatusCode();
        technicalDetails = pr.getTechnicalDetails();
      }
    }

    GroupScrapeMetrics toMetrics() {
      return new GroupScrapeMetrics(
          totalExecutionMs,
          fallbackUrl,
          errorMessage,
          httpStatusCode,
          technicalDetails,
          hasFailed,
          hasBotProtected,
          hasSpaFallback,
          hasSuccess);
    }
  }

  private record GroupScrapeMetrics(
      long totalExecutionMs,
      String fallbackUrl,
      String errorMessage,
      Integer httpStatusCode,
      String technicalDetails,
      boolean hasFailed,
      boolean hasBotProtected,
      boolean hasSpaFallback,
      boolean hasSuccess) {}

  private record ScrapeStatusDetails(
      PortalScrapeStatus status,
      String message,
      Integer httpStatusCode,
      String technicalDetails) {}
}
