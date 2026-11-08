package ingatlanok.persistence.specification;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;

import ingatlanok.common.CityUtils;
import ingatlanok.persistence.PropertySearchCriteria;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.entity.PropertyEntityFields;

/** Type-safe criteria builder for listing history queries (#401 / JAVA-025). */
public final class PropertySpecifications {

  private PropertySpecifications() {}

  public static Specification<PropertyEntity> from(PropertySearchCriteria criteria) {
    if (criteria == null) {
      return (root, query, cb) -> cb.conjunction();
    }
    Specification<PropertyEntity> spec = buildLocationAndPortalSpecs(criteria);
    spec = spec.and(buildNumericRangeSpecs(criteria));
    return spec.and(buildFlagSpecs(criteria));
  }

  private static Specification<PropertyEntity> buildLocationAndPortalSpecs(
      PropertySearchCriteria criteria) {
    Specification<PropertyEntity> spec = (root, query, cb) -> cb.conjunction();
    if (criteria.cities() != null && !criteria.cities().isEmpty()) {
      Set<String> cities =
          criteria.cities().stream().map(CityUtils::normalizeCityKey).collect(Collectors.toSet());
      spec = spec.and((root, query, cb) -> root.get(PropertyEntityFields.NORMALIZED_CITY).in(cities));
    }
    if (criteria.portals() != null && !criteria.portals().isEmpty()) {
      Set<String> portals =
          criteria.portals().stream().map(p -> p.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
      spec = spec.and((root, query, cb) -> cb.lower(root.get(PropertyEntityFields.PORTAL)).in(portals));
    }
    return spec;
  }

  private static Specification<PropertyEntity> buildNumericRangeSpecs(
      PropertySearchCriteria criteria) {
    Specification<PropertyEntity> spec = (root, query, cb) -> cb.conjunction();
    if (criteria.minPrice() != null) {
      spec = spec.and((root, query, cb) -> cb.ge(root.get(PropertyEntityFields.PRICE_MILLIONS), criteria.minPrice()));
    }
    if (criteria.maxPrice() != null) {
      spec = spec.and((root, query, cb) -> cb.le(root.get(PropertyEntityFields.PRICE_MILLIONS), criteria.maxPrice()));
    }
    if (criteria.minArea() != null) {
      spec = spec.and((root, query, cb) -> cb.ge(root.get(PropertyEntityFields.AREA_SQM), criteria.minArea()));
    }
    if (criteria.maxArea() != null) {
      spec = spec.and((root, query, cb) -> cb.le(root.get(PropertyEntityFields.AREA_SQM), criteria.maxArea()));
    }
    if (criteria.minRooms() != null) {
      spec = spec.and((root, query, cb) -> cb.ge(root.get(PropertyEntityFields.ROOMS), criteria.minRooms()));
    }
    if (criteria.maxRooms() != null) {
      spec = spec.and((root, query, cb) -> cb.le(root.get(PropertyEntityFields.ROOMS), criteria.maxRooms()));
    }
    return spec;
  }

  private static Specification<PropertyEntity> buildFlagSpecs(PropertySearchCriteria criteria) {
    Specification<PropertyEntity> spec = (root, query, cb) -> cb.isFalse(root.get(PropertyEntityFields.HIDDEN));
    if (criteria.excludeViewed()) {
      spec = spec.and((root, query, cb) -> cb.isFalse(root.get(PropertyEntityFields.VIEWED)));
    }
    if (criteria.onlyDiscounted()) {
      spec = spec.and((root, query, cb) -> cb.lt(root.get(PropertyEntityFields.PRICE_DELTA), 0L));
    }
    if (criteria.onlyNewConstruction()) {
      spec = spec.and((root, query, cb) -> cb.isTrue(root.get(PropertyEntityFields.NEW_CONSTRUCTION)));
    }
    return spec;
  }
}
