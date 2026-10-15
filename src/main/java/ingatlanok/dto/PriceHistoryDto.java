package ingatlanok.dto;

import ingatlanok.model.PriceHistoryEntry;

import java.io.Serializable;

/**
 * Típusbiztos ártörténet DTO (Record).
 */
public record PriceHistoryDto(
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

    public PriceHistoryEntry toModel() {
        return new PriceHistoryEntry(
                id,
                propertyId,
                recordedAt,
                priceRaw,
                priceAmount,
                priceMillions,
                pricePerSqm,
                previousPriceAmount,
                priceDelta,
                changePercent
        );
    }
}
