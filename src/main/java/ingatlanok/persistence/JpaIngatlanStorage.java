package ingatlanok.persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingObservation;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchPreset;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.model.TrackedCityInfo;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.mapper.JpaEntityMapper;
import ingatlanok.persistence.repository.PropertyRepository;
import ingatlanok.persistence.service.PropertyPersistenceHelper;
import ingatlanok.persistence.service.SearchPresetPersistenceHelper;

@Service
@Primary
@Transactional
public class JpaIngatlanStorage implements ListingStorage, ConfigStorage {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final PropertyRepository propertyRepo;
  private final JpaEntityMapper mapper;
  private final PropertyPersistenceHelper persistenceHelper;
  private final SearchPresetPersistenceHelper presetHelper;
  private final JpaPropertyQueryHelper queryHelper;
  private final DedupOverrideStorage dedupOverrideStorage;

  @Autowired
  public JpaIngatlanStorage(
      PropertyRepository propertyRepo,
      JpaEntityMapper mapper,
      PropertyPersistenceHelper persistenceHelper,
      SearchPresetPersistenceHelper presetHelper,
      JpaPropertyQueryHelper queryHelper,
      @Autowired(required = false) DedupOverrideStorage dedupOverrideStorage) {
    this.propertyRepo = propertyRepo;
    this.mapper = mapper;
    this.persistenceHelper = persistenceHelper;
    this.presetHelper = presetHelper;
    this.queryHelper = queryHelper;
    this.dedupOverrideStorage = dedupOverrideStorage;
  }

  @Override
  @CacheEvict(cacheNames = {"portals", "cities"}, allEntries = true)
  public void saveCityItems(String city, Collection<IngatlanItem> items) {
    saveAll(items);
  }

