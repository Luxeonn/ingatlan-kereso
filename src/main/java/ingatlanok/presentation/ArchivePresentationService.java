package ingatlanok.presentation;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import ingatlanok.core.DataQualityEngine;
import ingatlanok.core.DeduplicationEngine;
import ingatlanok.model.IngatlanItem;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.persistence.PropertySearchCriteria;

/**
 * Layer 1a presentation facade for persisted archive, hidden, and discounted property views,
 * ensuring consistent multi-signal deduplication (including manual {@code DedupOverride}s) and
 * quality evaluation across all archive endpoints.
 */
@Service
@RequiredArgsConstructor
public class ArchivePresentationService {

  private final ListingStorage listingStorage;
  private final DeduplicationEngine dedupEngine;
  private final DataQualityEngine qualityEngine;

  public List<IngatlanItem> getHiddenProperties() {
    return deduplicateAndEvaluate(listingStorage.getHiddenProperties());
  }

  public List<IngatlanItem> getDiscountedProperties(String city) {
    return deduplicateAndEvaluate(listingStorage.getDiscountedProperties(city));
  }

  public List<IngatlanItem> getHistory(PropertySearchCriteria criteria) {
    if (criteria == null) {
      return deduplicateAndEvaluate(listingStorage.findByCriteria(null));
    }
    int offset = Math.max(0, criteria.offset());
    int size = Math.max(1, Math.min(200, criteria.size()));
    // Fetch an expanded window from offset 0 before clustering so merged sources don't split across pages
    int fetchLimit = Math.min(200, Math.max(200, offset + size));
    PropertySearchCriteria expanded =
        new PropertySearchCriteria(
            criteria.cities(),
            criteria.portals(),
            criteria.minPrice(),
            criteria.maxPrice(),
            criteria.minArea(),
            criteria.maxArea(),
            criteria.minRooms(),
            criteria.maxRooms(),
            criteria.excludeViewed(),
            criteria.onlyDiscounted(),
            criteria.onlyNewConstruction(),
            criteria.sortBy(),
            0,
            fetchLimit);
    List<IngatlanItem> deduplicated = deduplicateAndEvaluate(listingStorage.findByCriteria(expanded));
    if (offset >= deduplicated.size()) {
      return List.of();
    }
    int end = Math.min(deduplicated.size(), offset + size);
    return deduplicated.subList(offset, end);
  }

  private List<IngatlanItem> deduplicateAndEvaluate(List<IngatlanItem> rawItems) {
    if (rawItems == null || rawItems.isEmpty()) {
      return List.of();
    }
    List<IngatlanItem> deduplicated = dedupEngine.deduplicateMultiSignal(rawItems);
    qualityEngine.evaluateAll(deduplicated);
    return deduplicated;
  }
}
