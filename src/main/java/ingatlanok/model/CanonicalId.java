package ingatlanok.model;

import java.io.Serializable;

/**
 * Value object representing a cross-portal deduplicated canonical property identifier (#401 / JAVA-015).
 */
public record CanonicalId(String value) implements Serializable {

  public CanonicalId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("CanonicalId cannot be null or blank");
    }
  }

  public static CanonicalId of(String value) {
    return new CanonicalId(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
