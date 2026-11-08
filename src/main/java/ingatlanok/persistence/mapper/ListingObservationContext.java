package ingatlanok.persistence.mapper;

import ingatlanok.model.ListingObservation;

/**
 * Context object bundling parameters for creating ListingObservationEntity (#401 / JAVA-008).
 */
public record ListingObservationContext(
    String propertyId,
    String url,
    String canonicalId,
    String observedAt,
    Long priceHuf,
    Long pricePerM2,
    ListingObservation.StatusEvent statusEvent,
    Long priceDelta,
    Double changePercent) {

  public ListingObservationContext(
      String propertyId,
      String url,
      String canonicalId,
      String observedAt,
      Long priceHuf,
      Long pricePerM2,
      String statusEvent,
      Long priceDelta,
      Double changePercent) {
    this(
        propertyId,
        url,
        canonicalId,
        observedAt,
        priceHuf,
        pricePerM2,
        ListingObservation.StatusEvent.fromString(statusEvent),
        priceDelta,
        changePercent);
  }
}
