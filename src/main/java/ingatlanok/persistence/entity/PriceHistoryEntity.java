package ingatlanok.persistence.entity;

import java.io.Serializable;

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
    name = "price_history",
    indexes = {
      @Index(name = "idx_price_history_prop_id", columnList = "propertyId"),
      @Index(name = "idx_price_history_url", columnList = "url")
    })
@Getter
@Setter
@NoArgsConstructor
public class PriceHistoryEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String propertyId;

  @Column(length = 2048)
  private String url;

  @Column(length = 50)
  private String recordedAt;

  @Column(length = 255)
  private String priceRaw;

  private Long priceAmount;

  private Double priceMillions;

  private Long pricePerSqm;

  private Long previousPriceAmount;

  private Long priceDelta;

  private Double changePercent;
}
