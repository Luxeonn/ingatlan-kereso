package ingatlanok.persistence.mapper;

import ingatlanok.model.ListingStatus;

/**
 * Context object bundling property update parameters for JpaEntityMapper (#401 / JAVA-001).
 */
public record PropertyUpdateContext(
    String now,
    Long priceDelta,
    Double priceChangePercent,
    String lastPriceChangeAt,
    ListingStatus targetStatus) {

  public PropertyUpdateContext(
      String now,
      Long priceDelta,
      Double priceChangePercent,
      String lastPriceChangeAt,
      String targetStatus) {
    this(
        now,
        priceDelta,
        priceChangePercent,
        lastPriceChangeAt,
        ListingStatus.fromString(targetStatus));
  }
}
