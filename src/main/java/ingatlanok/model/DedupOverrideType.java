package ingatlanok.model;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Manual deduplication override direction between two listing URLs. */
public enum DedupOverrideType {
  FORCE_SPLIT,
  FORCE_MERGE;

  @JsonValue
  public String toCanonicalString() {
    return name();
  }

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static DedupOverrideType fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("A felülbírálás típusa nem lehet üres");
    }
    String upper = raw.trim().toUpperCase(Locale.ROOT);
    for (DedupOverrideType value : values()) {
      if (value.name().equals(upper)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Ismeretlen felülbírálás típus: " + raw);
  }
}
