package ingatlanok.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchPreset;

public class InMemoryListingStorage implements ListingStorage, ConfigStorage {

  private final Map<String, List<IngatlanItem>> cityItems = new ConcurrentHashMap<>();
  private final Map<String, IngatlanItem> urlIndex = new ConcurrentHashMap<>();
  private final Set<String> viewedUrls = ConcurrentHashMap.newKeySet();
  private final Map<Long, SearchPreset> presets = new ConcurrentHashMap<>();
  private final AtomicLong presetIdSequence = new AtomicLong(1);
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
        updateItemStateOnSave(item);
        urlIndex.put(item.getUrl(), item);
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
        updateItemStateOnSave(item);
        urlIndex.put(item.getUrl(), item);
        String city = item.getCity() != null ? item.getCity().toLowerCase(Locale.ROOT) : "default";
        cityItems.computeIfAbsent(city, k -> new CopyOnWriteArrayList<>()).add(item);
      }
    }
    this.lastRunTime = java.time.LocalDateTime.now().toString();
  }

  private void updateItemStateOnSave(IngatlanItem item) {
    String url = item.getUrl();
    IngatlanItem existing = urlIndex.get(url);
    String now = java.time.LocalDateTime.now().toString();
    updateViewedState(item, existing, url, now);
    updatePriceAndStatus(item, existing, now);
  }

  private void updateViewedState(IngatlanItem item, IngatlanItem existing, String url, String now) {
    boolean isViewed =
        viewedUrls.contains(url) || (existing != null && existing.isViewed()) || item.isViewed();
    if (!isViewed) {
      return;
    }
    viewedUrls.add(url);
    item.setViewed(true);
    if (item.getViewedAt() == null) {
      item.setViewedAt(
          existing != null && existing.getViewedAt() != null ? existing.getViewedAt() : now);
    }
  }

  private void updatePriceAndStatus(IngatlanItem item, IngatlanItem existing, String now) {
    Long newAmount = item.getPriceInfo() != null ? item.getPriceInfo().getAmountHuf() : null;
    if (existing == null) {
      applyNewPriceDefaults(item, newAmount);
      return;
    }
    Long prevAmount =
        existing.getPriceInfo() != null ? existing.getPriceInfo().getAmountHuf() : null;
    if (hasPriceChanged(prevAmount, newAmount)) {
      applyPriceChange(item, prevAmount, newAmount, now);
    } else {
      retainExistingPriceState(item, existing);
    }
  }

  private void applyNewPriceDefaults(IngatlanItem item, Long newAmount) {
    if (newAmount != null && newAmount > 0 && item.getPriceDelta() == null) {
      item.setPriceDelta(0L);
      item.setPriceChangePercent(0.0);
    }
  }

  private boolean hasPriceChanged(Long prevAmount, Long newAmount) {
    return prevAmount != null
        && prevAmount > 0
        && newAmount != null
        && newAmount > 0
        && !prevAmount.equals(newAmount);
  }

  private void applyPriceChange(IngatlanItem item, Long prevAmount, Long newAmount, String now) {
    long delta = newAmount - prevAmount;
    double pct = Math.round(((double) delta / prevAmount * 100.0) * 100.0) / 100.0;
    item.setPriceDelta(delta);
    item.setPriceChangePercent(pct);
    item.setLastPriceChangeAt(now);
    item.setStatus(delta < 0 ? "PRICE_DROP" : "PRICE_INCREASE");
  }

  private void retainExistingPriceState(IngatlanItem item, IngatlanItem existing) {
    if (item.getPriceDelta() == null) {
      item.setPriceDelta(existing.getPriceDelta());
    }
    if (item.getPriceChangePercent() == null) {
      item.setPriceChangePercent(existing.getPriceChangePercent());
    }
    if (item.getLastPriceChangeAt() == null) {
      item.setLastPriceChangeAt(existing.getLastPriceChangeAt());
    }
    if ("ACTIVE".equals(item.getStatus()) && existing.getStatus() != null) {
      item.setStatus(existing.getStatus());
    }
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

  @Override
  public List<SearchPreset> getPresets() {
    List<SearchPreset> list = new ArrayList<>(presets.values());
    list.sort(
        (a, b) -> {
          String t1 = a.updatedAt() != null ? a.updatedAt() : "";
          String t2 = b.updatedAt() != null ? b.updatedAt() : "";
          return t2.compareTo(t1);
        });
    return list;
  }

  @Override
  public Optional<SearchPreset> getPresetById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(presets.get(id));
  }

  @Override
  public SearchPreset savePreset(SearchPreset preset) {
    if (preset == null) {
      throw new IllegalArgumentException("Preset cannot be null");
    }
    long id = presetIdSequence.getAndIncrement();
    String now = java.time.Instant.now().toString();
    SearchPreset saved =
        new SearchPreset(
            id,
            preset.name() != null ? preset.name().trim() : "Névtelen sablon",
            preset.criteria() != null ? preset.criteria() : new SearchCriteria(),
            now,
            now,
            preset.lastUsedAt());
    presets.put(id, saved);
    return saved;
  }

  @Override
  public Optional<SearchPreset> updatePreset(Long id, SearchPreset preset) {
    if (id == null || preset == null) {
      return Optional.empty();
    }
    SearchPreset existing = presets.get(id);
    if (existing == null) {
      return Optional.empty();
    }
    String now = java.time.Instant.now().toString();
    String name =
        preset.name() != null && !preset.name().isBlank() ? preset.name().trim() : existing.name();
    SearchCriteria criteria = preset.criteria() != null ? preset.criteria() : existing.criteria();
    SearchPreset updated =
        new SearchPreset(
            id,
            name,
            criteria,
            existing.createdAt() != null ? existing.createdAt() : now,
            now,
            existing.lastUsedAt());
    presets.put(id, updated);
    return Optional.of(updated);
  }

  @Override
  public boolean deletePreset(Long id) {
    if (id == null) {
      return false;
    }
    return presets.remove(id) != null;
  }

  @Override
  public Optional<SearchPreset> markPresetAsUsed(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    SearchPreset existing = presets.get(id);
    if (existing == null) {
      return Optional.empty();
    }
    String now = java.time.Instant.now().toString();
    SearchPreset updated =
        new SearchPreset(
            id,
            existing.name(),
            existing.criteria(),
            existing.createdAt(),
            existing.updatedAt(),
            now);
    presets.put(id, updated);
    return Optional.of(updated);
  }

  @Override
  public Map<String, IngatlanItem> findByUrls(Collection<String> urls) {
    if (urls == null || urls.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, IngatlanItem> result = new HashMap<>();
    for (String url : urls) {
      if (url != null && !url.isBlank()) {
        String key = url.trim();
        IngatlanItem item = urlIndex.get(key);
        if (item != null) {
          if (viewedUrls.contains(key)) {
            item.setViewed(true);
          }
          result.put(key, item);
        }
      }
    }
    return result;
  }
}
