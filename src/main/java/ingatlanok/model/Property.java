package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Property implements Serializable {

  private String canonicalId;
  private String normalizedCity;
  private String city;
  private PropertyType propertyType = PropertyType.EGYEB_ISMERETLEN;
  private Double floorAreaSquareMeters;
  private Double roomCount;
  private String location;
  private Long minActivePriceHuf;
  private List<SourceListing> sources = new ArrayList<>();

  public Property() {}

  public Property(
      String canonicalId,
      String normalizedCity,
      String city,
      PropertyType propertyType,
      Double floorAreaSquareMeters,
      Double roomCount,
      String location,
      Long minActivePriceHuf,
      List<SourceListing> sources) {
    this.canonicalId = canonicalId;
    this.normalizedCity = normalizedCity;
    this.city = city;
    this.propertyType = propertyType != null ? propertyType : PropertyType.EGYEB_ISMERETLEN;
    this.floorAreaSquareMeters = floorAreaSquareMeters;
    this.roomCount = roomCount;
    this.location = location;
    this.minActivePriceHuf = minActivePriceHuf;
    if (sources != null) {
      this.sources = new ArrayList<>(sources);
    }
  }

  public String getCanonicalId() {
    return canonicalId;
  }

  public void setCanonicalId(String canonicalId) {
    this.canonicalId = canonicalId;
  }

  public String getNormalizedCity() {
    return normalizedCity;
  }

  public void setNormalizedCity(String normalizedCity) {
    this.normalizedCity = normalizedCity;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public PropertyType getPropertyType() {
    return propertyType != null ? propertyType : PropertyType.EGYEB_ISMERETLEN;
  }

  public void setPropertyType(PropertyType propertyType) {
    this.propertyType = propertyType != null ? propertyType : PropertyType.EGYEB_ISMERETLEN;
  }

  public Double getFloorAreaSquareMeters() {
    return floorAreaSquareMeters;
  }

  public void setFloorAreaSquareMeters(Double floorAreaSquareMeters) {
    this.floorAreaSquareMeters = floorAreaSquareMeters;
  }

  public Double getRoomCount() {
    return roomCount;
  }

  public void setRoomCount(Double roomCount) {
    this.roomCount = roomCount;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public Long getMinActivePriceHuf() {
    if (minActivePriceHuf != null) {
      return minActivePriceHuf;
    }
    return calculateMinActivePriceHuf();
  }

  public void setMinActivePriceHuf(Long minActivePriceHuf) {
    this.minActivePriceHuf = minActivePriceHuf;
  }

  public List<SourceListing> getSources() {
    if (sources == null) {
      sources = new ArrayList<>();
    }
    return sources;
  }

  public void setSources(List<SourceListing> sources) {
    this.sources = sources != null ? new ArrayList<>(sources) : new ArrayList<>();
  }

  public void addSource(SourceListing source) {
    if (source != null) {
      if (this.sources == null) {
        this.sources = new ArrayList<>();
      }
      this.sources.add(source);
    }
  }

  public int getSourcesCount() {
    return sources != null ? sources.size() : 0;
  }

  public List<SourceListing> getActiveSources() {
    if (sources == null || sources.isEmpty()) {
      return Collections.emptyList();
    }
    return sources.stream().filter(SourceListing::isActive).toList();
  }

  public Long calculateMinActivePriceHuf() {
    List<SourceListing> active = getActiveSources();
    List<SourceListing> candidates = !active.isEmpty() ? active : (sources != null ? sources : List.of());
    return candidates.stream()
        .map(SourceListing::getPriceHuf)
        .filter(Objects::nonNull)
        .filter(p -> p > 0)
        .min(Long::compareTo)
        .orElse(null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Property property = (Property) o;
    if (canonicalId != null && property.canonicalId != null) {
      return Objects.equals(canonicalId, property.canonicalId);
    }
    return Objects.equals(normalizedCity, property.normalizedCity)
        && propertyType == property.propertyType
        && Objects.equals(floorAreaSquareMeters, property.floorAreaSquareMeters)
        && Objects.equals(roomCount, property.roomCount);
  }

  @Override
  public int hashCode() {
    if (canonicalId != null) {
      return Objects.hashCode(canonicalId);
    }
    return Objects.hash(normalizedCity, propertyType, floorAreaSquareMeters, roomCount);
  }

  @Override
  public String toString() {
    return "Property{"
        + "canonicalId='" + canonicalId + '\''
        + ", city='" + city + '\''
        + ", propertyType=" + propertyType
        + ", floorAreaSquareMeters=" + floorAreaSquareMeters
        + ", roomCount=" + roomCount
        + ", minActivePriceHuf=" + getMinActivePriceHuf()
        + ", sourcesCount=" + getSourcesCount()
        + '}';
  }
}
