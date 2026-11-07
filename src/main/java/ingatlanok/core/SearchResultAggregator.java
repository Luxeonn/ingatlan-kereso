package ingatlanok.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ingatlanok.common.BrowserUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.ConfigStorage;

public class SearchResultAggregator {

  private static final Comparator<IngatlanItem> ITEM_COMPARATOR =
      (a, b) -> {
        int priceCmp = comparePrices(a, b);
        if (priceCmp != 0) {
          return priceCmp;
        }
        if (a.isNew() != b.isNew()) {
          return Boolean.compare(!a.isNew(), !b.isNew());
        }
        int dateCmp = compareDiscoveredDates(a.getDiscoveredAt(), b.getDiscoveredAt());
        if (dateCmp != 0) {
          return dateCmp;
        }
        return compareUrls(a.getUrl(), b.getUrl());
      };

  public SearchResult aggregate(
      List<IngatlanItem> uniqueAggregatedList,
      SearchCriteria criteria,
      ConfigStorage configStorage,
      Map<String, Integer> portalCounts,
      Map<String, Integer> cityCounts,
      List<String> newCitiesInitialized) {

    List<IngatlanItem> newItems =
        uniqueAggregatedList.stream().filter(IngatlanItem::isNew).collect(Collectors.toList());

    if (configStorage != null) {
      configStorage.saveLastCriteria(criteria);
    }

    handleAutoOpenBrowser(criteria, newItems);
    uniqueAggregatedList.sort(ITEM_COMPARATOR);

    return SearchResult.builder()
        .items(uniqueAggregatedList)
        .newCount(newItems.size())
        .totalFound(uniqueAggregatedList.size())
        .executedAt(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()))
        .portalCounts(portalCounts != null ? portalCounts : new HashMap<>())
        .cityCounts(cityCounts != null ? cityCounts : new HashMap<>())
        .newCitiesInitialized(
            newCitiesInitialized != null
                ? new ArrayList<>(newCitiesInitialized)
                : new ArrayList<>())
        .build();
  }

  private static int comparePrices(IngatlanItem a, IngatlanItem b) {
    Long priceA = extractPrice(a);
    Long priceB = extractPrice(b);
    if (priceA != null && priceB != null) {
      return Long.compare(priceA, priceB);
    }
    if (priceA != null) {
      return -1;
    }
    if (priceB != null) {
      return 1;
    }
    return 0;
  }

  private static Long extractPrice(IngatlanItem item) {
    if (item.getPriceInfo() != null && item.getPriceInfo().getAmountHuf() != null) {
      return item.getPriceInfo().getAmountHuf();
    }
    if (item.getPrice() != null) {
      return PropertyParser.parsePrice(item.getPrice()).getAmountHuf();
    }
    return null;
  }

  private static int compareDiscoveredDates(String dateA, String dateB) {
    if (dateA == null && dateB == null) {
      return 0;
    }
    if (dateA == null) {
      return 1;
    }
    if (dateB == null) {
      return -1;
    }
    return dateB.compareTo(dateA);
  }

  private static int compareUrls(String urlA, String urlB) {
    if (urlA == null && urlB == null) {
      return 0;
    }
    if (urlA == null) {
      return 1;
    }
    if (urlB == null) {
      return -1;
    }
    return urlA.compareTo(urlB);
  }

  private void handleAutoOpenBrowser(SearchCriteria criteria, List<IngatlanItem> newItems) {
    if (criteria != null && criteria.isAutoOpenBrowser() && !newItems.isEmpty()) {
      Set<String> urlsToOpen =
          newItems.stream()
              .map(IngatlanItem::getUrl)
              .collect(Collectors.toCollection(LinkedHashSet::new));
      BrowserUtils.openUrls(urlsToOpen);
    }
  }
}
