package ingatlanok.dto;

import java.io.Serializable;

import ingatlanok.model.PropertyDetails;

/** Típusbiztos ingatlan-részletek DTO (Record) (alapterület, szobaszám, fajlagos m² ár). */
public record PropertyDetailsDto(
    String details, Double areaSquareMeters, Double roomCount, Long pricePerSquareMeter)
    implements Serializable {

  public PropertyDetails toModel() {
    return new PropertyDetails(details, areaSquareMeters, roomCount, pricePerSquareMeter);
  }

  public static PropertyDetailsDto fromModel(PropertyDetails model) {
    if (model == null) {
      return null;
    }
    return new PropertyDetailsDto(
        model.getRawText(),
        model.getAreaSquareMeters(),
        model.getRoomCount(),
        model.getPricePerSquareMeter());
  }
}
