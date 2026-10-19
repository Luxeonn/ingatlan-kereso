package ingatlanok.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

/** Keresési eredmények aggregálásáért, rendezéséért és formázásáért felelős segédmotor. */
public class SearchResultAggregator {

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

    if (criteria != null && criteria.isAutoOpenBrowser() && !newItems.isEmpty()) {
      Set<String> urlsToOpen =
          newItems.stream()
              .map(IngatlanItem::getUrl)
              .collect(Collectors.toCollection(LinkedHashSet::new));
      BrowserUtils.openUrls(urlsToOpen);
    }

    uniqueAggregatedList.sort(
        (a, b) -> {
          Long priceA = (a.getPriceInfo() != null) ? a.getPriceInfo().getAmountHuf() : null;
          if (priceA == null && a.getPrice() != null) {
            priceA = PropertyParser.parsePrice(a.getPrice()).getAmountHuf();
          }
          Long priceB = (b.getPriceInfo() != null) ? b.getPriceInfo().getAmountHuf() : null;
          if (priceB == null && b.getPrice() != null) {
            priceB = PropertyParser.parsePrice(b.getPrice()).getAmountHuf();
          }

          if (priceA != null && priceB != null) {
            int cmp = Long.compare(priceA, priceB);
            if (cmp != 0) {
              return cmp;
            }
          } else if (priceA != null) {
            return -1;
          } else if (priceB != null) {
            return 1;
          }

          if (a.isNew() != b.isNew()) {
            return Boolean.compare(!a.isNew(), !b.isNew());
          }
          if (a.getDiscoveredAt() == null) {
            return 1;
          }
          if (b.getDiscoveredAt() == null) {
            return -1;
          }
          return b.getDiscoveredAt().compareTo(a.getDiscoveredAt());
        });

    SearchResult result = new SearchResult();
    result.setItems(uniqueAggregatedList);
    result.setNewCount(newItems.size());
    result.setTotalFound(uniqueAggregatedList.size());
    result.setExecutedAt(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
    result.setPortalCounts(portalCounts != null ? portalCounts : new HashMap<>());
    result.setCityCounts(cityCounts != null ? cityCounts : new HashMap<>());
    if (newCitiesInitialized != null) {
      result.getNewCitiesInitialized().addAll(newCitiesInitialized);
    }

    return result;
  }
}
