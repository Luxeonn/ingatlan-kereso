package ingatlanok.model;

import lombok.Builder;

/**
 * Immutable domain value object representing statistical metrics for a single city and property
 * type segment.
 */
@Builder(toBuilder = true)
public record MarketSegmentStats(
    String city,
    PropertyType propertyType,
    String propertyTypeName,
    boolean residential,
    boolean plot,
    int sampleSize,
    boolean insufficientData,
    Long medianPricePerM2,
    Long averagePricePerM2,
    Double medianPriceMillion,
    Double averagePriceMillion,
    Double minPriceMillion,
    Double maxPriceMillion,
    int bargainCount) {}
