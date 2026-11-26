package ingatlanok.presentation;

import lombok.Builder;

import ingatlanok.model.PropertyType;

/**
 * Detailed statistical segment summary for a specific city and property type bucket.
 */
@Builder(toBuilder = true)
public record MarketSegmentStatsDto(
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
