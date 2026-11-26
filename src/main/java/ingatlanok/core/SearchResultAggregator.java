package ingatlanok.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.ConfigStorage;

@Component
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

    List<IngatlanItem> activeItems =
        uniqueAggregatedList.stream().filter(item -> item != null && !item.isHidden()).toList();
    List<IngatlanItem> newItems =
        activeItems.stream().filter(IngatlanItem::isNew).collect(Collectors.toList());

    if (configStorage != null) {
      configStorage.saveLastCriteria(criteria);
    }

    uniqueAggregatedList.sort(ITEM_COMPARATOR);

    return SearchResult.builder()
        .items(uniqueAggregatedList)
        .newCount(newItems.size())
        .totalFound(activeItems.size())
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
    if (item.getPriceInfo() != null && item.getPriceInfo().amountHuf() != null) {
      return item.getPriceInfo().amountHuf();
    }
    if (item.getPrice() != null) {
      return PropertyParser.parsePrice(item.getPrice()).amountHuf();
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
}
