package ingatlanok.model;

import java.io.Serializable;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Closed set of sorting options for property listing queries (#401 / JAVA-004).
 */
public enum ListingSortOption implements Serializable {
  DISCOVERED_DESC,
  PRICE_ASC,
  PRICE_DESC,
  AREA_ASC,
  AREA_DESC,
  ROOMS_ASC,
  ROOMS_DESC,
  BARGAIN_DESC;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static ListingSortOption fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return DISCOVERED_DESC;
    }
    String upper = raw.trim().toUpperCase(Locale.ROOT);
    for (ListingSortOption option : values()) {
      if (option.name().equals(upper)) {
        return option;
      }
    }
    throw new IllegalArgumentException("Ismeretlen rendezési opció: " + raw);
  }

  @JsonValue
  public String toValue() {
    return name();
  }
}
