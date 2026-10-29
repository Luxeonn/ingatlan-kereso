package ingatlanok.model;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PropertyDetails implements Serializable {

  private String rawText;
  private Double areaSquareMeters;
  private Double roomCount;
  private Long pricePerSquareMeter;

  @Builder(toBuilder = true)
  public PropertyDetails(
      String rawText, Double areaSquareMeters, Double roomCount, Long pricePerSquareMeter) {
    this.rawText = rawText;
    this.areaSquareMeters = areaSquareMeters;
    this.roomCount = roomCount;
    this.pricePerSquareMeter = pricePerSquareMeter;
  }

  @Override
  public String toString() {
    return rawText != null ? rawText : (areaSquareMeters != null ? areaSquareMeters + " m²" : "");
  }
}
