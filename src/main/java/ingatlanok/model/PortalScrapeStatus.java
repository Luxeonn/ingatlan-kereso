package ingatlanok.model;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PortalScrapeStatus {
  SUCCESS,
  EMPTY,
  BOT_PROTECTED,
  SPA_FALLBACK,
  FAILED;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static PortalScrapeStatus fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return FAILED;
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT);
    for (PortalScrapeStatus status : values()) {
      if (status.name().equals(normalized)) {
        return status;
      }
    }
    return FAILED;
  }

  @JsonValue
  public String jsonValue() {
    return name();
  }
}
