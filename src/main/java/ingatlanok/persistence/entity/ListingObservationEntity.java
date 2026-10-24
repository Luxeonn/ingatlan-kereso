package ingatlanok.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "listing_observations",
    indexes = {
      @Index(name = "idx_obs_url", columnList = "url"),
      @Index(name = "idx_obs_prop_id", columnList = "propertyId"),
      @Index(name = "idx_obs_canonical_id", columnList = "canonicalId"),
      @Index(name = "idx_obs_observed_at", columnList = "observedAt")
    })
public class ListingObservationEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 255)
  private String propertyId;

  @Column(nullable = false, length = 2048)
  private String url;

  @Column(length = 128)
  private String canonicalId;

  @Column(length = 50)
  private String observedAt;

  private Long priceHuf;

  private Long pricePerM2;

  @Column(length = 50)
  private String statusEvent;

  private Long priceDelta;

  private Double changePercent;

  public ListingObservationEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPropertyId() {
    return propertyId;
  }

  public void setPropertyId(String propertyId) {
    this.propertyId = propertyId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getCanonicalId() {
    return canonicalId;
  }

  public void setCanonicalId(String canonicalId) {
    this.canonicalId = canonicalId;
  }

  public String getObservedAt() {
    return observedAt;
  }

  public void setObservedAt(String observedAt) {
    this.observedAt = observedAt;
  }

  public Long getPriceHuf() {
    return priceHuf;
  }

  public void setPriceHuf(Long priceHuf) {
    this.priceHuf = priceHuf;
  }

  public Long getPricePerM2() {
    return pricePerM2;
  }

  public void setPricePerM2(Long pricePerM2) {
    this.pricePerM2 = pricePerM2;
  }

  public String getStatusEvent() {
    return statusEvent;
  }

  public void setStatusEvent(String statusEvent) {
    this.statusEvent = statusEvent;
  }

  public Long getPriceDelta() {
    return priceDelta;
  }

  public void setPriceDelta(Long priceDelta) {
    this.priceDelta = priceDelta;
  }

  public Double getChangePercent() {
    return changePercent;
  }

  public void setChangePercent(Double changePercent) {
    this.changePercent = changePercent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListingObservationEntity that = (ListingObservationEntity) o;
    if (id != null && that.id != null) {
      return Objects.equals(id, that.id);
    }
    return Objects.equals(url, that.url) && Objects.equals(observedAt, that.observedAt);
  }

  @Override
  public int hashCode() {
    if (id != null) {
      return Objects.hashCode(id);
    }
    return Objects.hash(url, observedAt);
  }
}
