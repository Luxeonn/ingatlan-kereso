package ingatlanok.model;

public record PriceHistoryEntry(
    int id,
    String propertyId,
    String recordedAt,
    String priceRaw,
    long priceAmount,
    double priceMillions,
    Long pricePerSqm,
    Long previousPriceAmount,
    Long priceDelta,
    Double changePercent) {}
