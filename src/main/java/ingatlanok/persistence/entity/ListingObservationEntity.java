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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "listing_observations",
    indexes = {
      @Index(name = "idx_obs_url", columnList = "url"),
      @Index(name = "idx_obs_prop_id", columnList = "propertyId"),
      @Index(name = "idx_obs_canonical_id", columnList = "canonicalId"),
      @Index(name = "idx_obs_observed_at", columnList = "observedAt")
    })
@Getter
@Setter
@NoArgsConstructor
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
