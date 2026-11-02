package ingatlanok.presentation;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import ingatlanok.model.PropertyType;

/**
 * Summary DTO representing a listing with a registered price reduction.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class PriceDropItemDto implements Serializable {

  private String id;
  private String url;
  private String title;
  private String city;
  private String portal;
  private PropertyType propertyType;
  private Long currentPriceHuf;
  private Double currentPriceMillion;
  private Long priceDelta;
  private Double changePercent;
  private Long pricePerM2;
  private String imageUrl;
  private String lastPriceChangeAt;
}
