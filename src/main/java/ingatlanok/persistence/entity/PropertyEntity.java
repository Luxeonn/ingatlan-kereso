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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter
@Setter
@NoArgsConstructor
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

  public PropertyType getPropertyType() {
    return propertyType != null ? propertyType : PropertyType.EGYEB_ISMERETLEN;
  }

  public void setPropertyType(PropertyType propertyType) {
    this.propertyType = propertyType != null ? propertyType : PropertyType.EGYEB_ISMERETLEN;
  }
}
