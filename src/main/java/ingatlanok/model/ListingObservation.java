package ingatlanok.model;

import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ListingObservation implements Serializable {

  public enum StatusEvent {
    NEW,
    UNCHANGED,
    PRICE_CHANGED,
    RELISTED;

    public static StatusEvent fromString(String raw) {
      if (raw == null || raw.isBlank()) {
        return UNCHANGED;
      }
      try {
        return StatusEvent.valueOf(raw.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        return UNCHANGED;
      }
    }
  }

  private String observedAt;
  private Long priceHuf;
  private Long pricePerM2;
  private StatusEvent statusEvent = StatusEvent.UNCHANGED;
  private Long priceDelta;
  private Double changePercent;

  public ListingObservation(
      String observedAt,
      Long priceHuf,
      Long pricePerM2,
      StatusEvent statusEvent,
      Long priceDelta,
      Double changePercent) {
    this.observedAt = observedAt;
    this.priceHuf = priceHuf;
    this.pricePerM2 = pricePerM2;
    this.statusEvent = statusEvent != null ? statusEvent : StatusEvent.UNCHANGED;
    this.priceDelta = priceDelta;
    this.changePercent = changePercent;
  }

  public ListingObservation(
      String observedAt,
      Long priceHuf,
      Long pricePerM2,
      String statusEventStr,
      Long priceDelta,
      Double changePercent) {
    this(
        observedAt,
        priceHuf,
        pricePerM2,
        StatusEvent.fromString(statusEventStr),
        priceDelta,
        changePercent);
  }

  public StatusEvent getStatusEvent() {
    return statusEvent != null ? statusEvent : StatusEvent.UNCHANGED;
  }

  public void setStatusEvent(StatusEvent statusEvent) {
    this.statusEvent = statusEvent != null ? statusEvent : StatusEvent.UNCHANGED;
  }

  public void setStatusEvent(String statusEventStr) {
    this.statusEvent = StatusEvent.fromString(statusEventStr);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListingObservation that = (ListingObservation) o;
    return Objects.equals(observedAt, that.observedAt)
        && Objects.equals(priceHuf, that.priceHuf)
        && statusEvent == that.statusEvent;
  }

  @Override
  public int hashCode() {
    return Objects.hash(observedAt, priceHuf, statusEvent);
  }

  @Override
  public String toString() {
    return "ListingObservation{"
        + "observedAt='" + observedAt + '\''
        + ", priceHuf=" + priceHuf
        + ", statusEvent=" + statusEvent
        + ", priceDelta=" + priceDelta
        + '}';
  }
}
