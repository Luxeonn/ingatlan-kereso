package ingatlanok.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ingatlanok.common.AppLogger;
import ingatlanok.common.CityUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.persistence.entity.ConfigEntity;
import ingatlanok.persistence.entity.PriceHistoryEntity;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.entity.StatusLogEntity;
import ingatlanok.persistence.repository.ConfigRepository;
import ingatlanok.persistence.repository.PriceHistoryRepository;
import ingatlanok.persistence.repository.PropertyRepository;
import ingatlanok.persistence.repository.StatusLogRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spring Data JPA és beágyazott H2 adatbázis alapú ListingStorage és ConfigStorage megvalósítás.
 */
@Service
@Primary
@Transactional
public class JpaIngatlanStorage implements ListingStorage, ConfigStorage {

    private static final AppLogger LOGGER = AppLogger.getLogger(JpaIngatlanStorage.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CONFIG_KEY_LAST_CRITERIA = "last_criteria";

    private final PropertyRepository propertyRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final StatusLogRepository statusLogRepository;
    private final ConfigRepository configRepository;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JpaIngatlanStorage(PropertyRepository propertyRepository,
                              PriceHistoryRepository priceHistoryRepository,
                              StatusLogRepository statusLogRepository,
                              ConfigRepository configRepository) {
        this.propertyRepository = propertyRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.statusLogRepository = statusLogRepository;
        this.configRepository = configRepository;
    }

    @Override
    public void saveCityItems(String city, Collection<IngatlanItem> items) {
        saveItemsInternal(items);
    }

    @Override
    public void saveAll(Collection<IngatlanItem> items) {
        saveItemsInternal(items);
    }

    private void saveItemsInternal(Collection<IngatlanItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        for (IngatlanItem item : items) {
            if (item == null) continue;
            String url = item.getUrl() != null ? item.getUrl().trim() : "";
            if (url.isBlank()) continue;

            String cityName = item.getCity() != null && !item.getCity().isBlank() ? item.getCity().trim() : "Ismeretlen";
            String cityKey = CityUtils.normalizeCityKey(cityName);
            String portal = item.getPortal() != null ? item.getPortal().trim() : "Egyéb";
            String title = item.getTitle() != null ? item.getTitle() : "";
            String discoveredAt = item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now;

            PriceInfo priceInfo = item.getPriceInfo();
            if (priceInfo == null && item.getPrice() != null && !item.getPrice().isBlank()) {
                priceInfo = PropertyParser.parsePrice(item.getPrice());
                item.setPriceInfo(priceInfo);
            }
            String priceRaw = (priceInfo != null && priceInfo.getRawText() != null) ? priceInfo.getRawText() : (item.getPrice() != null ? item.getPrice() : "");
            Long priceAmount = (priceInfo != null) ? priceInfo.getAmountHuf() : null;
            Double priceMillions = (priceInfo != null) ? priceInfo.getAmountMillion() : null;

            PropertyDetails detailsInfo = item.getDetailsInfo();
            if (detailsInfo == null && item.getDetails() != null && !item.getDetails().isBlank()) {
                detailsInfo = PropertyParser.parseDetails(item.getDetails(), priceInfo);
                item.setDetailsInfo(detailsInfo);
            }
            Double areaSqm = (detailsInfo != null) ? detailsInfo.getAreaSquareMeters() : null;
            Double rooms = (detailsInfo != null) ? detailsInfo.getRoomCount() : null;
            Long pricePerSqm = (detailsInfo != null) ? detailsInfo.getPricePerSquareMeter() : null;
            String details = item.getDetails() != null ? item.getDetails() : "";

            Optional<PropertyEntity> optExisting = propertyRepository.findByUrl(url);
            boolean isNew = optExisting.isEmpty();

            PropertyEntity entity = optExisting.orElseGet(PropertyEntity::new);
            String propertyId;
            Long prevAmount = null;
            String prevStatus = null;
            boolean previouslyViewed = false;
            String previouslyViewedAt = null;

            if (!isNew) {
                propertyId = entity.getPropertyId();
                prevAmount = entity.getPriceAmount();
                prevStatus = entity.getStatus();
                previouslyViewed = entity.isViewed();
                previouslyViewedAt = entity.getViewedAt();
            } else {
                String rawId = item.getId() != null ? item.getId().trim() : "";
                propertyId = (!rawId.isBlank()) ? portal + "_" + rawId : portal + "_" + Integer.toUnsignedString(url.hashCode());
                entity.setUrl(url);
                entity.setPropertyId(propertyId);
                entity.setDiscoveredAt(discoveredAt);
            }

            boolean finalViewed = previouslyViewed || item.isViewed();
            String finalViewedAt = previouslyViewed ? previouslyViewedAt : (item.isViewed() ? (item.getViewedAt() != null ? item.getViewedAt() : now) : null);
            String targetStatus = (item.getStatus() != null && !item.getStatus().isBlank()) ? item.getStatus() : "ACTIVE";

            Long priceDelta = entity.getPriceDelta();
            Double priceChangePercent = entity.getPriceChangePercent();
            String lastPriceChangeAt = entity.getLastPriceChangeAt();

            if (priceAmount != null && priceAmount > 0) {
                if (isNew) {
                    PriceHistoryEntity ph = new PriceHistoryEntity();
                    ph.setPropertyId(propertyId);
                    ph.setUrl(url);
                    ph.setRecordedAt(discoveredAt);
                    ph.setPriceRaw(priceRaw);
                    ph.setPriceAmount(priceAmount);
                    ph.setPriceMillions(priceMillions);
                    ph.setPricePerSqm(pricePerSqm);
                    ph.setPreviousPriceAmount(null);
                    ph.setPriceDelta(0L);
                    ph.setChangePercent(0.0);
                    priceHistoryRepository.save(ph);
                } else if (prevAmount != null && prevAmount > 0 && !prevAmount.equals(priceAmount)) {
                    long delta = priceAmount - prevAmount;
                    double changePct = Math.round(((double) delta / prevAmount * 100.0) * 100.0) / 100.0;
                    priceDelta = delta;
                    priceChangePercent = changePct;
                    lastPriceChangeAt = now;

                    PriceHistoryEntity ph = new PriceHistoryEntity();
                    ph.setPropertyId(propertyId);
                    ph.setUrl(url);
                    ph.setRecordedAt(now);
                    ph.setPriceRaw(priceRaw);
                    ph.setPriceAmount(priceAmount);
                    ph.setPriceMillions(priceMillions);
                    ph.setPricePerSqm(pricePerSqm);
                    ph.setPreviousPriceAmount(prevAmount);
                    ph.setPriceDelta(delta);
                    ph.setChangePercent(changePct);
                    priceHistoryRepository.save(ph);

                    if (delta < 0) {
                        targetStatus = "PRICE_DROP";
                    } else if (delta > 0) {
                        targetStatus = "PRICE_INCREASE";
                    }
                    LOGGER.info("[Árváltozás] {} - Régi: {} Ft -> Új: {} Ft (Delta: {} Ft, {}%)",
                            title, prevAmount, priceAmount, delta, changePct);
                }
            }

            if (isNew) {
                StatusLogEntity sl = new StatusLogEntity();
                sl.setPropertyId(propertyId);
                sl.setUrl(url);
                sl.setStatus(targetStatus);
                sl.setChangedAt(discoveredAt);
                sl.setNote("Kezdeti állapot felfedezve");
                statusLogRepository.save(sl);
            } else if (prevStatus != null && !prevStatus.equalsIgnoreCase(targetStatus)) {
                StatusLogEntity sl = new StatusLogEntity();
                sl.setPropertyId(propertyId);
                sl.setUrl(url);
                sl.setStatus(targetStatus);
                sl.setChangedAt(now);
                sl.setNote("Státusz változás: " + prevStatus + " -> " + targetStatus);
                statusLogRepository.save(sl);
            }

            entity.setPortal(portal);
            entity.setCity(cityName);
            entity.setNormalizedCity(cityKey);
            entity.setLocation(cityName);
            entity.setTitle(title);
            entity.setPriceRaw(priceRaw);
            entity.setPriceAmount(priceAmount);
            entity.setPriceMillions(priceMillions);
            entity.setPricePerSqm(pricePerSqm);
            entity.setDetails(details);
            entity.setAreaSqm(areaSqm);
            entity.setRooms(rooms);
            entity.setImageUrl(item.getImageUrl());
            entity.setNew(item.isNew());
            entity.setViewed(finalViewed);
            entity.setViewedAt(finalViewedAt);
            entity.setStatus(targetStatus);
            entity.setPriceDelta(priceDelta);
            entity.setPriceChangePercent(priceChangePercent);
            entity.setLastPriceChangeAt(lastPriceChangeAt);
            entity.setUpdatedAt(now);

            propertyRepository.save(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngatlanItem> getItemsByCity(String city) {
        if (city == null || city.isBlank()) return Collections.emptyList();
        String cityKey = CityUtils.normalizeCityKey(city);
        return propertyRepository.findByNormalizedCityOrderByDiscoveredAtDesc(cityKey).stream()
                .map(this::entityToItem)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngatlanItem> getAll() {
        return propertyRepository.findAllByOrderByDiscoveredAtDesc().stream()
                .map(this::entityToItem)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCityDatabase(String city) {
        if (city == null || city.isBlank()) return false;
        return propertyRepository.existsByNormalizedCity(CityUtils.normalizeCityKey(city));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNewUrl(String city, String url) {
        if (url == null || url.isBlank()) return false;
        String trimmed = url.trim();
        if (city != null && !city.isBlank()) {
            if (!hasCityDatabase(city)) {
                return false;
            }
            return !propertyRepository.existsByUrlAndNormalizedCity(trimmed, CityUtils.normalizeCityKey(city));
        }
        return !propertyRepository.existsByUrl(trimmed);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean containsUrl(String url) {
        if (url == null || url.isBlank()) return false;
        return propertyRepository.existsByUrl(url.trim());
    }

    @Override
    public boolean markAsViewed(String url) {
        if (url == null || url.isBlank()) return false;
        String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        int updated = propertyRepository.markAsViewed(url.trim(), now);
        return updated > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isViewed(String url) {
        if (url == null || url.isBlank()) return false;
        return propertyRepository.findByUrl(url.trim()).map(PropertyEntity::isViewed).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getAllUrls() {
        return new HashSet<>(propertyRepository.findAllUrls());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTrackedCitiesInfo() {
        List<Object[]> rows = propertyRepository.countByCityGroup();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            String city = (String) r[0];
            String normalizedCity = (String) r[1];
            long count = ((Number) r[2]).longValue();
            list.add(Map.of(
                    "name", city != null ? city : "Ismeretlen",
                    "key", normalizedCity != null ? normalizedCity : CityUtils.normalizeCityKey(city),
                    "count", count
            ));
        }
        return list;
    }

    @Override
    public void clearCityHistory(String city) {
        if (city == null || city.isBlank()) return;
        String cityKey = CityUtils.normalizeCityKey(city);
        List<PropertyEntity> props = propertyRepository.findByNormalizedCityOrderByDiscoveredAtDesc(cityKey);
        List<String> urls = props.stream().map(PropertyEntity::getUrl).toList();
        propertyRepository.deleteByNormalizedCity(cityKey);
        if (!urls.isEmpty()) {
            priceHistoryRepository.deleteByUrlIn(urls);
            statusLogRepository.deleteByUrlIn(urls);
        }
    }

    @Override
    public void clearHistory() {
        propertyRepository.deleteAll();
        priceHistoryRepository.deleteAll();
        statusLogRepository.deleteAll();
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalCount() {
        return (int) propertyRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByPortal() {
        List<Object[]> rows = propertyRepository.countByPortalGroup();
        Map<String, Long> map = new HashMap<>();
        for (Object[] r : rows) {
            String portal = (String) r[0];
            long count = ((Number) r[1]).longValue();
            if (portal != null && !portal.isBlank()) {
                map.put(portal, count);
            }
        }
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public String getLastRunTime() {
        return propertyRepository.findLatestDiscoveredAt();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceHistoryEntry> getPriceHistory(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isBlank()) return Collections.emptyList();
        String key = idOrUrl.trim();
        return priceHistoryRepository.findByUrlOrPropertyId(key).stream()
                .map(ph -> new PriceHistoryEntry(
                        ph.getId() != null ? ph.getId().intValue() : 0,
                        ph.getPropertyId(),
                        ph.getRecordedAt(),
                        ph.getPriceRaw(),
                        ph.getPriceAmount() != null ? ph.getPriceAmount() : 0L,
                        ph.getPriceMillions() != null ? ph.getPriceMillions() : 0.0,
                        ph.getPricePerSqm(),
                        ph.getPreviousPriceAmount(),
                        ph.getPriceDelta(),
                        ph.getChangePercent()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusLogEntry> getStatusLog(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isBlank()) return Collections.emptyList();
        String key = idOrUrl.trim();
        return statusLogRepository.findByUrlOrPropertyId(key).stream()
                .map(sl -> new StatusLogEntry(
                        sl.getId() != null ? sl.getId().intValue() : 0,
                        sl.getPropertyId(),
                        sl.getStatus(),
                        sl.getChangedAt(),
                        sl.getNote()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngatlanItem> getDiscountedProperties(String city) {
        ListingFilterDto filter = ListingFilterDto.discounted(city);
        return findByCriteria(filter).stream()
                .map(ListingDto::toItem)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingDto> findByCriteria(ListingFilterDto filter) {
        List<IngatlanItem> all = getAll();
        Stream<IngatlanItem> stream = all.stream();

        if (filter != null) {
            if (filter.cities() != null && !filter.cities().isEmpty()) {
                Set<String> normalizedFilterCities = filter.cities().stream()
                        .map(CityUtils::normalizeCityKey)
                        .collect(Collectors.toSet());
                stream = stream.filter(item -> {
                    String k = CityUtils.normalizeCityKey(item.getCity());
                    return normalizedFilterCities.contains(k);
                });
            }

            if (filter.portals() != null && !filter.portals().isEmpty()) {
                Set<String> normalizedFilterPortals = filter.portals().stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
                stream = stream.filter(item -> {
                    String p = item.getPortal() != null ? item.getPortal().toLowerCase() : "";
                    return normalizedFilterPortals.contains(p);
                });
            }

            if (filter.minPriceMillion() != null) {
                stream = stream.filter(item -> item.getPriceInfo() != null && item.getPriceInfo().getAmountMillion() >= filter.minPriceMillion());
            }

            if (filter.maxPriceMillion() != null) {
                stream = stream.filter(item -> item.getPriceInfo() != null && item.getPriceInfo().getAmountMillion() <= filter.maxPriceMillion());
            }

            if (filter.minArea() != null) {
                stream = stream.filter(item -> item.getDetailsInfo() != null && item.getDetailsInfo().getAreaSquareMeters() >= filter.minArea());
            }

            if (filter.maxArea() != null) {
                stream = stream.filter(item -> item.getDetailsInfo() != null && item.getDetailsInfo().getAreaSquareMeters() <= filter.maxArea());
            }

            if (filter.minRooms() != null) {
                stream = stream.filter(item -> item.getDetailsInfo() != null && item.getDetailsInfo().getRoomCount() >= filter.minRooms());
            }

            if (filter.maxRooms() != null) {
                stream = stream.filter(item -> item.getDetailsInfo() != null && item.getDetailsInfo().getRoomCount() <= filter.maxRooms());
            }

            if (filter.excludeViewed()) {
                stream = stream.filter(item -> !item.isViewed());
            }

            if (filter.onlyDiscounted()) {
                stream = stream.filter(item -> item.getPriceDelta() != null && item.getPriceDelta() < 0);
            }

            String sortBy = filter.sortBy() != null ? filter.sortBy() : "DISCOVERED_DESC";
            Comparator<IngatlanItem> comparator = switch (sortBy.toUpperCase()) {
                case "PRICE_ASC" -> Comparator.comparingDouble(i -> (i.getPriceInfo() != null ? i.getPriceInfo().getAmountMillion() : Double.MAX_VALUE));
                case "PRICE_DESC" -> Comparator.comparingDouble((IngatlanItem i) -> (i.getPriceInfo() != null ? i.getPriceInfo().getAmountMillion() : 0.0)).reversed();
                case "AREA_ASC" -> Comparator.comparingDouble(i -> (i.getDetailsInfo() != null ? i.getDetailsInfo().getAreaSquareMeters() : Double.MAX_VALUE));
                case "AREA_DESC" -> Comparator.comparingDouble((IngatlanItem i) -> (i.getDetailsInfo() != null ? i.getDetailsInfo().getAreaSquareMeters() : 0.0)).reversed();
                case "DISCOUNT_DESC" -> Comparator.comparingLong((IngatlanItem i) -> (i.getPriceDelta() != null ? i.getPriceDelta() : 0L));
                default -> Comparator.comparing(IngatlanItem::getDiscoveredAt, Comparator.nullsLast(Comparator.reverseOrder()));
            };
            stream = stream.sorted(comparator);

            if (filter.offset() != null && filter.offset() > 0) {
                stream = stream.skip(filter.offset());
            }
            if (filter.limit() != null && filter.limit() > 0) {
                stream = stream.limit(filter.limit());
            }
        }

        return stream.map(ListingDto::fromItem).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SearchCriteria getLastCriteria() {
        try {
            Optional<ConfigEntity> opt = configRepository.findById(CONFIG_KEY_LAST_CRITERIA);
            if (opt.isPresent() && opt.get().getValue() != null) {
                SearchCriteria sc = gson.fromJson(opt.get().getValue(), SearchCriteria.class);
                if (sc != null) return sc;
            }
        } catch (Exception e) {
            LOGGER.error("Hiba a konfiguráció H2-ből való olvasásakor: {}", e.getMessage(), e);
        }
        return new SearchCriteria();
    }

    @Override
    public void saveLastCriteria(SearchCriteria criteria) {
        if (criteria == null) return;
        try {
            String json = gson.toJson(criteria);
            configRepository.save(new ConfigEntity(CONFIG_KEY_LAST_CRITERIA, json));
        } catch (Exception e) {
            LOGGER.error("Hiba a konfiguráció H2-be mentésekor: {}", e.getMessage(), e);
        }
    }

    private IngatlanItem entityToItem(PropertyEntity entity) {
        IngatlanItem item = new IngatlanItem();
        item.setId(entity.getPropertyId());
        item.setUrl(entity.getUrl());
        item.setPortal(entity.getPortal());
        item.setTitle(entity.getTitle());
        item.setPrice(entity.getPriceRaw());
        item.setLocation(entity.getLocation());
        item.setCity(entity.getCity());
        item.setDetails(entity.getDetails());
        item.setDiscoveredAt(entity.getDiscoveredAt());
        item.setNew(entity.isNew());
        item.setViewed(entity.isViewed());
        item.setViewedAt(entity.getViewedAt());
        item.setImageUrl(entity.getImageUrl());
        item.setStatus(entity.getStatus());
        item.setPriceDelta(entity.getPriceDelta());
        item.setPriceChangePercent(entity.getPriceChangePercent());
        item.setLastPriceChangeAt(entity.getLastPriceChangeAt());

        if (entity.getPriceAmount() != null || entity.getPriceMillions() != null) {
            item.setPriceInfo(new PriceInfo(
                    entity.getPriceRaw(),
                    entity.getPriceAmount() != null ? entity.getPriceAmount() : 0L,
                    entity.getPriceMillions() != null ? entity.getPriceMillions() : 0.0
            ));
        }

        if (entity.getAreaSqm() != null || entity.getRooms() != null || entity.getPricePerSqm() != null) {
            item.setDetailsInfo(new PropertyDetails(
                    entity.getDetails(),
                    entity.getAreaSqm() != null ? entity.getAreaSqm() : 0.0,
                    entity.getRooms() != null ? entity.getRooms() : 0.0,
                    entity.getPricePerSqm() != null ? entity.getPricePerSqm() : 0L
            ));
        }

        return item;
    }
}
