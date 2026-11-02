package ingatlanok.persistence.mapper;

/**
 * Context object bundling property update parameters for JpaEntityMapper.
 */
public record PropertyUpdateContext(
    String now,
    Long priceDelta,
    Double priceChangePercent,
    String lastPriceChangeAt,
    String targetStatus) {}
