package ingatlanok.model;

import java.io.Serializable;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Currency code for real estate prices (#401 / JAVA-005).
 */
public enum CurrencyCode implements Serializable {
  HUF,
  EUR;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static CurrencyCode fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return HUF;
    }
    String upper = raw.trim().toUpperCase(Locale.ROOT);
    for (CurrencyCode code : values()) {
      if (code.name().equals(upper)) {
        return code;
      }
    }
    return HUF;
  }

  @JsonValue
  public String toValue() {
    return name();
  }
}
