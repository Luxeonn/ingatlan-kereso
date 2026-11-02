package ingatlanok.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingObservation;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.StatusLogEntry;

public interface ListingStorage {

  void saveCityItems(String city, Collection<IngatlanItem> items);

  void saveAll(Collection<IngatlanItem> items);

  List<IngatlanItem> getItemsByCity(String city);

  List<IngatlanItem> getAll();

  boolean hasCityDatabase(String city);

  boolean isNewUrl(String city, String url);

  boolean containsUrl(String url);

  boolean markAsViewed(String url);

  boolean isViewed(String url);

  Set<String> getAllUrls();

  List<Map<String, Object>> getTrackedCitiesInfo();

  void clearCityHistory(String city);

  void clearHistory();

  int getTotalCount();

  default Map<String, Long> countByPortal() {
    return Map.of();
  }

  String getLastRunTime();

  default List<PriceHistoryEntry> getPriceHistory(String idOrUrl) {
    return List.of();
  }

  default List<StatusLogEntry> getStatusLog(String idOrUrl) {
    return List.of();
  }

  default List<ListingObservation> getListingObservations(String idOrUrl) {
    return List.of();
  }

  default List<IngatlanItem> getDiscountedProperties(String city) {
    return List.of();
  }

  default List<IngatlanItem> findByCriteria(PropertySearchCriteria filter) {
    return List.of();
  }

  default Map<String, IngatlanItem> findByUrls(Collection<String> urls) {
    return Map.of();
  }
}
