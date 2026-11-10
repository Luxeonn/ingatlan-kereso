package ingatlanok.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key representing a market segment partition by city and property type (#401 / JAVA-016).
 */
public record MarketSegmentKey(String normalizedCity, PropertyType propertyType)
    implements Serializable {

  public MarketSegmentKey {
    Objects.requireNonNull(normalizedCity, "normalizedCity must not be null");
    Objects.requireNonNull(propertyType, "propertyType must not be null");
  }
}
