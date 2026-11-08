package ingatlanok.model;

import java.io.Serializable;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle status of a property listing across portal aggregations (#401 / JAVA-001).
 */
public enum ListingStatus implements Serializable {
  ACTIVE,
  PRICE_DROP,
  PRICE_INCREASE,
  INACTIVE,
  REMOVED,
  ARCHIVED;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static ListingStatus fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return ACTIVE;
    }
    String trimmed = raw.trim().toUpperCase(Locale.ROOT);
    for (ListingStatus status : values()) {
      if (status.name().equals(trimmed)) {
        return status;
      }
    }
    return ACTIVE;
  }

  @JsonValue
  public String toCanonicalString() {
    return name();
  }
}
