package ingatlanok.model;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable domain value object representing a listing with a registered price reduction.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class PriceDropItem implements Serializable {

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

  public static PriceDropItem from(IngatlanItem item, Long pricePerM2) {
    if (item == null) {
      return null;
    }
    PriceInfo priceInfo = item.getPriceInfo();
    return PriceDropItem.builder()
        .id(item.getId())
        .url(item.getUrl())
        .title(item.getTitle())
        .city(item.getCity())
        .portal(item.getPortal())
        .propertyType(item.getPropertyType())
        .currentPriceHuf(priceInfo != null ? priceInfo.getAmountHuf() : null)
        .currentPriceMillion(priceInfo != null ? priceInfo.getAmountMillion() : null)
        .priceDelta(item.getPriceDelta())
        .changePercent(item.getPriceChangePercent())
        .pricePerM2(pricePerM2)
        .imageUrl(item.getImageUrl())
        .lastPriceChangeAt(item.getLastPriceChangeAt())
        .build();
  }
}
