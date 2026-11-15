package ingatlanok.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingStatus;
import ingatlanok.model.PortalId;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.ListingStorage;

/**
 * Merges active persisted listings back into search results when live portal scrapes are
 * rate-limited, bot-protected, or truncated mid-pagination.
 */
final class PersistedFallbackMerger {

  private static final AppLogger LOGGER = AppLogger.getLogger(PersistedFallbackMerger.class);

  private PersistedFallbackMerger() {}

  static void mergeIfIncomplete(
      ListingStorage listingStorage,
      PropertyFilterEngine filterEngine,
      String currentCity,
      SearchCriteria cityCriteria,
      List<PortalScrapeResult> results,
      List<IngatlanItem> targetItems) {
    Set<PortalId> incompletePortals = collectIncompletePortals(results);
    if (incompletePortals.isEmpty()) {
      return;
    }

    List<IngatlanItem> storedCityItems = listingStorage.getItemsByCity(currentCity);
    if (storedCityItems == null || storedCityItems.isEmpty()) {
      return;
    }

    Set<String> existingUrls = new HashSet<>();
    for (IngatlanItem item : targetItems) {
      if (item != null && item.getUrl() != null) {
        existingUrls.add(item.getUrl().trim());
      }
    }

    List<IngatlanItem> fallbackCandidates = new ArrayList<>();
    for (IngatlanItem stored : storedCityItems) {
      if (isEligible(stored, existingUrls, incompletePortals, cityCriteria)) {
        stored.setNew(false);
        fallbackCandidates.add(stored);
      }
    }
    if (fallbackCandidates.isEmpty()) {
      return;
    }

    PropertyFilterEngine.FilterResult filtered =
        filterEngine.filterRelevance(fallbackCandidates, cityCriteria, currentCity);
    if (!filtered.matchingItems().isEmpty()) {
      targetItems.addAll(filtered.matchingItems());
      LOGGER.info(
          "  ├─ [{}] DB fallback: +{} aktív tárolt hirdetés bevonva részleges/blokkolt portáloktól {}",
          currentCity,
          filtered.matchingItems().size(),
          incompletePortals);
    }
  }

  private static Set<PortalId> collectIncompletePortals(List<PortalScrapeResult> results) {
    Set<PortalId> incomplete = new HashSet<>();
    if (results == null) {
      return incomplete;
    }
    for (PortalScrapeResult pr : results) {
      if (pr == null) {
        continue;
      }
      if (pr.getStatus() == PortalScrapeStatus.FAILED
          || pr.getStatus() == PortalScrapeStatus.BOT_PROTECTED
          || isPartialSuccess(pr)) {
        PortalId pid = PortalId.fromString(pr.getPortalId());
        if (pid == PortalId.UNKNOWN && pr.getPortalName() != null) {
          pid = PortalId.fromString(pr.getPortalName());
        }
        if (pid != PortalId.UNKNOWN) {
          incomplete.add(pid);
        }
      }
    }
    return incomplete;
  }

  private static boolean isPartialSuccess(PortalScrapeResult pr) {
    if (pr.getStatus() != PortalScrapeStatus.SUCCESS) {
      return false;
    }
    String msg = pr.getMessage() != null ? pr.getMessage().toLowerCase(Locale.ROOT) : "";
    String details = pr.getTechnicalDetails() != null ? pr.getTechnicalDetails() : "";
    return msg.contains("részleges") || details.contains("INTERMEDIATE_PAGE_");
  }

  private static boolean isEligible(
      IngatlanItem stored,
      Set<String> existingUrls,
      Set<PortalId> incompletePortals,
      SearchCriteria criteria) {
    if (stored == null || stored.getUrl() == null || stored.getUrl().isBlank()) {
      return false;
    }
    if (existingUrls.contains(stored.getUrl().trim()) || stored.isHidden()) {
      return false;
    }
    if (!isActiveStatus(stored.getStatus())) {
      return false;
    }
    if (!incompletePortals.contains(resolveItemPortal(stored))) {
      return false;
    }
    return matchesTargetType(stored, criteria);
  }

  private static boolean isActiveStatus(ListingStatus status) {
    return status != ListingStatus.INACTIVE
        && status != ListingStatus.REMOVED
        && status != ListingStatus.ARCHIVED;
  }

  private static boolean matchesTargetType(IngatlanItem stored, SearchCriteria criteria) {
    PropertyType targetType = criteria != null ? criteria.getPropertyType() : null;
    return targetType == null
        || stored.getPropertyType() == null
        || stored.getPropertyType() == targetType;
  }

  private static PortalId resolveItemPortal(IngatlanItem item) {
    if (item.getSource() != null && item.getSource().getPortalId() != null) {
      PortalId byId = PortalId.fromString(item.getSource().getPortalId());
      if (byId != PortalId.UNKNOWN) {
        return byId;
      }
    }
    if (item.getPortal() != null) {
      return PortalId.fromString(item.getPortal());
    }
    return PortalId.UNKNOWN;
  }
}
