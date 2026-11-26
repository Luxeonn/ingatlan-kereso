package ingatlanok.model;

/**
 * Value object representing a portal-native property identifier (#401 / JAVA-015).
 */
public record PropertyId(String value) {

  public PropertyId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("PropertyId cannot be null or blank");
    }
  }

  public static PropertyId of(String value) {
    return new PropertyId(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
