package ingatlanok.persistence;

import java.util.List;

import ingatlanok.model.ListingSortOption;

/**
 * Database-side filters for the persisted listing history (#401 / JAVA-004).
 */
public record PropertySearchCriteria(
    List<String> cities,
    List<String> portals,
    Double minPrice,
    Double maxPrice,
    Double minArea,
    Double maxArea,
    Double minRooms,
    Double maxRooms,
    boolean excludeViewed,
    boolean onlyDiscounted,
    boolean onlyNewConstruction,
    ListingSortOption sortBy,
    int offset,
    int size) {

  public PropertySearchCriteria(
      List<String> cities,
      List<String> portals,
      Double minPrice,
      Double maxPrice,
      Double minArea,
      Double maxArea,
      Double minRooms,
      Double maxRooms,
      boolean excludeViewed,
      boolean onlyDiscounted,
      String sortBy,
      int offset,
      int size) {
    this(
        cities,
        portals,
        minPrice,
        maxPrice,
        minArea,
        maxArea,
        minRooms,
        maxRooms,
        excludeViewed,
        onlyDiscounted,
        false,
        ListingSortOption.fromString(sortBy),
        offset,
        size);
  }

  public PropertySearchCriteria(
      List<String> cities,
      List<String> portals,
      Double minPrice,
      Double maxPrice,
      Double minArea,
      Double maxArea,
      Double minRooms,
      Double maxRooms,
      boolean excludeViewed,
      boolean onlyDiscounted,
      boolean onlyNewConstruction,
      String sortBy,
      int offset,
      int size) {
    this(
        cities,
        portals,
        minPrice,
        maxPrice,
        minArea,
        maxArea,
        minRooms,
        maxRooms,
        excludeViewed,
        onlyDiscounted,
        onlyNewConstruction,
        ListingSortOption.fromString(sortBy),
        offset,
        size);
  }
}
