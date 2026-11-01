package ingatlanok.persistence.service;

import org.springframework.stereotype.Component;

import ingatlanok.common.AppLogger;

@Component
public class PriceHistoryTracker {

  private static final AppLogger LOGGER = AppLogger.getLogger(PriceHistoryTracker.class);

  public record ExistingPriceState(
      boolean isNew,
      Long prevAmount,
      Long existingDelta,
      Double existingPct,
      String existingLastChangeAt,
      String currentStatus) {}

  public record PriceDeltaCalculation(
      Long delta,
      Double changePercent,
      String lastPriceChangeAt,
      String resolvedStatus,
      boolean isNewPriceRecord,
      boolean priceChanged,
      Long previousPriceAmount) {}

  public PriceDeltaCalculation calculate(
      ExistingPriceState existing,
      Long newAmount,
      String title,
      String now) {
    if (newAmount == null || newAmount <= 0) {
      return fallbackCalculation(existing);
    }
    if (existing.isNew()) {
      return newPriceCalculation(existing);
    }
    if (existing.prevAmount() != null
        && existing.prevAmount() > 0
        && !existing.prevAmount().equals(newAmount)) {
      return priceChangedCalculation(existing, newAmount, title, now);
    }
    return fallbackCalculation(existing);
  }

  private PriceDeltaCalculation fallbackCalculation(ExistingPriceState existing) {
    String status = resolveStatus(existing.currentStatus());
    return new PriceDeltaCalculation(
        existing.existingDelta(),
        existing.existingPct(),
        existing.existingLastChangeAt(),
        status,
        false,
        false,
        existing.prevAmount());
  }

  private PriceDeltaCalculation newPriceCalculation(ExistingPriceState existing) {
    String status = resolveStatus(existing.currentStatus());
    return new PriceDeltaCalculation(0L, 0.0, null, status, true, false, null);
  }

  private PriceDeltaCalculation priceChangedCalculation(
      ExistingPriceState existing, Long newAmount, String title, String now) {
    long prev = existing.prevAmount();
    long delta = newAmount - prev;
    double changePct = Math.round(((double) delta / prev * 100.0) * 100.0) / 100.0;
    String newStatus = delta < 0 ? "PRICE_DROP" : (delta > 0 ? "PRICE_INCREASE" : existing.currentStatus());

    LOGGER.info(
        "[Árváltozás] {} - Régi: {} Ft -> Új: {} Ft (Delta: {} Ft, {}%)",
        title != null ? title : "", prev, newAmount, delta, changePct);

    return new PriceDeltaCalculation(delta, changePct, now, newStatus, false, true, prev);
  }

  private String resolveStatus(String currentStatus) {
    return (currentStatus != null && !currentStatus.isBlank()) ? currentStatus : "ACTIVE";
  }
}
