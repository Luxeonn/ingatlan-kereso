package ingatlanok.model;

import java.io.Serializable;

public class PropertyDetails implements Serializable {

  private String rawText;
  private Double areaSquareMeters;
  private Double roomCount;
  private Long pricePerSquareMeter;

  public PropertyDetails() {}

  public PropertyDetails(
      String rawText, Double areaSquareMeters, Double roomCount, Long pricePerSquareMeter) {
    this.rawText = rawText;
    this.areaSquareMeters = areaSquareMeters;
    this.roomCount = roomCount;
    this.pricePerSquareMeter = pricePerSquareMeter;
  }

  public String getRawText() {
    return rawText;
  }

  public void setRawText(String rawText) {
    this.rawText = rawText;
  }

  public Double getAreaSquareMeters() {
    return areaSquareMeters;
  }

  public void setAreaSquareMeters(Double areaSquareMeters) {
    this.areaSquareMeters = areaSquareMeters;
  }

  public Double getRoomCount() {
    return roomCount;
  }

  public void setRoomCount(Double roomCount) {
    this.roomCount = roomCount;
  }

  public Long getPricePerSquareMeter() {
    return pricePerSquareMeter;
  }

  public void setPricePerSquareMeter(Long pricePerSquareMeter) {
    this.pricePerSquareMeter = pricePerSquareMeter;
  }

  @Override
  public String toString() {
    return rawText != null ? rawText : (areaSquareMeters != null ? areaSquareMeters + " m²" : "");
  }
}