  @Override
  @CacheEvict(cacheNames = {"portals", "cities"}, allEntries = true)
  public void saveAll(Collection<IngatlanItem> items) {
    if (items == null || items.isEmpty()) {
      return;
    }
    String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
    for (IngatlanItem item : items) {
      if (item != null && item.getUrl() != null && !item.getUrl().trim().isBlank()) {
        persistenceHelper.saveItem(item, now);
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<IngatlanItem> getItemsByCity(String city) {
    return (city == null || city.isBlank())
        ? Collections.emptyList()
        : mapper.toItems(
            propertyRepo.findByNormalizedCityAndHiddenFalseOrderByDiscoveredAtDesc(
                CityUtils.normalizeCityKey(city)));
  }

  @Override
  @Transactional(readOnly = true)
  public List<IngatlanItem> getAll() {
    return mapper.toItems(propertyRepo.findAllByHiddenFalseOrderByDiscoveredAtDesc());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasCityDatabase(String city) {
    return city != null
        && !city.isBlank()
        && propertyRepo.existsByNormalizedCity(CityUtils.normalizeCityKey(city));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isNewUrl(String city, String url) {
    if (url == null || url.isBlank()) {
      return false;
    }
    String t = url.trim();
    return (city != null && !city.isBlank())
        ? (hasCityDatabase(city)
            && !propertyRepo.existsByUrlAndNormalizedCity(t, CityUtils.normalizeCityKey(city)))
        : !propertyRepo.existsByUrl(t);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean containsUrl(String url) {
    return url != null && !url.isBlank() && propertyRepo.existsByUrl(url.trim());
  }

  @Override
  public boolean markAsViewed(String url) {
    return url != null
        && !url.isBlank()
        && propertyRepo.markAsViewed(url.trim(), LocalDateTime.now().format(DATE_TIME_FORMATTER))
            > 0;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isViewed(String url) {
    return url != null
        && !url.isBlank()
        && propertyRepo.findByUrl(url.trim()).map(PropertyEntity::isViewed).orElse(false);
  }

  @Override
  @Transactional
  public boolean hideProperty(String idOrUrl) {
    if (idOrUrl == null || idOrUrl.isBlank()) {
      return false;
    }
    String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
    return propertyRepo.markKeyAsHidden(idOrUrl.trim(), now) > 0;
  }

  @Override
  @Transactional
  public boolean restoreProperty(String idOrUrl) {
    if (idOrUrl == null || idOrUrl.isBlank()) {
      return false;
    }
    return propertyRepo.restoreKeyFromHidden(idOrUrl.trim()) > 0;
  }

  @Override
  @Transactional(readOnly = true)
  public List<IngatlanItem> getHiddenProperties() {
    return mapper.toItems(propertyRepo.findByHiddenTrueOrderByDiscoveredAtDesc());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isHidden(String url) {
    return url != null
        && !url.isBlank()
        && propertyRepo.findByUrl(url.trim()).map(PropertyEntity::isHidden).orElse(false);
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> getAllUrls() {
    return new HashSet<>(propertyRepo.findAllUrls());
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedCityInfo> getTrackedCitiesInfo() {
    return queryHelper.getTrackedCitiesInfo();
  }

  @Override
  @CacheEvict(cacheNames = {"portals", "cities"}, allEntries = true)
  public void clearCityHistory(String city) {
    if (city == null || city.isBlank()) {
      return;
    }
    String key = CityUtils.normalizeCityKey(city);
    List<String> urls =
        propertyRepo.findByNormalizedCityOrderByDiscoveredAtDesc(key).stream()
            .map(PropertyEntity::getUrl)
            .toList();
    propertyRepo.deleteByNormalizedCity(key);
    persistenceHelper.deleteCityRelatedHistory(urls);
    if (dedupOverrideStorage != null) {
      dedupOverrideStorage.clearCityOverrides(key);
    }
  }

  @Override
  @CacheEvict(cacheNames = {"portals", "cities"}, allEntries = true)
  public void clearHistory() {
    persistenceHelper.deleteAllHistory();
    if (dedupOverrideStorage != null) {
      dedupOverrideStorage.clearAllOverrides();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public int getTotalCount() {
    return (int) propertyRepo.count();
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Long> countByPortal() {
    return queryHelper.countByPortal();
  }

  @Override
  @Transactional(readOnly = true)
  public String getLastRunTime() {
    return propertyRepo.findLatestDiscoveredAt();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PriceHistoryEntry> getPriceHistory(String idOrUrl) {
    return queryHelper.getPriceHistory(idOrUrl);
  }

  @Override
  @Transactional(readOnly = true)
  public List<StatusLogEntry> getStatusLog(String idOrUrl) {
    return queryHelper.getStatusLog(idOrUrl);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ListingObservation> getListingObservations(String idOrUrl) {
    return queryHelper.getListingObservations(idOrUrl);
  }

  @Override
  @Transactional(readOnly = true)
  public List<IngatlanItem> getDiscountedProperties(String city) {
    return queryHelper.getDiscountedProperties(city);
  }

  @Override
  @Transactional(readOnly = true)
  public List<IngatlanItem> findByCriteria(PropertySearchCriteria filter) {
    return queryHelper.findByCriteria(filter);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, IngatlanItem> findByUrls(Collection<String> urls) {
    return queryHelper.findByUrls(urls);
  }

  @Override
  @Transactional(readOnly = true)
  public SearchCriteria getLastCriteria() {
    return queryHelper.getLastCriteria();
  }

  @Override
  public void saveLastCriteria(SearchCriteria criteria) {
    queryHelper.saveLastCriteria(criteria);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SearchPreset> getPresets() {
    return presetHelper != null ? presetHelper.getPresets() : List.of();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SearchPreset> getPresetById(Long id) {
    return presetHelper != null ? presetHelper.getPresetById(id) : Optional.empty();
  }

  @Override
  public SearchPreset savePreset(SearchPreset preset) {
    if (presetHelper == null) {
      throw new IllegalStateException("Preset helper not configured");
    }
    return presetHelper.savePreset(preset);
  }

  @Override
  public Optional<SearchPreset> updatePreset(Long id, SearchPreset preset) {
    return presetHelper != null ? presetHelper.updatePreset(id, preset) : Optional.empty();
  }

  @Override
  public boolean deletePreset(Long id) {
    return presetHelper != null && presetHelper.deletePreset(id);
  }

  @Override
  public Optional<SearchPreset> markPresetAsUsed(Long id) {
    return presetHelper != null ? presetHelper.markPresetAsUsed(id) : Optional.empty();
  }
}
