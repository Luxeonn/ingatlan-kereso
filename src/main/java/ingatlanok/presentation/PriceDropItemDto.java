package ingatlanok.presentation;

import lombok.Builder;

import ingatlanok.model.PropertyType;

/**
 * Summary DTO representing a listing with a registered price reduction.
 */
@Builder
public record PriceDropItemDto(
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
    String lastPriceChangeAt) {}
