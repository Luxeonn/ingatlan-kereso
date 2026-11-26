package ingatlanok.model;

import lombok.Builder;

/**
 * Immutable value object representing parsed listing price information.
 */
@Builder
public record PriceInfo(
    String rawText,
    Long amountHuf,
    Double amountMillion,
    String currency) {

  public PriceInfo {
    if (currency == null || currency.isBlank()) {
      currency = "HUF";
    }
  }

  public PriceInfo() {
    this(null, null, null, "HUF");
  }

  public PriceInfo(String rawText, Long amountHuf, Double amountMillion) {
    this(rawText, amountHuf, amountMillion, "HUF");
  }

  @Override
  public String toString() {
    return rawText != null ? rawText : (amountMillion != null ? amountMillion + " M Ft" : "N/A");
  }
}
