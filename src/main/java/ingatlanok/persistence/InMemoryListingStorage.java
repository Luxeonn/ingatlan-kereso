package ingatlanok.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

/** Könnyűsúlyú, szálbiztos memóriabeli tároló a tesztekhez és standalone futtatáshoz. */
public class InMemoryListingStorage implements ListingStorage, ConfigStorage {

  private final Map<String, List<IngatlanItem>> cityItems = new ConcurrentHashMap<>();
  private final Map<String, IngatlanItem> urlIndex = new ConcurrentHashMap<>();
  private final Set<String> viewedUrls = ConcurrentHashMap.newKeySet();
  private volatile SearchCriteria lastCriteria = new SearchCriteria();
  private volatile String lastRunTime;

  @Override
  public synchronized void saveCityItems(String city, Collection<IngatlanItem> items) {
    if (city == null || items == null) {
      return;
    }
    String key = city.toLowerCase(Locale.ROOT);
    cityItems.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
    for (IngatlanItem item : items) {
      if (item != null && item.getUrl() != null) {
        urlIndex.put(item.getUrl(), item);
        if (item.isViewed()) {
          viewedUrls.add(item.getUrl());
        }
      }
    }
    List<IngatlanItem> current = cityItems.get(key);
    current.addAll(items);
    this.lastRunTime = java.time.LocalDateTime.now().toString();
  }

  @Override
  public synchronized void saveAll(Collection<IngatlanItem> items) {
    if (items == null) {
      return;
    }
    for (IngatlanItem item : items) {
      if (item != null && item.getUrl() != null) {
        urlIndex.put(item.getUrl(), item);
        if (item.isViewed()) {
          viewedUrls.add(item.getUrl());
        }
        String city = item.getCity() != null ? item.getCity().toLowerCase(Locale.ROOT) : "default";
        cityItems.computeIfAbsent(city, k -> new CopyOnWriteArrayList<>()).add(item);
      }
    }
    this.lastRunTime = java.time.LocalDateTime.now().toString();
  }

  @Override
  public List<IngatlanItem> getItemsByCity(String city) {
    if (city == null) {
      return Collections.emptyList();
    }
    List<IngatlanItem> list = cityItems.get(city.toLowerCase(Locale.ROOT));
    return list != null ? new ArrayList<>(list) : Collections.emptyList();
  }

  @Override
  public List<IngatlanItem> getAll() {
    return new ArrayList<>(urlIndex.values());
  }

  @Override
  public boolean hasCityDatabase(String city) {
    if (city == null) {
      return false;
    }
    List<IngatlanItem> list = cityItems.get(city.toLowerCase(Locale.ROOT));
    return list != null && !list.isEmpty();
  }

  @Override
  public boolean isNewUrl(String city, String url) {
    if (url == null) {
      return false;
    }
    if (!hasCityDatabase(city)) {
      return false;
    }
    return !urlIndex.containsKey(url);
  }

  @Override
  public boolean containsUrl(String url) {
    return url != null && urlIndex.containsKey(url);
  }

  @Override
  public boolean markAsViewed(String url) {
    if (url == null || url.isBlank() || !urlIndex.containsKey(url)) {
      return false;
    }
    viewedUrls.add(url);
    IngatlanItem item = urlIndex.get(url);
    if (item != null) {
      item.setViewed(true);
    }
    return true;
  }

  @Override
  public boolean isViewed(String url) {
    if (url == null || url.isBlank()) {
      return false;
    }
    return viewedUrls.contains(url);
  }

  @Override
  public Set<String> getAllUrls() {
    return new HashSet<>(urlIndex.keySet());
  }

  @Override
  public List<Map<String, Object>> getTrackedCitiesInfo() {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map.Entry<String, List<IngatlanItem>> entry : cityItems.entrySet()) {
      Map<String, Object> info = new HashMap<>();
      info.put("key", entry.getKey());
      info.put("name", entry.getKey());
      info.put("count", entry.getValue().size());
      result.add(info);
    }
    return result;
  }

  @Override
  public synchronized void clearCityHistory(String city) {
    if (city == null) {
      return;
    }
    List<IngatlanItem> removed = cityItems.remove(city.toLowerCase(Locale.ROOT));
    if (removed != null) {
      for (IngatlanItem item : removed) {
        if (item.getUrl() != null) {
          urlIndex.remove(item.getUrl());
          viewedUrls.remove(item.getUrl());
        }
      }
    }
  }

  @Override
  public synchronized void clearHistory() {
    cityItems.clear();
    urlIndex.clear();
    viewedUrls.clear();
  }

  @Override
  public int getTotalCount() {
    return urlIndex.size();
  }

  @Override
  public String getLastRunTime() {
    return lastRunTime;
  }

  @Override
  public SearchCriteria getLastCriteria() {
    return lastCriteria;
  }

  @Override
  public void saveLastCriteria(SearchCriteria criteria) {
    this.lastCriteria = criteria;
  }
}
