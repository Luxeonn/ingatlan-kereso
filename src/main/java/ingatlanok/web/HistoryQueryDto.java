package ingatlanok.web;

import java.util.List;

import ingatlanok.model.ListingSortOption;
import ingatlanok.model.PortalId;
import ingatlanok.persistence.PropertySearchCriteria;

/**
 * Encapsulates GET /api/history query parameters into a strongly typed record (#401 / JAVA-004).
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
    ListingSortOption sortBy,
    Integer limit,
    Integer offset) {

  public HistoryQueryDto(
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
    this(
        city,
        onlyDiscounted,
        portal,
        minPrice,
        maxPrice,
        minArea,
        maxArea,
        minRooms,
        maxRooms,
        excludeViewed,
        onlyNewConstruction,
        ListingSortOption.fromString(sortBy),
        limit,
        offset);
  }

  public void validate() {
    validatePrices();
    validateAreas();
    validateRooms();
    validatePaging();
    validatePortalAndCity();
  }

  private void validatePrices() {
    if (minPrice != null && minPrice < 0) {
      throw new IllegalArgumentException("A minimum ár nem lehet negatív: " + minPrice);
    }
    if (maxPrice != null && maxPrice < 0) {
      throw new IllegalArgumentException("A maximum ár nem lehet negatív: " + maxPrice);
    }
    if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
      throw new IllegalArgumentException(
          "A minimum ár nem lehet nagyobb a maximum árnál: " + minPrice + " > " + maxPrice);
    }
  }

  private void validateAreas() {
    if (minArea != null && minArea < 0) {
      throw new IllegalArgumentException("A minimum alapterület nem lehet negatív: " + minArea);
    }
    if (maxArea != null && maxArea < 0) {
      throw new IllegalArgumentException("A maximum alapterület nem lehet negatív: " + maxArea);
    }
    if (minArea != null && maxArea != null && minArea > maxArea) {
      throw new IllegalArgumentException(
          "A minimum alapterület nem lehet nagyobb a maximum alapterületnél: " + minArea + " > " + maxArea);
    }
  }

  private void validateRooms() {
    if (minRooms != null && minRooms < 0) {
      throw new IllegalArgumentException("A minimum szobaszám nem lehet negatív: " + minRooms);
    }
    if (maxRooms != null && maxRooms < 0) {
      throw new IllegalArgumentException("A maximum szobaszám nem lehet negatív: " + maxRooms);
    }
    if (minRooms != null && maxRooms != null && minRooms > maxRooms) {
      throw new IllegalArgumentException(
          "A minimum szobaszám nem lehet nagyobb a maximum szobaszámnál: " + minRooms + " > " + maxRooms);
    }
  }

  private void validatePaging() {
    if (limit != null && limit <= 0) {
      throw new IllegalArgumentException("A limit értéke csak pozitív egész szám lehet: " + limit);
    }
    if (offset != null && offset < 0) {
      throw new IllegalArgumentException("Az offset értéke nem lehet negatív: " + offset);
    }
  }

  private void validatePortalAndCity() {
    if (portal != null && !portal.isBlank() && PortalId.fromString(portal) == PortalId.UNKNOWN) {
      throw new IllegalArgumentException("Ismeretlen portál azonosító: " + portal);
    }
    if (city != null && city.length() > 200) {
      throw new IllegalArgumentException("A település neve túl hosszú: " + city);
    }
  }

  public PropertySearchCriteria toCriteria() {
    validate();
    List<String> cities = (city != null && !city.isBlank()) ? List.of(city) : null;
    List<String> portals = (portal != null && !portal.isBlank()) ? List.of(portal) : null;
    int pageSize = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
    int pageOffset = offset == null || offset <= 0 ? 0 : offset;
    ListingSortOption effectiveSort = (sortBy != null) ? sortBy : ListingSortOption.DISCOVERED_DESC;

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
