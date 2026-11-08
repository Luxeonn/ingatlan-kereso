package ingatlanok.persistence.service;

import org.springframework.stereotype.Component;

import ingatlanok.model.ListingStatus;

@Component
public class StatusLogTracker {

  public record StatusDecision(
      boolean shouldRecord, ListingStatus status, String changedAt, String note) {}

  public StatusDecision evaluate(
      boolean isNew, ListingStatus prevStatus, ListingStatus targetStatus, String timestamp) {
    ListingStatus effectiveStatus = targetStatus != null ? targetStatus : ListingStatus.ACTIVE;
    if (isNew) {
      return new StatusDecision(true, effectiveStatus, timestamp, "Kezdeti állapot felfedezve");
    }
    if (prevStatus != null && prevStatus != effectiveStatus) {
      return new StatusDecision(
          true,
          effectiveStatus,
          timestamp,
          "Státusz változás: " + prevStatus.name() + " -> " + effectiveStatus.name());
    }
    return new StatusDecision(false, effectiveStatus, timestamp, null);
  }

  public StatusDecision evaluate(
      boolean isNew, String prevStatus, String targetStatus, String timestamp) {
    return evaluate(
        isNew,
        prevStatus != null ? ListingStatus.fromString(prevStatus) : null,
        targetStatus != null ? ListingStatus.fromString(targetStatus) : null,
        timestamp);
  }
}
