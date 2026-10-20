package ingatlanok.persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.persistence.entity.ConfigEntity;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.mapper.JpaEntityMapper;
import ingatlanok.persistence.repository.ConfigRepository;
import ingatlanok.persistence.repository.PriceHistoryRepository;
import ingatlanok.persistence.repository.PropertyRepository;
import ingatlanok.persistence.repository.StatusLogRepository;
import ingatlanok.persistence.specification.PropertySpecifications;
import ingatlanok.persistence.service.PriceHistoryTracker;
import ingatlanok.persistence.service.PriceHistoryTracker.PriceDeltaCalculation;
import ingatlanok.persistence.service.StatusLogTracker;
import ingatlanok.persistence.service.StatusLogTracker.StatusDecision;

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
  private final PriceHistoryTracker priceTracker;
  private final StatusLogTracker statusTracker;

  public JpaIngatlanStorage(
      PropertyRepository propertyRepo,
      PriceHistoryRepository priceHistoryRepo,
      StatusLogRepository statusLogRepo,
      ConfigRepository configRepo,
      JpaEntityMapper mapper,
      ObjectMapper objectMapper,
      PriceHistoryTracker priceTracker,
      StatusLogTracker statusTracker) {
    this.propertyRepo = propertyRepo;
    this.priceHistoryRepo = priceHistoryRepo;
    this.statusLogRepo = statusLogRepo;
    this.configRepo = configRepo;
    this.mapper = mapper;
    this.objectMapper = objectMapper;
    this.priceTracker = priceTracker;
    this.statusTracker = statusTracker;
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
        saveItem(item, now);
      }
    }
  }

  private void saveItem(IngatlanItem item, String now) {
    mapper.normalizeItem(item);
    String url = item.getUrl().trim();
    PropertyEntity existing = propertyRepo.findByUrl(url).orElse(null);
    String propId =
        existing != null
            ? existing.getPropertyId()
            : mapper.generatePropertyId(item.getPortal(), item.getId(), url);
    PriceInfo pi = item.getPriceInfo();
    Long priceAmount = pi != null ? pi.getAmountHuf() : null;
    Long prevAmount = existing != null ? existing.getPriceAmount() : null;

    PriceDeltaCalculation calc =
        priceTracker.calculate(
            existing == null,
            prevAmount,
            priceAmount,
            existing != null ? existing.getPriceDelta() : null,
            existing != null ? existing.getPriceChangePercent() : null,
            existing != null ? existing.getLastPriceChangeAt() : null,
            item.getStatus(),
            item.getTitle(),
            now);

    if (priceAmount != null && priceAmount > 0) {
      Double m = pi.getAmountMillion();
      String raw =
          pi.getRawText() != null
              ? pi.getRawText()
              : (item.getPrice() != null ? item.getPrice() : "");
      Long perSqm =
          item.getDetailsInfo() != null ? item.getDetailsInfo().getPricePerSquareMeter() : null;
      if (calc.isNewPriceRecord()) {
        priceHistoryRepo.save(
            mapper.createPriceHistoryEntity(
                propId,
                url,
                item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now,
                raw,
                priceAmount,
                m,
                perSqm,
                null,
                0L,
                0.0));
      } else if (calc.priceChanged()) {
        priceHistoryRepo.save(
            mapper.createPriceHistoryEntity(
                propId,
                url,
                now,
                raw,
                priceAmount,
                m,
                perSqm,
                calc.previousPriceAmount(),
                calc.delta(),
                calc.changePercent()));
      }
    }

    StatusDecision sd =
        statusTracker.evaluate(
            existing == null,
            existing != null ? existing.getStatus() : null,
            calc.resolvedStatus(),
            existing == null && item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now);
    if (sd.shouldRecord()) {
      statusLogRepo.save(
          mapper.createStatusLogEntity(propId, url, sd.status(), sd.changedAt(), sd.note()));
    }
    propertyRepo.save(
        mapper.toEntity(
            item,
            existing,
            now,
            calc.delta(),
            calc.changePercent(),
            calc.lastPriceChangeAt(),
            calc.resolvedStatus()));
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
    if (!urls.isEmpty()) {
      priceHistoryRepo.deleteByUrlIn(urls);
      statusLogRepo.deleteByUrlIn(urls);
    }
  }

  @Override
  @CacheEvict(cacheNames = {"portals", "cities"}, allEntries = true)
  public void clearHistory() {
    propertyRepo.deleteAll();
    priceHistoryRepo.deleteAll();
    statusLogRepo.deleteAll();
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
  public List<IngatlanItem> getDiscountedProperties(String city) {
    var spec = PropertySpecifications.from(new PropertySearchCriteria(
        city == null || city.isBlank() ? null : List.of(city), null, null, null, null, null,
        null, null, false, true, "DISCOVERED_DESC", 0, 1000));
    return mapper.toItems(
        propertyRepo.findAll(spec, Sort.by(Sort.Direction.DESC, "discoveredAt")));
  }

  @Override
  @Transactional(readOnly = true)
  public List<IngatlanItem> findByCriteria(PropertySearchCriteria filter) {
    PropertySearchCriteria criteria = filter != null ? filter : new PropertySearchCriteria(
        null, null, null, null, null, null, null, null, false, false,
        "DISCOVERED_DESC", 0, 100);
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
    return mapper.toItems(propertyRepo.findAll(PropertySpecifications.from(criteria),
        new OffsetPageRequest(offset, size, sort)).getContent());
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
      configRepo.save(new ConfigEntity(CONFIG_KEY_LAST_CRITERIA,
          objectMapper.writeValueAsString(criteria)));
    }
  }
}
