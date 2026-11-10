package ingatlanok.model;

import java.io.Serializable;

/**
 * Value object representing floor or land area in square meters (#401 / JAVA-013).
 */
public record Area(double squareMeters) implements Serializable, Comparable<Area> {

  public Area {
    if (squareMeters < 0) {
      throw new IllegalArgumentException("Area cannot be negative: " + squareMeters);
    }
  }

  public static Area of(double squareMeters) {
    return new Area(squareMeters);
  }

  @Override
  public int compareTo(Area o) {
    return Double.compare(this.squareMeters, o.squareMeters);
  }
}
