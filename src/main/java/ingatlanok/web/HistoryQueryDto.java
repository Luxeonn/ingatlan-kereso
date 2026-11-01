package ingatlanok.web;

import java.util.List;

import ingatlanok.persistence.PropertySearchCriteria;

/**
 * Encapsulates GET /api/history query parameters into a strongly typed record.
 */
public record HistoryQueryDto(
    String city,
    Boolean onlyDiscounted,
    String portal,
    Double minPrice,
    Double maxPrice,
    Double minArea,
    Double maxArea,
    Double minRooms,
    Double maxRooms,
    Boolean excludeViewed,
    Boolean onlyNewConstruction,
    String sortBy,
    Integer limit,
    Integer offset) {

  public PropertySearchCriteria toCriteria() {
    List<String> cities = (city != null && !city.isBlank()) ? List.of(city) : null;
    List<String> portals = (portal != null && !portal.isBlank()) ? List.of(portal) : null;
    int pageSize = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
    int pageOffset = offset == null || offset <= 0 ? 0 : offset;
    String effectiveSort = (sortBy != null && !sortBy.isBlank()) ? sortBy : "DISCOVERED_DESC";

    return new PropertySearchCriteria(
        cities,
        portals,
        minPrice,
        maxPrice,
        minArea,
        maxArea,
        minRooms,
        maxRooms,
        Boolean.TRUE.equals(excludeViewed),
        Boolean.TRUE.equals(onlyDiscounted),
        Boolean.TRUE.equals(onlyNewConstruction),
        effectiveSort,
        pageOffset,
        pageSize);
  }
}
