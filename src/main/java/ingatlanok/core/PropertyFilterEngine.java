package ingatlanok.core;

import java.util.ArrayList;
import java.util.List;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;

public class PropertyFilterEngine {

  public record FilterResult(
      List<IngatlanItem> matchingItems,
      int totalInputCount,
      int filteredByPriceCount,
      int filteredByAreaCount,
      int filteredByViewedCount,
      int filteredByCityCount,
      int filteredByDiscountCount,
      int filteredByNewConstructionCount) {
    public FilterResult(
        List<IngatlanItem> matchingItems,
        int totalInputCount,
        int filteredByPriceCount,
        int filteredByAreaCount,
        int filteredByViewedCount,
        int filteredByCityCount,
        int filteredByDiscountCount) {
      this(
          matchingItems,
          totalInputCount,
          filteredByPriceCount,
          filteredByAreaCount,
          filteredByViewedCount,
          filteredByCityCount,
          filteredByDiscountCount,
          0);
    }

    public FilterResult(
        List<IngatlanItem> matchingItems,
        int totalInputCount,
        int filteredByPriceCount,
        int filteredByAreaCount,
        int filteredByViewedCount,
        int filteredByCityCount) {
      this(
          matchingItems,
          totalInputCount,
          filteredByPriceCount,
          filteredByAreaCount,
          filteredByViewedCount,
          filteredByCityCount,
          0,
          0);
    }
  }

  public FilterResult filter(List<IngatlanItem> items, SearchCriteria criteria, String targetCity) {
    FilterResult relevanceResult = filterRelevance(items, criteria, targetCity);
    FilterResult viewResult = filterView(relevanceResult.matchingItems(), criteria);
    return new FilterResult(
        viewResult.matchingItems(),
        relevanceResult.totalInputCount(),
        relevanceResult.filteredByPriceCount(),
        relevanceResult.filteredByAreaCount(),
        viewResult.filteredByViewedCount(),
        relevanceResult.filteredByCityCount(),
        viewResult.filteredByDiscountCount(),
        viewResult.filteredByNewConstructionCount());
  }

  public FilterResult filterRelevance(
      List<IngatlanItem> items, SearchCriteria criteria, String targetCity) {
    List<IngatlanItem> matching = new ArrayList<>();
    if (items == null || items.isEmpty()) {
      return new FilterResult(matching, 0, 0, 0, 0, 0, 0, 0);
    }

    int filteredPrice = 0;
    int filteredArea = 0;
    int filteredCity = 0;

    for (IngatlanItem item : items) {
      if (item == null) {
        continue;
      }
      populateMissingCity(item, targetCity);
      PriceInfo priceInfo = resolvePriceInfo(item);
      if (!matchesPrice(priceInfo, criteria)) {
        filteredPrice++;
        continue;
      }
      Double area = resolveArea(item, priceInfo);
      if (!matchesArea(area, criteria)) {
        filteredArea++;
        continue;
      }
      if (targetCity != null && !SearchCriteria.matchesCity(item, targetCity)) {
        filteredCity++;
        continue;
      }
      matching.add(item);
    }

    return new FilterResult(
        matching, items.size(), filteredPrice, filteredArea, 0, filteredCity, 0, 0);
  }

  private void populateMissingCity(IngatlanItem item, String targetCity) {
    if (item.getCity() == null || item.getCity().isBlank()) {
      item.setCity(targetCity);
    }
    if (item.getLocation() == null || item.getLocation().isBlank()) {
      item.setLocation(targetCity);
    }
  }

  private PriceInfo resolvePriceInfo(IngatlanItem item) {
    PriceInfo priceInfo = item.getPriceInfo();
    if (priceInfo == null && item.getPrice() != null && !item.getPrice().isBlank()) {
      priceInfo = PropertyParser.parsePrice(item.getPrice());
      item.setPriceInfo(priceInfo);
    }
    return priceInfo;
  }

  private Double resolveArea(IngatlanItem item, PriceInfo priceInfo) {
    Double area =
        (item.getDetailsInfo() != null) ? item.getDetailsInfo().getAreaSquareMeters() : null;
    if (area == null && item.getDetails() != null && !item.getDetails().isBlank()) {
      PropertyDetails parsed = PropertyParser.parseDetails(item.getDetails(), priceInfo);
      item.setDetailsInfo(parsed);
      area = parsed.getAreaSquareMeters();
    }
    return area;
  }

  private boolean matchesPrice(PriceInfo priceInfo, SearchCriteria criteria) {
    if (criteria == null) {
      return true;
    }
    return PropertyParser.isPriceInRange(
        priceInfo, criteria.getMinPriceMillion(), criteria.getMaxPriceMillion());
  }

  private boolean matchesArea(Double area, SearchCriteria criteria) {
    if (criteria == null) {
      return true;
    }
    return PropertyParser.isAreaInRange(area, criteria.getMinArea(), criteria.getMaxArea());
  }

  public FilterResult filterView(List<IngatlanItem> items, SearchCriteria criteria) {
    List<IngatlanItem> matching = new ArrayList<>();
    if (items == null || items.isEmpty()) {
      return new FilterResult(matching, 0, 0, 0, 0, 0, 0, 0);
    }

    int filteredViewed = 0;
    int filteredDiscount = 0;
    int filteredNewConstruction = 0;

    for (IngatlanItem item : items) {
      if (item == null) {
        continue;
      }
      if (item.isHidden()) {
        continue;
      }
      if (isExcludedViewed(item, criteria)) {
        filteredViewed++;
        continue;
      }
      if (isExcludedDiscount(item, criteria)) {
        filteredDiscount++;
        continue;
      }
      if (isExcludedNewConstruction(item, criteria)) {
        filteredNewConstruction++;
        continue;
      }
      matching.add(item);
    }

    return new FilterResult(
        matching,
        items.size(),
        0,
        0,
        filteredViewed,
        0,
        filteredDiscount,
        filteredNewConstruction);
  }

  private boolean isExcludedViewed(IngatlanItem item, SearchCriteria criteria) {
    return criteria != null && criteria.isExcludeViewed() && item.isViewed();
  }

  private boolean isExcludedDiscount(IngatlanItem item, SearchCriteria criteria) {
    return criteria != null && criteria.isOnlyDiscounted() && !item.isDiscounted();
  }

  private boolean isExcludedNewConstruction(IngatlanItem item, SearchCriteria criteria) {
    return criteria != null && criteria.isOnlyNewConstruction() && !item.isNewConstruction();
  }
}
