package ingatlanok.model;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable domain value object representing statistical metrics for a single city and property
 * type segment.
 */
@Getter
@NoArgsConstructor
public class MarketSegmentStats implements Serializable {

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

  @Builder(toBuilder = true)
  public MarketSegmentStats(
      String city,
      PropertyType propertyType,
      String propertyTypeName,
      boolean residential,
      boolean plot,
      int sampleSize,
      boolean insufficientData,
      Long medianPricePerM2,
      Long averagePricePerM2,
      Double medianPriceMillion,
      Double averagePriceMillion,
      Double minPriceMillion,
      Double maxPriceMillion,
      int bargainCount) {
    this.city = city;
    this.propertyType = propertyType;
    this.propertyTypeName = propertyTypeName;
    this.residential = residential;
    this.plot = plot;
    this.sampleSize = sampleSize;
    this.insufficientData = insufficientData;
    this.medianPricePerM2 = medianPricePerM2;
    this.averagePricePerM2 = averagePricePerM2;
    this.medianPriceMillion = medianPriceMillion;
    this.averagePriceMillion = averagePriceMillion;
    this.minPriceMillion = minPriceMillion;
    this.maxPriceMillion = maxPriceMillion;
    this.bargainCount = bargainCount;
  }
}
