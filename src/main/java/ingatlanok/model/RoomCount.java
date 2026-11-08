package ingatlanok.model;

import java.io.Serializable;

/**
 * Value object representing a property room count (#401 / JAVA-013).
 */
public record RoomCount(double count) implements Serializable, Comparable<RoomCount> {

  public RoomCount {
    if (count < 0) {
      throw new IllegalArgumentException("Room count cannot be negative: " + count);
    }
  }

  public static RoomCount of(double count) {
    return new RoomCount(count);
  }

  @Override
  public int compareTo(RoomCount o) {
    return Double.compare(this.count, o.count);
  }
}
