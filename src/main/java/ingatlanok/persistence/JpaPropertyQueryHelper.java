package ingatlanok.persistence;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingObservation;
import ingatlanok.model.ListingSortOption;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.model.TrackedCityInfo;
import ingatlanok.persistence.entity.ConfigEntity;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.mapper.JpaEntityMapper;
import ingatlanok.persistence.repository.ConfigRepository;
import ingatlanok.persistence.repository.ListingObservationRepository;
import ingatlanok.persistence.repository.PortalCountProjection;
import ingatlanok.persistence.repository.PriceHistoryRepository;
import ingatlanok.persistence.repository.PropertyRepository;
import ingatlanok.persistence.repository.StatusLogRepository;
import ingatlanok.persistence.specification.PropertySpecifications;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class JpaPropertyQueryHelper {

  private static final String CONFIG_KEY_LAST_CRITERIA = "last_criteria";

  private final PropertyRepository propertyRepo;
  private final PriceHistoryRepository priceHistoryRepo;
  private final StatusLogRepository statusLogRepo;
  private final ConfigRepository configRepo;
  private final ListingObservationRepository listingObservationRepo;
  private final JpaEntityMapper mapper;
  private final ObjectMapper objectMapper;

  List<TrackedCityInfo> getTrackedCitiesInfo() {
    return propertyRepo.countByCityGroup().stream()
        .map(
            r ->
                new TrackedCityInfo(
                    r.city() != null ? r.city() : "Ismeretlen",
                    r.normalizedCity() != null
                        ? r.normalizedCity()
                        : CityUtils.normalizeCityKey(r.city()),
                    r.count()))
        .toList();
  }

  Map<String, Long> countByPortal() {
    Map<String, Long> map = new HashMap<>();
    for (PortalCountProjection r : propertyRepo.countByPortalGroup()) {
      if (r.portal() != null && !r.portal().isBlank()) {
        map.put(r.portal(), r.count());
      }
    }
    return map;
  }

  List<PriceHistoryEntry> getPriceHistory(String idOrUrl) {
    return (idOrUrl == null || idOrUrl.isBlank())
        ? Collections.emptyList()
        : priceHistoryRepo.findByUrlOrPropertyId(idOrUrl.trim()).stream()
            .map(mapper::toPriceHistoryEntry)
            .toList();
  }

  List<StatusLogEntry> getStatusLog(String idOrUrl) {
    return (idOrUrl == null || idOrUrl.isBlank())
        ? Collections.emptyList()
        : statusLogRepo.findByUrlOrPropertyId(idOrUrl.trim()).stream()
            .map(mapper::toStatusLogEntry)
            .toList();
  }

  List<ListingObservation> getListingObservations(String idOrUrl) {
    return (idOrUrl == null || idOrUrl.isBlank() || listingObservationRepo == null)
        ? Collections.emptyList()
        : listingObservationRepo.findByUrlOrPropertyIdOrCanonicalId(idOrUrl.trim()).stream()
            .map(mapper::toListingObservation)
            .toList();
  }

  List<IngatlanItem> getDiscountedProperties(String city) {
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

  List<IngatlanItem> findByCriteria(PropertySearchCriteria filter) {
    PropertySearchCriteria criteria =
        filter != null
            ? filter
            : new PropertySearchCriteria(
                null, null, null, null, null, null, null, null, false, false, false,
                ListingSortOption.DISCOVERED_DESC, 0, 100);
    ListingSortOption sortOption =
        criteria.sortBy() != null ? criteria.sortBy() : ListingSortOption.DISCOVERED_DESC;
    Sort sort = resolveSort(sortOption);
    int offset = Math.max(0, criteria.offset());
    int size = Math.max(1, Math.min(200, criteria.size()));
    return mapper.toItems(
        propertyRepo
            .findAll(
                PropertySpecifications.from(criteria), new OffsetPageRequest(offset, size, sort))
            .getContent());
  }

  private Sort resolveSort(ListingSortOption sortOption) {
    return switch (sortOption) {
      case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "priceMillions");
      case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "priceMillions");
      case AREA_ASC -> Sort.by(Sort.Direction.ASC, "areaSqm");
      case AREA_DESC -> Sort.by(Sort.Direction.DESC, "areaSqm");
      case ROOMS_ASC -> Sort.by(Sort.Direction.ASC, "rooms");
      case ROOMS_DESC -> Sort.by(Sort.Direction.DESC, "rooms");
      case DISCOVERED_DESC, BARGAIN_DESC -> Sort.by(Sort.Direction.DESC, "discoveredAt");
    };
  }

  Map<String, IngatlanItem> findByUrls(Collection<String> urls) {
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

  SearchCriteria getLastCriteria() {
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

  @Transactional
  void saveLastCriteria(SearchCriteria criteria) {
    if (criteria != null) {
      configRepo.save(
          new ConfigEntity(CONFIG_KEY_LAST_CRITERIA, objectMapper.writeValueAsString(criteria)));
    }
  }
}
