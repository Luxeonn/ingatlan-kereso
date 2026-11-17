package ingatlanok.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import ingatlanok.model.ListingSortOption;
import ingatlanok.model.PortalId;
import ingatlanok.persistence.PropertySearchCriteria;

/**
 * Encapsulates GET /api/history query parameters into a strongly typed record (#401 / JAVA-004).
 */
public record HistoryQueryDto(
    @Size(max = 200, message = "A település neve túl hosszú") String city,
    Boolean onlyDiscounted,
    String portal,
    @PositiveOrZero(message = "A minimum ár nem lehet negatív") Double minPrice,
    @PositiveOrZero(message = "A maximum ár nem lehet negatív") Double maxPrice,
    @PositiveOrZero(message = "A minimum alapterület nem lehet negatív") Double minArea,
    @PositiveOrZero(message = "A maximum alapterület nem lehet negatív") Double maxArea,
    @PositiveOrZero(message = "A minimum szobaszám nem lehet negatív") Double minRooms,
    @PositiveOrZero(message = "A maximum szobaszám nem lehet negatív") Double maxRooms,
    Boolean excludeViewed,
    Boolean onlyNewConstruction,
    ListingSortOption sortBy,
    @Positive(message = "A limit értéke csak pozitív egész szám lehet") Integer limit,
    @PositiveOrZero(message = "Az offset értéke nem lehet negatív") Integer offset) {

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

  @JsonIgnore
  @AssertTrue(message = "A minimum ár nem lehet nagyobb a maximum árnál")
  public boolean isPriceRangeValid() {
    return minPrice == null || maxPrice == null || minPrice <= maxPrice;
  }

  @JsonIgnore
  @AssertTrue(message = "A minimum alapterület nem lehet nagyobb a maximum alapterületnél")
  public boolean isAreaRangeValid() {
    return minArea == null || maxArea == null || minArea <= maxArea;
  }

  @JsonIgnore
  @AssertTrue(message = "A minimum szobaszám nem lehet nagyobb a maximum szobaszámnál")
  public boolean isRoomRangeValid() {
    return minRooms == null || maxRooms == null || minRooms <= maxRooms;
  }

  @JsonIgnore
  @AssertTrue(message = "Ismeretlen portál azonosító")
  public boolean isPortalValid() {
    return portal == null || portal.isBlank() || PortalId.fromString(portal) != PortalId.UNKNOWN;
  }

  public PropertySearchCriteria toCriteria() {
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
