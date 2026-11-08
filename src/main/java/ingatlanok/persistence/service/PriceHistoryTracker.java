package ingatlanok.persistence.service;

import org.springframework.stereotype.Component;

import ingatlanok.common.AppLogger;
import ingatlanok.model.ListingStatus;

@Component
public class PriceHistoryTracker {

  private static final AppLogger LOGGER = AppLogger.getLogger(PriceHistoryTracker.class);

  public record ExistingPriceState(
      boolean isNew,
      Long prevAmount,
      Long existingDelta,
      Double existingPct,
      String existingLastChangeAt,
      ListingStatus currentStatus) {

    public ExistingPriceState(
        boolean isNew,
        Long prevAmount,
        Long existingDelta,
        Double existingPct,
        String existingLastChangeAt,
        String currentStatus) {
      this(
          isNew,
          prevAmount,
          existingDelta,
          existingPct,
          existingLastChangeAt,
          currentStatus != null ? ListingStatus.fromString(currentStatus) : null);
    }
  }

  public record PriceDeltaCalculation(
      Long delta,
      Double changePercent,
      String lastPriceChangeAt,
      ListingStatus resolvedStatus,
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
    ListingStatus status = resolveStatus(existing.currentStatus());
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
    ListingStatus status = resolveStatus(existing.currentStatus());
    return new PriceDeltaCalculation(0L, 0.0, null, status, true, false, null);
  }

  private PriceDeltaCalculation priceChangedCalculation(
      ExistingPriceState existing, Long newAmount, String title, String now) {
    long prev = existing.prevAmount();
    long delta = newAmount - prev;
    double changePct = Math.round(((double) delta / prev * 100.0) * 100.0) / 100.0;
    ListingStatus newStatus =
        delta < 0
            ? ListingStatus.PRICE_DROP
            : (delta > 0 ? ListingStatus.PRICE_INCREASE : resolveStatus(existing.currentStatus()));

    LOGGER.info(
        "[Árváltozás] {} - Régi: {} Ft -> Új: {} Ft (Delta: {} Ft, {}%)",
        title != null ? title : "", prev, newAmount, delta, changePct);

    return new PriceDeltaCalculation(delta, changePct, now, newStatus, false, true, prev);
  }

  private ListingStatus resolveStatus(ListingStatus currentStatus) {
    return currentStatus != null ? currentStatus : ListingStatus.ACTIVE;
  }
}
