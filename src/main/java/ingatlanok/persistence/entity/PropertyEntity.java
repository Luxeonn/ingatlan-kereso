package ingatlanok.persistence.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import ingatlanok.model.PropertyType;

@Entity
@Table(
    name = "properties",
    indexes = {
      @Index(name = "idx_properties_url", columnList = "url", unique = true),
      @Index(name = "idx_properties_city_key", columnList = "normalizedCity"),
      @Index(name = "idx_properties_discovered", columnList = "discoveredAt"),
      @Index(name = "idx_properties_portal", columnList = "portal"),
      @Index(name = "idx_properties_city_portal", columnList = "normalizedCity,portal"),
      @Index(name = "idx_properties_price", columnList = "priceMillions"),
      @Index(name = "idx_properties_area", columnList = "areaSqm"),
      @Index(name = "idx_properties_rooms", columnList = "rooms"),
      @Index(name = "idx_properties_viewed", columnList = "viewed"),
      @Index(name = "idx_properties_price_delta", columnList = "priceDelta"),
      @Index(name = "idx_properties_property_type", columnList = "property_type"),
      @Index(name = "idx_properties_canonical_id", columnList = "canonicalId")
    })
public class PropertyEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long internalId;

  @Column(nullable = false, unique = true, length = 2048)
  private String url;

  @Column(length = 255)
  private String propertyId;

  @Column(length = 100)
  private String portal;

  @Column(length = 255)
  private String city;

  @Column(length = 255)
  private String normalizedCity;

  @Column(length = 255)
  private String location;

  @Column(length = 1024)
  private String title;

  @Column(length = 255)
  private String priceRaw;

  private Long priceAmount;

  private Double priceMillions;

  private Long pricePerSqm;

  @Column(length = 2048)
  private String details;

  private Double areaSqm;

  private Double rooms;

  @Column(length = 2048)
  private String imageUrl;

  @Column(nullable = false)
  @ColumnDefault("false")
  private boolean newConstruction;

  @Column(length = 50)
  private String discoveredAt;

  @Column(nullable = false)
  @ColumnDefault("false")
  private boolean isNew;

  @Column(nullable = false)
  @ColumnDefault("false")
  private boolean viewed;

  @Column(length = 50)
  private String viewedAt;

  @Column(length = 50)
  private String status = "ACTIVE";

  private Long priceDelta;

  private Double priceChangePercent;

  @Column(length = 50)
  private String lastPriceChangeAt;

  @Column(length = 50)
  private String updatedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "property_type", length = 50)
  private PropertyType propertyType = PropertyType.EGYEB_ISMERETLEN;

  @Column(length = 128)
  private String canonicalId;

  @Column(length = 50)
  private String firstSeenAt;

  @Column(length = 50)
  private String lastSeenAt;

  public PropertyEntity() {}

  public Long getInternalId() {
    return internalId;
  }

  public void setInternalId(Long internalId) {
    this.internalId = internalId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getPropertyId() {
    return propertyId;
  }

  public void setPropertyId(String propertyId) {
    this.propertyId = propertyId;
  }

  public String getPortal() {
    return portal;
  }

  public void setPortal(String portal) {
    this.portal = portal;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getNormalizedCity() {
    return normalizedCity;
  }

  public void setNormalizedCity(String normalizedCity) {
    this.normalizedCity = normalizedCity;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPriceRaw() {
    return priceRaw;
  }

  public void setPriceRaw(String priceRaw) {
    this.priceRaw = priceRaw;
  }

  public Long getPriceAmount() {
    return priceAmount;
  }

  public void setPriceAmount(Long priceAmount) {
    this.priceAmount = priceAmount;
  }

  public Double getPriceMillions() {
    return priceMillions;
  }

  public void setPriceMillions(Double priceMillions) {
    this.priceMillions = priceMillions;
  }

  public Long getPricePerSqm() {
    return pricePerSqm;
  }

  public void setPricePerSqm(Long pricePerSqm) {
    this.pricePerSqm = pricePerSqm;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public Double getAreaSqm() {
    return areaSqm;
  }

  public void setAreaSqm(Double areaSqm) {
    this.areaSqm = areaSqm;
  }

  public Double getRooms() {
    return rooms;
  }

  public void setRooms(Double rooms) {
    this.rooms = rooms;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public boolean isNewConstruction() {
    return newConstruction;
  }

  public void setNewConstruction(boolean newConstruction) {
    this.newConstruction = newConstruction;
  }

  public String getDiscoveredAt() {
    return discoveredAt;
  }

  public void setDiscoveredAt(String discoveredAt) {
    this.discoveredAt = discoveredAt;
  }

  public boolean isNew() {
    return isNew;
  }

  public void setNew(boolean isNew) {
    this.isNew = isNew;
  }

  public boolean isViewed() {
    return viewed;
  }

  public void setViewed(boolean viewed) {
    this.viewed = viewed;
  }

  public String getViewedAt() {
    return viewedAt;
  }

  public void setViewedAt(String viewedAt) {
    this.viewedAt = viewedAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getPriceDelta() {
    return priceDelta;
  }

  public void setPriceDelta(Long priceDelta) {
    this.priceDelta = priceDelta;
  }

  public Double getPriceChangePercent() {
    return priceChangePercent;
  }

  public void setPriceChangePercent(Double priceChangePercent) {
    this.priceChangePercent = priceChangePercent;
  }

  public String getLastPriceChangeAt() {
    return lastPriceChangeAt;
  }

  public void setLastPriceChangeAt(String lastPriceChangeAt) {
    this.lastPriceChangeAt = lastPriceChangeAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  public PropertyType getPropertyType() {
    return propertyType != null ? propertyType : PropertyType.EGYEB_ISMERETLEN;
  }

  public void setPropertyType(PropertyType propertyType) {
    this.propertyType = propertyType != null ? propertyType : PropertyType.EGYEB_ISMERETLEN;
  }

  public String getCanonicalId() {
    return canonicalId;
  }

  public void setCanonicalId(String canonicalId) {
    this.canonicalId = canonicalId;
  }

  public String getFirstSeenAt() {
    return firstSeenAt;
  }

  public void setFirstSeenAt(String firstSeenAt) {
    this.firstSeenAt = firstSeenAt;
  }

  public String getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(String lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }
}
