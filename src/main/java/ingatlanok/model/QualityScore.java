package ingatlanok.model;

import java.io.Serializable;

/**
 * Value object representing a validated data quality score in the [0, 100] range (#401 / JAVA-014).
 */
public record QualityScore(int value) implements Serializable, Comparable<QualityScore> {

  public QualityScore {
    if (value < 0 || value > 100) {
      throw new IllegalArgumentException("Quality score must be between 0 and 100: " + value);
    }
  }

  public static QualityScore of(int value) {
    return new QualityScore(value);
  }

  @Override
  public int compareTo(QualityScore o) {
    return Integer.compare(this.value, o.value);
  }
}
