package ingatlanok.portal.sidecar;

import java.io.Serializable;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Execution status reported by external sidecar scrapers (#401 / JAVA-009).
 */
public enum SidecarStatus implements Serializable {
  SUCCESS,
  FAILED,
  BOT_PROTECTED,
  TIMEOUT;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static SidecarStatus fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return FAILED;
    }
    String upper = raw.trim().toUpperCase(Locale.ROOT);
    for (SidecarStatus s : values()) {
      if (s.name().equals(upper)) {
        return s;
      }
    }
    return FAILED;
  }

  @JsonValue
  public String toValue() {
    return name();
  }
}
