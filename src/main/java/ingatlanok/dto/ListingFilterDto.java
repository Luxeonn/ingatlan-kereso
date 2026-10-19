package ingatlanok.dto;

import java.io.Serializable;
import java.util.List;

public record ListingFilterDto(
    List<String> cities,
    List<String> portals,
    Double minPriceMillion,
    Double maxPriceMillion,
    Double minArea,
    Double maxArea,
    Double minRooms,
    Double maxRooms,
    boolean excludeViewed,
    boolean onlyDiscounted,
    String sortBy,
    Integer limit,
    Integer offset)
    implements Serializable {

  public static ListingFilterDto all() {
    return new ListingFilterDto(
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
        null,
        null);
  }

  public static ListingFilterDto byCity(String city) {
    return new ListingFilterDto(
        List.of(city),
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
        null,
        null);
  }

  public static ListingFilterDto discounted(String city) {
    List<String> cities = (city != null && !city.isBlank()) ? List.of(city) : null;
    return new ListingFilterDto(
        cities, null, null, null, null, null, null, null, false, true, "DISCOUNT_DESC", null, null);
  }
}
