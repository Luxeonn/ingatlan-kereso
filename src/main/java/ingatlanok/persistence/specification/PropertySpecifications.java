package ingatlanok.persistence.specification;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;

import ingatlanok.common.CityUtils;
import ingatlanok.persistence.PropertySearchCriteria;
import ingatlanok.persistence.entity.PropertyEntity;

/** Type-safe criteria builder for listing history queries. */
public final class PropertySpecifications {

  private PropertySpecifications() {}

  public static Specification<PropertyEntity> from(PropertySearchCriteria criteria) {
    Specification<PropertyEntity> spec = (root, query, cb) -> cb.conjunction();
    if (criteria == null) {
      return spec;
    }
    if (criteria.cities() != null && !criteria.cities().isEmpty()) {
      Set<String> cities =
          criteria.cities().stream().map(CityUtils::normalizeCityKey).collect(Collectors.toSet());
      spec = spec.and((root, query, cb) -> root.get("normalizedCity").in(cities));
    }
    if (criteria.portals() != null && !criteria.portals().isEmpty()) {
      Set<String> portals =
          criteria.portals().stream().map(p -> p.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
      spec = spec.and((root, query, cb) -> cb.lower(root.get("portal")).in(portals));
    }
    if (criteria.minPrice() != null) {
      spec = spec.and((root, query, cb) -> cb.ge(root.get("priceMillions"), criteria.minPrice()));
    }
    if (criteria.maxPrice() != null) {
      spec = spec.and((root, query, cb) -> cb.le(root.get("priceMillions"), criteria.maxPrice()));
    }
    if (criteria.minArea() != null) {
      spec = spec.and((root, query, cb) -> cb.ge(root.get("areaSqm"), criteria.minArea()));
    }
    if (criteria.maxArea() != null) {
      spec = spec.and((root, query, cb) -> cb.le(root.get("areaSqm"), criteria.maxArea()));
    }
    if (criteria.minRooms() != null) {
      spec = spec.and((root, query, cb) -> cb.ge(root.get("rooms"), criteria.minRooms()));
    }
    if (criteria.maxRooms() != null) {
      spec = spec.and((root, query, cb) -> cb.le(root.get("rooms"), criteria.maxRooms()));
    }
    if (criteria.excludeViewed()) {
      spec = spec.and((root, query, cb) -> cb.isFalse(root.get("viewed")));
    }
    if (criteria.onlyDiscounted()) {
      spec = spec.and((root, query, cb) -> cb.lt(root.get("priceDelta"), 0L));
    }
    if (criteria.onlyNewConstruction()) {
      spec = spec.and((root, query, cb) -> cb.isTrue(root.get("newConstruction")));
    }
    return spec;
  }
}
