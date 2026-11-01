package ingatlanok.persistence.mapper;

/**
 * Context object bundling parameters for creating PriceHistoryEntity.
 */
public record PriceHistoryContext(
    String propertyId,
    String url,
    String recordedAt,
    String priceRaw,
    Long priceAmount,
    Double priceMillions,
    Long pricePerSqm,
    Long prevAmount,
    Long delta,
    Double changePercent) {}
