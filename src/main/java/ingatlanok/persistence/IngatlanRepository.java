package ingatlanok.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

public class IngatlanRepository implements ListingStorage, ConfigStorage {

  private static final IngatlanRepository INSTANCE = new IngatlanRepository();

  private volatile ListingStorage listingStorage;
  private volatile ConfigStorage configStorage;

  private IngatlanRepository() {}

  public IngatlanRepository(ListingStorage listingStorage, ConfigStorage configStorage) {
    this.listingStorage = listingStorage;
    this.configStorage = configStorage;
  }

  public synchronized void setStorage(ListingStorage listingStorage, ConfigStorage configStorage) {
    if (listingStorage != null) {
      this.listingStorage = listingStorage;
    }
    if (configStorage != null) {
      this.configStorage = configStorage;
    }
  }

  public static void resetStorage() {
    synchronized (INSTANCE) {
      INSTANCE.listingStorage = null;
      INSTANCE.configStorage = null;
    }
  }

  private static ListingStorage createDefaultListingStorage() {
    return new InMemoryListingStorage();
  }

  private static ConfigStorage createDefaultConfigStorage() {
    return new InMemoryListingStorage();
  }

  public static IngatlanRepository getInstance() {
    return INSTANCE;
  }

  public synchronized ListingStorage getListingStorage() {
    if (listingStorage == null) {
      listingStorage = createDefaultListingStorage();
    }
    return listingStorage;
  }

  public synchronized ConfigStorage getConfigStorage() {
    if (configStorage == null) {
      configStorage = createDefaultConfigStorage();
    }
    return configStorage;
  }

  @Override
  public boolean hasCityDatabase(String city) {
    return getListingStorage().hasCityDatabase(city);
  }

  @Override
  public boolean isNewUrl(String city, String url) {
    return getListingStorage().isNewUrl(city, url);
  }

  @Override
  public boolean containsUrl(String url) {
    return getListingStorage().containsUrl(url);
  }

  @Override
  public void saveCityItems(String city, Collection<IngatlanItem> items) {
    getListingStorage().saveCityItems(city, items);
  }

  @Override
  public void saveAll(Collection<IngatlanItem> items) {
    getListingStorage().saveAll(items);
  }

  @Override
  public boolean markAsViewed(String url) {
    return getListingStorage().markAsViewed(url);
  }

  @Override
  public boolean isViewed(String url) {
    return getListingStorage().isViewed(url);
  }

  @Override
  public Set<String> getAllUrls() {
    return getListingStorage().getAllUrls();
  }

  @Override
  public List<IngatlanItem> getAll() {
    return getListingStorage().getAll();
  }

  @Override
  public List<IngatlanItem> getItemsByCity(String city) {
    return getListingStorage().getItemsByCity(city);
  }

  @Override
  public List<Map<String, Object>> getTrackedCitiesInfo() {
    return getListingStorage().getTrackedCitiesInfo();
  }

  @Override
  public void clearCityHistory(String city) {
    getListingStorage().clearCityHistory(city);
  }

  @Override
  public void clearHistory() {
    getListingStorage().clearHistory();
  }

  @Override
  public SearchCriteria getLastCriteria() {
    return getConfigStorage().getLastCriteria();
  }

  @Override
  public void saveLastCriteria(SearchCriteria criteria) {
    getConfigStorage().saveLastCriteria(criteria);
  }

  @Override
  public int getTotalCount() {
    return getListingStorage().getTotalCount();
  }

  @Override
  public Map<String, Long> countByPortal() {
    return getListingStorage().countByPortal();
  }

  @Override
  public String getLastRunTime() {
    return getListingStorage().getLastRunTime();
  }

  @Override
  public List<ingatlanok.model.PriceHistoryEntry> getPriceHistory(String idOrUrl) {
    return getListingStorage().getPriceHistory(idOrUrl);
  }

  @Override
  public List<ingatlanok.model.StatusLogEntry> getStatusLog(String idOrUrl) {
    return getListingStorage().getStatusLog(idOrUrl);
  }

  @Override
  public List<IngatlanItem> getDiscountedProperties(String city) {
    return getListingStorage().getDiscountedProperties(city);
  }

  @Override
  public List<ingatlanok.dto.ListingDto> findByCriteria(ingatlanok.dto.ListingFilterDto filter) {
    return getListingStorage().findByCriteria(filter);
  }
}
