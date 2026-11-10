package ingatlanok.model;

import java.io.Serializable;

/**
 * Value object representing a monetary value in Hungarian Forint (#401 / JAVA-012).
 */
public record Money(long amountHuf) implements Comparable<Money>, Serializable {

  public Money {
    if (amountHuf < 0) {
      throw new IllegalArgumentException("Monetary amount cannot be negative: " + amountHuf);
    }
  }

  public static Money ofHuf(long huf) {
    return new Money(huf);
  }

  public static Money ofMillions(double millions) {
    return new Money(Math.round(millions * 1_000_000L));
  }

  public double toMillions() {
    return amountHuf / 1_000_000.0;
  }

  @Override
  public int compareTo(Money o) {
    return Long.compare(this.amountHuf, o.amountHuf);
  }
}
