package ingatlanok.model;

import java.io.Serializable;

/**
 * Idősoros árváltozási rekord (Price History Entry).
 */
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
        Double changePercent
) implements Serializable {
}
