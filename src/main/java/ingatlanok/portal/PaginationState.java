package ingatlanok.portal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ingatlanok.model.raw.RawPropertyListing;

/**
 * Per-invocation mutable state for {@link AbstractPaginatedExtractionStrategy} so concurrent
 * virtual-thread searches remain completely isolated.
 */
public final class PaginationState {

  private final List<RawPropertyListing> allListings = new ArrayList<>();
  private final Set<String> processedUrls = new HashSet<>();
  private final Set<String> processedNativeIds = new HashSet<>();
  private final Set<String> visitedPageUrls = new HashSet<>();
  private final Map<String, String> attributes = new HashMap<>();
  private int pagesProcessed;

  public boolean markVisitedPageUrl(String pageUrl) {
    return pageUrl != null && !pageUrl.isBlank() && visitedPageUrls.add(pageUrl);
  }

  public void incrementPagesProcessed() {
    pagesProcessed++;
  }

  public int getPagesProcessed() {
    return pagesProcessed;
  }

  public int addNewListings(List<RawPropertyListing> candidates) {
    if (candidates == null || candidates.isEmpty()) {
      return 0;
    }
    int added = 0;
    for (RawPropertyListing raw : candidates) {
      if (raw != null
          && PortalSupport.markProcessedListing(
              processedUrls, processedNativeIds, raw.url(), raw.nativeId())) {
        allListings.add(raw);
        added++;
      }
    }
    return added;
  }

  public boolean markProcessedListing(RawPropertyListing raw) {
    return raw != null
        && PortalSupport.markProcessedListing(
            processedUrls, processedNativeIds, raw.url(), raw.nativeId());
  }

  public List<RawPropertyListing> getAllListings() {
    return allListings;
  }

  public boolean isEmpty() {
    return allListings.isEmpty();
  }

  public void putAttribute(String key, String value) {
    if (key != null) {
      attributes.put(key, value);
    }
  }

  public String getAttribute(String key) {
    return key != null ? attributes.get(key) : null;
  }
}
