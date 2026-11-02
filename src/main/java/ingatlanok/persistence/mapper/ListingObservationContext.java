package ingatlanok.persistence.mapper;

/**
 * Context object bundling parameters for creating ListingObservationEntity.
 */
public record ListingObservationContext(
    String propertyId,
    String url,
    String canonicalId,
    String observedAt,
    Long priceHuf,
    Long pricePerM2,
    String statusEvent,
    Long priceDelta,
    Double changePercent) {}
