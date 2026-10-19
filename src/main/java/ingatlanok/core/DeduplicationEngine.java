package ingatlanok.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ingatlanok.model.IngatlanItem;
import ingatlanok.persistence.ListingStorage;

public class DeduplicationEngine {

  private final ListingStorage listingStorage;

  public DeduplicationEngine(ListingStorage listingStorage) {
    this.listingStorage = listingStorage;
  }

  public List<IngatlanItem> deduplicateByUrl(Collection<IngatlanItem> items) {
    if (items == null || items.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String, IngatlanItem> unique = new LinkedHashMap<>();
    for (IngatlanItem item : items) {
      if (item != null && item.getUrl() != null) {
        unique.putIfAbsent(item.getUrl(), item);
      }
    }
    return new ArrayList<>(unique.values());
  }

  public void applyNewStatus(String city, Collection<IngatlanItem> items, boolean isFirstSearch) {
    if (items == null || items.isEmpty()) {
      return;
    }

    for (IngatlanItem item : items) {
      if (item == null || item.getUrl() == null) {
        continue;
      }

      if (isFirstSearch) {
        item.setNew(false);
      } else if (listingStorage != null) {
        boolean isNew = listingStorage.isNewUrl(city, item.getUrl());
        item.setNew(isNew);
      }
    }
  }
}
