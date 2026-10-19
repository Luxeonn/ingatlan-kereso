package ingatlanok.persistence.service;

import org.springframework.stereotype.Component;

/** Szolgáltatás a hirdetések státuszváltozásainak és naplóbejegyzéseinek értékeléséhez. */
@Component
public class StatusLogTracker {

  public record StatusDecision(
      boolean shouldRecord, String status, String changedAt, String note) {}

  public StatusDecision evaluate(
      boolean isNew, String prevStatus, String targetStatus, String timestamp) {
    String effectiveStatus =
        (targetStatus != null && !targetStatus.isBlank()) ? targetStatus : "ACTIVE";
    if (isNew) {
      return new StatusDecision(true, effectiveStatus, timestamp, "Kezdeti állapot felfedezve");
    }
    if (prevStatus != null && !prevStatus.equalsIgnoreCase(effectiveStatus)) {
      return new StatusDecision(
          true,
          effectiveStatus,
          timestamp,
          "Státusz változás: " + prevStatus + " -> " + effectiveStatus);
    }
    return new StatusDecision(false, effectiveStatus, timestamp, null);
  }
}
