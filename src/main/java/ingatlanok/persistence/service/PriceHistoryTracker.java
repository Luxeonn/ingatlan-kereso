package ingatlanok.persistence.service;

import ingatlanok.common.AppLogger;
import org.springframework.stereotype.Component;

/**
 * Szolgáltatás az árváltozások kiszámításához, százalékos kerekítéshez és státusz-meghatározáshoz.
 */
@Component
public class PriceHistoryTracker {

    private static final AppLogger LOGGER = AppLogger.getLogger(PriceHistoryTracker.class);

    public record PriceDeltaCalculation(
            Long delta,
            Double changePercent,
            String lastPriceChangeAt,
            String resolvedStatus,
            boolean isNewPriceRecord,
            boolean priceChanged,
            Long previousPriceAmount
    ) {}

    public PriceDeltaCalculation calculate(
            boolean isNew,
            Long prevAmount,
            Long newAmount,
            Long existingDelta,
            Double existingPct,
            String existingLastChangeAt,
            String currentStatus,
            String title,
            String now
    ) {
        if (newAmount == null || newAmount <= 0) {
            String status = (currentStatus != null && !currentStatus.isBlank()) ? currentStatus : "ACTIVE";
            return new PriceDeltaCalculation(existingDelta, existingPct, existingLastChangeAt, status,
                    false, false, prevAmount);
        }

        if (isNew) {
            String status = (currentStatus != null && !currentStatus.isBlank()) ? currentStatus : "ACTIVE";
            return new PriceDeltaCalculation(0L, 0.0, null, status,
                    true, false, null);
        }

        if (prevAmount != null && prevAmount > 0 && !prevAmount.equals(newAmount)) {
            long delta = newAmount - prevAmount;
            double changePct = Math.round(((double) delta / prevAmount * 100.0) * 100.0) / 100.0;
            String newStatus = delta < 0 ? "PRICE_DROP" : (delta > 0 ? "PRICE_INCREASE" : currentStatus);

            LOGGER.info("[Árváltozás] {} - Régi: {} Ft -> Új: {} Ft (Delta: {} Ft, {}%)",
                    title != null ? title : "", prevAmount, newAmount, delta, changePct);

            return new PriceDeltaCalculation(delta, changePct, now, newStatus,
                    false, true, prevAmount);
        }

        String status = (currentStatus != null && !currentStatus.isBlank()) ? currentStatus : "ACTIVE";
        return new PriceDeltaCalculation(existingDelta, existingPct, existingLastChangeAt, status,
                false, false, prevAmount);
    }
}
