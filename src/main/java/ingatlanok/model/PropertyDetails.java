package ingatlanok.model;

import lombok.Builder;

/**
 * Immutable value object representing parsed property size, room count, and unit price details.
 */
@Builder(toBuilder = true)
public record PropertyDetails(
    String rawText,
    Double areaSquareMeters,
    Double roomCount,
    Long pricePerSquareMeter) {

  @Override
  public String toString() {
    return rawText != null ? rawText : (areaSquareMeters != null ? areaSquareMeters + " m²" : "");
  }
}
