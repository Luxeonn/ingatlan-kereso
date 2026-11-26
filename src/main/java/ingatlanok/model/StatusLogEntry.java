package ingatlanok.model;

/**
 * Audit log entry recording listing status transitions (#401 / JAVA-001).
 */
public record StatusLogEntry(
    int id, String propertyId, ListingStatus status, String changedAt, String note) {

  public StatusLogEntry(
      int id, String propertyId, String status, String changedAt, String note) {
    this(id, propertyId, ListingStatus.fromString(status), changedAt, note);
  }
}
