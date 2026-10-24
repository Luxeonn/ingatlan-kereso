package ingatlanok.presentation;

import java.io.Serializable;

import ingatlanok.model.PropertyType;

/**
 * Detailed statistical segment summary for a specific city and property type bucket.
 */
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

  public MarketSegmentStatsDto() {}

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public PropertyType getPropertyType() {
    return propertyType;
  }

  public void setPropertyType(PropertyType propertyType) {
    this.propertyType = propertyType;
  }

  public String getPropertyTypeName() {
    return propertyTypeName;
  }

  public void setPropertyTypeName(String propertyTypeName) {
    this.propertyTypeName = propertyTypeName;
  }

  public boolean isResidential() {
    return residential;
  }

  public void setResidential(boolean residential) {
    this.residential = residential;
  }

  public boolean isPlot() {
    return plot;
  }

  public void setPlot(boolean plot) {
    this.plot = plot;
  }

  public int getSampleSize() {
    return sampleSize;
  }

  public void setSampleSize(int sampleSize) {
    this.sampleSize = sampleSize;
  }

  public boolean isInsufficientData() {
    return insufficientData;
  }

  public void setInsufficientData(boolean insufficientData) {
    this.insufficientData = insufficientData;
  }

  public Long getMedianPricePerM2() {
    return medianPricePerM2;
  }

  public void setMedianPricePerM2(Long medianPricePerM2) {
    this.medianPricePerM2 = medianPricePerM2;
  }

  public Long getAveragePricePerM2() {
    return averagePricePerM2;
  }

  public void setAveragePricePerM2(Long averagePricePerM2) {
    this.averagePricePerM2 = averagePricePerM2;
  }

  public Double getMedianPriceMillion() {
    return medianPriceMillion;
  }

  public void setMedianPriceMillion(Double medianPriceMillion) {
    this.medianPriceMillion = medianPriceMillion;
  }

  public Double getAveragePriceMillion() {
    return averagePriceMillion;
  }

  public void setAveragePriceMillion(Double averagePriceMillion) {
    this.averagePriceMillion = averagePriceMillion;
  }

  public Double getMinPriceMillion() {
    return minPriceMillion;
  }

  public void setMinPriceMillion(Double minPriceMillion) {
    this.minPriceMillion = minPriceMillion;
  }

  public Double getMaxPriceMillion() {
    return maxPriceMillion;
  }

  public void setMaxPriceMillion(Double maxPriceMillion) {
    this.maxPriceMillion = maxPriceMillion;
  }

  public int getBargainCount() {
    return bargainCount;
  }

  public void setBargainCount(int bargainCount) {
    this.bargainCount = bargainCount;
  }
}
