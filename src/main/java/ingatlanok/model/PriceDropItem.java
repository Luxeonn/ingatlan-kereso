package ingatlanok.model;

import lombok.Builder;

/**
 * Immutable domain value object representing a listing with a registered price reduction.
 */
@Builder
public record PriceDropItem(
    String id,
    String url,
    String title,
    String city,
    String portal,
    PropertyType propertyType,
    Long currentPriceHuf,
    Double currentPriceMillion,
    Long priceDelta,
    Double changePercent,
    Long pricePerM2,
    String imageUrl,
    String lastPriceChangeAt) {

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
        .currentPriceHuf(priceInfo != null ? priceInfo.amountHuf() : null)
        .currentPriceMillion(priceInfo != null ? priceInfo.amountMillion() : null)
        .priceDelta(item.getPriceDelta())
        .changePercent(item.getPriceChangePercent())
        .pricePerM2(pricePerM2)
        .imageUrl(item.getImageUrl())
        .lastPriceChangeAt(item.getLastPriceChangeAt())
        .build();
  }
}
