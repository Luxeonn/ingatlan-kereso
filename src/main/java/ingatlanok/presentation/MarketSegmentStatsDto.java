package ingatlanok.presentation;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import ingatlanok.model.PropertyType;

/**
 * Detailed statistical segment summary for a specific city and property type bucket.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class MarketSegmentStatsDto implements Serializable {

  private String city;
  private PropertyType propertyType;
  private String propertyTypeName;
  private boolean residential;
  private boolean plot;
  private int sampleSize;
  private boolean insufficientData;
  private Long medianPricePerM2;
  private Long averagePricePerM2;
  private Double medianPriceMillion;
  private Double averagePriceMillion;
  private Double minPriceMillion;
  private Double maxPriceMillion;
  private int bargainCount;
}
