package ingatlanok.persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingObservation;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchPreset;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.persistence.entity.ConfigEntity;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.mapper.JpaEntityMapper;
import ingatlanok.persistence.repository.ConfigRepository;
import ingatlanok.persistence.repository.ListingObservationRepository;
import ingatlanok.persistence.repository.PriceHistoryRepository;
import ingatlanok.persistence.repository.PropertyRepository;
import ingatlanok.persistence.repository.StatusLogRepository;
import ingatlanok.persistence.service.PriceHistoryTracker;
import ingatlanok.persistence.service.PropertyPersistenceHelper;
import ingatlanok.persistence.service.SearchPresetPersistenceHelper;
import ingatlanok.persistence.service.StatusLogTracker;
import ingatlanok.persistence.specification.PropertySpecifications;

@Service
@Primary
@Transactional
public class JpaIngatlanStorage implements ListingStorage, ConfigStorage {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final String CONFIG_KEY_LAST_CRITERIA = "last_criteria";

  private final PropertyRepository propertyRepo;
  private final PriceHistoryRepository priceHistoryRepo;
  private final StatusLogRepository statusLogRepo;
  private final ConfigRepository configRepo;
  private final JpaEntityMapper mapper;
  private final ObjectMapper objectMapper;
  private final ListingObservationRepository listingObservationRepo;
  private final PropertyPersistenceHelper persistenceHelper;
  private final SearchPresetPersistenceHelper presetHelper;

  @Autowired
  public JpaIngatlanStorage(
      PropertyRepository propertyRepo,
      PriceHistoryRepository priceHistoryRepo,
      StatusLogRepository statusLogRepo,
      ConfigRepository configRepo,
      JpaEntityMapper mapper,
      ObjectMapper objectMapper,
      PriceHistoryTracker priceTracker,
      StatusLogTracker statusTracker,
      ListingObservationRepository listingObservationRepo,
      SearchPresetPersistenceHelper presetHelper) {
    this.propertyRepo = propertyRepo;
    this.priceHistoryRepo = priceHistoryRepo;
    this.statusLogRepo = statusLogRepo;
    this.configRepo = configRepo;
    this.mapper = mapper;
    this.objectMapper = objectMapper;
    this.listingObservationRepo = listingObservationRepo;
    this.presetHelper = presetHelper;
    this.persistenceHelper =
        new PropertyPersistenceHelper(
            propertyRepo,
            priceHistoryRepo,
            statusLogRepo,
            listingObservationRepo,
            mapper,
            priceTracker,
            statusTracker);
  }

  public JpaIngatlanStorage(
      PropertyRepository propertyRepo,
      PriceHistoryRepository priceHistoryRepo,
      StatusLogRepository statusLogRepo,
      ConfigRepository configRepo,
      JpaEntityMapper mapper,
      ObjectMapper objectMapper,
      PriceHistoryTracker priceTracker,
      StatusLogTracker statusTracker) {
    this(
        propertyRepo,
        priceHistoryRepo,
        statusLogRepo,
        configRepo,
        mapper,
        objectMapper,
        priceTracker,
        statusTracker,
        null,
        null);
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
            propertyRepo.findByNormalizedCityOrderByDiscoveredAtDesc(
                CityUtils.normalizeCityKey(city)));
  }

  @Override
  @Transactional(readOnly = true)
  public List<IngatlanItem> getAll() {
    return mapper.toItems(propertyRepo.findAllByOrderByDiscoveredAtDesc());
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
  @Transactional(readOnly = true)
  public Set<String> getAllUrls() {
    return new HashSet<>(propertyRepo.findAllUrls());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Map<String, Object>> getTrackedCitiesInfo() {
    return propertyRepo.countByCityGroup().stream()
        .map(
            r ->
                Map.<String, Object>of(
                    "name", r[0] != null ? (String) r[0] : "Ismeretlen",
                    "key", r[1] != null ? (String) r[1] : CityUtils.normalizeCityKey((String) r[0]),
                    "count", ((Number) r[2]).longValue()))
        .toList();
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
  }

  @Override
  @CacheEvict(cacheNames = {"portals", "cities"}, allEntries = true)
  public void clearHistory() {
    persistenceHelper.deleteAllHistory();
  }

  @Override
  @Transactional(readOnly = true)
  public int getTotalCount() {
    return (int) propertyRepo.count();
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Long> countByPortal() {
    Map<String, Long> map = new HashMap<>();
    for (Object[] r : propertyRepo.countByPortalGroup()) {
      if (r[0] != null && !((String) r[0]).isBlank()) {
        map.put((String) r[0], ((Number) r[1]).longValue());
      }
    }
    return map;
  }

  @Override
  @Transactional(readOnly = true)
  public String getLastRunTime() {
    return propertyRepo.findLatestDiscoveredAt();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PriceHistoryEntry> getPriceHistory(String idOrUrl) {
    return (idOrUrl == null || idOrUrl.isBlank())
        ? Collections.emptyList()
        : priceHistoryRepo.findByUrlOrPropertyId(idOrUrl.trim()).stream()
            .map(mapper::toPriceHistoryEntry)
            .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<StatusLogEntry> getStatusLog(String idOrUrl) {
    return (idOrUrl == null || idOrUrl.isBlank())
        ? Collections.emptyList()
        : statusLogRepo.findByUrlOrPropertyId(idOrUrl.trim()).stream()
            .map(mapper::toStatusLogEntry)
            .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ListingObservation> getListingObservations(String idOrUrl) {
    return (idOrUrl == null || idOrUrl.isBlank() || listingObservationRepo == null)
        ? Collections.emptyList()
        : listingObservationRepo.findByUrlOrPropertyIdOrCanonicalId(idOrUrl.trim()).stream()
            .map(mapper::toListingObservation)
            .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<IngatlanItem> getDiscountedProperties(String city) {
    var spec =
        PropertySpecifications.from(
            new PropertySearchCriteria(
                city == null || city.isBlank() ? null : List.of(city),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                true,
                "DISCOVERED_DESC",
                0,
                1000));
    return mapper.toItems(
        propertyRepo.findAll(spec, Sort.by(Sort.Direction.DESC, "discoveredAt")));
  }

  @Override
  @Transactional(readOnly = true)
  public List<IngatlanItem> findByCriteria(PropertySearchCriteria filter) {
    PropertySearchCriteria criteria =
        filter != null
            ? filter
            : new PropertySearchCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                "DISCOVERED_DESC",
                0,
                100);
    Sort sort = switch (criteria.sortBy() == null ? "" : criteria.sortBy().toUpperCase()) {
      case "PRICE_ASC" -> Sort.by(Sort.Direction.ASC, "priceMillions");
      case "PRICE_DESC" -> Sort.by(Sort.Direction.DESC, "priceMillions");
      case "AREA_ASC" -> Sort.by(Sort.Direction.ASC, "areaSqm");
      case "AREA_DESC" -> Sort.by(Sort.Direction.DESC, "areaSqm");
      case "ROOMS_ASC" -> Sort.by(Sort.Direction.ASC, "rooms");
      case "ROOMS_DESC" -> Sort.by(Sort.Direction.DESC, "rooms");
      default -> Sort.by(Sort.Direction.DESC, "discoveredAt");
    };
    int offset = Math.max(0, criteria.offset());
    int size = Math.max(1, Math.min(200, criteria.size()));
    return mapper.toItems(
        propertyRepo
            .findAll(PropertySpecifications.from(criteria), new OffsetPageRequest(offset, size, sort))
            .getContent());
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, IngatlanItem> findByUrls(Collection<String> urls) {
    if (urls == null || urls.isEmpty()) {
      return Collections.emptyMap();
    }
    List<String> cleanedUrls =
        urls.stream()
            .filter(u -> u != null && !u.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    if (cleanedUrls.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, IngatlanItem> result = new HashMap<>();
    for (PropertyEntity entity : propertyRepo.findByUrlIn(cleanedUrls)) {
      IngatlanItem item = mapper.toItem(entity);
      if (item != null && item.getUrl() != null) {
        result.put(item.getUrl(), item);
      }
    }
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public SearchCriteria getLastCriteria() {
    return configRepo
        .findById(CONFIG_KEY_LAST_CRITERIA)
        .map(ConfigEntity::getValue)
        .map(
            v -> {
              try {
                return objectMapper.readValue(v, SearchCriteria.class);
              } catch (Exception e) {
                return null;
              }
            })
        .orElseGet(SearchCriteria::new);
  }

  @Override
  public void saveLastCriteria(SearchCriteria criteria) {
    if (criteria != null) {
      configRepo.save(
          new ConfigEntity(CONFIG_KEY_LAST_CRITERIA, objectMapper.writeValueAsString(criteria)));
    }
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
